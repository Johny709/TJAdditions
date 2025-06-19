package com.johny.tj.builder;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.MatchingMode;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.map.MapFluidIngredient;
import gregtech.api.recipes.map.MapItemStackIngredient;
import gregtech.api.util.GTUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.*;


public final class ParallelRecipeMap {

    private final int minInputs, maxInputs;
    private final int minOutputs, maxOutputs;
    private final int minFluidInputs, maxFluidInputs;
    private final int minFluidOutputs, maxFluidOutputs;
    private final Collection<Recipe> recipeList;
    private final Map<MapFluidIngredient, Collection<Recipe>> recipeFluidMap;
    private final Map<MapItemStackIngredient, Collection<Recipe>> recipeItemMap;
    private final Map<Recipe, Byte> recipeIngredientCountMap = new HashMap<>();
    private final RecipeMap<?> recipeMap;

    public ParallelRecipeMap(RecipeMap<?> recipeMap) {
        this.recipeMap = recipeMap;
        this.minInputs = recipeMap.getMinInputs();
        this.minFluidInputs = recipeMap.getMinFluidInputs();
        this.minOutputs = recipeMap.getMinOutputs();
        this.minFluidOutputs = recipeMap.getMinFluidOutputs();

        this.maxInputs = recipeMap.getMaxInputs();
        this.maxFluidInputs = recipeMap.getMaxFluidInputs();
        this.maxOutputs = recipeMap.getMaxOutputs();
        this.maxFluidOutputs = recipeMap.getMaxFluidOutputs();

        this.recipeList = Collections.unmodifiableCollection(recipeMap.getRecipeList());
        Map<MapFluidIngredient, Collection<Recipe>> recipeFluidMapInit = new HashMap<>();
        Map<MapItemStackIngredient, Collection<Recipe>> recipeItemMapInit = new HashMap<>();
        this.recipeList.forEach(recipe -> {

            HashSet<MapFluidIngredient> uniqueFluidIngredients = new HashSet<>();
            for (FluidStack fluid : recipe.getFluidInputs()) {
                MapFluidIngredient fluidIngredient = new MapFluidIngredient(fluid);
                uniqueFluidIngredients.add(fluidIngredient);
                recipeFluidMapInit.computeIfAbsent(fluidIngredient, k -> new HashSet<>(1)).add(recipe);
            }

            HashSet<MapItemStackIngredient> uniqueItemIngredients = new HashSet<>();
            for (CountableIngredient item : recipe.getInputs()) {
                Ingredient ingredient = item.getIngredient();
                ItemStack[] itemStacks = ingredient.getMatchingStacks();
                if (itemStacks.length == 0) continue;
                uniqueItemIngredients.add(new MapItemStackIngredient(itemStacks[0].copy()));
                for (ItemStack itemStack : itemStacks) {
                    ItemStack newItemStack = itemStack.copy();
                    recipeItemMapInit.computeIfAbsent(new MapItemStackIngredient(newItemStack), k -> new HashSet<>(1)).add(recipe);
                }
            }
            byte uniqueIngredients = 0;
            uniqueIngredients += (byte) (uniqueFluidIngredients.size() + uniqueItemIngredients.size());
            recipeIngredientCountMap.put(recipe, uniqueIngredients);
        });

        this.recipeFluidMap = Collections.unmodifiableMap(recipeFluidMapInit);
        this.recipeItemMap = Collections.unmodifiableMap(recipeItemMapInit);
    }

    public RecipeMap<?> getRecipeMap() {
        return recipeMap;
    }

    public int getMinFluidOutputs() {
        return minFluidOutputs;
    }

    public int getMinFluidInputs() {
        return minFluidInputs;
    }

    public int getMinInputs() {
        return minInputs;
    }

    public int getMinOutputs() {
        return minOutputs;
    }

    @Nullable
    public Recipe findRecipe(long voltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, int outputFluidTankCapacity, boolean useOptimizedRecipeLookUp, Recipe[] occupiedRecipes, boolean distinct) {
        return this.findRecipe(voltage, GTUtility.itemHandlerToList(inputs), GTUtility.fluidHandlerToList(fluidInputs), outputFluidTankCapacity, MatchingMode.DEFAULT, useOptimizedRecipeLookUp, occupiedRecipes, distinct);
    }

