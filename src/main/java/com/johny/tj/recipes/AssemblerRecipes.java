package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockAbilityCasings;
import com.johny.tj.blocks.BlockFusionCasings;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import gregicadditions.GAValues;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMetaItems;
import gregicadditions.item.fusion.GAFusionCasing;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import gregtech.api.unification.material.MarkerMaterials;
import gregtech.api.unification.material.type.IngotMaterial;
import gregtech.api.unification.material.type.Material;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import java.util.Objects;

import static com.johny.tj.items.TJMetaItems.*;
import static com.johny.tj.machines.TJMetaTileEntities.COKE_OVEN;
import static com.johny.tj.machines.TJMetaTileEntities.*;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.item.GAMetaItems.*;
import static gregicadditions.machines.GATileEntities.*;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;
import static gregtech.api.unification.material.MarkerMaterials.Tier.Infinite;
import static gregtech.api.unification.material.MarkerMaterials.Tier.Master;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.common.items.MetaItems.*;
import static gregtech.common.metatileentities.MetaTileEntities.*;

public class AssemblerRecipes {

    public static Material[][] materialTier = {{Steel, Aluminium, StainlessSteel, Titanium, TungstenSteel, RhodiumPlatedPalladium, IngotMaterial.MATERIAL_REGISTRY.getObject("star_metal_alloy"), Tritanium, Seaborgium, Bohrium, Adamantium, Vibranium, HeavyQuarkDegenerateMatter, Neutronium},
                                                {IngotMaterial.MATERIAL_REGISTRY.getObject("lv_superconductor"), MVSuperconductor, HVSuperconductor, EVSuperconductor, IVSuperconductor, LuVSuperconductor, ZPMSuperconductor, UVSuperconductor, UHVSuperconductor, UEVSuperconductor, UIVSuperconductor, UMVSuperconductor, UXVSuperconductor, MarkerMaterials.Tier.Superconductor}};
    public static MetaTileEntityLargeBoiler[] boilerType = {LARGE_BRONZE_BOILER, LARGE_STEEL_BOILER, LARGE_TITANIUM_BOILER, LARGE_TUNGSTENSTEEL_BOILER};

