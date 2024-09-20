package org.teacon.areacontrol.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.teacon.areacontrol.impl.AreaEntitySelectorChecker;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin {

    /**
     * Modify check to exclude entities that failed area check, and notify selector initiator on
     * how many entities were excluded.
     */
    @ModifyReturnValue(method = "findEntities", at = @At("RETURN"))
    private List<? extends Entity> checkArea(List<? extends Entity> original, CommandSourceStack sourceStack) {
        var removedDueToACtrl = 0;
        var filtered = new ArrayList<Entity>();
        for (var e : original) {
            if (AreaEntitySelectorChecker.check(sourceStack, e)) {
                filtered.add(e);
            } else {
                removedDueToACtrl++;
            }
        }
        if (removedDueToACtrl > 0) {
            final String formattedCount = Integer.toString(removedDueToACtrl);
            sourceStack.sendSuccess(() -> Component.translatable("area_control.notice.selector_filtered", formattedCount), true);
        }
        return List.copyOf(filtered);
    }
}
