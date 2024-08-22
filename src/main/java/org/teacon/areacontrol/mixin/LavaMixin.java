package org.teacon.areacontrol.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.Optional;

/**
 * @author USS_Shenzhou
 */
@Mixin(LavaFluid.class)
public class LavaMixin {

    /**
     * Prepend an AreaControl check to determine whether lava can perform random tick.
     * If allowFireSpread is not specified in the area, continue to use doFireTick in gameRule instead.
     */
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean randomTickCheck(GameRules self, GameRules.Key<GameRules.BooleanValue> key, Operation<Boolean> original, Level level, BlockPos pos) {
        Area area = AreaManager.INSTANCE.findBy(level, pos);
        Optional<Boolean> allowFireSpread = AreaProperties.getBoolOptional(area, AreaProperties.ALLOW_FIRE_SPREAD);
        return allowFireSpread.orElseGet(() -> original.call(self, key));
    }
}
