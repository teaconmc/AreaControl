package org.teacon.areacontrol;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACSendNearbyArea;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public enum AreaControlPlayerTracker {

    INSTANCE;

    private final Set<UUID> playersWithExt = new HashSet<>();

    public void markPlayerAsSupportExt(UUID playerUid) {
        this.playersWithExt.add(playerUid);
    }

    public void undoMarkPlayer(UUID playerUid) {
        this.playersWithExt.remove(playerUid);
    }

    public void sendNearbyAreasToClient(ResourceKey<Level> dim, ServerPlayer requester, double radius) {
        if (this.playersWithExt.contains(requester.getGameProfile().getId())) {
            var nearbyAreas = AreaManager.INSTANCE.getAreaSummariesSurround(dim, requester.blockPosition(), radius);
            ACNetworking.acNetworkChannel.send(PacketDistributor.PLAYER.with(() -> requester), new ACSendNearbyArea(nearbyAreas));
        }
    }
}
