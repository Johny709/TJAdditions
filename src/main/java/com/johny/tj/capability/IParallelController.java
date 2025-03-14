package com.johny.tj.capability;

import gregtech.api.recipes.RecipeMap;

public interface IParallelController {

    long getMaxEUt();

    int getEUBonus();

    long getTotalEnergy();

    long getVoltageTier();

    RecipeMap<?> getMultiblockRecipe();
}
