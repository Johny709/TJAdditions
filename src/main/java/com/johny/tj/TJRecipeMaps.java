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

    public static final RecipeMap<SteamRecipeBuilder> PRIMITIVE_ALLOY_RECIPES = new RecipeMap<>("primitive_alloy", 0, 2, 1, 1, 1, 1, 0, 0, new SteamRecipeBuilder().EUt(0).fluidInputs(Steam.getFluid(1000))).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SteamRecipeBuilder> COKE_OVEN_RECIPES = new RecipeMap<>("coke_oven_2", 0, 1, 1, 1, 0, 0, 1, 1, new SteamRecipeBuilder().EUt(0)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SteamRecipeBuilder> HEAT_EXCHANGER_RECIPES = new RecipeMap<>("heat_exchanger", 0, 0, 0, 0, 1, 2, 1, 2, new SteamRecipeBuilder().EUt(0)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> ARMOR_INFUSER_RECIPES = new RecipeMap<>("armor_infuser", 0, 12, 0, 1, 0, 1, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> DRAGON_REPLICATOR_RECIPES = new RecipeMap<>("dragon_egg_replicator", 0, 2, 0, 3, 0, 1, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> CHAOS_REPLICATOR_RECIPES = new RecipeMap<>("chaos_replicator", 0, 4, 0, 2, 0, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> LARGE_POWERED_SPAWNER_RECIPES = new RecipeMap<>("large_powered_spawner", 0, 2, 0, 1, 0, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> LARGE_VIAL_PROCESSOR_RECIPES = new RecipeMap<>("large_vial_processor", 0, 1, 0, 14, 0, 0, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> GREENHOUSE_TREE_RECIPES = new RecipeMap<>("greenhouse_tree", 0, 1, 0, 8, 0, 1, 0, 0, new SimpleRecipeBuilder().EUt(7680)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> ARCHITECT_RECIPES = new RecipeMap<>("architect", 2, 2, 0, 1, 0, 0, 0, 0, new SimpleRecipeBuilder().EUt(30)).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static final RecipeMap<SimpleRecipeBuilder> ROCK_BREAKER_RECIPES = new RecipeMap<>("rock_breaker", 1, 1, 1, 1, 0, 2, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    public static MultiRecipeMap MULTI_CHEMICAL_REACTOR_RECIPES;
    public static MultiRecipeMap MULTI_CHEMICAL_PLANT_RECIPES;
    public static MultiRecipeMap MULTI_MACERATOR_RECIPES;
    public static MultiRecipeMap MULTI_ORE_WASHER_RECIPES;
    public static MultiRecipeMap MULTI_CENTRIFUGE_RECIPES;
    public static MultiRecipeMap MULTI_THERMAL_CENTRIFUGE_RECIPES;
    public static MultiRecipeMap MULTI_SIFTER_RECIPES;
    public static MultiRecipeMap MULTI_ELECTROLYZER_RECIPES;

    public static void multiRecipesInit() {
        MULTI_CHEMICAL_REACTOR_RECIPES = new MultiRecipeMap(
                0, 3, 0, 3, 0, 5, 0, 4, GARecipeMaps.LARGE_CHEMICAL_RECIPES.getRecipeList());

        MULTI_CHEMICAL_PLANT_RECIPES = new MultiRecipeMap(
                0, 6, 0, 4, 0, 5, 0, 4, GARecipeMaps.CHEMICAL_PLANT_RECIPES.getRecipeList());

        MULTI_MACERATOR_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 0, 0, 0, 0, RecipeMaps.MACERATOR_RECIPES.getRecipeList());

        MULTI_ORE_WASHER_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 0, 1, 0, 0, RecipeMaps.ORE_WASHER_RECIPES.getRecipeList());

        MULTI_CENTRIFUGE_RECIPES = new MultiRecipeMap(
                0, 1, 0, 6, 0, 1, 0, 6, RecipeMaps.CENTRIFUGE_RECIPES.getRecipeList());

        MULTI_THERMAL_CENTRIFUGE_RECIPES = new MultiRecipeMap(
                1, 1, 1, 3, 0, 0, 0, 0, RecipeMaps.THERMAL_CENTRIFUGE_RECIPES.getRecipeList());

        MULTI_SIFTER_RECIPES = new MultiRecipeMap(
                1, 1, 0, 6, 0, 0, 0, 0, RecipeMaps.SIFTER_RECIPES.getRecipeList());

        MULTI_ELECTROLYZER_RECIPES = new MultiRecipeMap(
                0, 1, 0, 6, 0, 1, 0, 6, RecipeMaps.ELECTROLYZER_RECIPES.getRecipeList());
    }

}
