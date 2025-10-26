package tj.items.handlers;

import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.util.TriConsumer;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Recommended to use TJSlotWidget for player interacting with this.
 */
public class FilteredItemStackHandler extends LargeItemStackHandler {

    private TriConsumer<Integer, ItemStack, Boolean> onContentsChanged;
    private BiPredicate<Integer, ItemStack> itemStackPredicate;
    private final MetaTileEntity tileEntity;

    public FilteredItemStackHandler(MetaTileEntity tileEntity) {
        this(tileEntity, 1, 64);
    }

    public FilteredItemStackHandler(MetaTileEntity tileEntity, int slots) {
        this(tileEntity, slots, 64);
    }

    public FilteredItemStackHandler(MetaTileEntity tileEntity, int slots, int capacity) {
        super(slots, capacity);
        this.tileEntity = tileEntity;
    }

    public FilteredItemStackHandler setOnContentsChanged(TriConsumer<Integer, ItemStack, Boolean> onContentsChanged) {
        this.onContentsChanged = onContentsChanged;
        return this;
    }

    public FilteredItemStackHandler setItemStackPredicate(BiPredicate<Integer, ItemStack> itemStackPredicate) {
        this.itemStackPredicate = itemStackPredicate;
        return this;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (this.itemStackPredicate != null && !this.itemStackPredicate.test(slot, stack))
            return stack;
        ItemStack upgradeStack = stack.copy();
        stack = super.insertItem(slot, stack, simulate);
        if (!simulate) {
            if (this.onContentsChanged != null)
                this.onContentsChanged.accept(slot, upgradeStack, true);
            if (this.tileEntity != null)
                this.tileEntity.markDirty();
        }
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack upgradeStack = this.getStackInSlot(slot).copy();
        ItemStack stack = super.extractItem(slot, amount, simulate);
        if (!simulate) {
            if (this.onContentsChanged != null)
                this.onContentsChanged.accept(slot, upgradeStack, false);
            if (this.tileEntity != null)
                this.tileEntity.markDirty();
        }
        return stack;
    }
}
