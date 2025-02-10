package com.johny.tj.machines;

import com.johny.tj.TJ;
import com.johny.tj.TJConfig;
import com.johny.tj.TJRecipeMaps;
import com.johny.tj.machines.multi.electric.*;
import com.johny.tj.machines.multi.steam.*;
import com.johny.tj.multiblockpart.rotorholder.MetaTileEntityRotorHolderForNuclearCoolantUHVPlus;
import com.johny.tj.multiblockpart.rotorholder.MetaTileEntityRotorHolderUHVPlus;
import com.johny.tj.multiblockpart.utility.MetaTileEntityMachineController;
import com.johny.tj.multiblockpart.utility.MetaTileEntityTJMultiFluidHatch;
import gregicadditions.GAValues;
import gregicadditions.machines.multi.multiblockpart.GAMetaTileEntityEnergyHatch;
import gregicadditions.machines.multi.nuclear.MetaTileEntityHotCoolantTurbine;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.GTValues;
import gregtech.api.GregTechAPI;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.util.ResourceLocation;

import static gregicadditions.machines.GATileEntities.*;

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
    public static MetaTileEntityMegaBoiler[] MEGA_BOILER = new MetaTileEntityMegaBoiler[4];
    public static MetaTileEntityXLTurbine XL_STEAM_TURBINE;
    public static MetaTileEntityXLTurbine XL_GAS_TURBINE;
    public static MetaTileEntityXLTurbine XL_PLASMA_TURBINE;
    public static MetaTileEntityXLHotCoolantTurbine XL_COOLANT_TURBINE;
    public static MetaTileEntityRotorHolderUHVPlus ROTOR_HOLDER_UMV;
    public static MetaTileEntityRotorHolderForNuclearCoolantUHVPlus COOLANT_ROTOR_HOLDER_UMV;
    public static MetaTileEntityLargeDecayChamber LARGE_DECAY_CHAMBER;
    public static MetaTileEntityLargeAlloySmelter LARGE_ALLOY_SMELTER;
    public static MetaTileEntityIndustrialFusionReactor INDUSTRIAL_FUSION_REACTOR_LUV;
    public static MetaTileEntityIndustrialFusionReactor INDUSTRIAL_FUSION_REACTOR_ZPM;
    public static MetaTileEntityIndustrialFusionReactor INDUSTRIAL_FUSION_REACTOR_UV;
    public static MetaTileEntityParallelLargeChemicalReactor PARALLEL_CHEMICAL_REACTOR;
    public static MetaTileEntityTJMultiFluidHatch QUADRUPLE_QUADRUPLE_INPUT_HATCH;
    public static MetaTileEntityTJMultiFluidHatch QUADRUPLE_QUADRUPLE_OUTPUT_HATCH;
    public static MetaTileEntityLargeGreenhouse LARGE_GREENHOUSE;
    public static MetaTileEntityLargeArchitectWorkbench LARGE_ARCHITECT_WORKBENCH;
    public static MetaTileEntityMachineController MACHINE_CONTROLLER;
    public static MetaTileEntityEliteLargeMiner ELITE_LARGE_MINER;
    public static MetaTileEntityUltimateLargeMiner ULTIMATE_LARGE_MINER;
    public static MetaTileEntityWorldDestroyer WORLD_DESTROYER;
    public static MetaTileEntityLargeWorldAccelerator LARGE_WORLD_ACCELERATOR;
    public static MetaTileEntityLargeRockBreaker LARGE_ROCK_BREAKER;
    public static MetaTileEntityInfiniteFluidDrill INFINITE_FLUID_DRILL;
    public static GAMetaTileEntityEnergyHatch[] ENERGY_INPUT_HATCH_256A = new GAMetaTileEntityEnergyHatch[14];
    public static GAMetaTileEntityEnergyHatch[] ENERGY_OUTPUT_HATCH_256A = new GAMetaTileEntityEnergyHatch[14];
    public static MetaTileEntityLargeAtmosphereCollector[] LARGE_ATMOSPHERE_COLLECTOR = new MetaTileEntityLargeAtmosphereCollector[3];

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

        MEGA_COKE_OVEN = GregTechAPI.registerMetaTileEntity(4205, new MetaTileEntityMegaCokeOven(TJId("mega_coke_oven")));
        XL_STEAM_TURBINE = GregTechAPI.registerMetaTileEntity(4206, new MetaTileEntityXLTurbine(TJId("xl_turbine.steam"), MetaTileEntityLargeTurbine.TurbineType.STEAM));
        XL_GAS_TURBINE = GregTechAPI.registerMetaTileEntity(4207, new MetaTileEntityXLTurbine(TJId("xl_turbine.gas"), MetaTileEntityLargeTurbine.TurbineType.GAS));
        XL_PLASMA_TURBINE = GregTechAPI.registerMetaTileEntity(4208, new MetaTileEntityXLTurbine(TJId("xl_turbine.plasma"), MetaTileEntityLargeTurbine.TurbineType.PLASMA));
        XL_COOLANT_TURBINE = GregTechAPI.registerMetaTileEntity(4209, new MetaTileEntityXLHotCoolantTurbine(TJId("xl_turbine.coolant"), MetaTileEntityHotCoolantTurbine.TurbineType.HOT_COOLANT));
        ROTOR_HOLDER_UMV = GregTechAPI.registerMetaTileEntity(5000, new MetaTileEntityRotorHolderUHVPlus(TJId("rotor_holder.umv"), GAValues.UMV, 2.5f));
        COOLANT_ROTOR_HOLDER_UMV = GregTechAPI.registerMetaTileEntity(5001, new MetaTileEntityRotorHolderForNuclearCoolantUHVPlus(TJId("coolant_rotor_holder.umv"), GAValues.UMV, 2.5f));
        LARGE_DECAY_CHAMBER = GregTechAPI.registerMetaTileEntity(5002, new MetaTileEntityLargeDecayChamber(TJId("large_decay_chamber")));
        LARGE_ALLOY_SMELTER = GregTechAPI.registerMetaTileEntity(5003, new MetaTileEntityLargeAlloySmelter(TJId("large_alloy_smelter")));
        LARGE_GREENHOUSE = GregTechAPI.registerMetaTileEntity(5004, new MetaTileEntityLargeGreenhouse(TJId("large_greenhouse"), GARecipeMaps.GREEN_HOUSE_RECIPES));

        INDUSTRIAL_FUSION_REACTOR_LUV = GregTechAPI.registerMetaTileEntity(5005, new MetaTileEntityIndustrialFusionReactor(TJId("industrial_fusion_reactor.luv"), 6));
        INDUSTRIAL_FUSION_REACTOR_ZPM = GregTechAPI.registerMetaTileEntity(5006, new MetaTileEntityIndustrialFusionReactor(TJId("industrial_fusion_reactor.zpm"), 7));
        INDUSTRIAL_FUSION_REACTOR_UV = GregTechAPI.registerMetaTileEntity(5007, new MetaTileEntityIndustrialFusionReactor(TJId("industrial_fusion_reactor.uv"), 8));

        QUADRUPLE_QUADRUPLE_INPUT_HATCH = GregTechAPI.registerMetaTileEntity(5008, new MetaTileEntityTJMultiFluidHatch(TJId("fluid_input_quad_quad_luv"), 4, false, 64000));
        QUADRUPLE_QUADRUPLE_OUTPUT_HATCH = GregTechAPI.registerMetaTileEntity(5009, new MetaTileEntityTJMultiFluidHatch(TJId("fluid_output_quad_quad_luv"), 4, true, 64000));
        MACHINE_CONTROLLER = GregTechAPI.registerMetaTileEntity(5010, new MetaTileEntityMachineController(TJId("machine_controller")));
        ELITE_LARGE_MINER = GregTechAPI.registerMetaTileEntity(5011, new MetaTileEntityEliteLargeMiner(TJId("elite_large_miner"), TJMiner.Type.ELITE));
        ULTIMATE_LARGE_MINER = GregTechAPI.registerMetaTileEntity(5012, new MetaTileEntityUltimateLargeMiner(TJId("ultimate_large_miner"), TJMiner.Type.ULTIMATE));
        WORLD_DESTROYER = GregTechAPI.registerMetaTileEntity(5013, new MetaTileEntityWorldDestroyer(TJId("world_destroyer"), TJMiner.Type.DESTROYER));

        PARALLEL_CHEMICAL_REACTOR = GregTechAPI.registerMetaTileEntity(5014, new MetaTileEntityParallelLargeChemicalReactor(TJId("parallel_chemical_reactor")));
        LARGE_ARCHITECT_WORKBENCH = GregTechAPI.registerMetaTileEntity(5015, new MetaTileEntityLargeArchitectWorkbench(TJId("large_architect_workbench"), TJRecipeMaps.ARCHITECT_RECIPES));
        LARGE_WORLD_ACCELERATOR = GregTechAPI.registerMetaTileEntity(5052, new MetaTileEntityLargeWorldAccelerator(TJId("large_world_accelerator")));
        LARGE_ROCK_BREAKER = GregTechAPI.registerMetaTileEntity(5053, new MetaTileEntityLargeRockBreaker(TJId("large_rock_breaker")));

        MEGA_BOILER[0] = GregTechAPI.registerMetaTileEntity(5054, new MetaTileEntityMegaBoiler(TJId("mega_bronze_boiler"), MetaTileEntityLargeBoiler.BoilerType.BRONZE, 256));
        MEGA_BOILER[1] = GregTechAPI.registerMetaTileEntity(5055, new MetaTileEntityMegaBoiler(TJId("mega_steel_boiler"), MetaTileEntityLargeBoiler.BoilerType.STEEL, 256));
        MEGA_BOILER[2] = GregTechAPI.registerMetaTileEntity(5056, new MetaTileEntityMegaBoiler(TJId("mega_titanium_boiler"), MetaTileEntityLargeBoiler.BoilerType.TITANIUM, 256));
        MEGA_BOILER[3] = GregTechAPI.registerMetaTileEntity(5057, new MetaTileEntityMegaBoiler(TJId("mega_tungstensteel_boiler"), MetaTileEntityLargeBoiler.BoilerType.TUNGSTENSTEEL, 256));

        LARGE_ATMOSPHERE_COLLECTOR[0] = GregTechAPI.registerMetaTileEntity(5078, new MetaTileEntityLargeAtmosphereCollector(TJId("steam_air_collector_turbine"), MetaTileEntityLargeTurbine.TurbineType.STEAM));
        LARGE_ATMOSPHERE_COLLECTOR[1] = GregTechAPI.registerMetaTileEntity(5079, new MetaTileEntityLargeAtmosphereCollector(TJId("gas_air_collector_turbine"), MetaTileEntityLargeTurbine.TurbineType.GAS));
        LARGE_ATMOSPHERE_COLLECTOR[2] = GregTechAPI.registerMetaTileEntity(5080, new MetaTileEntityLargeAtmosphereCollector(TJId("plasma_air_collector_turbine"), MetaTileEntityLargeTurbine.TurbineType.PLASMA));

        INFINITE_FLUID_DRILL = GregTechAPI.registerMetaTileEntity(5081, new MetaTileEntityInfiniteFluidDrill(TJId("infinite_fluid_drill")));


        int energyHatchId = 5016; // occupies ID range 5016 - 5043
        for (int i = 0, tier = 1; tier < GAValues.VN.length; i++, tier++) {
            ENERGY_INPUT_HATCH_256A[i] = GregTechAPI.registerMetaTileEntity(energyHatchId++, new GAMetaTileEntityEnergyHatch(TJId("energy_input_256_" + GAValues.VN[tier]), tier, 256, false));
            ENERGY_OUTPUT_HATCH_256A[i] = GregTechAPI.registerMetaTileEntity(energyHatchId++, new GAMetaTileEntityEnergyHatch(TJId("energy_output_256_" + GAValues.VN[tier]), tier, 256, true));
        }

        ENERGY_INPUT_HATCH_4_AMPS.add(GregTechAPI.registerMetaTileEntity(5044, new GAMetaTileEntityEnergyHatch(location("energy_hatch.input.max.4"), 14, 4, false)));
        ENERGY_INPUT_HATCH_16_AMPS.add(GregTechAPI.registerMetaTileEntity(5045, new GAMetaTileEntityEnergyHatch(location("energy_hatch.input.max.16"), 14, 16, false)));
        ENERGY_OUTPUT_HATCH_16_AMPS.add(GregTechAPI.registerMetaTileEntity(5046, new GAMetaTileEntityEnergyHatch(location("energy_hatch.output.max.16"), 14, 16, true)));
        ENERGY_OUTPUT_HATCH_32_AMPS.add(GregTechAPI.registerMetaTileEntity(5047, new GAMetaTileEntityEnergyHatch(location("energy_hatch.output.max.32"), 14, 32, true)));
        ENERGY_INPUT_HATCH_64_AMPS.add(GregTechAPI.registerMetaTileEntity(5048, new GAMetaTileEntityEnergyHatch(location("energy_hatch.input.max.64"), 14, 64, false)));
        ENERGY_OUTPUT_HATCH_64_AMPS.add(GregTechAPI.registerMetaTileEntity(5049, new GAMetaTileEntityEnergyHatch(location("energy_hatch.output.max.64"), 14, 64, true)));
        ENERGY_INPUT_HATCH_128_AMPS.add(GregTechAPI.registerMetaTileEntity(5050, new GAMetaTileEntityEnergyHatch(location("energy_hatch.input.max.128"), 14, 128, false)));
        ENERGY_OUTPUT_HATCH_128_AMPS.add(GregTechAPI.registerMetaTileEntity(5051, new GAMetaTileEntityEnergyHatch(location("energy_hatch.output.max.128"), 14, 128, true)));

    }

    private static ResourceLocation gregtechId(String name) {
        return new ResourceLocation(GTValues.MODID, name);
    }

    private static ResourceLocation TJId(String name) {
        return new ResourceLocation(TJ.MODID, name);
    }
}