    @Nullable
    public Recipe findRecipe(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, int outputFluidTankCapacity, MatchingMode matchingMode, boolean useOptimizedRecipeLookUp, Recipe[] occupiedRecipes, boolean distinct) {

        if (recipeList.isEmpty())
            return null;
        if (minFluidInputs > 0 && GTUtility.amountOfNonNullElements(fluidInputs) < minFluidInputs) {
            return null;
        }
        if (minInputs > 0 && GTUtility.amountOfNonEmptyStacks(inputs) < minInputs) {
            return null;
        }

        if (useOptimizedRecipeLookUp) {
            return findWithHashMap(voltage, inputs, fluidInputs, matchingMode, occupiedRecipes, distinct);
        }

        if (maxInputs > 0) {
            return findByInputs(voltage, inputs, fluidInputs, matchingMode, occupiedRecipes, distinct);
        } else {
            return findByFluidInputs(voltage, inputs, fluidInputs, matchingMode, occupiedRecipes, distinct);
        }
    }

    @Nullable
    private Recipe findByFluidInputs(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, MatchingMode matchingMode, Recipe[] occupiedRecipes, boolean distinct) {
        for (FluidStack fluid : fluidInputs) {
            if (fluid == null) continue;
            Collection<Recipe> recipes = recipeFluidMap.get(new MapFluidIngredient(fluid));
            if (recipes == null) continue;
            for (Recipe tmpRecipe : recipes) {
                if (tmpRecipe.matches(false, inputs, fluidInputs, matchingMode)) {
                    if (distinct) {
                        if (Arrays.asList(occupiedRecipes).contains(tmpRecipe))
                            continue;
                    }
                    return voltage >= tmpRecipe.getEUt() ? tmpRecipe : null;
                }
            }
        }
        return null;
    }

    @Nullable
    private Recipe findByInputs(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, MatchingMode matchingMode, Recipe[] occupiedRecipes, boolean distinct) {
        for (Recipe recipe : recipeList) {
            if (recipe.matches(false, inputs, fluidInputs, matchingMode)) {
                if (distinct) {
                    if (Arrays.asList(occupiedRecipes).contains(recipe))
                        continue;
                }
                return voltage >= recipe.getEUt() ? recipe : null;
            }
        }
        return null;
    }

    @Nullable
    public Recipe findByInputsAndOutputs(long voltage, List<ItemStack> inputs, List<ItemStack> outputs, List<FluidStack> fluidInputs, List<FluidStack> fluidOutputs) {
        for (Recipe recipe : recipeList) {
            if (RecipeUtility.recipeMatches(recipe, inputs, outputs, fluidInputs, fluidOutputs)) {
                return voltage >= recipe.getEUt() ? recipe : null;
            }
        }
        return null;
    }

