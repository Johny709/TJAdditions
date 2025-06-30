package tj;


import tj.capability.LinkEvent;
import tj.event.MTELinkEvent;
import tj.items.TJMetaItems;
import tj.recipes.LateRecipes;
import tj.recipes.RecipeInit;
import tj.util.EnderWorldData;
import tj.util.PlayerWorldIDData;
import gregtech.api.metatileentity.MetaTileEntity;
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
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.UUID;
import java.util.function.Function;

import static tj.blocks.TJMetaBlocks.*;


@Mod.EventBusSubscriber(modid = TJ.MODID)
public class CommonProxy {

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();

        registry.register(SOLID_CASING);
        registry.register(ABILITY_CASING);
        registry.register(PIPE_CASING);
        registry.register(FUSION_CASING);
        registry.register(FUSION_GLASS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        registry.register(createItemBlock(SOLID_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(ABILITY_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(PIPE_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(FUSION_CASING, VariantItemBlock::new));
        registry.register(createItemBlock(FUSION_GLASS, VariantItemBlock::new));
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        RecipeInit.init();
    }


    @SubscribeEvent
    public static void registerOrePrefix(RegistryEvent.Register<IRecipe> event) {
        TJMetaItems.registerOreDict();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        MapStorage storage = event.getWorld().getMapStorage();
        WorldSavedData worldData = storage.getOrLoadData(EnderWorldData.class, "EnderWorldData");
        WorldSavedData playerWorldData = storage.getOrLoadData(PlayerWorldIDData.class, "PlayerWorldListData");

        if (worldData == null) {
            storage.setData("EnderWorldData", new EnderWorldData("EnderWorldData"));
        }
        if (playerWorldData == null) {
            storage.setData("PlayerWorldListData", new PlayerWorldIDData("PlayerWorldListData"));
        }
        EnderWorldData.setInstance((EnderWorldData) worldData);
        PlayerWorldIDData.setInstance((PlayerWorldIDData) playerWorldData);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        EnderWorldData.setDirty();
        PlayerWorldIDData.setDirty();
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        EnderWorldData.setDirty();
        PlayerWorldIDData.setDirty();
    }

    @SubscribeEvent
    public static void onLink(MTELinkEvent event) {
        MetaTileEntity transmitter = event.getTransmitter();
        MetaTileEntity receiver = event.getReceiver();
        if (transmitter instanceof LinkEvent)
            ((LinkEvent) transmitter).onLink(receiver);
        if (receiver instanceof LinkEvent)
            ((LinkEvent) receiver).onLink(transmitter);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        UUID player = event.player.getUniqueID();
        int worldID = event.player.world.provider.getDimension();
        PlayerWorldIDData.getPlayerWorldIDMap().put(player, worldID);
    }

    @SubscribeEvent
    public static void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        UUID player = event.player.getUniqueID();
        int worldID = event.toDim;
        PlayerWorldIDData.getPlayerWorldIDMap().put(player, worldID);
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
        TJRecipeMaps.parallelRecipesInit();
        LateRecipes.init();
    }
}
