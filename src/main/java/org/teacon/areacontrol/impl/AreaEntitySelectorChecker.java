package org.teacon.areacontrol.impl;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.phys.Vec3;
import org.teacon.areacontrol.AreaControlConfig;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.AreaProperties;

import java.util.function.Supplier;

public class AreaEntitySelectorChecker {

    /**
     * Check whether the command source may use entity selector to select given entity
     * @param sourceStack The initiator of entity selector
     * @param e The entity that may or may not be selected
     * @return true if the entity may be selected; false otherwise.
     */
    public static boolean check(CommandSourceStack sourceStack, Entity e) {

        /*
         * Implementation summary
         * This is an O(n) algorithm to check if entity selector initiated
         * within an area can include entity from another area.
         * Its best-case is initiator and target entity are within the same area,
         * making this O(1) (6 comparisons).
         * Its average case is we have to walk along the area hierarchy tree,
         * checking properties of each area, which is O(n).
         */

        // This check applies to entities and Command Blocks only.
        var trueSrc = sourceStack.source;
        String selectFromChild = null, selectFromParent = null;
        Supplier<Boolean> selectFromChildFallBack = null, selectFromParentFallBack = null;
        if (trueSrc instanceof Entity) {
            selectFromChild = AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_CHILD;
            selectFromParent = AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_PARENT;
            selectFromChildFallBack = AreaControlConfig.allowEntitySelectingFromChild;
            selectFromParentFallBack = AreaControlConfig.allowEntitySelectingFromParent;
        } else if (trueSrc instanceof BaseCommandBlock) {
            selectFromChild = AreaProperties.ALLOW_CB_USE_SELECTOR_FROM_CHILD;
            selectFromParent = AreaProperties.ALLOW_ENTITY_USE_SELECTOR_FROM_PARENT;
            selectFromChildFallBack = AreaControlConfig.allowCBSelectingFromChild;
            selectFromParentFallBack = AreaControlConfig.allowCBSelectingFromParent;
        }
        if (selectFromChild != null) {
            var area = AreaManager.INSTANCE.findBy(sourceStack.getLevel(), sourceStack.getPosition());
            // Get the area in which the target entity locates.
            // Do note that, EntitySelector can select entities from a different dimension,
            // so we must use the level from the target entity.
            final var targetArea = AreaManager.INSTANCE.findBy(e.level(), new Vec3(e.xo, e.yo, e.zo));
            // If the entity is in the same area as the selector initiator, then it may be selected
            if (area == targetArea) {
                return true;
            }
            // Otherwise, we follow this procedure to determine.
            var currentlyChecking = area;
            // 1. Walk up from the area hierarchy tree, checking if all the parent areas
            //    allow "selecting entities from child area".
            //    The walking stops at the area that is common ancestor to both the
            //    origin area and target area.
            do {
                currentlyChecking = AreaManager.INSTANCE.findBy(currentlyChecking.belongingArea);
                var result = AreaProperties.getBoolOptional(currentlyChecking, selectFromChild);
                if (!result.orElseGet(selectFromChildFallBack)) {
                    return false;
                }
            } while (!AreaMath.isEnclosing(currentlyChecking, targetArea));
            // 2. If we are at the target area, then we are done, we can select this entity.
            //    Else, we have to walk down along the hierarchy tree, to the target area.
            if (currentlyChecking != targetArea) {
                var reverseChecking = targetArea;
                // 3. For each area that we encounter, we check if it allows "selecting entities
                //    from parent area".
                do {
                    var result = AreaProperties.getBoolOptional(reverseChecking, selectFromParent);
                    if (!result.orElseGet(selectFromParentFallBack)) {
                        return false;
                    }
                    reverseChecking = AreaManager.INSTANCE.findBy(reverseChecking.belongingArea);
                } while (reverseChecking != currentlyChecking);
            }
        }
        // If the program hits here, it means that we have successfully conducted all checks,
        // or there isn't anything to check (because it was server console or other sources).
        // Return true as our result.
        return true;
    }
}
