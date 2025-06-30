package tj.items.handlers;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class LargeItemStackHandler extends ItemStackHandler {

    private final int capacity;

    public LargeItemStackHandler(int size, int capacity) {
        super(size);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    protected int getStackLimit(int slot, @NotNull ItemStack stack) {
        return capacity;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0)
            return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int toExtract = Math.min(amount, capacity);

        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                this.stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
            }
            return existing;
        }
        else {
            if (!simulate) {
                this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                onContentsChanged(slot);
            }
            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
        }
    }
}
