package com.johny.tj.items;

import gregtech.api.items.metaitem.MetaItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class TJMetaItems {

    public static List<MetaItem<?>> ITEMS = MetaItem.getMetaItems();

    public static MetaItem<?>.MetaValueItem CREATIVE_FLUID_COVER;
    public static MetaItem<?>.MetaValueItem CREATIVE_ITEM_COVER;
    public static MetaItem<?>.MetaValueItem CREATIVE_ENERGY_COVER;
    public static MetaItem<?>.MetaValueItem LINKING_DEVICE;
    public static MetaItem<?>.MetaValueItem VOID_PLUNGER;
    public static MetaItem<?>.MetaValueItem NBT_READER;

    public static MetaItem<?>.MetaValueItem ULV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem LV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem MV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem HV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem EV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem IV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem LUV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem ZPM_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem UV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem UHV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem UEV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem UIV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem UMV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem UXV_UNIVERSAL_CIRCUIT;
    public static MetaItem<?>.MetaValueItem MAX_UNIVERSAL_CIRCUIT;
    public static final MetaItem<?>.MetaValueItem[] UNIVERSAL_CIRCUITS = new MetaItem.MetaValueItem[15];

    public static void init() {
        TJMetaItem1 item = new TJMetaItem1();
        item.setRegistryName("meta_item");
    }

    public static void registerOreDict() {
        for (MetaItem<?> item : ITEMS) {
            if (item instanceof TJMetaItem1) {
                ((TJMetaItem1) item).registerOreDict();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        MinecraftForge.EVENT_BUS.register(TJMetaItems.class);
        for (MetaItem<?> item : ITEMS) {
            item.registerModels();
        }
    }
}
