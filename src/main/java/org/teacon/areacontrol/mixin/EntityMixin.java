package org.teacon.areacontrol.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.impl.AreaChecks;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyReceiver(method = "collectColliders", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;"))
    private static ImmutableList.Builder<VoxelShape> addMoreBeforeBuild(ImmutableList.Builder<VoxelShape> original,
                                                                        @Nullable Entity entity,
                                                                        Level level,
                                                                        List<VoxelShape> collisions,
                                                                        AABB boundingBox) {
        if (entity == null) {
            return original;
        }
        BlockPos pos = entity.getOnPos();
        var nearbyAreas = AreaManager.INSTANCE.getAreaSummariesSurround(level.dimension(), pos, 32);
        var entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        for (Area area : nearbyAreas) {
            if (!AreaChecks.checkPropFor(area, entity, "move_in", entityTypeId, () -> true)) {
                original.add(Shapes.box(area.minX, area.minY, area.minZ, area.maxX + 1, area.maxY + 1, area.maxZ + 1));
            }
        }
        return original;
    }
}
