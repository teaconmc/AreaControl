package org.teacon.areacontrol.mixin;

import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/world/level/block/entity/LecternBlockEntity$1")
public interface LecternContainerAccessor {
    @Accessor
    LecternBlockEntity getThis$0();
}
