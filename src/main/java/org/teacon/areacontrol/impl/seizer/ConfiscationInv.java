package org.teacon.areacontrol.impl.seizer;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Collections;

public class ConfiscationInv implements ValueIOSerializable {

    private static final Codec<NonNullList<ItemStack>> INV_LIST_CODEC = NonNullList.codecOf(ItemStack.OPTIONAL_CODEC);

    private NonNullList<ItemStack> seizedItems = NonNullList.withSize(54, ItemStack.EMPTY);

    transient final Container containerView = new ContainerWrapper();

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

    /** Overloaded version of {@link #add(ItemStack)} that supports {@link ItemResource}. */
    public void add(ItemResource item, int seizedAmount) {
        for (int i = 0; i < this.seizedItems.size(); i++) {
            if (seizedAmount <= 0) {
                break;
            }
            ItemStack target = this.seizedItems.get(i);
            if (target.isEmpty()) {
                this.seizedItems.set(i, item.toStack(seizedAmount));
                break;
            }
            if (target.isStackable() && item.matches(target)) {
                int delta = Math.min(seizedAmount, target.getMaxStackSize() - target.getCount());
                target.setCount(target.getCount() + delta);
                seizedAmount -= delta;
            }
        }
    }

    public void clear() {
        Collections.fill(this.seizedItems, ItemStack.EMPTY);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("seized_items", INV_LIST_CODEC, this.seizedItems);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read("seized_items", INV_LIST_CODEC).ifPresent(items -> this.seizedItems = items);
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
