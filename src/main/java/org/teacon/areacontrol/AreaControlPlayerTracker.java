package org.teacon.areacontrol;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaChecks;
import org.teacon.areacontrol.impl.AreaMath;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACSendCurrentSelection;
import org.teacon.areacontrol.network.ACSendNearbyArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "area_control")
public enum AreaControlPlayerTracker {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("PlayerTracker");

    private static final Component HOW_TO_TURN_ON = Component.translatable("area_control.bypass.how_to_turn_on",
            Component.literal("/ac current bypass global")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/ac current bypass global")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ac current bypass global"))),
            Component.literal("/ac current bypass local")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/ac current bypass local")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ac current bypass local"))));
    private static final Component HOW_TO_TURN_OFF = Component.translatable("area_control.bypass.how_to_turn_off",
            Component.literal("/ac current bypass off")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/ac current bypass off")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ac current bypass off"))));

    private final Map<UUID, UUID> playerLocation = new ConcurrentHashMap<>();
    private final Set<UUID> playersWithExt = ConcurrentHashMap.newKeySet();
    /**
     * Set of players with global exemption enabled.
     */
    private final Set<UUID> playersWithGlobalExempt = ConcurrentHashMap.newKeySet();
    /**
     * Player UUID to Area UUID map that denotes exemption status.
     */
    private final Map<UUID, Set<UUID>> playerExemptionStatus = new ConcurrentHashMap<>();
    /**
     * Set of players with exemption of wildness area (places without any claimed area).
     */
    private final Set<UUID> playersWithWildnessExemption = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getEntity();
        var currentArea = AreaManager.INSTANCE.findBy(player.level(), player.blockPosition());
        if (currentArea == null) {
            INSTANCE.playerLocation.remove(player.getGameProfile().getId());
        } else {
            INSTANCE.playerLocation.put(player.getGameProfile().getId(), currentArea.uid);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            var player = event.player;
            var playerId = player.getGameProfile().getId();
            var prevAreaId = INSTANCE.playerLocation.get(playerId);

            var prevArea = AreaManager.INSTANCE.findBy(prevAreaId);
            var currentArea = AreaManager.INSTANCE.findBy(player.level(), player.blockPosition());
            if (prevArea != currentArea) {
                if (currentArea == null) {
                    INSTANCE.playerLocation.remove(playerId);
                } else {
                    INSTANCE.playerLocation.put(playerId, currentArea.uid);
                    if (AreaProperties.getBool(currentArea, "area.display_welcome_message")) {
                        player.displayClientMessage(Component.translatable("area_control.claim.welcome", currentArea.name), true);
                    }
                }
            }

            // Seize items if disallowed
            var mainInv = player.getInventory();
            AreaChecks.checkInv(mainInv.items, currentArea, player);
            AreaChecks.checkInv(mainInv.armor, currentArea, player);
            AreaChecks.checkInv(mainInv.offhand, currentArea, player);
            // Seize vehicles if disallowed
            var riding = player.getVehicle();
            if (riding != null && !AreaChecks.checkPropFor(currentArea, player, AreaProperties.ALLOW_RIDE, ForgeRegistries.ENTITY_TYPES.getKey(riding.getType()), AreaControlConfig.allowRideEntity)) {
                player.displayClientMessage(Component.translatable("area_control.notice.ride_disabled", riding.getDisplayName()), true);
                player.stopRiding();
            }

            // 检查玩家的 Bypass 状态并更新。
            INSTANCE.updatePlayerExemptionStatus(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var player = event.getEntity();
        var playerId = player.getGameProfile().getId();
        INSTANCE.undoMarkPlayer(playerId);
        INSTANCE.playerLocation.remove(playerId);
        if (player instanceof ServerPlayer sp) {
            INSTANCE.clearExemptFor(sp);
        }
    }

    private void updatePlayerExemptionStatus(Player p) {
        var playerId = p.getGameProfile().getId();
        // 玩家 Reach Distance（默认 6 格，需要动态获取）的两倍范围，并且平方
        // Reach distance 选取 Block Reach 和 Entity Reach 中的较大值
        var doubleReachDistance = Math.max(p.getBlockReach(), p.getEntityReach()) * 2;
        var doubleReachDistanceSq = doubleReachDistance * doubleReachDistance;
        Set<UUID> exemptedAreas = INSTANCE.playerExemptionStatus.get(playerId);
        if (exemptedAreas == null) {
            exemptedAreas = ConcurrentHashMap.newKeySet();
            INSTANCE.playerExemptionStatus.put(playerId, exemptedAreas);
        }
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
                p.displayClientMessage(Component.translatable("area_control.bypass.area.passive_off", areaObj.name), false);
                p.displayClientMessage(HOW_TO_TURN_ON, false);
            }
        }
        var currArea = AreaManager.INSTANCE.findBy(this.playerLocation.get(playerId));
        // 如果玩家是全局 Bypass：
        if (this.playersWithGlobalExempt.contains(playerId)) {
            if (currArea != null) {
                // 如果不在，检查是否已远离野外两倍 reach distance
                if (AreaMath.distanceFromInteriorToBoundary(currArea, p.xo, p.yo, p.zo) >= doubleReachDistance) {
                    // 若已远离，则关闭野外的 Bypass
                    if (this.playersWithWildnessExemption.remove(playerId)) {
                        p.displayClientMessage(Component.translatable("area_control.bypass.wildness.passive_off"), false);
                        p.displayClientMessage(HOW_TO_TURN_ON, false);
                    }
                }
                // 玩家如果是切换后领地/野外的 Builder，或者拥有 area_control.command.admin 权限（注意野外）
                if (AreaChecks.isACtrlAreaBuilder((ServerPlayer) p, currArea)) {
                    // 则自动为该领地/野外开启 Bypass 模式（若还没有），并发送消息
                    if (exemptedAreas.add(currArea.uid)) {
                        p.displayClientMessage(Component.translatable("area_control.bypass.area.passive_on", currArea.name), false);
                        p.displayClientMessage(HOW_TO_TURN_OFF, false);
                    }
                }
            } else {
                // // 玩家如果是切换后领地/野外的 Builder，或者拥有 area_control.command.admin 权限（注意野外）
                if (AreaChecks.isACtrlAdmin((ServerPlayer) p)) {
                    // 则自动为该领地/野外开启 Bypass 模式（若还没有），并发送消息
                    if (this.playersWithWildnessExemption.add(playerId)) {
                        p.displayClientMessage(Component.translatable("area_control.bypass.wildness.passive_on"), false);
                        p.displayClientMessage(HOW_TO_TURN_OFF, false);
                    }
                }
            }
        }
    }

    public void markPlayerAsSupportExt(ServerPlayer player) {
        if (player != null) {
            this.playersWithExt.add(player.getGameProfile().getId());
        }
    }

    public void undoMarkPlayer(UUID playerUid) {
        this.playersWithExt.remove(playerUid);
    }

    public void sendNearbyAreasToClient(ResourceKey<Level> dim, ServerPlayer requester, double radius, boolean permanent) {
        LOGGER.debug(MARKER, "Player {} has requested nearby area. Center: {}, radius: {}", requester.getGameProfile().getName(), requester.blockPosition(), radius);
        var nearbyAreas = AreaManager.INSTANCE.getAreaSummariesSurround(dim, requester.blockPosition(), radius);
        requester.displayClientMessage(Component.translatable("area_control.claim.nearby", nearbyAreas.size()), false);
        LOGGER.debug(MARKER, "Nearby area count: {}", nearbyAreas.size());
        var summaries = new ArrayList<Area.Summary>();
        for (var nearbyArea : nearbyAreas) {
            LOGGER.debug(MARKER, "Nearby area: {}", nearbyArea.uid);
            var summary = new Area.Summary(nearbyArea);
            summaries.add(summary);
            requester.displayClientMessage(Util.describe(nearbyArea, requester.level()), false);
        }
        if (this.playersWithExt.contains(requester.getGameProfile().getId())) {
            var expire = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 60000;
            ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> requester), new ACSendNearbyArea(summaries, expire));
        } else {
            requester.displayClientMessage(Component.translatable("area_control.claim.nearby.visual"), false);
        }
        LOGGER.debug(MARKER, "End of the request");
    }

    public void clearNearbyAreasForClient(ServerPlayer requester) {
        ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> requester), new ACSendNearbyArea(Collections.emptyList(), 0L));
    }

    public void sendCurrentSelectionToClient(ServerPlayer receiver, AreaControlClaimHandler.RectangleRegion region) {
        if (this.playersWithExt.contains(receiver.getGameProfile().getId())) {
            ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> receiver), new ACSendCurrentSelection(false, region.start(), region.end()));
        }
    }

    public void clearSelectionForClient(ServerPlayer receiver) {
        if (this.playersWithExt.contains(receiver.getGameProfile().getId())) {
            ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> receiver), new ACSendCurrentSelection(true, null, null));
        }
    }

    public @Nullable Area getCurrentAreaForPlayer(UUID playerUUID) {
        var areaId = this.playerLocation.get(playerUUID);
        return AreaManager.INSTANCE.findBy(areaId);
    }

    public boolean thisPlayerHasClientExt(ServerPlayer p) {
        return this.playersWithExt.contains(p.getGameProfile().getId());
    }

    public boolean hasBypassModeOnForArea(@NotNull Entity actor, @Nullable Area area) {
        return this.hasBypassModeOnForArea(actor.getUUID(), area);
    }

    public boolean hasBypassModeOnForArea(@NotNull Player p, @Nullable Area area) {
        return this.hasBypassModeOnForArea(p.getGameProfile().getId(), area);
    }

    public boolean hasBypassModeOnForArea(@NotNull UUID id, @Nullable Area area) {
        if (area == null) {
            return this.playersWithWildnessExemption.contains(id);
        } else {
            return this.playerExemptionStatus.getOrDefault(id, Set.of()).contains(area.uid);
        }
    }

    public void setGlobalExempt(ServerPlayer p, boolean global) {
        var area = AreaManager.INSTANCE.findBy(p.level(), p.position());
        var playerId = p.getGameProfile().getId();
        var exemptedArea = this.playerExemptionStatus.get(playerId);
        if (exemptedArea == null) {
            exemptedArea = ConcurrentHashMap.newKeySet();
            this.playerExemptionStatus.put(playerId, exemptedArea);
        }
        if (global) {
            this.playersWithGlobalExempt.add(p.getGameProfile().getId());
            if (!AreaChecks.isACtrlAreaBuilder(p, area)) {
                return;
            }
            if (area == null) {
                this.playersWithWildnessExemption.add(playerId);
                p.displayClientMessage(Component.translatable("area_control.bypass.global.wildness.on"), false);
                p.displayClientMessage(HOW_TO_TURN_OFF, false);
            } else {
                exemptedArea.add(area.uid);
                p.displayClientMessage(Component.translatable("area_control.bypass.global.area.on", area.name), false);
                p.displayClientMessage(HOW_TO_TURN_OFF, false);
            }
        } else {
            if (!AreaChecks.isACtrlAreaBuilder(p, area)) {
                return;
            }
            if (area == null) {
                this.playersWithWildnessExemption.add(playerId);
                p.displayClientMessage(Component.translatable("area_control.bypass.local.wildness.on"), false);
                p.displayClientMessage(HOW_TO_TURN_OFF, false);
            } else {
                exemptedArea.add(area.uid);
                p.displayClientMessage(Component.translatable("area_control.bypass.local.area.on", area.name), false);
                p.displayClientMessage(HOW_TO_TURN_OFF, false);
            }
        }

    }

    public void clearExemptFor(ServerPlayer p) {
        var id = p.getGameProfile().getId();
        if (this.playersWithGlobalExempt.remove(id)) {
            var previouslyExempted = this.playerExemptionStatus.remove(id);
            for (var areaId : previouslyExempted) {
                var areaName = AreaManager.INSTANCE.findBy(areaId);
                p.displayClientMessage(Component.translatable("area_control.bypass.global.area.off", areaName), false);
                p.displayClientMessage(HOW_TO_TURN_ON, false);
            }
            if (this.playersWithWildnessExemption.remove(id)) {
                p.displayClientMessage(Component.translatable("area_control.bypass.global.wildness.off"), false);
                p.displayClientMessage(HOW_TO_TURN_ON, false);
            }
        } else {
            var previouslyExempted = this.playerExemptionStatus.remove(id);
            for (var areaId : previouslyExempted) {
                var areaName = AreaManager.INSTANCE.findBy(areaId);
                p.displayClientMessage(Component.translatable("area_control.bypass.local.area.off", areaName), false);
                p.displayClientMessage(HOW_TO_TURN_ON, false);
            }
            if (this.playersWithWildnessExemption.remove(id)) {
                p.displayClientMessage(Component.translatable("area_control.bypass.local.wildness.off"), false);
                p.displayClientMessage(HOW_TO_TURN_ON, false);
            }
        }

    }
}
