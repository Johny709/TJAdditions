package tj.gui.widgets;

import net.minecraft.item.ItemStack;

public interface IItemSlotHandler {

    ItemStack insertStack(int slot, ItemStack stack, boolean simulate);
}
