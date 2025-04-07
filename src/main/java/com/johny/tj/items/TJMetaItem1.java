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

        UNIVERSAL_CIRCUITS[0] = ULV_UNIVERSAL_CIRCUIT = addItem(1006, GAValues.VN[0].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[0]);
        UNIVERSAL_CIRCUITS[1] = LV_UNIVERSAL_CIRCUIT = addItem(1007, GAValues.VN[1].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[1]);
        UNIVERSAL_CIRCUITS[2] = MV_UNIVERSAL_CIRCUIT = addItem(1008, GAValues.VN[2].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[2]);
        UNIVERSAL_CIRCUITS[3] = HV_UNIVERSAL_CIRCUIT = addItem(1009, GAValues.VN[3].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[3]);
        UNIVERSAL_CIRCUITS[4] = EV_UNIVERSAL_CIRCUIT = addItem(1010, GAValues.VN[4].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[4]);
        UNIVERSAL_CIRCUITS[5] = IV_UNIVERSAL_CIRCUIT = addItem(1011, GAValues.VN[5].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[5]);
        UNIVERSAL_CIRCUITS[6] = LUV_UNIVERSAL_CIRCUIT = addItem(10012, GAValues.VN[6].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[6]);
        UNIVERSAL_CIRCUITS[7] = ZPM_UNIVERSAL_CIRCUIT = addItem(1013, GAValues.VN[7].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[7]);
        UNIVERSAL_CIRCUITS[8] = UV_UNIVERSAL_CIRCUIT = addItem(1014, GAValues.VN[8].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[8]);
        UNIVERSAL_CIRCUITS[9] = UHV_UNIVERSAL_CIRCUIT = addItem(1015, GAValues.VN[9].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[9]);
        UNIVERSAL_CIRCUITS[10] = UEV_UNIVERSAL_CIRCUIT = addItem(1016, GAValues.VN[10].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[10]);
        UNIVERSAL_CIRCUITS[11] = UIV_UNIVERSAL_CIRCUIT = addItem(1017, GAValues.VN[11].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[11]);
        UNIVERSAL_CIRCUITS[12] = UMV_UNIVERSAL_CIRCUIT = addItem(1018, GAValues.VN[12].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[12]);
        UNIVERSAL_CIRCUITS[13] = UXV_UNIVERSAL_CIRCUIT = addItem(1019, GAValues.VN[13].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[13]);
        UNIVERSAL_CIRCUITS[14] = MAX_UNIVERSAL_CIRCUIT = addItem(1020, GAValues.VN[14].toLowerCase() + "_universal_circuit").setUnificationData(OrePrefix.circuit, CIRCUIT_TIERS[14]);
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        return stack.copy();
    }
}
