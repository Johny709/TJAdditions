package com.johny.tj.machines;

import com.johny.tj.TJ;
import com.johny.tj.TJConfig;
import com.johny.tj.TJRecipeMaps;
import com.johny.tj.machines.multi.electric.*;
import com.johny.tj.machines.multi.steam.*;
import com.johny.tj.multiblockpart.rotorholder.MetaTileEntityRotorHolderForNuclearCoolantUHVPlus;
import com.johny.tj.multiblockpart.rotorholder.MetaTileEntityRotorHolderUHVPlus;
import com.johny.tj.multiblockpart.rotorholder.MetaTileEntityTJMultiFluidHatch;
import com.johny.tj.multiblockpart.utility.MetaTileEntityMachineController;
import gregicadditions.GAValues;
import gregicadditions.machines.multi.nuclear.MetaTileEntityHotCoolantTurbine;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.GTValues;
import gregtech.api.GregTechAPI;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.util.ResourceLocation;

public class TJMetaTileEntities {

    public static MetaTileEntityPrimitiveAlloy PRIMITIVE_ALLOY;
    public static MetaTileEntityCokeOven COKE_OVEN;
    public static MetaTileEntityMegaCokeOven MEGA_COKE_OVEN;
    public static MetaTileEntityHeatExchanger HEAT_EXCHANGER;
    public static MetaTileEntityArmorInfuser ARMOR_INFUSER;
    public static MetaTileEntityDragonReplicator DRAGON_REPLICATOR;
    public static MetaTileEntityChaosReplicator CHAOS_REPLICATOR;
    public static MetaTileEntityLargePoweredSpawner LARGE_POWERED_SPAWNER;
    public static MetaTileEntityLargeVialProcessor LARGE_VIAL_PROCESSOR;
    public static MetaTileEntityMegaBoiler MEGA_TUNGSTENSTEEL_BOILER;
    public static MetaTileEntityXLTurbine XL_STEAM_TURBINE;
    public static MetaTileEntityXLTurbine XL_GAS_TURBINE;
    public static MetaTileEntityXLTurbine XL_PLASMA_TURBINE;
    public static MetaTileEntityXLHotCoolantTurbine XL_COOLANT_TURBINE;
    public static MetaTileEntityRotorHolderUHVPlus ROTOR_HOLDER_UMV;
    public static MetaTileEntityRotorHolderForNuclearCoolantUHVPlus COOLANT_ROTOR_HOLDER_UMV;
    public static MetaTileEntityLargeDecayChamber LARGE_DECAY_CHAMBER;
    public static MetaTileEntityLargeAlloySmelter LARGE_ALLOY_SMELTER;
    public static MetaTileEntityIndustrialFusionReactor INDUSTRIAL_FUSION_REACTOR_UV;
    public static MetaTileEntityParallelLargeChemicalReactor PARALLEL_CHEMICAL_REACTOR;
    public static MetaTileEntityTJMultiFluidHatch INPUT_HATCH_MULTI_MAX;
    public static MetaTileEntityTJMultiFluidHatch OUTPUT_HATCH_MULTI_MAX;
    public static MetaTileEntityLargeGreenhouse LARGE_GREENHOUSE;
    public static MetaTileEntityLargeArchitectWorkbench LARGE_ARCHITECT_WORKBENCH;
    public static MetaTileEntityMachineController MACHINE_CONTROLLER;

