package com.johny.tj.builder;

import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.util.EnumValidationResult;
import gregtech.api.util.GTLog;
import gregtech.api.util.ValidationResult;
import org.jetbrains.annotations.NotNull;

public class SteamRecipeBuilder extends RecipeBuilder<SteamRecipeBuilder> {

    public SteamRecipeBuilder () {
    }

    public SteamRecipeBuilder(RecipeBuilder<SteamRecipeBuilder> recipeBuilder) {
        super(recipeBuilder);
    }

    @Override
    @NotNull
    public SteamRecipeBuilder copy() {
        return new SteamRecipeBuilder(this);
    }

    @Override
    @NotNull
    public ValidationResult<Recipe> build() {
        return ValidationResult.newResult(finalizeAndValidate(),
                new Recipe(inputs, outputs, chancedOutputs, fluidInputs, fluidOutputs, duration, EUt, hidden));
    }

    @Override
    @NotNull
    protected EnumValidationResult validate() {
        if (EUt < 0) {
            GTLog.logger.error("EU/t cannot be less than 0", new IllegalArgumentException());
            recipeStatus = EnumValidationResult.INVALID;
        }
        if (duration <= 0) {
            GTLog.logger.error("Duration cannot be less or equal to 0", new IllegalArgumentException());
            recipeStatus = EnumValidationResult.INVALID;
        }
        if (recipeStatus == EnumValidationResult.INVALID) {
            GTLog.logger.error("Invalid recipe, read the errors above: {}", this);
        }
        return recipeStatus;
    }
}
