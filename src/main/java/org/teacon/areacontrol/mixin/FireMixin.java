package org.teacon.areacontrol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Optional;

@Mixin(FireBlock.class)
public class FireMixin {

    /**
     * Prepend an AreaControl check to determine whether fire tick should be stopped.
     * Note that this can also stop rain from extinguishing fire. Manually destroying
     * fire blocks is not affected.
     * If allowFireSpread is not specified in the area, continue to use doFireTick in gameRule instead.
     */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean fireSpreadCheck(GameRules self, GameRules.Key<GameRules.BooleanValue> key, Operation<Boolean> original, BlockState state, ServerLevel level, BlockPos pos) {
        Area area = AreaManager.INSTANCE.findBy(level, pos);
        Optional<Boolean> allowFireSpread = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_FIRE_SPREAD);
        return allowFireSpread.orElseGet(() -> original.call(self, key));
    }
}
