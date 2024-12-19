package com.johny.tj;


import crafttweaker.annotations.ZenRegister;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.builders.SimpleRecipeBuilder;
import stanhebben.zenscript.annotations.ZenProperty;

@ZenRegister
public class TJRecipeMaps {

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> PRIMITIVE_ALLOY_RECIPES = new RecipeMap<>("primitive_alloy", 0, 2, 1, 1, 1, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> COKE_OVEN_RECIPES = new RecipeMap<>("coke_oven_2", 0, 1, 1, 1, 0, 0, 1, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> HEAT_EXCHANGER_RECIPES = new RecipeMap<>("heat_exchanger", 0, 0, 0, 0, 1, 2, 1, 2, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> ARMOR_INFUSER_RECIPES = new RecipeMap<>("armor_infuser", 0, 12, 0, 1, 0, 1, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> DRAGON_REPLICATOR_RECIPES = new RecipeMap<>("dragon_egg_replicator", 0, 2, 0, 3, 0, 1, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> CHAOS_REPLICATOR_RECIPES = new RecipeMap<>("chaos_replicator", 0, 4, 0, 2, 0, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> LARGE_POWERED_SPAWNER_RECIPES = new RecipeMap<>("large_powered_spawner", 0, 2, 0, 1, 0, 1, 0, 0, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

    @ZenProperty
    public static final RecipeMap<SimpleRecipeBuilder> LARGE_VIAL_PROCESSOR_RECIPES = new RecipeMap<>("large_vial_processor", 0, 1, 0, 14, 0, 0, 0, 1, new SimpleRecipeBuilder()).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL);

}
