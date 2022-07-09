package org.teacon.areacontrol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ACPingServer {

    public ACPingServer() {
        // No-op
    }

    public ACPingServer(FriendlyByteBuf buf) {
        // No-op
    }

    public void write(FriendlyByteBuf buf) {
        // No-op
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().setPacketHandled(true);
    }
}
