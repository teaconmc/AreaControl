package org.teacon.areacontrol.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.client.AreaControlClientSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ACSendNearbyArea {

    private List<Area.Summary> areas = new ArrayList<>();
    private long expireAfter = 0L;

    public ACSendNearbyArea(List<Area.Summary> areas, long expireAfter) {
        this.areas.addAll(areas);
        this.expireAfter = expireAfter;
    }

    public ACSendNearbyArea(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID uid = buf.readUUID();
            int minX = buf.readVarInt(), minY = buf.readVarInt(), minZ = buf.readVarInt();
            int maxX = buf.readVarInt(), maxY = buf.readVarInt(), maxZ = buf.readVarInt();
            boolean enclosed = buf.readBoolean();
            this.areas.add(new Area.Summary(uid, minX, minY, minZ, maxX, maxY, maxZ, enclosed));
        }
        this.expireAfter = buf.readVarLong();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.areas.size());
        for (var area : this.areas) {
            buf.writeUUID(area.uid).writeVarInt(area.minX).writeVarInt(area.minY).writeVarInt(area.minZ)
                    .writeVarInt(area.maxX).writeVarInt(area.maxY).writeVarInt(area.maxZ).writeBoolean(area.enclosed);
        }
        buf.writeVarLong(this.expireAfter);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        AreaControlClientSupport.knownAreas = this.areas;
        AreaControlClientSupport.knownAreasExpiresAt = this.expireAfter;
        contextSupplier.get().setPacketHandled(true);
    }

}
