package com.johny.tj.items;

import gregicadditions.GAValues;
import gregtech.api.items.materialitem.MaterialMetaItem;
import gregtech.api.unification.ore.OrePrefix;
import net.minecraft.item.ItemStack;

import static com.johny.tj.TJValues.CIRCUIT_TIERS;
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
        FLUID_REGULATOR_UHV = addItem(1047, "fluid.regulator.uhv");

        for (int i = 0; i < UNIVERSAL_CIRCUITS.length; i++) { // occupies range 1006 - 1021
            UNIVERSAL_CIRCUITS[i] = addItem(1006 + i, GAValues.VN[i].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[i]);
        }
        int enderCoverID = 1022; // occupies range 1022 - 1046
        for (int i = 0; i < ENDER_FLUID_COVERS.length; i++) {
            ENDER_FLUID_COVERS[i] = addItem(enderCoverID++, "ender_fluid_cover_" + GAValues.VN[i + 3].toLowerCase()).addComponents(new EnderCoverBehaviour(EnderCoverBehaviour.EnderCoverType.FLUID,i + 3));
            ENDER_ITEM_COVERS[i] = addItem(enderCoverID++, "ender_item_cover_" + GAValues.VN[i + 3].toLowerCase()).addComponents(new EnderCoverBehaviour(EnderCoverBehaviour.EnderCoverType.ITEM, i + 3));
        }
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        return stack.copy();
    }
}
