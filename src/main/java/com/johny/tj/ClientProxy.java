package com.johny.tj;


import codechicken.lib.texture.TextureUtils;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.items.TJMetaItems;
import com.johny.tj.textures.TJTextures;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public void onPreLoad() {
        super.onPreLoad();
        TextureUtils.addIconRegister(TJTextures::register);
    }

    @Override
    public void onLoad() {

    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        TJMetaBlocks.registerItemModels();
        TJMetaItems.registerModels();
    }
}
