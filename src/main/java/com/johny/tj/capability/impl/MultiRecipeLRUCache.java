package com.johny.tj.capability.impl;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class MultiRecipeLRUCache {
    private final int capacity;
    private Recipe lastAccessedRecipe;
    private final Map<Integer, LinkedList<Recipe>> recipeCaches;
    private int cacheHit = 0;
    private int cacheMiss = 0;
    private boolean isReadAscending = true;

    public MultiRecipeLRUCache(int capacity) {
        this.capacity = capacity;
        this.recipeCaches = new HashMap<>();
    }

    public int getCachedRecipeCount() {
        return this.recipeCaches.size();
    }

    public int getCacheHit() {
        return this.cacheHit;
    }

    public int getCacheMiss() {
        return this.cacheMiss;
    }

    public boolean getIsReadAscending() {
        return this.isReadAscending;
    }

    public void setIsReadAscending(boolean isAscending) {
        this.isReadAscending = isAscending;
    }

    public boolean toggleIsReadAscending() {
        setIsReadAscending(!this.isReadAscending);
        return this.isReadAscending;
    }

    public void clear() {
        this.cacheHit = 0;
        this.cacheMiss = 0;
        this.lastAccessedRecipe = null;
        this.recipeCaches.clear();
    }

    public Recipe get(IItemHandlerModifiable inputItems, IMultipleTankHandler inputFluids, int i) {
        this.recipeCaches.computeIfAbsent(i, k -> new LinkedList<>());
        if (!this.isReadAscending) {
            return getReverse(inputItems, inputFluids, i);
        }
        for (Recipe recipeCache : this.recipeCaches.get(i)) {
            boolean foundMatches = recipeCache.matches(false, inputItems, inputFluids);
            if (foundMatches) {
                this.lastAccessedRecipe = recipeCache;
                return recipeCache;
            }
        }
        return null;
    }

    public Recipe getReverse(IItemHandlerModifiable inputItems, IMultipleTankHandler inputFluids, int i) {
        Iterator<Recipe> recipeCachesIterator = this.recipeCaches.get(i).descendingIterator();
        while (recipeCachesIterator.hasNext()) {
            Recipe recipeCache = recipeCachesIterator.next();
            boolean foundMatches = recipeCache.matches(false, inputItems, inputFluids);
            if (foundMatches) {
                this.lastAccessedRecipe = recipeCache;
                return recipeCache;
            }
        }
        return null;
    }

    public int cacheUtilized(int i) {
        this.recipeCaches.computeIfAbsent(i, k -> new LinkedList<>());
        if (this.lastAccessedRecipe == null) {
            return this.cacheHit;
        }
        this.recipeCaches.get(i).remove(this.lastAccessedRecipe);
        this.recipeCaches.get(i).addFirst(this.lastAccessedRecipe);
        this.cacheHit++;
        return this.cacheHit;
    }

    public int cacheUnutilized() {
        this.cacheMiss++;
        return this.cacheMiss;
    }

    public void put(Recipe value, int i) {
        this.recipeCaches.computeIfAbsent(i, k -> new LinkedList<>());
        if (capacity <= 0) {
            return;
        }

        if (this.recipeCaches.get(i).size() >= this.capacity) {
            this.recipeCaches.get(i).removeLast();
        }

        this.recipeCaches.get(i).addFirst(value);
    }
}
