package org.teacon.areacontrol.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Optional;
import java.util.Random;

@Mixin(FireBlock.class)
public class FireMixin {

    /**
     * Prepend an AreaControl check to determine whether fire tick should be stopped.
     * Note that this can also stop rain from extinguishing fire. Manually destroying
     * fire blocks is not affected.
     * If allowFireSpread is not specified in the area, continue to use doFireTick in gameRule instead.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean fireSpreadCheck(GameRules instance, GameRules.Key<GameRules.BooleanValue> pKey, BlockState pState, ServerLevel pLevel, BlockPos pPos) {
        Area area = AreaManager.INSTANCE.findBy(pLevel, pPos);
        Optional<Boolean> allowFireSpread = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_FIRE_SPREAD);
        return allowFireSpread.orElseGet(() -> pLevel.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK));
    }
}
