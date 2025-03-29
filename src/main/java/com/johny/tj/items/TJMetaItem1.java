package com.johny.tj.items;

import gregtech.api.items.materialitem.MaterialMetaItem;
import net.minecraft.item.ItemStack;

import static com.johny.tj.items.TJMetaItems.*;

public class TJMetaItem1 extends MaterialMetaItem {

    @Override
    public void registerSubItems() {
        CREATIVE_FLUID_COVER = addItem(1000, "creative.fluid.cover");
        CREATIVE_ITEM_COVER = addItem(1001, "creative.item.cover");
        CREATIVE_ENERGY_COVER = addItem(1002, "creative.energy.cover");
        LINKING_DEVICE = addItem(1003,"item.linking.device").addComponents(new LinkingDeviceBehavior()).setMaxStackSize(1);
        VOID_PLUNGER = addItem(1004, "void_plunger").addComponents(new VoidPlungerBehaviour()).setMaxStackSize(1);
        NBT_READER = addItem(1005, "nbt_reader").addComponents(new NBTReaderBehaviour()).setMaxStackSize(1);
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        return stack.copy();
    }
}
