package com.johny.tj.machines;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.PhantomSlotWidget;
import gregtech.api.util.LargeStackSizeItemStackHandler;
import gregtech.common.covers.filter.SimpleItemFilter;

import java.util.function.Consumer;

public class ExtendedItemFilter extends SimpleItemFilter {

    public ExtendedItemFilter() {
        this.itemFilterSlots = new LargeStackSizeItemStackHandler(50) {
            @Override
            public int getSlotLimit(int slot) {
                return getMaxStackSize();
            }
        };
    }

    @Override
    public void initUI(Consumer<Widget> widgetGroup) {
        for (int i = 0; i < 50; i++) {
            widgetGroup.accept(new PhantomSlotWidget(itemFilterSlots, i, 9 + 18 * (i % 10), 30 + 18 * (i / 10))
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
    }
}
