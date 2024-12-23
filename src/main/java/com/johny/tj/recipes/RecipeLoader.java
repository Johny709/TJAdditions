package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import gregicadditions.machines.GATileEntities;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.UnificationEntry;
import gregtech.loaders.recipe.CraftingComponent;
import net.minecraft.init.Blocks;

import static com.johny.tj.machines.TJMetaTileEntities.*;
import static com.johny.tj.materials.TJMaterials.PahoehoeLava;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.ELECTROLYZER_RECIPES;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.common.metatileentities.MetaTileEntities.COKE_OVEN;
import static gregtech.common.metatileentities.MetaTileEntities.LARGE_TUNGSTENSTEEL_BOILER;

public class RecipeLoader {

    public static void init() {

        craftingRecipes();
        GreenhouseRecipes.init();
        ArchitectureRecipes.init();

    }
    private static void craftingRecipes() {

        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(COKE_OVEN.getStackForm(64))
                .inputs(COKE_OVEN.getStackForm(64))
                .inputs(COKE_OVEN.getStackForm(64))
                .inputs(COKE_OVEN.getStackForm(64))
                .outputs(MEGA_COKE_OVEN.getStackForm(1))
                .EUt(30)
                .duration(1200)
                .buildAndRegister();

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


        if (TJConfig.machines.replaceCTMultis) {
            ELECTROLYZER_RECIPES.recipeBuilder()
                    .fluidInputs(PahoehoeLava.getFluid(10000))
                    .output(Blocks.OBSIDIAN)
                    .output(OrePrefix.dust, Sulfur)
                    .output(OrePrefix.dust, Carbon)
                    .duration(20)
                    .EUt(7000)
                    .buildAndRegister();
        }
    }
}
