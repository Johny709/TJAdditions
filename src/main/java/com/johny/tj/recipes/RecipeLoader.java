package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import gregicadditions.machines.GATileEntities;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.UnificationEntry;
import gregtech.common.items.MetaItems;
import gregtech.loaders.recipe.CraftingComponent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.johny.tj.machines.TJMetaTileEntities.*;
import static com.johny.tj.materials.TJMaterials.PahoehoeLava;
import static gregicadditions.GAMaterials.ZirconiumCarbide;
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
