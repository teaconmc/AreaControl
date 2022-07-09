package org.teacon.areacontrol.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ACNetworking {

    public static SimpleChannel acNetworkChannel = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("area_control", "network"),
            () -> "0.1.0",
            remoteVer -> "0.1.0".equals(remoteVer) || NetworkRegistry.ABSENT.equals(remoteVer) || NetworkRegistry.ACCEPTVANILLA.equals(remoteVer),
            clientVer -> "0.1.0".equals(clientVer) || NetworkRegistry.ABSENT.equals(clientVer) || NetworkRegistry.ACCEPTVANILLA.equals(clientVer)
    );

    public static void init() {
        acNetworkChannel.registerMessage(0, ACPingServer.class, ACPingServer::write, ACPingServer::new, ACPingServer::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        acNetworkChannel.registerMessage(1, ACSendNearbyArea.class, ACSendNearbyArea::write, ACSendNearbyArea::new, ACSendNearbyArea::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
