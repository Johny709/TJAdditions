package com.johny.tj;


import com.johny.tj.builder.MultiRecipeMap;
import com.johny.tj.builder.SteamRecipeBuilder;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.builders.SimpleRecipeBuilder;

import static gregtech.api.unification.material.Materials.Steam;

public class TJRecipeMaps {

    public static final RecipeMap<SteamRecipeBuilder> PRIMITIVE_ALLOY_RECIPES = new RecipeMap<>("primitive_alloy.tj", 0, 2, 1, 1, 1, 1, 0, 0, new SteamRecipeBuilder().EUt(0).fluidInputs(Steam.getFluid(1000))).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SteamRecipeBuilder> COKE_OVEN_RECIPES = new RecipeMap<>("coke_oven_2.tj", 0, 1, 1, 1, 0, 0, 1, 1, new SteamRecipeBuilder().EUt(0)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SteamRecipeBuilder> HEAT_EXCHANGER_RECIPES = new RecipeMap<>("heat_exchanger.tj", 0, 0, 0, 0, 1, 2, 1, 2, new SteamRecipeBuilder().EUt(0)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> ARMOR_INFUSER_RECIPES = new RecipeMap<>("armor_infuser.tj", 0, 12, 0, 1, 0, 1, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> DRAGON_REPLICATOR_RECIPES = new RecipeMap<>("dragon_egg_replicator.tj", 0, 2, 0, 3, 0, 1, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> CHAOS_REPLICATOR_RECIPES = new RecipeMap<>("chaos_replicator.tj", 0, 4, 0, 2, 0, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> LARGE_POWERED_SPAWNER_RECIPES = new RecipeMap<>("large_powered_spawner.tj", 0, 2, 0, 1, 0, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> LARGE_VIAL_PROCESSOR_RECIPES = new RecipeMap<>("large_vial_processor.tj", 0, 1, 0, 14, 0, 0, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> GREENHOUSE_TREE_RECIPES = new RecipeMap<>("greenhouse_tree", 0, 1, 0, 8, 0, 1, 0, 0, new SimpleRecipeBuilder().EUt(7680)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> ARCHITECT_RECIPES = new RecipeMap<>("architect", 1, 2, 0, 1, 0, 0, 0, 0, new SimpleRecipeBuilder().EUt(30)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> ROCK_BREAKER_RECIPES = new RecipeMap<>("rock_breaker", 1, 1, 1, 1, 0, 2, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static MultiRecipeMap PARALLEL_CHEMICAL_REACTOR_RECIPES;
    public static MultiRecipeMap PARALLEL_CHEMICAL_PLANT_RECIPES;
    public static MultiRecipeMap PARALLEL_MACERATOR_RECIPES;
    public static MultiRecipeMap PARALLEL_ORE_WASHER_RECIPES;
    public static MultiRecipeMap PARALLEL_CHEMICAL_BATH_RECIPES;
    public static MultiRecipeMap PARALLEL_SIMPLE_ORE_WASHER_RECIPES;
    public static MultiRecipeMap PARALLEL_AUTOCLAVE_RECIPES;
    public static MultiRecipeMap PARALLEL_CENTRIFUGE_RECIPES;
    public static MultiRecipeMap PARALLEL_THERMAL_CENTRIFUGE_RECIPES;
    public static MultiRecipeMap PARALLEL_GAS_CENTRIFUGE_RECIPES;
    public static MultiRecipeMap PARALLEL_SIFTER_RECIPES;
    public static MultiRecipeMap PARALLEL_ELECTROLYZER_RECIPES;
    public static MultiRecipeMap PARALLEL_BREWING_MACHINE_RECIPES;
    public static MultiRecipeMap PARALLEL_FERMENTING_RECIPES;
    public static MultiRecipeMap PARALLEL_CHEMICAL_DEHYDRATOR_RECIPES;
    public static MultiRecipeMap PARALLEL_CRACKING_UNIT_RECIPES;

    public static void multiRecipesInit() {
        PARALLEL_CHEMICAL_REACTOR_RECIPES = new MultiRecipeMap(
                0, 3, 0, 3, 0, 5, 0, 4, GARecipeMaps.LARGE_CHEMICAL_RECIPES.getRecipeList());

        PARALLEL_CHEMICAL_PLANT_RECIPES = new MultiRecipeMap(
                0, 6, 0, 4, 0, 5, 0, 4, GARecipeMaps.CHEMICAL_PLANT_RECIPES.getRecipeList());

        PARALLEL_MACERATOR_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 0, 0, 0, 0, RecipeMaps.MACERATOR_RECIPES.getRecipeList());

        PARALLEL_ORE_WASHER_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 0, 1, 0, 0, RecipeMaps.ORE_WASHER_RECIPES.getRecipeList());

        PARALLEL_CHEMICAL_BATH_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 1, 1, 0, 0, RecipeMaps.CHEMICAL_BATH_RECIPES.getRecipeList());

        PARALLEL_SIMPLE_ORE_WASHER_RECIPES = new MultiRecipeMap(
                1, 1, 1, 1, 0, 1, 0, 0, GARecipeMaps.SIMPLE_ORE_WASHER_RECIPES.getRecipeList());

        PARALLEL_AUTOCLAVE_RECIPES = new MultiRecipeMap(
                1, 1, 1, 1, 1, 1, 0, 0, RecipeMaps.AUTOCLAVE_RECIPES.getRecipeList());

        PARALLEL_CENTRIFUGE_RECIPES = new MultiRecipeMap(
                0, 1, 0, 6, 0, 1, 0, 6, GARecipeMaps.LARGE_CENTRIFUGE_RECIPES.getRecipeList());

        PARALLEL_THERMAL_CENTRIFUGE_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 0, 0, 0, 0, RecipeMaps.THERMAL_CENTRIFUGE_RECIPES.getRecipeList());

        PARALLEL_GAS_CENTRIFUGE_RECIPES = new MultiRecipeMap(
                0, 1, 0, 0, 1, 1, 1, 1, GARecipeMaps.GAS_CENTRIFUGE_RECIPES.getRecipeList());

        PARALLEL_SIFTER_RECIPES = new MultiRecipeMap(
                1, 1, 0, 6, 0, 0, 0, 0, RecipeMaps.SIFTER_RECIPES.getRecipeList());

        PARALLEL_ELECTROLYZER_RECIPES = new MultiRecipeMap(
                0, 1, 0, 6, 0, 1, 0, 6, RecipeMaps.ELECTROLYZER_RECIPES.getRecipeList());

        PARALLEL_BREWING_MACHINE_RECIPES = new MultiRecipeMap(
                1, 1, 0, 0, 1, 1, 1, 1, RecipeMaps.BREWING_RECIPES.getRecipeList());

        PARALLEL_FERMENTING_RECIPES = new MultiRecipeMap(
                0, 0, 0, 0, 1, 1, 1, 1, RecipeMaps.FERMENTING_RECIPES.getRecipeList());

        PARALLEL_CHEMICAL_DEHYDRATOR_RECIPES = new MultiRecipeMap(
                0, 2, 0, 9, 0, 2, 0, 2, GARecipeMaps.CHEMICAL_DEHYDRATOR_RECIPES.getRecipeList());

        PARALLEL_CRACKING_UNIT_RECIPES = new MultiRecipeMap(
                0, 0, 0, 0, 2, 2, 1, 2, RecipeMaps.CRACKING_RECIPES.getRecipeList());
    }
}
