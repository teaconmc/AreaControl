package org.teacon.areacontrol;

import net.minecraft.ChatFormatting;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.compat.curios.CuriosCapability;
import org.teacon.areacontrol.impl.AreaChecks;
import org.teacon.areacontrol.impl.AreaMath;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACSendCurrentSelection;
import org.teacon.areacontrol.network.ACSendNearbyArea;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = "area_control")
public enum AreaControlPlayerTracker {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("PlayerTracker");

    private static final Component HOW_TO_TURN_ON = Component.translatable("area_control.bypass.how_to_turn_on",
            Component.literal("/ac current bypass global")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("/ac current bypass global")))
                            .withClickEvent(new ClickEvent.RunCommand("/ac current bypass global"))),
            Component.literal("/ac current bypass local")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("/ac current bypass local")))
                            .withClickEvent(new ClickEvent.RunCommand("/ac current bypass local"))));
    private static final Component HOW_TO_TURN_OFF = Component.translatable("area_control.bypass.how_to_turn_off",
            Component.literal("/ac current bypass none")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("/ac current bypass none")))
                            .withClickEvent(new ClickEvent.RunCommand("/ac current bypass none"))));

    // For future reference - if you need to backport, or port forward, or port to a different framework,
    // make sure you always get AreaControlStatusData from here, to minimize the workload.
    //
    // 给未来的维护者：如果你想移植到新版游戏、旧版游戏、其他的框架上，请确保所有需要 AreaControlStatusData 的地方都通过
    // 这个方法调用获得，这样可以减少工作量。
    public static AreaControlStatusData getFrom(Entity actor) {
        return actor.getData(AreaControlPreSetup.PLAYER_STATUS);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        var status = getFrom(player);
        status.currentArea = AreaManager.INSTANCE.findBy(player.level(), player.blockPosition());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getEntity().setData(AreaControlPreSetup.PLAYER_STATUS, getFrom(event.getOriginal()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        if (player instanceof ServerPlayer) {
            var status = getFrom(player);
            var prevArea = status.currentArea;

            var currentArea = AreaManager.INSTANCE.findBy(player.level(), player.blockPosition());
            if (prevArea != currentArea) {
                status.currentArea = currentArea;
                if (currentArea != null && AreaProperties.getBoolOptional(currentArea, AreaProperties.SHOW_WELCOME).orElse(Boolean.FALSE)) {
                    player.sendOverlayMessage(Component.translatable("area_control.claim.welcome", currentArea.name));
                }
            }

            // Seize items if disallowed
            var mainInv = player.getInventory();
            AreaChecks.checkInv(mainInv, currentArea, player);
            var extraInv = player.getCapability(CuriosCapability.CURIO_INV);
            if (extraInv != null) {
                AreaChecks.checkInv(extraInv, currentArea, player);
            }
            // Seize vehicles if disallowed
            var riding = player.getVehicle();
            if (riding != null && !AreaChecks.checkPropFor(currentArea, player, AreaProperties.ALLOW_RIDE, BuiltInRegistries.ENTITY_TYPE.getKey(riding.getType()), AreaControlConfig.allowRideEntity)) {
                player.sendOverlayMessage(Component.translatable("area_control.notice.ride_disabled", riding.getDisplayName()));
                player.stopRiding();
            }
            // Clear disallowed effects
            for (Holder<@NonNull MobEffect> effectId : List.copyOf(player.getActiveEffectsMap().keySet())) {
                var regKey = effectId.getKey();
                if (regKey == null) {
                    // This is impossible - no effect should lack registry id at runtime, unless it is unregistered.
                    continue;
                }
                if (!AreaChecks.checkPropFor(currentArea, player, AreaProperties.ALLOW_ACTIVE_EFFECT, regKey.identifier(), AreaControlConfig.allowActiveEffect)) {
                    player.removeEffect(effectId);
                    player.sendOverlayMessage(Component.translatable("area_control.notice.clear_effect", effectId.value().getDisplayName()));
                }
            }

            // 检查玩家的 Bypass 状态并更新。
            INSTANCE.updatePlayerExemptionStatus(player, status, prevArea);
        }
    }

    private void updatePlayerExemptionStatus(Player p, AreaControlStatusData status, Area prevArea) {
        // 玩家 Reach Distance（默认 6 格，需要动态获取）的两倍范围，并且平方
        // Reach distance 选取 Block Reach 和 Entity Reach 中的较大值
        var doubleReachDistance = Math.max(p.blockInteractionRange(), p.entityInteractionRange()) * 2;
        var doubleReachDistanceSq = doubleReachDistance * doubleReachDistance;
        Set<UUID> exemptedAreas = status.areaIdsWithBypassModeOn;
        // 检查所有已针对当前玩家开启 bypass 模式的领地
        for (Iterator<UUID> iterator = exemptedAreas.iterator(); iterator.hasNext(); ) {
            var areaId = iterator.next();
            var areaObj = AreaManager.INSTANCE.findBy(areaId);
            // 剔除 ID 不存在的领地（可由领地删除产生）
            if (areaObj == null) {
                iterator.remove();
                continue;
            }
            // 若玩家已离开领地服务端 Reach Distance 的两倍范围之外：
            var distanceSq = AreaMath.distanceSqBetween(areaObj, p.xo, p.yo, p.zo);
            if (distanceSq > doubleReachDistanceSq) {
                // 自动为该领地/野外清除 Bypass 模式，
                iterator.remove();
                // 并发送消息。
                p.sendSystemMessage(Component.translatable("area_control.bypass.area.passive_off", areaObj.name));
                p.sendSystemMessage(HOW_TO_TURN_ON);
            }
        }
        var currArea = status.currentArea;
        // 如果玩家是全局 Bypass：
        if (status.globalBypassMode) {
            if (currArea != null) {
                // 如果不在，检查是否已远离野外两倍 reach distance
                if (prevArea == null && AreaMath.distanceFromInteriorToBoundary(currArea, p.xo, p.yo, p.zo) >= doubleReachDistance) {
                    // 若已远离，则关闭野外的 Bypass
                    status.wildnessBypassMode = false;
                    p.sendSystemMessage(Component.translatable("area_control.bypass.wildness.passive_off"));
                    p.sendSystemMessage(HOW_TO_TURN_ON);
                }
                // 玩家如果是切换后领地/野外的 Builder，或者拥有 area_control.command.admin 权限（注意野外）
                if (AreaChecks.isACtrlAreaBuilder((ServerPlayer) p, currArea)) {
                    // 则自动为该领地/野外开启 Bypass 模式（若还没有），并发送消息
                    if (exemptedAreas.add(currArea.uid)) {
                        p.sendSystemMessage(Component.translatable("area_control.bypass.area.passive_on", currArea.name));
                        p.sendSystemMessage(HOW_TO_TURN_OFF);
                    }
                }
            } else {
                // // 玩家如果是切换后领地/野外的 Builder，或者拥有 area_control.command.admin 权限（注意野外）
                if (prevArea != null && AreaChecks.isACtrlAdmin((ServerPlayer) p)) {
                    // 则自动为该领地/野外开启 Bypass 模式（若还没有），并发送消息
                    if (!status.wildnessBypassMode) {
                        status.wildnessBypassMode = true;
                        p.sendSystemMessage(Component.translatable("area_control.bypass.wildness.passive_on"));
                        p.sendSystemMessage(HOW_TO_TURN_OFF);
                    }
                }
            }
        }
    }

    public void sendNearbyAreasToClient(ResourceKey<Level> dim, ServerPlayer requester, double radius, boolean permanent) {
        LOGGER.debug(MARKER, "Player {} has requested nearby area. Center: {}, radius: {}", requester.getGameProfile().name(), requester.blockPosition(), radius);
        var nearbyAreas = AreaManager.INSTANCE.getAreaSummariesSurround(dim, requester.blockPosition(), radius);
        requester.sendSystemMessage(Component.translatable("area_control.claim.nearby", nearbyAreas.size()), false);
        LOGGER.debug(MARKER, "Nearby area count: {}", nearbyAreas.size());
        var summaries = new ArrayList<Area.Summary>();
        for (var nearbyArea : nearbyAreas) {
            LOGGER.debug(MARKER, "Nearby area: {}", nearbyArea.uid);
            var summary = new Area.Summary(nearbyArea);
            summaries.add(summary);
            requester.sendSystemMessage(Util.describe(nearbyArea, requester.level()), false);
        }
        if (thisPlayerHasClientExt(requester)) {
            var expire = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 60000;
            ACNetworking.send(requester, new ACSendNearbyArea(summaries, expire));
        } else {
            requester.sendSystemMessage(Component.translatable("area_control.claim.nearby.visual"), false);
        }
        LOGGER.debug(MARKER, "End of the request");
    }

    public void sendCurrentAreaToClient(ServerPlayer requester, Area current, boolean permanent) {
        var summaries = List.of(new Area.Summary(current));
        if (thisPlayerHasClientExt(requester)) {
            var expire = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 60000;
            ACNetworking.send(requester, new ACSendNearbyArea(summaries, expire));
        } else {
            requester.sendSystemMessage(Component.translatable("area_control.claim.nearby.visual"), false);
        }
    }

    public void sendCurrentSelectionToClient(ServerPlayer receiver, AreaControlClaimHandler.RectangleRegion region) {
        if (thisPlayerHasClientExt(receiver)) {
            ACNetworking.send(receiver, ACSendCurrentSelection.of(false, region.start(), region.end()));
        }
    }

    public void clearSelectionForClient(ServerPlayer receiver) {
        if (thisPlayerHasClientExt(receiver)) {
            ACNetworking.send(receiver, ACSendCurrentSelection.of(true, null, null));
        }
    }

    public static @Nullable Area getCurrentAreaForPlayer(ServerPlayer player) {
        return getFrom(player).currentArea;
    }

    public static boolean thisPlayerHasClientExt(ServerPlayer player) {
        return getFrom(player).clientExtensionEnabled;
    }

    public static boolean hasBypassModeOnForArea(@NotNull Entity actor, @Nullable Area area) {
        var status = getFrom(actor);
        if (area == null) {
            return status.wildnessBypassMode;
        } else {
            return status.areaIdsWithBypassModeOn.contains(area.uid);
        }
    }

    public void setGlobalExempt(ServerPlayer p, boolean global) {
        var area = AreaManager.INSTANCE.findBy(p.level(), p.position());
        var status = getFrom(p);
        var exemptedArea = status.areaIdsWithBypassModeOn;
        if (global) {
            status.globalBypassMode = true;
            if (!AreaChecks.isACtrlAreaBuilder(p, area, false)) {
                p.sendSystemMessage(Component.translatable("area_control.error.insufficient_permission_for_bypass"), false);
                return;
            }
            if (area == null) {
                status.wildnessBypassMode = true;
                p.sendSystemMessage(Component.translatable("area_control.bypass.global.wildness.on"), false);
                p.sendSystemMessage(HOW_TO_TURN_OFF, false);
            } else {
                exemptedArea.add(area.uid);
                p.sendSystemMessage(Component.translatable("area_control.bypass.global.area.on", area.name), false);
                p.sendSystemMessage(HOW_TO_TURN_OFF, false);
            }
        } else {
            if (!AreaChecks.isACtrlAreaBuilder(p, area, false)) {
                p.sendSystemMessage(Component.translatable("area_control.error.insufficient_permission_for_bypass"), false);
                return;
            }
            if (area == null) {
                status.wildnessBypassMode = true;
                p.sendSystemMessage(Component.translatable("area_control.bypass.local.wildness.on"), false);
                p.sendSystemMessage(HOW_TO_TURN_OFF, false);
            } else {
                exemptedArea.add(area.uid);
                p.sendSystemMessage(Component.translatable("area_control.bypass.local.area.on", area.name), false);
                p.sendSystemMessage(HOW_TO_TURN_OFF, false);
            }
        }

    }

    public void clearExemptFor(ServerPlayer p) {
        var status = getFrom(p);
        var previouslyExempted = status.areaIdsWithBypassModeOn;
        if (status.globalBypassMode) {
            // This can happen if player disconnected before its first tick.
            if (previouslyExempted != null) {
                for (var areaId : previouslyExempted) {
                    var area = AreaManager.INSTANCE.findBy(areaId);
                    p.sendSystemMessage(Component.translatable("area_control.bypass.global.area.off", area.name), false);
                    p.sendSystemMessage(HOW_TO_TURN_ON, false);
                }
                status.areaIdsWithBypassModeOn.clear();
            }
            status.globalBypassMode = false;
            status.wildnessBypassMode = false;
            p.sendSystemMessage(Component.translatable("area_control.bypass.global.wildness.off"), false);
            p.sendSystemMessage(HOW_TO_TURN_ON, false);
        } else {
            // This can happen if player disconnected before its first tick.
            if (previouslyExempted != null) {
                for (var areaId : previouslyExempted) {
                    var areaName = AreaManager.INSTANCE.findBy(areaId).name;
                    p.sendSystemMessage(Component.translatable("area_control.bypass.local.area.off", areaName), false);
                    p.sendSystemMessage(HOW_TO_TURN_ON, false);
                }
                status.areaIdsWithBypassModeOn.clear();
            }
            status.globalBypassMode = false;
            status.wildnessBypassMode = false;
            p.sendSystemMessage(Component.translatable("area_control.bypass.local.wildness.off"), false);
            p.sendSystemMessage(HOW_TO_TURN_ON, false);
        }

    }
}
