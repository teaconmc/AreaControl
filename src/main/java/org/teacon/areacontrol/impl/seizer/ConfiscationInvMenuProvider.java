package org.teacon.areacontrol.impl.seizer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public class ConfiscationInvMenuProvider implements MenuProvider {

    @Override
    public Component getDisplayName() {
        return Component.translatable("area_control.screen.confiscated_items.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        ConfiscationInv inv = player.getData(AreaControlBorderControl.CONFISCATION_INV.get());
        return new ConfiscationInvMenu(MenuType.GENERIC_9x6, id, inventory, inv.containerView, 6);
    }
}