    public static void init() {
        MetaItem<?>.MetaValueItem[] emitters = {EMITTER_LV, EMITTER_MV, EMITTER_HV, EMITTER_EV, EMITTER_IV, EMITTER_LUV, EMITTER_ZPM, EMITTER_UV, EMITTER_UHV, EMITTER_UEV, EMITTER_UIV, EMITTER_UMV, EMITTER_UXV, EMITTER_MAX};
        MetaItem<?>.MetaValueItem[] sensors = {SENSOR_LV, SENSOR_MV, SENSOR_HV, SENSOR_EV, SENSOR_IV, SENSOR_LUV, SENSOR_ZPM, SENSOR_UV, SENSOR_UHV, SENSOR_UEV, SENSOR_UIV, SENSOR_UMV, SENSOR_UXV, SENSOR_MAX};
        MetaItem<?>.MetaValueItem[] pumps = {ELECTRIC_PUMP_LV, ELECTRIC_PUMP_MV, ELECTRIC_PUMP_HV, ELECTRIC_PUMP_EV, ELECTRIC_PUMP_IV, ELECTRIC_PUMP_LUV, ELECTRIC_PUMP_ZPM, ELECTRIC_PUMP_UV, ELECTRIC_PUMP_UHV, ELECTRIC_PUMP_UEV, ELECTRIC_PUMP_UIV, ELECTRIC_PUMP_UMV, ELECTRIC_PUMP_UXV, ELECTRIC_PUMP_MAX};
        MetaItem<?>.MetaValueItem[] conveyors = {CONVEYOR_MODULE_LV, CONVEYOR_MODULE_MV, CONVEYOR_MODULE_HV, CONVEYOR_MODULE_EV, CONVEYOR_MODULE_IV, CONVEYOR_MODULE_LUV, CONVEYOR_MODULE_ZPM, CONVEYOR_MODULE_UV, CONVEYOR_MODULE_UHV, CONVEYOR_MODULE_UEV, CONVEYOR_MODULE_UIV, CONVEYOR_MODULE_UMV, CONVEYOR_MODULE_UXV, CONVEYOR_MODULE_MAX};

        for (int i = 0; i < boilerType.length; i++) {
            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(boilerType[i].getStackForm(64))
                    .inputs(boilerType[i].getStackForm(64))
                    .inputs(boilerType[i].getStackForm(64))
                    .inputs(boilerType[i].getStackForm(64))
                    .outputs(MEGA_BOILER[i].getStackForm(1))
                    .EUt(GAValues.VA[2 + i])
                    .duration(1200)
                    .buildAndRegister();
        }

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.plate, Duranium, 6)
                .input(OrePrefix.frameGt, Duranium)
                .notConsumable(new IntCircuitIngredient(0))
                .outputs(TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.DURANIUM_CASING, 3))
                .duration(50)
                .EUt(16)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.plate, Seaborgium, 6)
                .input(OrePrefix.frameGt, Seaborgium)
                .notConsumable(new IntCircuitIngredient(0))
                .outputs(TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.SEABORGIUM_CASING, 3))
                .duration(50)
                .EUt(16)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.plate, TungstenTitaniumCarbide, 6)
                .input(OrePrefix.frameGt, TungstenTitaniumCarbide)
                .notConsumable(new IntCircuitIngredient(0))
                .outputs(TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.TUNGSTEN_TITANIUM_CARBIDE_CASING, 3))
                .duration(50)
                .EUt(16)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.pipeLarge, Naquadah, 9)
                .inputs(MetaTileEntities.HULL[6].getStackForm())
                .notConsumable(new IntCircuitIngredient(0))
                .outputs(QUADRUPLE_QUADRUPLE_INPUT_HATCH.getStackForm())
                .duration(100)
                .EUt(7680)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.pipeLarge, Naquadah, 9)
                .inputs(MetaTileEntities.HULL[6].getStackForm())
                .notConsumable(new IntCircuitIngredient(1))
                .outputs(QUADRUPLE_QUADRUPLE_OUTPUT_HATCH.getStackForm())
                .duration(100)
                .EUt(7680)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(MetaTileEntities.ENERGY_INPUT_HATCH[6].getStackForm())
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(2))
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Ultimate)
                .input(OrePrefix.plate, TungstenSteel, 8)
                .outputs(TJMetaBlocks.ABILITY_CASING.getItemVariant(BlockAbilityCasings.AbilityType.ENERGY_PORT_LUV))
                .duration(200)
                .EUt(GAValues.VA[6])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(MetaTileEntities.ENERGY_INPUT_HATCH[7].getStackForm())
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(2))
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.plate, Rutherfordium, 8)
                .outputs(TJMetaBlocks.ABILITY_CASING.getItemVariant(BlockAbilityCasings.AbilityType.ENERGY_PORT_ZPM))
                .duration(200)
                .EUt(GAValues.VA[7])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(MetaTileEntities.ENERGY_INPUT_HATCH[8].getStackForm())
                .inputs(GAMetaItems.UHPIC.getStackForm(2))
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.plate, Dubnium, 8)
                .outputs(TJMetaBlocks.ABILITY_CASING.getItemVariant(BlockAbilityCasings.AbilityType.ENERGY_PORT_UV))
                .duration(200)
                .EUt(GAValues.VA[8])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(ENERGY_INPUT[0].getStackForm())
                .inputs(GAMetaItems.UHPIC.getStackForm(4))
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.plate, Seaborgium, 8)
                .outputs(TJMetaBlocks.ABILITY_CASING.getItemVariant(BlockAbilityCasings.AbilityType.ENERGY_PORT_UHV))
                .duration(200)
                .EUt(GAValues.VA[9])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(ENERGY_INPUT[1].getStackForm())
                .inputs(UHPIC.getStackForm(8))
                .input(OrePrefix.circuit, UIV)
                .input(OrePrefix.plate, Bohrium, 8)
                .outputs(TJMetaBlocks.ABILITY_CASING.getItemVariant(BlockAbilityCasings.AbilityType.ENERGY_PORT_UEV))
                .duration(200)
                .EUt(GAValues.VA[10])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(HULL[8].getStackForm())
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite, 4)
                .input(OrePrefix.cableGtSingle, NaquadahAlloy, 16)
                .inputs(MetaItems.FIELD_GENERATOR_UV.getStackForm(2))
                .inputs(MetaItems.SENSOR_UV.getStackForm(2))
                .inputs(MetaItems.EMITTER_UV.getStackForm(2))
                .outputs(ACCELERATOR_ANCHOR_POINT.getStackForm())
                .fluidInputs(SolderingAlloy.getFluid(1152))
                .duration(200)
                .EUt(GAValues.VA[8])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.plate, HastelloyK243, 6)
                .inputs(GAMetaBlocks.FUSION_CASING.getItemVariant(GAFusionCasing.CasingType.FUSION_3))
                .outputs(TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_CASING_UHV))
                .duration(50)
                .EUt(500000)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .fluidInputs(Periodicium.getFluid(144))
                .input(OrePrefix.plate, SuperheavyLAlloy, 6)
                .input(OrePrefix.plate, SuperheavyHAlloy, 6)
                .inputs(TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_CASING_UHV))
                .outputs(TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_CASING_UEV))
                .duration(50)
                .EUt(8000000)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.circuit, Master, 2)
                .inputs(ELECTRIC_PUMP_LUV.getStackForm())
                .notConsumable(new IntCircuitIngredient(1))
                .outputs(FLUID_REGULATOR_LUV.getStackForm())
                .duration(100)
                .EUt(GAValues.VA[6])
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.circuit, Infinite, 2)
                .inputs(ELECTRIC_PUMP_UHV.getStackForm())
                .notConsumable(new IntCircuitIngredient(1))
                .outputs(FLUID_REGULATOR_UHV.getStackForm())
                .duration(100)
                .EUt(GAValues.VA[9])
                .buildAndRegister();

        for (int i = 0; i < SUPER_ITEM_INPUT_BUS.length; i++) {
            int tier = 3 * (1 + i);
            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(ITEM_IMPORT_BUS[tier].getStackForm(64))
                    .input(OrePrefix.gear, materialTier[0][tier - 1], 16)
                    .inputs(i == 0 ? MetaItems.ROBOT_ARM_HV.getStackForm(4)
                            : i == 1 ? MetaItems.ROBOT_ARM_LUV.getStackForm(4)
                            : GAMetaItems.ROBOT_ARM_UHV.getStackForm(4))
                    .outputs(SUPER_ITEM_INPUT_BUS[i].getStackForm())
                    .fluidInputs(i < 2 ? Polybenzimidazole.getFluid(9216) : Polyetheretherketone.getFluid(9216))
                    .duration(1200)
                    .EUt(GAValues.VA[tier])
                    .buildAndRegister();

            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(ITEM_EXPORT_BUS[tier].getStackForm(64))
                    .input(OrePrefix.gear, materialTier[0][tier - 1], 16)
                    .inputs(i == 0 ? MetaItems.CONVEYOR_MODULE_HV.getStackForm(4)
                            : i == 1 ? MetaItems.CONVEYOR_MODULE_LUV.getStackForm(4)
                            : GAMetaItems.CONVEYOR_MODULE_UHV.getStackForm(4))
                    .outputs(SUPER_ITEM_OUTPUT_BUS[i].getStackForm())
                    .fluidInputs(i < 2 ? Polybenzimidazole.getFluid(9216) : Polyetheretherketone.getFluid(9216))
                    .duration(1200)
                    .EUt(GAValues.VA[tier])
                    .buildAndRegister();

            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(i < 2 ? INPUT_HATCH_MULTI.get(i).getStackForm(64) : QUADRUPLE_QUADRUPLE_INPUT_HATCH.getStackForm(64))
                    .input(OrePrefix.gear, materialTier[0][tier - 1], 16)
                    .inputs(i == 0 ? MetaItems.FLUID_REGULATOR_HV.getStackForm(4)
                            : i == 1 ? MetaItems.FLUID_REGULATOR_LUV.getStackForm(4)
                            : FLUID_REGULATOR_UHV.getStackForm(4))
                    .outputs(SUPER_FLUID_INPUT_HATCH[i].getStackForm())
                    .fluidInputs(i < 2 ? Polybenzimidazole.getFluid(9216) : Polyetheretherketone.getFluid(9216))
                    .duration(1200)
                    .EUt(GAValues.VA[tier])
                    .buildAndRegister();

            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(i < 2 ? OUTPUT_HATCH_MULTI.get(i).getStackForm(64) : QUADRUPLE_QUADRUPLE_OUTPUT_HATCH.getStackForm(64))
                    .input(OrePrefix.gear, materialTier[0][tier - 1], 16)
                    .inputs(i == 0 ? MetaItems.ELECTRIC_PUMP_HV.getStackForm(4)
                            : i == 1 ? MetaItems.ELECTRIC_PUMP_LUV.getStackForm(4)
                            : GAMetaItems.ELECTRIC_PUMP_UHV.getStackForm(4))
                    .outputs(SUPER_FLUID_OUTPUT_HATCH[i].getStackForm())
                    .fluidInputs(i < 2 ? Polybenzimidazole.getFluid(9216) : Polyetheretherketone.getFluid(9216))
                    .duration(1200)
                    .EUt(GAValues.VA[tier])
                    .buildAndRegister();
        }


        for (int i = 0; i < materialTier[0].length; i++) {

            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(ENERGY_INPUT_HATCH_128_AMPS.get(i + 1).getStackForm())
                    .input(OrePrefix.wireGtHex, materialTier[1][i], 2)
                    .input(OrePrefix.plate, materialTier[0][i], 6)
                    .outputs(ENERGY_INPUT_HATCH_256A[i].getStackForm())
                    .duration(600)
                    .EUt(GAValues.VA[i + 1])
                    .buildAndRegister();

            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(ENERGY_OUTPUT_HATCH_128_AMPS.get(i + 1).getStackForm())
                    .input(OrePrefix.wireGtHex, materialTier[1][i], 2)
                    .input(OrePrefix.plate, materialTier[0][i], 6)
                    .outputs(ENERGY_OUTPUT_HATCH_256A[i].getStackForm())
                    .duration(600)
                    .EUt(GAValues.VA[i + 1])
                    .buildAndRegister();
        }

        for (int i = 0; i < ENDER_FLUID_COVERS.length; i++) {
            ASSEMBLER_RECIPES.recipeBuilder()
                    .fluidInputs(SolderingAlloy.getFluid(576))
                    .input(OrePrefix.plate, EnderPearl, 9)
                    .input(OrePrefix.plateDense, StainlessSteel)
                    .inputs(emitters[i + 2].getStackForm(2))
                    .inputs(sensors[i + 2].getStackForm(2))
                    .inputs(pumps[i + 2].getStackForm(2))
                    .outputs(ENDER_FLUID_COVERS[i].getStackForm())
                    .duration(600)
                    .EUt(GAValues.VA[i + 3])
                    .buildAndRegister();

            ASSEMBLER_RECIPES.recipeBuilder()
                    .fluidInputs(SolderingAlloy.getFluid(576))
                    .input(OrePrefix.plate, EnderPearl, 9)
                    .input(OrePrefix.plateDense, StainlessSteel)
                    .inputs(emitters[i + 2].getStackForm(2))
                    .inputs(sensors[i + 2].getStackForm(2))
                    .inputs(conveyors[i + 2].getStackForm(2))
                    .outputs(ENDER_ITEM_COVERS[i].getStackForm())
                    .duration(600)
                    .EUt(GAValues.VA[i + 3])
                    .buildAndRegister();

            ASSEMBLER_RECIPES.recipeBuilder()
                    .fluidInputs(SolderingAlloy.getFluid(576))
                    .input(OrePrefix.plate, EnderPearl, 9)
                    .input(OrePrefix.plateDense, StainlessSteel)
                    .inputs(emitters[i + 2].getStackForm(2))
                    .inputs(sensors[i + 2].getStackForm(2))
                    .input(OrePrefix.cableGtHex, materialTier[1][i + 2], 2)
                    .outputs(ENDER_ENERGY_COVERS[i].getStackForm())
                    .duration(600)
                    .EUt(GAValues.VA[i + 3])
                    .buildAndRegister();
        }

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(new ItemStack(Blocks.LEVER))
                .input(OrePrefix.plate, Iron)
                .inputs(HULL[1].getStackForm())
                .fluidInputs(SolderingAlloy.getFluid(144))
                .outputs(MACHINE_CONTROLLER.getStackForm())
                .duration(200)
                .EUt(16)
                .buildAndRegister();

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(new ItemStack(Objects.requireNonNull(Block.getBlockFromName("enderio:block_reservoir")), 64))
                .inputs(GAMetaItems.UV_INFINITE_WATER_SOURCE.getStackForm())
                .inputs(GA_HULLS[0].getStackForm())
                .fluidInputs(Water.getFluid(4096000))
                .outputs(WATER_RESERVOIR_HATCH.getStackForm())
                .duration(1200)
                .EUt(GAValues.VA[9])
                .buildAndRegister();

        if (TJConfig.machines.replaceCTMultis) {
            ASSEMBLER_RECIPES.recipeBuilder()
                    .inputs(COKE_OVEN.getStackForm(64))
                    .inputs(COKE_OVEN.getStackForm(64))
                    .inputs(COKE_OVEN.getStackForm(64))
                    .inputs(COKE_OVEN.getStackForm(64))
                    .outputs(MEGA_COKE_OVEN.getStackForm(1))
                    .EUt(30)
                    .duration(1200)
                    .buildAndRegister();
        }
    }
}
