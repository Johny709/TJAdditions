package tj.items.handlers;

import gregtech.api.util.GTLog;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;

public class CabinetItemStackHandler extends LargeItemStackHandler {

    private String allowedItemName;
    private IntConsumer onSizeChanged;

    public CabinetItemStackHandler(int size, int capacity) {
        super(size, capacity);
    }

    public CabinetItemStackHandler setSizeChangeListener(IntConsumer onSlotChanged) {
        this.onSizeChanged = onSlotChanged;
        return this;
    }

    public CabinetItemStackHandler setAllowedItemByName(String allowedItemName) {
        this.allowedItemName = allowedItemName;
        return this;
    }

    public String getAllowedItemName() {
        return allowedItemName;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        String itemName = stack.getItem().getRegistryName().toString();
        if (!stack.isEmpty())
            if (this.allowedItemName == null)
                this.allowedItemName = itemName;
            else if (!this.allowedItemName.equals(itemName))
                return stack;
        ItemStack stack1 = super.insertItem(slot, stack, simulate);
        GTLog.logger.info(simulate);
        if (!simulate && slot == this.getSlots() - 1)
            this.onSizeChanged.accept(this.getSlots() + 1);
        return stack1;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = super.extractItem(slot, amount, simulate);
        GTLog.logger.info(simulate);
        if (!simulate && slot > 26 && slot == this.getSlots() - 1)
            this.onSizeChanged.accept(this.getSlots() - 1);
        if (!simulate && this.areSlotsEmpty())
            this.allowedItemName = null;
        return stack;
    }

    private boolean areSlotsEmpty() {
        for (int i = 0; i < this.getSlots(); i++) {
            if (!this.stacks.get(i).isEmpty())
                return false;
        }
        return true;
    }
}
