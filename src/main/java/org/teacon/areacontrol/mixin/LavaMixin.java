package org.teacon.areacontrol.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Optional;
import java.util.Random;

/**
 * @author USS_Shenzhou
 */
@Mixin(LavaFluid.class)
public class LavaMixin {

    /**
     * Prepend an AreaControl check to determine whether lava can perform random tick.
     * If allowFireSpread is not specified in the area, continue to use doFireTick in gameRule instead.
     */
    @Inject(method = "randomTick", at = @At(value = "HEAD"), cancellable = true)
    private void randomTickCheck(Level pLevel, BlockPos pPos, FluidState pState, Random pRandom, CallbackInfo ci) {
        Area area = AreaManager.INSTANCE.findBy(pLevel, pPos);
        Optional<Boolean> allowFireSpread = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_FIRE_SPREAD);
        if (allowFireSpread.isPresent() && !allowFireSpread.get()) {
            ci.cancel();
        }
    }
}
