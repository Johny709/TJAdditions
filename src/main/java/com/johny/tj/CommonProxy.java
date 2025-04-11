package com.johny.tj;


import com.johny.tj.items.TJMetaItems;
import com.johny.tj.recipes.RecipeLoader;
import com.johny.tj.util.EnderWorldData;
import gregtech.common.blocks.VariantItemBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Function;

import static com.johny.tj.blocks.TJMetaBlocks.*;


@Mod.EventBusSubscriber(modid = TJ.MODID)
public class CommonProxy {

    public CommonProxy() {
    }
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();

        registry.register(SOLID_CASING);
        registry.register(ABILITY_CASING);
        registry.register(PIPE_CASING);
        registry.register(FUSION_CASING);
    }
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        registry.register(createItemBlock(SOLID_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(ABILITY_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(PIPE_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(FUSION_CASING, VariantItemBlock::new));
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        RecipeLoader.init();
    }


    @SubscribeEvent
    public static void registerOrePrefix(RegistryEvent.Register<IRecipe> event) {
        TJMetaItems.registerOreDict();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        MapStorage storage = event.getWorld().getMapStorage();
        WorldSavedData worldData = storage.getOrLoadData(EnderWorldData.class, "EnderWorldData");

        if (worldData == null) {
            storage.setData("EnderWorldData", new EnderWorldData("EnderWorldData"));
        }
        EnderWorldData.setInstance((EnderWorldData) worldData);

        EnderWorldData.init();
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        EnderWorldData.setDirty();
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        EnderWorldData.setDirty();
    }


    private static <T extends Block> ItemBlock createItemBlock(T block, Function<T, ItemBlock> producer) {
        ItemBlock itemBlock = producer.apply(block);
        itemBlock.setRegistryName(block.getRegistryName());
        return itemBlock;
    }

    public void onPreLoad() {
        TJMetaItems.init();
    }

    public void onLoad() {

    }
    public void onPostLoad() {
        TJRecipeMaps.multiRecipesInit();
    }
}