    public static void init() {

        if (TJConfig.machines.replaceCTMultis) {
            COKE_OVEN = GregTechAPI.registerMetaTileEntity(1000, new MetaTileEntityCokeOven(gregtechId("coke_oven_2")));
            PRIMITIVE_ALLOY = GregTechAPI.registerMetaTileEntity(1002, new MetaTileEntityPrimitiveAlloy(gregtechId("primitive_alloy")));
            HEAT_EXCHANGER = GregTechAPI.registerMetaTileEntity(1003, new MetaTileEntityHeatExchanger(gregtechId("heat_exchanger")));
            ARMOR_INFUSER = GregTechAPI.registerMetaTileEntity(1004, new MetaTileEntityArmorInfuser(gregtechId("armor_infuser")));
            CHAOS_REPLICATOR = GregTechAPI.registerMetaTileEntity(1005, new MetaTileEntityChaosReplicator(gregtechId("chaos_replicator")));
            DRAGON_REPLICATOR = GregTechAPI.registerMetaTileEntity(1006, new MetaTileEntityDragonReplicator(gregtechId("dragon_egg_replicator")));
            LARGE_POWERED_SPAWNER = GregTechAPI.registerMetaTileEntity(4201, new MetaTileEntityLargePoweredSpawner(gregtechId("large_powered_spawner")));
            LARGE_VIAL_PROCESSOR = GregTechAPI.registerMetaTileEntity(4202, new MetaTileEntityLargeVialProcessor(gregtechId("large_vial_processor")));
        }

        MEGA_COKE_OVEN = GregTechAPI.registerMetaTileEntity(32000, new MetaTileEntityMegaCokeOven(TJId("mega_coke_oven")));
        MEGA_TUNGSTENSTEEL_BOILER = GregTechAPI.registerMetaTileEntity(4205, new MetaTileEntityMegaBoiler(TJId("mega_boiler"), MetaTileEntityLargeBoiler.BoilerType.TUNGSTENSTEEL, 256));

        XL_STEAM_TURBINE = GregTechAPI.registerMetaTileEntity(4206, new MetaTileEntityXLTurbine(TJId("xl_turbine.steam"), MetaTileEntityLargeTurbine.TurbineType.STEAM));
        XL_GAS_TURBINE = GregTechAPI.registerMetaTileEntity(4207, new MetaTileEntityXLTurbine(TJId("xl_turbine.gas"), MetaTileEntityLargeTurbine.TurbineType.GAS));
        XL_PLASMA_TURBINE = GregTechAPI.registerMetaTileEntity(4208, new MetaTileEntityXLTurbine(TJId("xl_turbine.plasma"), MetaTileEntityLargeTurbine.TurbineType.PLASMA));
        XL_COOLANT_TURBINE = GregTechAPI.registerMetaTileEntity(4209, new MetaTileEntityXLHotCoolantTurbine(TJId("xl_turbine.coolant"), MetaTileEntityHotCoolantTurbine.TurbineType.HOT_COOLANT));
        ROTOR_HOLDER_UMV = GregTechAPI.registerMetaTileEntity(5000, new MetaTileEntityRotorHolderUHVPlus(TJId("rotor_holder.umv"), GAValues.UMV, 2.5f));
        COOLANT_ROTOR_HOLDER_UMV = GregTechAPI.registerMetaTileEntity(5001, new MetaTileEntityRotorHolderForNuclearCoolantUHVPlus(TJId("coolant_rotor_holder.umv"), GAValues.UMV, 2.5f));
        LARGE_DECAY_CHAMBER = GregTechAPI.registerMetaTileEntity(5002, new MetaTileEntityLargeDecayChamber(TJId("large_decay_chamber")));
        LARGE_ALLOY_SMELTER = GregTechAPI.registerMetaTileEntity(5003, new MetaTileEntityLargeAlloySmelter(TJId("large_alloy_smelter")));
        LARGE_GREENHOUSE = GregTechAPI.registerMetaTileEntity(5004, new MetaTileEntityLargeGreenhouse(TJId("large_greenhouse"), GARecipeMaps.GREEN_HOUSE_RECIPES));

        INDUSTRIAL_FUSION_REACTOR_UV = GregTechAPI.registerMetaTileEntity(5005, new MetaTileEntityIndustrialFusionReactor(TJId("industrial_fusion_reactor.uv"), 8));
        PARALLEL_CHEMICAL_REACTOR = GregTechAPI.registerMetaTileEntity(5006, new MetaTileEntityParallelLargeChemicalReactor(TJId("parallel_chemical_reactor")));
        LARGE_ARCHITECT_WORKBENCH = GregTechAPI.registerMetaTileEntity(5007, new MetaTileEntityLargeArchitectWorkbench(TJId("large_architect_workbench"), TJRecipeMaps.ARCHITECT_RECIPES));

        INPUT_HATCH_MULTI_MAX = GregTechAPI.registerMetaTileEntity(5008, new MetaTileEntityTJMultiFluidHatch(TJId("fluid_input_multi_max"), 14, false, Integer.MAX_VALUE));
        OUTPUT_HATCH_MULTI_MAX = GregTechAPI.registerMetaTileEntity(5009, new MetaTileEntityTJMultiFluidHatch(TJId("fluid_output_multi_max"), 14, true, Integer.MAX_VALUE));
        MACHINE_CONTROLLER = GregTechAPI.registerMetaTileEntity(5010, new MetaTileEntityMachineController(TJId("machine_controller")));

    }

    private static ResourceLocation gregtechId(String name) {
        return new ResourceLocation(GTValues.MODID, name);
    }

    private static ResourceLocation TJId(String name) {
        return new ResourceLocation(TJ.MODID, name);
    }
}
