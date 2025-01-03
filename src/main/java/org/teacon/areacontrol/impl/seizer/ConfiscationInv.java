package org.teacon.areacontrol.impl.seizer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;

public class ConfiscationInv implements INBTSerializable<CompoundTag> {

    private NonNullList<ItemStack> seizedItems = NonNullList.withSize(54, ItemStack.EMPTY);

    transient final Container containerView = new ContainerWrapper();

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag data = new CompoundTag();
        ListTag seizedItemData = new ListTag();
        for (ItemStack item : this.seizedItems) {
            if (!item.isEmpty()) {
                seizedItemData.add(item.save(provider));
            }
        }
        data.put("seized_items", seizedItemData);
        return data;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag compoundTag) {
        ListTag seizedItemData = compoundTag.getList("seized_items", ListTag.TAG_COMPOUND);
        for (Tag data : seizedItemData) {
            Optional<ItemStack> maybeItem = ItemStack.parse(provider, data);
            if (maybeItem.isPresent()) {
                this.add(maybeItem.get());
            }
        }
    }

    public void add(ItemStack seized) {
        for (int i = 0; i < this.seizedItems.size(); i++) {
            if (seized.isEmpty()) {
                break;
            }
            ItemStack target = this.seizedItems.get(i);
            if (target.isEmpty()) {
                this.seizedItems.set(i, seized);
                break;
            }
            if (target.isStackable() && ItemStack.isSameItemSameComponents(target, seized)) {
                int delta = Math.min(seized.getCount(), target.getMaxStackSize() - target.getCount());
                target.setCount(target.getCount() + delta);
                seized.setCount(seized.getCount() - delta);
            }
        }
    }

    public void clear() {
        Collections.fill(this.seizedItems, ItemStack.EMPTY);
    }

    final class ContainerWrapper implements Container {

        @Override
        public int getContainerSize() {
            return ConfiscationInv.this.seizedItems.size();
        }

        @Override
        public boolean isEmpty() {
            return ConfiscationInv.this.seizedItems.isEmpty();
        }

        @Override
        public ItemStack getItem(int index) {
            return index < this.getContainerSize() ? ConfiscationInv.this.seizedItems.get(index) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int index, int amount) {
            ItemStack original = this.getItem(index);
            return original.split(amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return index < this.getContainerSize() ? ConfiscationInv.this.seizedItems.set(index, ItemStack.EMPTY) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int index, ItemStack itemStack) {
            ConfiscationInv.this.seizedItems.set(index, itemStack);
        }

        @Override
        public void setChanged() {
            // Nothing to do here
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            ConfiscationInv.this.seizedItems.clear();
        }
    }
}
