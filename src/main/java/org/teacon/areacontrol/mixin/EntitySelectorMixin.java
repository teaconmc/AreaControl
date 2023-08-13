package org.teacon.areacontrol.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.teacon.areacontrol.impl.AreaEntitySelectorChecker;

import java.util.List;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {

    /**
     * Modify check to exclude entities that failed area check, and notify selector initiator on
     * how many entities were excluded.
     * @param sourceStack Command initiator
     * @param cir callback
     */
    // TODO This is an example use-case of @ModifyReturnValue from MixinExtras, which we don't have.
    @Inject(method = "findEntities", at = @At("HEAD"), cancellable = true)
    private void checkArea(CommandSourceStack sourceStack, CallbackInfoReturnable<List<? extends Entity>> cir) {
        var original = this.findEntitiesRaw(sourceStack);
        var itr = original.iterator();
        int removedDueToACtrl = 0;
        while (itr.hasNext()) {
            var e = itr.next();
            if (!e.getType().isEnabled(sourceStack.enabledFeatures())) {
                itr.remove();
            } else if (!AreaEntitySelectorChecker.check(sourceStack, e)) {
                itr.remove();
                removedDueToACtrl++;
            }
        }
        final String formattedCount = Integer.toString(removedDueToACtrl);
        sourceStack.sendSuccess(() -> Component.translatable("area_control.notice.selector_filtered", formattedCount), true);
        cir.setReturnValue(original);
    }

    @Shadow
    protected abstract List<? extends Entity> findEntitiesRaw(CommandSourceStack sourceStack);

}
