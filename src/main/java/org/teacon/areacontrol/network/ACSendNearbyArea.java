package org.teacon.areacontrol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.client.AreaControlClientSupport;

import java.util.List;
import java.util.UUID;

public record ACSendNearbyArea(List<Area.Summary> areas, long expireAfter) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ACSendNearbyArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("area_control", "send_near_by_area"));

    public static final StreamCodec<FriendlyByteBuf, ACSendNearbyArea> STREAM_CODEC = StreamCodec.composite(
            new StreamCodec<FriendlyByteBuf, Area.Summary>() {
                @Override
                @NotNull
                public Area.Summary decode(@NotNull FriendlyByteBuf buf) {
                    UUID uid = buf.readUUID();
                    int minX = buf.readVarInt(), minY = buf.readVarInt(), minZ = buf.readVarInt();
                    int maxX = buf.readVarInt(), maxY = buf.readVarInt(), maxZ = buf.readVarInt();
                    boolean enclosed = buf.readBoolean();
                    return new Area.Summary(uid, minX, minY, minZ, maxX, maxY, maxZ, enclosed);
                }

                @Override
                public void encode(@NotNull FriendlyByteBuf buf, @NotNull Area.Summary area) {
                    buf.writeUUID(area.uid).writeVarInt(area.minX).writeVarInt(area.minY).writeVarInt(area.minZ)
                            .writeVarInt(area.maxX).writeVarInt(area.maxY).writeVarInt(area.maxZ).writeBoolean(area.enclosed);
                }
            }.apply(ByteBufCodecs.list()),
            ACSendNearbyArea::areas,
            ByteBufCodecs.VAR_LONG,
            ACSendNearbyArea::expireAfter,
            ACSendNearbyArea::new
    );

    public void handle(IPayloadContext context) {
        AreaControlClientSupport.knownAreas = areas;
        AreaControlClientSupport.knownAreasExpiresAt = expireAfter;
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
