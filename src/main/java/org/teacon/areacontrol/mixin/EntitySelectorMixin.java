package org.teacon.areacontrol.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.teacon.areacontrol.impl.AreaEntitySelectorChecker;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    /**
     * Modify the filter to include area check: if the entity in question is not in the area that
     * command source locates at, then it will be excluded.
     * @param sourceStack Command initiator
     * @param e Entity in question
     * @param cir callback
     */
    @Inject(method = { "lambda$findEntities$1", "m_244752_" }, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private static void checkArea(CommandSourceStack sourceStack, Entity e, CallbackInfoReturnable<Boolean> cir) {
        if (!AreaEntitySelectorChecker.check(sourceStack, e)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

}
