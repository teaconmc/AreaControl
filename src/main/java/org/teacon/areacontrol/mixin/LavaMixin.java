package org.teacon.areacontrol.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
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

/**
 * @author USS_Shenzhou
 */
@Mixin(LavaFluid.class)
public class LavaMixin {

    /**
     * Prepend an AreaControl check to determine whether lava can perform random tick.
     * If allowFireSpread is not specified in the area, continue to use doFireTick in gameRule instead.
     */
    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean randomTickCheck(GameRules instance, GameRules.Key<GameRules.BooleanValue> pKey, Level pLevel, BlockPos pPos) {
        Area area = AreaManager.INSTANCE.findBy(pLevel, pPos);
        Optional<Boolean> allowFireSpread = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_FIRE_SPREAD);
        return allowFireSpread.orElseGet(() -> pLevel.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK));
    }
}
