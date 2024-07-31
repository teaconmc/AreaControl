package org.teacon.areacontrol.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ACNetworking {
    public static void init(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("area_control")
                .versioned("0.4.0")
                .optional();

        registrar.playToServer(ACPingServer.TYPE, ACPingServer.STREAM_CODEC, ACPingServer::handle);
        registrar.playToClient(ACSendNearbyArea.TYPE, ACSendNearbyArea.STREAM_CODEC, ACSendNearbyArea::handle);
        registrar.playToClient(ACSendCurrentSelection.TYPE, ACSendCurrentSelection.STREAM_CODEC, ACSendCurrentSelection::handle);
        registrar.playToClient(ACShowPropEditScreen.TYPE, ACShowPropEditScreen.STREAM_CODEC, ACShowPropEditScreen::handle);
    }

    private static final CustomPacketPayload[] EMPTY = new CustomPacketPayload[0];

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload, EMPTY);
    }

    public static void send(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload, EMPTY);
    }
}
