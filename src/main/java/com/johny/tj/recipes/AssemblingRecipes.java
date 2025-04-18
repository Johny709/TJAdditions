package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockAbilityCasings;
import com.johny.tj.blocks.BlockFusionCasings;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import gregicadditions.GAValues;
import gregicadditions.item.CellCasing;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMetaItems;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.fusion.GAFusionCasing;
import gregicadditions.machines.GATileEntities;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Objects;

import static com.johny.tj.items.TJMetaItems.*;
import static com.johny.tj.machines.TJMetaTileEntities.COKE_OVEN;
import static com.johny.tj.machines.TJMetaTileEntities.*;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.item.GAMetaItems.*;
import static gregicadditions.machines.GATileEntities.*;
import static gregicadditions.recipes.GARecipeMaps.ASSEMBLY_LINE_RECIPES;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;
import static gregtech.api.unification.material.MarkerMaterials.Tier.Infinite;
import static gregtech.api.unification.material.MarkerMaterials.Tier.Master;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.common.items.MetaItems.*;
import static gregtech.common.metatileentities.MetaTileEntities.*;

public class AssemblingRecipes {

    public static Material[][] materialTier = {{Steel, Aluminium, StainlessSteel, Titanium, TungstenSteel, RhodiumPlatedPalladium, IngotMaterial.MATERIAL_REGISTRY.getObject("star_metal_alloy"), Tritanium, Seaborgium, Bohrium, Adamantium, Vibranium, HeavyQuarkDegenerateMatter, Neutronium},
                                                {IngotMaterial.MATERIAL_REGISTRY.getObject("lv_superconductor"), MVSuperconductor, HVSuperconductor, EVSuperconductor, IVSuperconductor, LuVSuperconductor, ZPMSuperconductor, UVSuperconductor, UHVSuperconductor, UEVSuperconductor, UIVSuperconductor, UMVSuperconductor, UXVSuperconductor, MarkerMaterials.Tier.Superconductor}};
    public static MetaTileEntityLargeBoiler[] boilerType = {LARGE_BRONZE_BOILER, LARGE_STEEL_BOILER, LARGE_TITANIUM_BOILER, LARGE_TUNGSTENSTEEL_BOILER};

