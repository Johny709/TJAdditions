package com.johny.tj.machines;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.PhantomSlotWidget;
import gregtech.api.util.LargeStackSizeItemStackHandler;
import gregtech.common.covers.filter.SimpleItemFilter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;

public class ExtendedItemFilter extends SimpleItemFilter {

    private final Map<Integer, IBlockState> blockToFilter;

    public ExtendedItemFilter(Map<Integer, IBlockState> blockToFilter) {
        this.blockToFilter = blockToFilter;
        this.itemFilterSlots = new LargeStackSizeItemStackHandler(50) {
            @Override
            public int getSlotLimit(int slot) {
                return getMaxStackSize();
            }

            @Override
            @Nonnull
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                Block block = Block.getBlockFromItem(stack.getItem());
                if (!block.equals(Blocks.AIR)) {
                    int meta = stack.getMetadata();
                    IBlockState state = block.getBlockState().getValidStates().get(meta);
                    blockToFilter.put(slot, state);
                    return super.insertItem(slot, stack, simulate);
                }
                return stack;
            }
        };
    }

    @Override
    public void initUI(Consumer<Widget> widgetGroup) {
        for (int i = 0; i < 50; i++) {
            widgetGroup.accept(new PhantomSlotWidget(itemFilterSlots, i, 9 + 18 * (i % 10), 30 + 18 * (i / 10)) {
                @Override
                public ItemStack slotClick(int dragType, ClickType clickType, EntityPlayer player) {
                    ItemStack stack = super.slotClick(dragType, clickType, player);
                    if (stack.getCount() == 1) {
                        if (clickType == ClickType.PICKUP || clickType == ClickType.CLONE || clickType == ClickType.THROW) {
                            slotReference.getStack().setCount(0);
                            blockToFilter.remove(slotReference.getSlotIndex());
                        }
                    }
                    return stack;
                }

            }.setBackgroundTexture(GuiTextures.SLOT));
        }
    }
}
