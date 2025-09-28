package tj.capability.impl;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import gregtech.api.util.GTUtility;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.builder.RecipeUtility;

import java.util.LinkedList;

public class CraftingRecipeLRUCache {

    private final int capacity;
    private long cacheHit;
    private long cacheMiss;
    private final LinkedList<IRecipe> recipeList = new LinkedList<>();

    public CraftingRecipeLRUCache(int capacity) {
        this.capacity = capacity;
    }

    public void clear() {
        this.recipeList.clear();
        this.cacheHit = 0;
        this.cacheMiss = 0;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public long getCacheHit() {
        return this.cacheHit;
    }

    public long getCacheMiss() {
        return this.cacheMiss;
    }

    public void put(IRecipe recipe) {
        if (this.recipeList.size() >= this.capacity) {
            this.recipeList.removeLast();
        }
        this.recipeList.addFirst(recipe);
    }

    public IRecipe get(IItemHandlerModifiable importInventory) {
        for (IRecipe recipe : this.recipeList) {
            if (recipe == null)
                continue;
            if (RecipeUtility.craftingRecipeMatches(GTUtility.itemHandlerToList(importInventory), recipe.getIngredients()).getLeft()) {
                this.recipeList.remove(recipe);
                this.recipeList.addFirst(recipe);
                this.cacheHit++;
                return recipe;
            }
        }
        this.cacheMiss++;
        return null;
    }
}
