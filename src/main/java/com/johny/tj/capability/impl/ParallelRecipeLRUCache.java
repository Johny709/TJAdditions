package com.johny.tj.capability.impl;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Arrays;
import java.util.LinkedList;

public class ParallelRecipeLRUCache {

    private final int capacity;
    private long cacheHit;
    private long cacheMiss;
    private final LinkedList<Recipe> recipeList = new LinkedList<>();

    public ParallelRecipeLRUCache(int capacity) {
        this.capacity = capacity;
    }

    public void clear() {
        recipeList.clear();
        cacheHit = 0;
        cacheMiss = 0;
    }

    public int getCapacity() {
        return capacity;
    }

    public long getCacheHit() {
        return cacheHit;
    }

    public long getCacheMiss() {
        return cacheMiss;
    }

    public void put(Recipe recipe) {
        if (recipeList.size() >= capacity) {
            recipeList.removeLast();
        }
        recipeList.addFirst(recipe);
    }

    public Recipe get(IItemHandlerModifiable importInventory, IMultipleTankHandler importFluids) {
        for (Recipe recipe : recipeList) {
            if (recipe == null)
                continue;
            if (recipe.matches(false, importInventory, importFluids)) {
                recipeList.remove(recipe);
                recipeList.addFirst(recipe);
                cacheHit++;
                return recipe;
            }
        }
        cacheMiss++;
        return null;
    }

    public Recipe get(IItemHandlerModifiable importInventory, IMultipleTankHandler importFluids, int i, Recipe[] occupiedRecipes) {
        for (Recipe recipe : recipeList) {
            if (recipe == null)
                continue;
            if (recipe.matches(false, importInventory, importFluids)) {
                if (Arrays.asList(occupiedRecipes).contains(recipe) && recipe != occupiedRecipes[i])
                    continue;
                recipeList.remove(recipe);
                recipeList.addFirst(recipe);
                cacheHit++;
                return recipe;
            }
        }
        cacheMiss++;
        return null;
    }
}
