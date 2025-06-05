package com.johny.tj.integration.jei;

import com.google.common.collect.ImmutableMap;
import com.johny.tj.TJ;
import com.johny.tj.TJConfig;
import com.johny.tj.integration.jei.multi.*;
import com.johny.tj.machines.TJMetaTileEntities;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler.BoilerType;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine.TurbineType;
import gregtech.integration.jei.multiblock.MultiblockInfoRecipeWrapper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.gui.recipes.RecipeLayout;
import net.minecraft.client.resources.I18n;

import static com.johny.tj.machines.TJMetaTileEntities.LARGE_WIRELESS_ENERGY_EMITTER;
import static com.johny.tj.machines.TJMetaTileEntities.LARGE_WIRELESS_ENERGY_RECEIVER;
import static com.johny.tj.machines.multi.electric.MetaTileEntityLargeWirelessEnergyEmitter.TransferType.INPUT;
import static com.johny.tj.machines.multi.electric.MetaTileEntityLargeWirelessEnergyEmitter.TransferType.OUTPUT;

public class TJMultiblockInfoCategory implements IRecipeCategory<MultiblockInfoRecipeWrapper> {
    private final IDrawable background;
    private final IGuiHelper guiHelper;
    private static ImmutableMap<String, MultiblockInfoRecipeWrapper> multiblockRecipes;

    public TJMultiblockInfoCategory(IJeiHelpers helpers) {
        this.guiHelper = helpers.getGuiHelper();
        this.background = this.guiHelper.createBlankDrawable(176, 166);
    }

