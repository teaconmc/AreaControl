package org.teacon.areacontrol.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.teacon.areacontrol.AreaControlPlayerTracker;

public record ACPingServer() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ACPingServer> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("area_control", "ping_server"));

    public static final StreamCodec<ByteBuf, ACPingServer> STREAM_CODEC = new StreamCodec<>() {
        @Override
        @NotNull
        public ACPingServer decode(@NotNull ByteBuf byteBuf) {
            return new ACPingServer();
        }

        @Override
        public void encode(@NotNull ByteBuf o, @NotNull ACPingServer instance) {
        }
    };

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            AreaControlPlayerTracker.getFrom(player).clientExtensionEnabled = true;
        });
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
