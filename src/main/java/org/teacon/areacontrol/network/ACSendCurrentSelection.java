package org.teacon.areacontrol.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.teacon.areacontrol.client.AreaControlClientSupport;

public record ACSendCurrentSelection(boolean clear, BlockPos pos1, BlockPos pos2) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ACSendCurrentSelection> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("area_control", "send_current_selection"));

    public static final StreamCodec<FriendlyByteBuf, ACSendCurrentSelection> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ACSendCurrentSelection::clear,
            ACNetworking.asNullableCodecValue(BlockPos.STREAM_CODEC),
            ACSendCurrentSelection::pos1,
            ACNetworking.asNullableCodecValue(BlockPos.STREAM_CODEC),
            ACSendCurrentSelection::pos2,
            ACSendCurrentSelection::new
    );

    public static ACSendCurrentSelection of(boolean clear, BlockPos pos1, BlockPos pos2) {
        if (pos1 == null && pos2 != null) {
            pos1 = pos2;
        } else if (pos1 != null && pos2 == null) {
            pos2 = pos1;
        }

        return new ACSendCurrentSelection(clear, pos1, pos2);
    }

    public void handle(IPayloadContext context) {
        if (clear) {
            AreaControlClientSupport.selectionMin = null;
            AreaControlClientSupport.selectionMax = null;
        } else {
            var box = new AABB(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
            AreaControlClientSupport.selectionMin = new BlockPos((int) box.minX, (int) box.minY, (int) box.minZ);
            AreaControlClientSupport.selectionMax = new BlockPos((int) box.maxX, (int) box.maxY, (int) box.maxZ);
        }
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