    public static ImmutableMap<String, MultiblockInfoRecipeWrapper> getMultiblockRecipes() {
        if (multiblockRecipes == null) {
            ImmutableMap.Builder<String, MultiblockInfoRecipeWrapper> multiblockRecipes = new ImmutableMap.Builder<>();

                    if (TJConfig.machines.replaceCTMultis) {
                    multiblockRecipes.put("primitive_alloy", new MultiblockInfoRecipeWrapper(new PrimitiveAlloyInfo()))
                                .put("coke_oven", new MultiblockInfoRecipeWrapper(new CokeOvenInfo()))
                                .put("heat_exchanger", new MultiblockInfoRecipeWrapper(new HeatExchangerInfo()))
                                .put("armor_infuser", new MultiblockInfoRecipeWrapper(new ArmorInfuserInfo()))
                                .put("dragon_egg_replicator", new MultiblockInfoRecipeWrapper(new DragonReplicatorInfo()))
                                .put("chaos_replicator", new MultiblockInfoRecipeWrapper(new ChaosReplicatorInfo()))
                                .put("large_powered_spawner", new MultiblockInfoRecipeWrapper(new LargePoweredSpawnerInfo()))
                                .put("large_vial_processor", new MultiblockInfoRecipeWrapper(new LargeVialProcessorInfo()));
                    }

                    multiblockRecipes.put("mega_coke_oven", new MultiblockInfoRecipeWrapper(new MegaCokeOvenInfo()))
                            .put("mega_bronze_boiler", new MultiblockInfoRecipeWrapper(new MegaBoilerInfo(BoilerType.BRONZE, TJMetaTileEntities.MEGA_BOILER[0])))
                            .put("mega_steel_boiler", new MultiblockInfoRecipeWrapper(new MegaBoilerInfo(BoilerType.STEEL, TJMetaTileEntities.MEGA_BOILER[1])))
                            .put("mega_titanium_boiler", new MultiblockInfoRecipeWrapper(new MegaBoilerInfo(BoilerType.TITANIUM, TJMetaTileEntities.MEGA_BOILER[2])))
                            .put("mega_tungstensteel_boiler", new MultiblockInfoRecipeWrapper(new MegaBoilerInfo(BoilerType.TUNGSTENSTEEL, TJMetaTileEntities.MEGA_BOILER[3])))
                            .put("xl_turbine.steam", new MultiblockInfoRecipeWrapper(new XLTurbineInfo(TJMetaTileEntities.XL_STEAM_TURBINE)))
                            .put("xl_turbine.gas", new MultiblockInfoRecipeWrapper(new XLTurbineInfo(TJMetaTileEntities.XL_GAS_TURBINE)))
                            .put("xl_turbine.plasma", new MultiblockInfoRecipeWrapper(new XLTurbineInfo(TJMetaTileEntities.XL_PLASMA_TURBINE)))
                            .put("xl_turbine.coolant", new MultiblockInfoRecipeWrapper(new XLHotCoolantTurbineInfo(TJMetaTileEntities.XL_COOLANT_TURBINE)))
                            .put("large_decay_chamber", new MultiblockInfoRecipeWrapper(new LargeDecayChamberInfo()))
                            .put("large_alloy_smelter", new MultiblockInfoRecipeWrapper(new LargeAlloySmelterInfo()))
                            .put("industrial_fusion_reactor.luv", new MultiblockInfoRecipeWrapper(new IndustrialFusionReactorInfo(TJMetaTileEntities.INDUSTRIAL_FUSION_REACTOR_LUV)))
                            .put("industrial_fusion_reactor.zpm", new MultiblockInfoRecipeWrapper(new IndustrialFusionReactorInfo(TJMetaTileEntities.INDUSTRIAL_FUSION_REACTOR_ZPM)))
                            .put("industrial_fusion_reactor.uv", new MultiblockInfoRecipeWrapper(new IndustrialFusionReactorInfo(TJMetaTileEntities.INDUSTRIAL_FUSION_REACTOR_UV)))
                            .put("industrial_fusion_reactor.uhv", new MultiblockInfoRecipeWrapper(new IndustrialFusionReactorInfo(TJMetaTileEntities.INDUSTRIAL_FUSION_REACTOR_UHV)))
                            .put("industrial_fusion_reactor.uev", new MultiblockInfoRecipeWrapper(new IndustrialFusionReactorInfo(TJMetaTileEntities.INDUSTRIAL_FUSION_REACTOR_UEV)))
                            .put("parallel_chemical_reactor", new MultiblockInfoRecipeWrapper(new ParallelChemicalReactorInfo()))
                            .put("large_greenhouse", new MultiblockInfoRecipeWrapper(new LargeGreenhouseInfo()))
                            .put("large_architect_workbench", new MultiblockInfoRecipeWrapper(new LargeArchitectWorkbenchInfo()))
                            .put("elite_large_miner", new MultiblockInfoRecipeWrapper(new LargeMinerInfo(TJMetaTileEntities.ELITE_LARGE_MINER)))
                            .put("ultimate_large_miner", new MultiblockInfoRecipeWrapper(new LargeMinerInfo(TJMetaTileEntities.ULTIMATE_LARGE_MINER)))
                            .put("world_destroyer", new MultiblockInfoRecipeWrapper(new LargeMinerInfo(TJMetaTileEntities.WORLD_DESTROYER)))
                            .put("large_world_accelerator", new MultiblockInfoRecipeWrapper(new LargeWorldAcceleratorInfo()))
                            .put("large_rock_breaker", new MultiblockInfoRecipeWrapper(new LargeRockBreakerInfo()))
                            .put("steam_air_collector_turbine", new MultiblockInfoRecipeWrapper(new LargeAtmosphereCollectorInfo(TurbineType.STEAM, TJMetaTileEntities.LARGE_ATMOSPHERE_COLLECTOR[0])))
                            .put("gas_air_collector_turbine", new MultiblockInfoRecipeWrapper(new LargeAtmosphereCollectorInfo(TurbineType.GAS, TJMetaTileEntities.LARGE_ATMOSPHERE_COLLECTOR[1])))
                            .put("plasma_air_collector_turbine", new MultiblockInfoRecipeWrapper(new LargeAtmosphereCollectorInfo(TurbineType.PLASMA, TJMetaTileEntities.LARGE_ATMOSPHERE_COLLECTOR[2])))
                            .put("infinite_fluid_drill", new MultiblockInfoRecipeWrapper(new InfiniteFluidDrillInfo()))
                            .put("industrial_steam_engine", new MultiblockInfoRecipeWrapper(new IndustrialSteamEngineInfo()))
                            .put("advanced_parallel_chemical_reactor", new MultiblockInfoRecipeWrapper(new ParallelAdvancedChemicalReactorInfo()))
                            .put("parallel_large_macerator", new MultiblockInfoRecipeWrapper(new ParallelLargeMaceratorInfo()))
                            .put("parallel_large_washing_machine", new MultiblockInfoRecipeWrapper(new ParallelLargeWashingMachineInfo()))
                            .put("parallel_large_centrifuge", new MultiblockInfoRecipeWrapper(new ParallelLargeCentrifugeInfo()))
                            .put("parallel_large_electrolyzer", new MultiblockInfoRecipeWrapper(new ParallelLargeElectrolyzerInfo()))
                            .put("parallel_large_sifter", new MultiblockInfoRecipeWrapper(new ParallelLargeSifterInfo()))
                            .put("parallel_large_brewery", new MultiblockInfoRecipeWrapper(new ParallelLargeBreweryInfo()))
                            .put("parallel_large_arc_furnace", new MultiblockInfoRecipeWrapper(new ParallelLargeArcFurnaceInfo()))
                            .put("parallel_large_assembler", new MultiblockInfoRecipeWrapper(new ParallelLargeAssemblerInfo()))
                            .put("parallel_large_canning_machine", new MultiblockInfoRecipeWrapper(new ParallelLargeCanningMachineInfo()))
                            .put("large_wireless_energy_emitter", new MultiblockInfoRecipeWrapper(new LargeWirelessEnergyEmitterInfo(INPUT, LARGE_WIRELESS_ENERGY_EMITTER)))
                            .put("large_wireless_energy_receiver", new MultiblockInfoRecipeWrapper(new LargeWirelessEnergyEmitterInfo(OUTPUT, LARGE_WIRELESS_ENERGY_RECEIVER)))
                            .put("large_battery_charger", new MultiblockInfoRecipeWrapper(new LargeBatteryChargerInfo()))
                            .put("void_more_miner", new MultiblockInfoRecipeWrapper(new VoidMOreMinerInfo()))
                            .put("teleporter", new MultiblockInfoRecipeWrapper(new TeleporterInfo()));
                    return TJMultiblockInfoCategory.multiblockRecipes = multiblockRecipes.build();
        }
        return multiblockRecipes;
    }

    public static void registerRecipes(IModRegistry registry) {
        registry.addRecipes(getMultiblockRecipes().values(), "gregtech:multiblock_info");
    }
    @Override
    public String getUid() {
        return "tj:multiblock_info";
    }
    @Override
    public String getTitle() {
        return I18n.format("gregtech.multiblock.title");
    }
    @Override
    public String getModName() {
        return TJ.MODID;
    }
    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, MultiblockInfoRecipeWrapper recipeWrapper, IIngredients ingredients) {
        recipeWrapper.setRecipeLayout((RecipeLayout) iRecipeLayout, guiHelper);

        IGuiItemStackGroup itemStackGroup = iRecipeLayout.getItemStacks();
        itemStackGroup.addTooltipCallback(recipeWrapper::addBlockTooltips);
    }
}
