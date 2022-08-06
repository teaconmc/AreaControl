package org.teacon.areacontrol;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.permission.PermissionAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.impl.AreaChecks;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACSendCurrentSelection;
import org.teacon.areacontrol.network.ACSendNearbyArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "area_control")
public enum AreaControlPlayerTracker {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("PlayerTracker");
    private final Map<UUID, UUID> playerLocation = new HashMap<>();
    private final Set<UUID> playersWithExt = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        var player = event.getPlayer();
        var currentArea = AreaManager.INSTANCE.findBy(player.level, player.blockPosition());
        INSTANCE.playerLocation.put(player.getGameProfile().getId(), currentArea.uid);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            var player = event.player;
            var prevAreaId = INSTANCE.playerLocation.get(player.getGameProfile().getId());
            if (prevAreaId != null) {
                var prevArea = AreaManager.INSTANCE.findBy(prevAreaId);
                var currentArea = AreaManager.INSTANCE.findBy(player.level, player.blockPosition());
                if (prevArea != currentArea) {
                    INSTANCE.playerLocation.put(player.getGameProfile().getId(), currentArea.uid);
                    if (AreaProperties.getBool(currentArea, "area.display_welcome_message") || PermissionAPI.getPermission((ServerPlayer) player, AreaControlPermissions.WELCOME_MSG)) {
                        player.displayClientMessage(new TranslatableComponent("area_control.claim.welcome", currentArea.name), true);
                    }
                }
                var mainInv = player.getInventory();
                AreaChecks.checkInv(mainInv.items, currentArea, player);
                AreaChecks.checkInv(mainInv.armor, currentArea, player);
                AreaChecks.checkInv(mainInv.offhand, currentArea, player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var playerId = event.getPlayer().getGameProfile().getId();
        INSTANCE.undoMarkPlayer(playerId);
        INSTANCE.playerLocation.remove(playerId);
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
        requester.displayClientMessage(new TranslatableComponent("area_control.claim.nearby", nearbyAreas.size()), false);
        LOGGER.debug(MARKER, "Nearby area count: {}", nearbyAreas.size());
        var summaries = new ArrayList<Area.Summary>();
        for (var nearbyArea : nearbyAreas) {
            LOGGER.debug(MARKER, "Nearby area: {}", nearbyArea.uid);
            var summary = new Area.Summary(nearbyArea);
            summaries.add(summary);
            requester.displayClientMessage(Util.describe(nearbyArea, requester.level), false);
        }
        if (this.playersWithExt.contains(requester.getGameProfile().getId())) {
            var expire = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 60000;
            ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> requester), new ACSendNearbyArea(summaries, expire));
        } else {
            requester.displayClientMessage(new TranslatableComponent("area_control.claim.nearby.visual"), false);
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
}
