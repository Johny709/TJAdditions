package tj.items.handlers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;

public class CabinetItemStackHandler extends LargeItemStackHandler {

    private String allowedItemName;
    private IntConsumer onSizeChanged;
    private boolean itemLocked;

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

    public CabinetItemStackHandler setItemLocked(boolean itemLocked) {
        this.itemLocked = itemLocked;
        return this;
    }

    public String getAllowedItemName() {
        return this.allowedItemName;
    }

    public boolean isItemLocked() {
        return this.itemLocked;
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
        if (!simulate && slot == this.getSlots() - 1)
            this.onSizeChanged.accept(this.getSlots() + 1);
        return stack1;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stack = super.extractItem(slot, amount, simulate);
        if (!simulate && slot > 26 && slot == this.getSlots() - 1)
            this.onSizeChanged.accept(this.getSlots() - 1);
        if (!simulate && !this.itemLocked && this.areSlotsEmpty())
            this.allowedItemName = null;
        return stack;
    }

    public void clear() {
        if (this.areSlotsEmpty())
            this.allowedItemName = null;
    }

    private boolean areSlotsEmpty() {
        for (int i = 0; i < this.getSlots(); i++) {
            if (!this.stacks.get(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = super.serializeNBT();
        nbt.setBoolean("ItemLocked", this.itemLocked);
        if (this.allowedItemName != null)
            nbt.setString("ItemName", this.allowedItemName);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        super.deserializeNBT(nbt);
        this.itemLocked = nbt.getBoolean("ItemLocked");
        if (nbt.hasKey("ItemName"))
            this.allowedItemName = nbt.getString("ItemName");
    }
}
