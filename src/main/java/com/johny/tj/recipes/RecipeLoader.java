package com.johny.tj.recipes;

import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import gregicadditions.machines.GATileEntities;
import gregtech.api.recipes.ModHandler;
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
import static gregicadditions.GAMaterials.*;
import static gregtech.api.recipes.RecipeMaps.ALLOY_SMELTER_RECIPES;
import static gregtech.api.unification.material.Materials.*;

public class RecipeLoader {

    public static void init() {

        craftingRecipes();
        GreenhouseRecipes.init();
        ArchitectureRecipes.init();
        AssemblingRecipes.assemblerRecipes();
        AssemblingRecipes.assemblyLineRecipes();

    }
    private static void craftingRecipes() {

        ALLOY_SMELTER_RECIPES.recipeBuilder()
                .input(Items.CLAY_BALL)
                .input(Blocks.SAND)
                .outputs(MetaItems.COKE_OVEN_BRICK.getStackForm(2))
                .EUt(64)
                .duration(600)
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

    }
}
