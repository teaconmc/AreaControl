package org.teacon.areacontrol.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Optional;

@Mixin(ServerLevel.class)
public class LevelMixin {

    @Inject(method = "canSpreadFireAround", at = @At("HEAD"), cancellable = true)
    private void checkFireSpread(BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        Area area = AreaManager.INSTANCE.findBy(((ServerLevel) (Object) this), position);
        Optional<Boolean> allowFireSpread = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_FIRE_SPREAD);
        allowFireSpread.ifPresent(cir::setReturnValue);
    }
}
