package org.teacon.areacontrol.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Random;

@Mixin(FireBlock.class)
public class FireMixin {

    /**
     * Prepend an AreaControl check to determine whether fire tick should be stopped.
     * Note that this can also stop rain from extinguishing fire. Manually destroying
     * fire blocks is not affected.
     * @param state Captured BlockState
     * @param level Captured ServerLevel
     * @param pos Captured BlockPos
     * @param rand Captured Random
     * @param ci The callback
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void fireSpreadCheck(BlockState state, ServerLevel level, BlockPos pos, Random rand, CallbackInfo ci) {
        Area current = AreaManager.INSTANCE.findBy(level, pos);
        if (!AreaProperties.getBool(current, "area.allow_fire_spread")) {
            ci.cancel();
        }
    }
}
