package com.johny.tj.items;

import gregtech.api.items.materialitem.MaterialMetaItem;
import net.minecraft.item.ItemStack;

import static com.johny.tj.items.TJMetaItems.CREATIVE_FLUID_COVER;
import static com.johny.tj.items.TJMetaItems.CREATIVE_ITEM_COVER;

public class TJMetaItem1 extends MaterialMetaItem {

    @Override
    public void registerSubItems() {
        CREATIVE_FLUID_COVER = addItem(1000, "creative.fluid.cover");
        CREATIVE_ITEM_COVER = addItem(1001, "creative.item.cover");
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        return stack.copy();
    }
}
