package org.teacon.areacontrol.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseCommandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.teacon.areacontrol.AreaManager;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    /**
     * Modify the filter to include area check: if the entity in question is not in the area that
     * command source locates at, then it will be excluded.
     * @param sourceStack Command initiator
     * @param e Entity in question
     * @param cir callback
     */
    @Inject(method = "lambda$findEntities$1", at = @At("HEAD"), cancellable = true)
    private static void checkArea(CommandSourceStack sourceStack, Entity e, CallbackInfoReturnable<Boolean> cir) {
        // This check applies to entities and Command Blocks only
        var trueSrc = sourceStack.source;
        if (trueSrc instanceof Entity || trueSrc instanceof BaseCommandBlock) {
            var level = sourceStack.getLevel();
            var pos = sourceStack.getPosition();
            var area = AreaManager.INSTANCE.findBy(level, pos);
            if (area != null) {
                if (e.xo < area.minX || e.xo > area.maxX || e.yo < area.minY || e.yo > area.maxY || e.zo < area.minZ || e.zo > area.maxZ) {
                    cir.setReturnValue(Boolean.FALSE);
                }
            }
        }
    }

}
