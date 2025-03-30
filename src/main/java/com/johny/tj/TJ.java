package com.johny.tj;

import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.capability.TJSimpleCapabilityManager;
import com.johny.tj.integration.theoneprobe.TheOneProbeCompatibility;
import com.johny.tj.items.TJCoverBehaviours;
import com.johny.tj.machines.TJMetaTileEntities;
import gregtech.api.GTValues;
import gregtech.api.util.GTLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;


@Mod(modid = TJ.MODID, name = TJ.NAME, version = TJ.VERSION)

public class TJ
{

    public static final String MODID = "tj";
    public static final String NAME = "TJ";
    public static final String VERSION = "1.0";

    @SidedProxy(modId = MODID, clientSide = "com.johny.tj.ClientProxy", serverSide = "com.johny.tj.CommonProxy")
    public static CommonProxy proxy;

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        proxy.onPreLoad();
        TJMetaBlocks.init();
        TJMetaTileEntities.init();
        TJSimpleCapabilityManager.init();
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.onLoad();
        if (GTValues.isModLoaded(GTValues.MODID_TOP)) {
            GTLog.logger.info("TheOneProbe found. Enabling integration...");
            TheOneProbeCompatibility.registerCompatibility();
        }
        TJCoverBehaviours.init();
    }
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.onPostLoad();
    }
}
