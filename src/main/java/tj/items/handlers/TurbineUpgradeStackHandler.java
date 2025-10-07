package tj.items.handlers;

import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class TurbineUpgradeStackHandler extends ItemStackHandler {

    private BiConsumer<ItemStack, Boolean> onContentsChanged;
    private Predicate<ItemStack> itemStackPredicate;
    private final MetaTileEntity tileEntity;

    public TurbineUpgradeStackHandler(MetaTileEntity tileEntity) {
        super();
        this.tileEntity = tileEntity;
    }

    public TurbineUpgradeStackHandler setOnContentsChanged(BiConsumer<ItemStack, Boolean> onContentsChanged) {
        this.onContentsChanged = onContentsChanged;
        return this;
    }

    public TurbineUpgradeStackHandler setItemStackPredicate(Predicate<ItemStack> itemStackPredicate) {
        this.itemStackPredicate = itemStackPredicate;
        return this;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (this.itemStackPredicate != null && !this.itemStackPredicate.test(stack))
            return stack;
        ItemStack upgradeStack = stack.copy();
        stack = super.insertItem(slot, stack, simulate);
        if (!simulate) {
            this.onContentsChanged.accept(upgradeStack, true);
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
            this.onContentsChanged.accept(upgradeStack, false);
            this.tileEntity.markDirty();
        }
        return stack;
    }
}
