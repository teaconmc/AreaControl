package org.teacon.areacontrol.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.areacontrol.client.AreaControlClientSupport;

import java.util.function.Supplier;

public class ACSendCurrentSelection {

    public boolean clear;
    public BlockPos pos1, pos2;

    public ACSendCurrentSelection(boolean clear, BlockPos pos1, BlockPos pos2) {
        this.clear = clear;
        this.pos1 = pos1;
        this.pos2 = pos2;
        if (this.pos1 == null && this.pos2 != null) {
            this.pos1 = this.pos2;
        } else if (this.pos1 != null && this.pos2 == null) {
            this.pos2 = this.pos1;
        }
    }

    public ACSendCurrentSelection(FriendlyByteBuf buf) {
        if (!(this.clear = buf.readBoolean())) {
            this.pos1 = buf.readBlockPos();
            this.pos2 = buf.readBlockPos();
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.clear);
        if (!this.clear) {
            buf.writeBlockPos(this.pos1).writeBlockPos(this.pos2);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        if (this.clear) {
            AreaControlClientSupport.selectionMin = null;
            AreaControlClientSupport.selectionMax = null;
        } else {
            var box = new AABB(this.pos1, this.pos2);
            AreaControlClientSupport.selectionMin = new BlockPos(box.minX, box.minY, box.minZ);
            AreaControlClientSupport.selectionMax = new BlockPos(box.maxX, box.maxY, box.maxZ);
        }
        contextSupplier.get().setPacketHandled(true);
    }
}
