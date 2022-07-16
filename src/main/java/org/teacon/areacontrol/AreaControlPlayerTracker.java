package org.teacon.areacontrol;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACSendNearbyArea;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public enum AreaControlPlayerTracker {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("PlayerTracker");
    private final Set<UUID> playersWithExt = new HashSet<>();

    public void markPlayerAsSupportExt(UUID playerUid) {
        this.playersWithExt.add(playerUid);
    }

    public void undoMarkPlayer(UUID playerUid) {
        this.playersWithExt.remove(playerUid);
    }

    public void sendNearbyAreasToClient(ResourceKey<Level> dim, ServerPlayer requester, double radius) {
        LOGGER.debug(MARKER, "Player {} has requested nearby area. Center: {}, radius: {}", requester.getGameProfile().getName(), requester.blockPosition(), radius);
        var nearbyAreas = AreaManager.INSTANCE.getAreaSummariesSurround(dim, requester.blockPosition(), radius);
        requester.displayClientMessage(new TranslatableComponent("area_control.claim.nearby", nearbyAreas.size()), false);
        LOGGER.debug(MARKER, "Nearby area count: {}", nearbyAreas.size());
        var summaries = new ArrayList<Area.Summary>();
        for (var nearbyArea : nearbyAreas) {
            LOGGER.debug(MARKER, "Nearby area: {}", nearbyArea.uid);
            var summary = new Area.Summary(nearbyArea);
            summaries.add(summary);
            requester.displayClientMessage(new TranslatableComponent("area_control.claim.nearby.detail",
                    new TextComponent(nearbyArea.name).setStyle(
                            Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(nearbyArea.uid.toString())))),
                    Util.toGreenText(nearbyArea),
                    new TranslatableComponent("area_control.claim.nearby.detail.go_there").setStyle(
                            Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("chat.coordinates.tooltip")))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + summary.midX + " " + summary.midY + " " + summary.midZ))
                                    .withColor(ChatFormatting.DARK_AQUA)
                    )), false);
        }
        if (this.playersWithExt.contains(requester.getGameProfile().getId())) {
            ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> requester), new ACSendNearbyArea(summaries));
        } else {
            requester.displayClientMessage(new TranslatableComponent("area_control.claim.nearby.visual"), false);
        }
        LOGGER.debug(MARKER, "End of the request");
    }
}
