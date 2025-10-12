package tj.capability.impl;

import gregtech.api.util.GTUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import tj.builder.RecipeUtility;

import java.util.LinkedList;
import java.util.List;

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

    public Pair<IRecipe, Integer> get(IItemHandlerModifiable importInventory, int parallel) {
        for (IRecipe recipe : this.recipeList) {
            if (recipe == null)
                continue;
            List<ItemStack> inputs = GTUtility.itemHandlerToList(importInventory);
            Triple<Boolean, int[], Integer> matchingRecipe = RecipeUtility.craftingRecipeMatches(inputs, recipe.getIngredients(), parallel);
            if (matchingRecipe.getLeft()) {
                int[] itemAmountInSlot = matchingRecipe.getMiddle();
                for (int i = 0; i < itemAmountInSlot.length; i++) {
                    ItemStack itemInSlot = inputs.get(i);
                    int itemAmount = itemAmountInSlot[i];
                    if (itemInSlot.isEmpty() || itemInSlot.getCount() == itemAmount)
                        continue;
                    itemInSlot.setCount(itemAmountInSlot[i]);
                }
                this.recipeList.remove(recipe);
                this.recipeList.addFirst(recipe);
                this.cacheHit++;
                return Pair.of(recipe, matchingRecipe.getRight());
            }
        }
        this.cacheMiss++;
        return null;
    }
}