    @Nullable
    private Recipe findWithHashMap(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, MatchingMode matchingMode, Recipe[] occupiedRecipes, boolean distinct) {
        HashSet<MapItemStackIngredient> uniqueItems = new HashSet<>();
        HashSet<MapFluidIngredient> uniqueFluids = new HashSet<>();

        for (ItemStack item : inputs) {
            uniqueItems.add(new MapItemStackIngredient(item));
        }
        for (FluidStack fluid : fluidInputs) {
            if (fluid == null) continue;
            uniqueFluids.add(new MapFluidIngredient(fluid));
        }

        HashMap<Recipe, Byte> recipeLeftoverIngredients = new HashMap<>();
        for (MapItemStackIngredient item : uniqueItems) {
            boolean hasRecipes = recipeItemMap.containsKey(item);
            if (!hasRecipes) continue;
            Collection<Recipe> recipes = recipeItemMap.get(item);
            for (Recipe recipe : recipes) {
                Byte leftOverIngredients = recipeLeftoverIngredients.getOrDefault(recipe,
                        recipeIngredientCountMap.getOrDefault(recipe, (byte) 0));
                leftOverIngredients--;
                recipeLeftoverIngredients.put(recipe, leftOverIngredients);
                if (leftOverIngredients > 0) {
                    continue;
                }
                int v = recipe.getEUt();
                if (voltage < v) {
                    continue;
                }
                boolean isMatch = recipe.matches(false, inputs, fluidInputs, matchingMode);
                if (isMatch) {
                    if (distinct) {
                        if (Arrays.asList(occupiedRecipes).contains(recipe))
                            continue;
                    }
                    return recipe;
                }
            }
        }
        for (MapFluidIngredient fluid : uniqueFluids) {
            boolean hasRecipes = recipeFluidMap.containsKey(fluid);
            if (!hasRecipes) continue;
            Collection<Recipe> recipes = recipeFluidMap.get(fluid);
            for (Recipe recipe : recipes) {
                Byte leftOverIngredients = recipeLeftoverIngredients.getOrDefault(recipe,
                        recipeIngredientCountMap.getOrDefault(recipe, (byte) 0));
                leftOverIngredients--;
                recipeLeftoverIngredients.put(recipe, leftOverIngredients);
                if (leftOverIngredients > 0) {
                    continue;
                }
                int v = recipe.getEUt();
                if (voltage < v) {
                    continue;
                }
                boolean isMatch = recipe.matches(false, inputs, fluidInputs, matchingMode);
                if (isMatch) {
                    if (distinct) {
                        if (Arrays.asList(occupiedRecipes).contains(recipe))
                            continue;
                    }
                    return recipe;
                }
            }
        }
        return null;
    }

    public Recipe findRecipe(Recipe recipe) {
        for (Map.Entry<MapItemStackIngredient, Collection<Recipe>> map : recipeItemMap.entrySet()) {
            if (map == null) continue;
            for (Recipe foundRecipe : map.getValue()) {
                if (foundRecipe == null) continue;
                if (foundRecipe.getInputs().toString().equals(recipe.getInputs().toString()) &&
                        getOutputCountMatches(recipe, foundRecipe) &&
                        foundRecipe.getFluidInputs().equals(recipe.getFluidInputs()) &&
                        foundRecipe.getFluidOutputs().equals(recipe.getFluidOutputs()) &&
                        foundRecipe.getChancedOutputs().equals(recipe.getChancedOutputs()) &&
                        foundRecipe.getEUt() == recipe.getEUt() &&
                        foundRecipe.getDuration() == recipe.getDuration()) {
                    return foundRecipe;
                }
            }
        }
        for (Map.Entry<MapFluidIngredient, Collection<Recipe>> map : recipeFluidMap.entrySet()) {
            if (map == null) continue;
            for (Recipe foundRecipe : map.getValue()) {
                if (foundRecipe == null) continue;
                if (foundRecipe.getInputs().toString().equals(recipe.getInputs().toString()) &&
                        getOutputCountMatches(recipe, foundRecipe) &&
                        foundRecipe.getFluidInputs().equals(recipe.getFluidInputs()) &&
                        foundRecipe.getFluidOutputs().equals(recipe.getFluidOutputs()) &&
                        foundRecipe.getChancedOutputs().equals(recipe.getChancedOutputs()) &&
                        foundRecipe.getEUt() == recipe.getEUt() &&
                        foundRecipe.getDuration() == recipe.getDuration()) {
                    return foundRecipe;
                }
            }
        }
        return null;
    }

    private static boolean getOutputCountMatches(Recipe recipe, Recipe foundRecipe) {
        int outputCountMatches = 0;
        for (int i = 0; i < recipe.getOutputs().size(); i++) {
            if (foundRecipe.getOutputs().isEmpty()) continue;
            ItemStack itemInput = foundRecipe.getOutputs().get(i);
            ItemStack newItemInput = recipe.getOutputs().get(i);
            if (itemInput.getTranslationKey().equals(newItemInput.getTranslationKey()) &&
                    itemInput.getCount() == newItemInput.getCount() &&
                    itemInput.getMetadata() == newItemInput.getMetadata()) outputCountMatches++;
        }
        return outputCountMatches >= foundRecipe.getOutputs().size();
    }

}
