package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import gregicadditions.item.GAMetaItems;
import gregicadditions.machines.GATileEntities;
import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import gregtech.api.unification.material.MarkerMaterials;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.UnificationEntry;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.loaders.recipe.CraftingComponent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.johny.tj.machines.TJMetaTileEntities.*;
import static com.johny.tj.materials.TJMaterials.PahoehoeLava;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.recipes.GARecipeMaps.ASSEMBLY_LINE_RECIPES;
import static gregtech.api.recipes.RecipeMaps.*;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.common.metatileentities.MetaTileEntities.LARGE_TUNGSTENSTEEL_BOILER;

public class RecipeLoader {

    public static void init() {

        craftingRecipes();
        GreenhouseRecipes.init();
        ArchitectureRecipes.init();

    }
    private static void craftingRecipes() {

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

        ASSEMBLER_RECIPES.recipeBuilder()
                .input(OrePrefix.plate, TungstenTitaniumCarbide, 6)
                .input(OrePrefix.frameGt, TungstenTitaniumCarbide)
                .notConsumable(new IntCircuitIngredient(0))
                .outputs(TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.TUNGSTEN_TITANIUM_CARBIDE_CASING, 3))
                .duration(50)
                .EUt(16)
                .buildAndRegister();

        ModHandler.addShapedRecipe("large_decay_chamber", LARGE_DECAY_CHAMBER.getStackForm(), "LCL", "FMF", "LCL",
                'L', new UnificationEntry(OrePrefix.plateDense, Lead),
                'C', CraftingComponent.CIRCUIT.getIngredient(6),
                'F', CraftingComponent.FIELD_GENERATOR.getIngredient(6),
                'M', GATileEntities.DECAY_CHAMBER[5].getMetaTileEntity().getStackForm());

        ModHandler.addShapedRecipe("large_alloy_smelter", LARGE_ALLOY_SMELTER.getStackForm(), "PCP", "WSW", "PCP",
                'P', new UnificationEntry(OrePrefix.plate, ZirconiumCarbide),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'W', new UnificationEntry(OrePrefix.cableGtOctal, Naquadah),
                'S', GATileEntities.ALLOY_SMELTER[4].getMetaTileEntity().getStackForm());

        ModHandler.addShapedRecipe("large_greenhouse", LARGE_GREENHOUSE.getStackForm(), "PCP", "RSR", "GCG",
                'P', new UnificationEntry(OrePrefix.plate, StainlessSteel),
                'G', new UnificationEntry(OrePrefix.gear, StainlessSteel),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'R', CraftingComponent.PUMP.getIngredient(5),
                'S', GATileEntities.GREEN_HOUSE[4].getMetaTileEntity().getStackForm());

        ModHandler.addShapedRecipe("large_architect_workbench", LARGE_ARCHITECT_WORKBENCH.getStackForm(), "GCG", "RSB", "GCG",
                'G', new UnificationEntry(OrePrefix.gear, Steel),
                'C', CraftingComponent.CIRCUIT.getIngredient(4),
                'R', CraftingComponent.ROBOT_ARM.getIngredient(4),
                'B', CraftingComponent.CONVEYOR.getIngredient(4),
                'S', new ItemStack(Item.getByNameOrId("architecturecraft:sawbench")));

        ModHandler.addShapedRecipe("elite_large_miner", ELITE_LARGE_MINER.getStackForm(), "GCG", "THT", "SCS",
                'G', new UnificationEntry(OrePrefix.gear, Duranium),
                'C', CraftingComponent.CIRCUIT.getIngredient(7),
                'T', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(),
                'H', CraftingComponent.HULL.getIngredient(7),
                'S', CraftingComponent.SENSOR.getIngredient(7));

        ModHandler.addShapedRecipe("ultimate_large_miner", ULTIMATE_LARGE_MINER.getStackForm(), "GCG", "THT", "SCS",
                'G', new UnificationEntry(OrePrefix.gear, Seaborgium),
                'C', CraftingComponent.CIRCUIT.getIngredient(8),
                'T', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(),
                'H', CraftingComponent.HULL.getIngredient(8),
                'S', CraftingComponent.SENSOR.getIngredient(8));

        ModHandler.addShapedRecipe("world_destroyer", WORLD_DESTROYER.getStackForm(), "GCG", "DTD", "SCS",
                'G', new UnificationEntry(OrePrefix.gear, TungstenTitaniumCarbide),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'D', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(),
                'T', MetaTileEntities.BLOCK_BREAKER[3].getStackForm(),
                'S', CraftingComponent.SENSOR.getIngredient(5));

        ModHandler.addShapedRecipe("duranium_casing", TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.DURANIUM_CASING, 3), "PhP", "PFP", "PwP",
                'P', new UnificationEntry(OrePrefix.plate, Duranium),
                'F', new UnificationEntry(OrePrefix.frameGt, Duranium));

        ModHandler.addShapedRecipe("seaborgium_casing", TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.SEABORGIUM_CASING, 3), "PhP", "PFP", "PwP",
                'P', new UnificationEntry(OrePrefix.plate, Seaborgium),
                'F', new UnificationEntry(OrePrefix.frameGt, Seaborgium));

        ModHandler.addShapedRecipe("tungsten_titanium_carbide_casing", TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.TUNGSTEN_TITANIUM_CARBIDE_CASING, 3), "PhP", "PFP", "PwP",
                'P', new UnificationEntry(OrePrefix.plate, TungstenTitaniumCarbide),
                'F', new UnificationEntry(OrePrefix.frameGt, TungstenTitaniumCarbide));


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

            ALLOY_SMELTER_RECIPES.recipeBuilder()
                    .input(Items.CLAY_BALL)
                    .input(Blocks.SAND)
                    .outputs(MetaItems.COKE_OVEN_BRICK.getStackForm(2))
                    .EUt(64)
                    .duration(600)
                    .buildAndRegister();
        }
    }
}
