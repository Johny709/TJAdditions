package com.johny.tj.capability;

import gregtech.api.recipes.RecipeMap;

public interface IParallelController {

    default long getMaxEUt() {
        return 0;
    }

    default int getEUBonus() {
        return 0;
    }

    default long getTotalEnergyConsumption() {
        return 0;
    }

    long getVoltageTier();

    default RecipeMap<?> getMultiblockRecipe() {
        return null;
    }
}
