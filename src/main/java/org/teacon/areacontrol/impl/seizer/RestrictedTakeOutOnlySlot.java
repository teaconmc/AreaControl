package org.teacon.areacontrol.impl.seizer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.impl.AreaChecks;

public class RestrictedTakeOutOnlySlot extends Slot {
    public RestrictedTakeOutOnlySlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    /**
     * Determines whether the item may be placed into this slot.
     * @param stack The item to be inserted in
     * @return Always return false; this implementation does not permit item insertion.
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /**
     * Determines whether the player may take item out of this slot.
     * @param player The player who tries to pick out items
     * @return true if the area player is in allows possession of the item in slot
     */
    @Override
    public boolean mayPickup(Player player) {
        var currentArea = AreaManager.INSTANCE.findBy(player.level(), player.blockPosition());
        return AreaChecks.checkPossess(currentArea, this.getItem().getItem());
    }
}