    public static void assemblerRecipes() {
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

    public static void assemblyLineRecipes() {

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .fluidInputs(Naquadria.getFluid(2304))
                .inputs(GATileEntities.LARGE_STEAM_TURBINE.getStackForm(12))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(16))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(16))
                .inputs(GATileEntities.MAINTENANCE_HATCH[2].getStackForm())
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(64))
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(64))
                .input(OrePrefix.wireGtSingle, UVSuperconductor, 64)
                .input(OrePrefix.pipeLarge, Steel, 64)
                .input(OrePrefix.plateDense, Steel, 7)
                .input(OrePrefix.gear, Steel, 16)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .outputs(XL_STEAM_TURBINE.getStackForm())
                .EUt(491520)
                .duration(2400)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .fluidInputs(Naquadria.getFluid(2304))
                .inputs(GATileEntities.LARGE_GAS_TURBINE.getStackForm(12))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(16))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(16))
                .inputs(GATileEntities.MAINTENANCE_HATCH[2].getStackForm())
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(64))
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(64))
                .input(OrePrefix.wireGtSingle, UVSuperconductor, 64)
                .input(OrePrefix.pipeLarge, StainlessSteel, 64)
                .input(OrePrefix.plateDense, StainlessSteel, 7)
                .input(OrePrefix.gear, StainlessSteel, 16)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .outputs(XL_GAS_TURBINE.getStackForm())
                .EUt(491520)
                .duration(2400)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .fluidInputs(Bohrium.getFluid(2304))
                .inputs(GATileEntities.HOT_COOLANT_TURBINE.getStackForm(12))
                .inputs(GAMetaItems.ELECTRIC_MOTOR_UHV.getStackForm(12))
                .inputs(GAMetaItems.ELECTRIC_PUMP_UHV.getStackForm(12))
                .inputs(GATileEntities.MAINTENANCE_HATCH[2].getStackForm())
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .input(OrePrefix.wireGtSingle, UHVSuperconductor, 64)
                .input(OrePrefix.pipeLarge, Ultimet, 64)
                .input(OrePrefix.plateDense, Stellite, 7)
                .input(OrePrefix.gear, Stellite, 16)
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.circuit, UEV)
                .outputs(XL_COOLANT_TURBINE.getStackForm())
                .EUt(1966080)
                .duration(2400)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .fluidInputs(Seaborgium.getFluid(2304))
                .inputs(GATileEntities.LARGE_PLASMA_TURBINE.getStackForm(12))
                .inputs(GAMetaItems.ELECTRIC_MOTOR_UHV.getStackForm(12))
                .inputs(GAMetaItems.ELECTRIC_PUMP_UHV.getStackForm(12))
                .inputs(GATileEntities.MAINTENANCE_HATCH[2].getStackForm())
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .input(OrePrefix.wireGtSingle, UHVSuperconductor, 64)
                .input(OrePrefix.pipeLarge, TungstenSteel, 64)
                .input(OrePrefix.plateDense, TungstenSteel, 7)
                .input(OrePrefix.gear, TungstenSteel, 16)
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.circuit, UEV)
                .input(OrePrefix.circuit, UEV)
                .outputs(XL_PLASMA_TURBINE.getStackForm())
                .EUt(1966080)
                .duration(2400)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(5760))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(Uranium238Isotope.getMaterial().getFluid(1440))
                .fluidInputs(Plutonium244Isotope.getMaterial().getFluid(1440))
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(64))
                .input(OrePrefix.wireGtQuadruple, LuVSuperconductor, 64)
                .input(OrePrefix.plateDense, Einsteinium.getMaterial(), 4)
                .input(OrePrefix.plateDense, Rutherfordium, 4)
                .inputs(MetaItems.FIELD_GENERATOR_LUV.getStackForm(16))
                .inputs(MetaItems.EMITTER_LUV.getStackForm(16))
                .inputs(MetaItems.SENSOR_LUV.getStackForm(16))
                .inputs(GATileEntities.FUSION_REACTOR[0].getStackForm())
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Ultimate)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Ultimate)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Ultimate)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Ultimate)
                .outputs(INDUSTRIAL_FUSION_REACTOR_LUV.getStackForm())
                .EUt(60000)
                .duration(1000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(5760))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(Polonium.getFluid(2880))
                .fluidInputs(Lutetium.getFluid(2880))
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(48))
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(48))
                .input(OrePrefix.wireGtOctal, ZPMSuperconductor, 64)
                .input(OrePrefix.plateDense, Fermium.getMaterial(), 4)
                .input(OrePrefix.plateDense, Dubnium, 4)
                .inputs(MetaItems.FIELD_GENERATOR_ZPM.getStackForm(16))
                .inputs(MetaItems.EMITTER_ZPM.getStackForm(16))
                .inputs(MetaItems.SENSOR_ZPM.getStackForm(16))
                .inputs(GATileEntities.FUSION_REACTOR[1].getStackForm())
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor, 4)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor, 4)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor, 4)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor, 4)
                .outputs(INDUSTRIAL_FUSION_REACTOR_ZPM.getStackForm())
                .EUt(120000)
                .duration(1000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(5760))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(Copernicium.getFluid(5760))
                .fluidInputs(Meitnerium.getFluid(5760))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .input(OrePrefix.wireGtHex, UVSuperconductor, 64)
                .input(OrePrefix.plateDense, Mendelevium.getMaterial(), 4)
                .input(OrePrefix.plateDense, Seaborgium, 4)
                .inputs(MetaItems.FIELD_GENERATOR_UV.getStackForm(16))
                .inputs(MetaItems.EMITTER_UV.getStackForm(16))
                .inputs(MetaItems.SENSOR_UV.getStackForm(16))
                .inputs(GATileEntities.FUSION_REACTOR[2].getStackForm())
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite, 16)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite, 16)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite, 16)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite,16)
                .outputs(INDUSTRIAL_FUSION_REACTOR_UV.getStackForm())
                .EUt(180000)
                .duration(1000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(Helium.getFluid(4000))
                .inputs(MetaItems.NEUTRON_REFLECTOR.getStackForm(8))
                .input(OrePrefix.cableGtQuadruple, UEVSuperconductor, 4)
                .inputs(GAMetaItems.FIELD_GENERATOR_UHV.getStackForm(2))
                .input(OrePrefix.plate, Bohrium, 2)
                .input(OrePrefix.circuit, UEV, 1)
                .outputs(TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_COIL_UHV))
                .duration(400)
                .EUt(GAValues.VA[9])
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(11520))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(SuperheavyLAlloy.getFluid(11520))
                .fluidInputs(SuperheavyHAlloy.getFluid(11520))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .input(OrePrefix.wireGtHex, UEVSuperconductor, 64)
                .input(OrePrefix.plateDense, Quantum, 4)
                .input(OrePrefix.plateDense, HastelloyX78, 4)
                .inputs(GAMetaItems.FIELD_GENERATOR_UEV.getStackForm(16))
                .inputs(GAMetaItems.EMITTER_UEV.getStackForm(16))
                .inputs(GAMetaItems.SENSOR_UEV.getStackForm(16))
                .inputs(TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_COIL_UHV))
                .input(OrePrefix.circuit, UIV, 16)
                .input(OrePrefix.circuit, UIV, 16)
                .input(OrePrefix.circuit, UIV, 16)
                .input(OrePrefix.circuit, UIV,16)
                .outputs(INDUSTRIAL_FUSION_REACTOR_UHV.getStackForm())
                .EUt(1800000)
                .duration(1000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(4608))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Naquadria.getFluid(2304))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(16))
                .inputs(MetaItems.CONVEYOR_MODULE_UV.getStackForm(16))
                .inputs(MetaItems.ELECTRIC_PISTON_UV.getStackForm(16))
                .inputs(MetaItems.EMITTER_UV.getStackForm(16))
                .inputs(MetaItems.FIELD_GENERATOR_UV.getStackForm(16))
                .inputs(MetaItems.ROBOT_ARM_UV.getStackForm(16))
                .inputs(MetaItems.SENSOR_UV.getStackForm(16))
                .input(OrePrefix.plateDense, Tritanium, 6)
                .input(OrePrefix.ring, Duranium, 64)
                .input(OrePrefix.screw, Dubnium, 32)
                .input(OrePrefix.screw, Tritanium, 32)
                .input(OrePrefix.wireGtDouble, ZPMSuperconductor, 32)
                .input(OrePrefix.wireFine, ThoriumDopedTungsten, 64)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite, 4)
                .outputs(LARGE_WORLD_ACCELERATOR.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(2400)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(4608))
                .fluidInputs(Polyetheretherketone.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Naquadria.getFluid(2304))
                .inputs(GATileEntities.FLUID_DRILLING_PLANT[2].getStackForm(16))
                .input(OrePrefix.circuit, UEV, 16)
                .inputs(GAMetaItems.ELECTRIC_MOTOR_UHV.getStackForm(64))
                .inputs(GAMetaItems.ELECTRIC_PUMP_UHV.getStackForm(64))
                .input(OrePrefix.stickLong, NaquadriaticTaranium, 16)
                .input(OrePrefix.gearSmall, TitanSteel, 32)
                .input(OrePrefix.gear, Taranium, 16)
                .input(OrePrefix.plate, Seaborgium, 32)
                .input(OrePrefix.foil, Pikyonium, 64)
                .input(OrePrefix.pipeLarge, EnrichedNaquadahAlloy, 64)
                .input(OrePrefix.screw, Duranium, 48)
                .input(OrePrefix.wireGtSingle, UHVSuperconductor, 64)
                .input(OrePrefix.frameGt, HDCS, 16)
                .outputs(INFINITE_FLUID_DRILL.getStackForm())
                .EUt(GAValues.VA[10])
                .duration(2400)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(Plastic.getFluid(9216))
                .fluidInputs(PolyvinylChloride.getFluid(4608))
                .fluidInputs(Polytetrafluoroethylene.getFluid(2304))
                .fluidInputs(Polybenzimidazole.getFluid(1152))
                .input(OrePrefix.foil, Polycaprolactam, 64)
                .input(OrePrefix.foil, Polystyrene, 64)
                .input(OrePrefix.foil, PolyphenyleneSulfide, 64)
                .input(OrePrefix.foil, Rubber, 64)
                .inputs(MetaItems.ELECTRIC_PUMP_IV.getStackForm(16))
                .input(OrePrefix.circuit, Master)
                .input(OrePrefix.circuit, Master)
                .inputs(GATileEntities.LARGE_CHEMICAL_REACTOR.getStackForm())
                .outputs(PARALLEL_CHEMICAL_REACTOR.getStackForm())
                .EUt(GAValues.VA[6])
                .duration(1200)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Polyetheretherketone.getFluid(18432))
                .inputs(PARALLEL_CHEMICAL_REACTOR.getStackForm(16))
                .input(OrePrefix.gearSmall, Tritanium, 32)
                .input(OrePrefix.stickLong, LithiumTitanate, 64)
                .input(OrePrefix.pipeLarge, Ultimet, 40)
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(64))
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite,16)
                .inputs(MetaItems.FIELD_GENERATOR_UV.getStackForm(10))
                .input(OrePrefix.wireFine, CarbonNanotubes, 64)
                .input(OrePrefix.foil, HDCS, 64)
                .input(OrePrefix.plateDense, Grisium, 8)
                .inputs(GAMetaBlocks.MUTLIBLOCK_CASING.getItemVariant(GAMultiblockCasing.CasingType.PTFE_PIPE, 64))
                .inputs(GAMetaBlocks.MUTLIBLOCK_CASING.getItemVariant(GAMultiblockCasing.CasingType.CHEMICALLY_INERT, 64))
                .outputs(ADVANCED_PARALLEL_CHEMICAL_REACTOR.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Stellite.getFluid(18432))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .inputs(LARGE_MACERATOR.getStackForm(16))
                .inputs(MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(64))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(32))
                .inputs(MetaItems.ELECTRIC_PISTON_UV.getStackForm(32))
                .input(OrePrefix.gear, TungstenTitaniumCarbide, 16)
                .input(OrePrefix.wireGtQuadruple, NaquadahAlloy, 64)
                .input(OrePrefix.frameGt, Stellite, 16)
                .input(OrePrefix.plateDense, Stellite, 8)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(PARALLEL_LARGE_MACERATOR.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Grisium.getFluid(18432))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .inputs(LARGE_WASHING_PLANT.getStackForm(16))
                .input(OrePrefix.pipeLarge, TungstenSteel, 64)
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(32))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(32))
                .input(OrePrefix.gear, Talonite, 16)
                .input(OrePrefix.cableGtQuadruple, Duranium, 64)
                .input(OrePrefix.frameGt, Grisium, 16)
                .input(OrePrefix.plateDense, Grisium, 8)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(PARALLEL_LARGE_WASHING_MACHINE.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(RedSteel.getFluid(18432))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .inputs(LARGE_CENTRIFUGE.getStackForm(16))
                .input(OrePrefix.pipeLarge, TungstenSteel, 64)
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(32))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(32))
                .input(OrePrefix.gear, Titanium, 16)
                .input(OrePrefix.cableGtQuadruple, Duranium, 64)
                .input(OrePrefix.frameGt, RedSteel, 16)
                .input(OrePrefix.plateDense, RedSteel, 8)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(PARALLEL_LARGE_CENTRIFUGE.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Potin.getFluid(18432))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .inputs(LARGE_ELECTROLYZER.getStackForm(16))
                .inputs(MetaItems.LARGE_FLUID_CELL_TUNGSTEN_STEEL.getStackForm(16))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(32))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(32))
                .input(OrePrefix.gear, Osmiridium, 16)
                .input(OrePrefix.cableGtQuadruple, Duranium, 64)
                .input(OrePrefix.frameGt, Potin, 16)
                .input(OrePrefix.plateDense, Potin, 8)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(PARALLEL_LARGE_ELECTROLYZER.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(EglinSteel.getFluid(18432))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .inputs(LARGE_SIFTER.getStackForm(16))
                .inputs(MetaItems.ITEM_FILTER.getStackForm(64))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(32))
                .inputs(MetaItems.ELECTRIC_PISTON_UV.getStackForm(32))
                .input(OrePrefix.gear, RoseGold, 16)
                .input(OrePrefix.cableGtQuadruple, NaquadahAlloy, 64)
                .input(OrePrefix.frameGt, EglinSteel, 16)
                .input(OrePrefix.plateDense, EglinSteel, 8)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(PARALLEL_LARGE_SIFTER.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(18432))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Grisium.getFluid(18432))
                .fluidInputs(Polybenzimidazole.getFluid(9216))
                .inputs(LARGE_BREWERY.getStackForm(16))
                .input(OrePrefix.pipeLarge, Zeron100, 64)
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(32))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(32))
                .input(OrePrefix.gear, Ultimet, 16)
                .input(OrePrefix.cableGtQuadruple, Duranium, 64)
                .input(OrePrefix.frameGt, Grisium, 16)
                .input(OrePrefix.plateDense, Grisium, 8)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(PARALLEL_LARGE_BREWERY.getStackForm())
                .EUt(GAValues.VA[9])
                .duration(3000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxcontroller")))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxcore"), 64))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxblock"), 16))
                .input(OrePrefix.plateDense, Talonite, 4)
                .inputs(MetaItems.EMITTER_IV.getStackForm(16))
                .inputs(MetaItems.EMITTER_LUV.getStackForm(16))
                .inputs(MetaItems.EMITTER_ZPM.getStackForm(16))
                .inputs(MetaItems.EMITTER_UV.getStackForm(16))
                .input(OrePrefix.circuit, Infinite, 4)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .outputs(LARGE_WIRELESS_ENERGY_EMITTER.getStackForm())
                .EUt(GAValues.VA[8])
                .duration(2000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxcontroller")))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxcore"), 64))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxblock"), 16))
                .input(OrePrefix.plateDense, Talonite, 4)
                .inputs(MetaItems.SENSOR_IV.getStackForm(16))
                .inputs(MetaItems.SENSOR_LUV.getStackForm(16))
                .inputs(MetaItems.SENSOR_ZPM.getStackForm(16))
                .inputs(MetaItems.SENSOR_UV.getStackForm(16))
                .input(OrePrefix.circuit, Infinite, 4)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .outputs(LARGE_WIRELESS_ENERGY_RECEIVER.getStackForm())
                .EUt(GAValues.VA[8])
                .duration(2000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(16000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxcontroller")))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxcore"), 64))
                .inputs(new ItemStack(Item.getByNameOrId("fluxnetworks:fluxblock"), 16))
                .input(OrePrefix.plateDense, Talonite, 4)
                .inputs(GAMetaBlocks.CELL_CASING.getItemVariant(CellCasing.CellType.CELL_HV, 16))
                .inputs(GAMetaBlocks.CELL_CASING.getItemVariant(CellCasing.CellType.CELL_EV, 16))
                .inputs(GAMetaBlocks.CELL_CASING.getItemVariant(CellCasing.CellType.CELL_IV, 16))
                .inputs(GAMetaBlocks.CELL_CASING.getItemVariant(CellCasing.CellType.CELL_LUV, 16))
                .inputs(GAMetaBlocks.CELL_CASING.getItemVariant(CellCasing.CellType.CELL_ZPM, 16))
                .inputs(GAMetaBlocks.CELL_CASING.getItemVariant(CellCasing.CellType.CELL_UV, 16))
                .input(OrePrefix.circuit, Infinite, 4)
                .input(OrePrefix.wireGtQuadruple, UVSuperconductor, 64)
                .outputs(LARGE_BATTERY_CHARGER.getStackForm())
                .EUt(GAValues.VA[8])
                .duration(2000)
                .buildAndRegister();
    }
}
