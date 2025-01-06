package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import gregicadditions.GAValues;
import gregicadditions.item.GAMetaItems;
import gregicadditions.machines.GATileEntities;
import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import gregtech.api.unification.material.MarkerMaterials;
import gregtech.api.unification.material.type.IngotMaterial;
import gregtech.api.unification.material.type.Material;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.init.Blocks;

import static com.johny.tj.machines.TJMetaTileEntities.*;
import static com.johny.tj.materials.TJMaterials.PahoehoeLava;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.machines.GATileEntities.ENERGY_INPUT_HATCH_128_AMPS;
import static gregicadditions.machines.GATileEntities.ENERGY_OUTPUT_HATCH_128_AMPS;
import static gregicadditions.recipes.GARecipeMaps.ASSEMBLY_LINE_RECIPES;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.ELECTROLYZER_RECIPES;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.common.metatileentities.MetaTileEntities.LARGE_TUNGSTENSTEEL_BOILER;

public class AssemblingRecipes {

    public static Material[][] materialTier = {{Steel, Aluminium, StainlessSteel, Titanium, TungstenSteel, RhodiumPlatedPalladium, IngotMaterial.MATERIAL_REGISTRY.getObject("star_metal_alloy"), Tritanium, Seaborgium, Bohrium, Adamantium, Vibranium, HeavyQuarkDegenerateMatter, Neutronium},
                                                {IngotMaterial.MATERIAL_REGISTRY.getObject("lv_superconductor"), MVSuperconductor, HVSuperconductor, EVSuperconductor, IVSuperconductor, LuVSuperconductor, ZPMSuperconductor, UVSuperconductor, UHVSuperconductor, UEVSuperconductor, UIVSuperconductor, UMVSuperconductor, UXVSuperconductor, MarkerMaterials.Tier.Superconductor}};

    public static void AssemblerRecipes() {

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(LARGE_TUNGSTENSTEEL_BOILER.getStackForm(64))
                .inputs(LARGE_TUNGSTENSTEEL_BOILER.getStackForm(64))
                .inputs(LARGE_TUNGSTENSTEEL_BOILER.getStackForm(64))
                .inputs(LARGE_TUNGSTENSTEEL_BOILER.getStackForm(64))
                .outputs(MEGA_TUNGSTENSTEEL_BOILER.getStackForm(1))
                .EUt(7680)
                .duration(1200)
                .buildAndRegister();

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

        if (TJConfig.machines.replaceCTMultis) {
            ELECTROLYZER_RECIPES.recipeBuilder()
                    .fluidInputs(PahoehoeLava.getFluid(10000))
                    .output(Blocks.OBSIDIAN)
                    .output(OrePrefix.dust, Sulfur)
                    .output(OrePrefix.dust, Carbon)
                    .duration(20)
                    .EUt(7000)
                    .buildAndRegister();

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

    public static void AssemblyLineRecipes() {

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(9216))
                .fluidInputs(Lubricant.getFluid(64000))
                .fluidInputs(Polybenzimidazole.getFluid(4608))
                .fluidInputs(Naquadria.getFluid(2304))
                .inputs(GATileEntities.LARGE_STEAM_TURBINE.getStackForm(12))
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(12))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(12))
                .inputs(GATileEntities.MAINTENANCE_HATCH[2].getStackForm())
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
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
                .inputs(MetaItems.ELECTRIC_MOTOR_UV.getStackForm(12))
                .inputs(MetaItems.ELECTRIC_PUMP_UV.getStackForm(12))
                .inputs(GATileEntities.MAINTENANCE_HATCH[2].getStackForm())
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .input(OrePrefix.wireGtSingle, UVSuperconductor, 64)
                .input(OrePrefix.pipeLarge, StainlessSteel, 64)
                .input(OrePrefix.plateDense, StainlessSteel, 7)
                .input(OrePrefix.gear, StainlessSteel, 16)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
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
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(64))
                .input(OrePrefix.wireGtDouble, LuVSuperconductor, 64)
                .input(OrePrefix.plateDense, Einsteinium.getMaterial(), 4)
                .input(OrePrefix.plateDense, Rutherfordium, 4)
                .inputs(MetaItems.FIELD_GENERATOR_LUV.getStackForm(16))
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
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(48))
                .inputs(MetaItems.HIGH_POWER_INTEGRATED_CIRCUIT.getStackForm(48))
                .input(OrePrefix.wireGtQuadruple, ZPMSuperconductor, 64)
                .input(OrePrefix.plateDense, Fermium.getMaterial(), 4)
                .input(OrePrefix.plateDense, Dubnium, 4)
                .inputs(MetaItems.FIELD_GENERATOR_ZPM.getStackForm(16))
                .inputs(GATileEntities.FUSION_REACTOR[1].getStackForm())
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Superconductor)
                .outputs(INDUSTRIAL_FUSION_REACTOR_ZPM.getStackForm())
                .EUt(120000)
                .duration(1000)
                .buildAndRegister();

        ASSEMBLY_LINE_RECIPES.recipeBuilder()
                .fluidInputs(SolderingAlloy.getFluid(5760))
                .fluidInputs(Lubricant.getFluid(16000))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .inputs(GAMetaItems.UHPIC.getStackForm(64))
                .input(OrePrefix.wireGtOctal, UVSuperconductor, 64)
                .input(OrePrefix.plateDense, Mendelevium.getMaterial(), 4)
                .input(OrePrefix.plateDense, Seaborgium, 4)
                .inputs(MetaItems.FIELD_GENERATOR_UV.getStackForm(16))
                .inputs(GATileEntities.FUSION_REACTOR[2].getStackForm())
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .input(OrePrefix.circuit, MarkerMaterials.Tier.Infinite)
                .outputs(INDUSTRIAL_FUSION_REACTOR_UV.getStackForm())
                .EUt(180000)
                .duration(1000)
                .buildAndRegister();
    }
}
