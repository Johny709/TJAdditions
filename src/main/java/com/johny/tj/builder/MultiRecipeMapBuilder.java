package com.johny.tj.builder;

import gregicadditions.recipes.impl.LargeRecipeBuilder;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.MatchingMode;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.map.MapFluidIngredient;
import gregtech.api.recipes.map.MapItemStackIngredient;
import gregtech.api.util.GTUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.*;


public class MultiRecipeMapBuilder {

    private final int minInputs, maxInputs;
    private final int minOutputs, maxOutputs;
    private final int minFluidInputs, maxFluidInputs;
    private final int minFluidOutputs, maxFluidOutputs;
    private final Collection<Recipe> recipeList = new ArrayList<>();
    private final Map<MapFluidIngredient, Collection<Recipe>> recipeFluidMap = new HashMap<>();
    private final Map<MapItemStackIngredient, Collection<Recipe>> recipeItemMap = new HashMap<>();
    private final Map<Recipe, Byte> recipeIngredientCountMap = new HashMap<>();

    public MultiRecipeMapBuilder(int minInputs, int maxInputs, int minOutputs, int maxOutputs, int minFluidInputs, int maxFluidInputs, int minFluidOutputs, int maxFluidOutputs, LargeRecipeBuilder defaultRecipe) {
        this.minInputs = minInputs;
        this.minFluidInputs = minFluidInputs;
        this.minOutputs = minOutputs;
        this.minFluidOutputs = minFluidOutputs;

        this.maxInputs = maxInputs;
        this.maxFluidInputs = maxFluidInputs;
        this.maxOutputs = maxOutputs;
        this.maxFluidOutputs = maxFluidOutputs;

    }

    public boolean removeRecipe(Recipe recipe) {
        //if we actually removed this recipe
        if (recipeList.remove(recipe)) {
            //also iterate through fluid mappings and remove recipe from them
            recipeFluidMap.values().forEach(fluidMap ->
                    fluidMap.removeIf(fluidRecipe -> fluidRecipe == recipe));
            //also iterate through item mappings and remove recipe from them
            recipeItemMap.values().forEach(itemMap ->
                    itemMap.removeIf(itemRecipe -> itemRecipe == recipe));
            return true;
        }
        return false;
    }

    public void addRecipe(Recipe recipe) {
        this.recipeList.add(recipe);
        HashSet<MapFluidIngredient> uniqueFluidIngredients = new HashSet<>();
        for (FluidStack fluid : recipe.getFluidInputs()) {
            MapFluidIngredient fluidIngredient = new MapFluidIngredient(fluid);
            uniqueFluidIngredients.add(fluidIngredient);
            recipeFluidMap.computeIfAbsent(fluidIngredient, k -> new HashSet<>(1)).add(recipe);
        }

        HashSet<MapItemStackIngredient> uniqueItemIngredients = new HashSet<>();
        for (CountableIngredient item : recipe.getInputs()) {
            Ingredient ingredient = item.getIngredient();
            ItemStack[] itemStacks = ingredient.getMatchingStacks();
            if (itemStacks.length == 0) continue;
            uniqueItemIngredients.add(new MapItemStackIngredient(itemStacks[0].copy()));
            for (ItemStack itemStack : itemStacks) {
                ItemStack newItemStack = itemStack.copy();
                recipeItemMap.computeIfAbsent(new MapItemStackIngredient(newItemStack), k -> new HashSet<>(1)).add(recipe);
            }
        }
        byte uniqueIngredients = 0;
        uniqueIngredients += (byte) (uniqueFluidIngredients.size() + uniqueItemIngredients.size());
        recipeIngredientCountMap.put(recipe, uniqueIngredients);
    }

    public void addRecipes(Collection<Recipe> listOfRecipes) {
        this.recipeList.addAll(listOfRecipes);
        listOfRecipes.forEach(recipe -> {

            HashSet<MapFluidIngredient> uniqueFluidIngredients = new HashSet<>();
            for (FluidStack fluid : recipe.getFluidInputs()) {
                MapFluidIngredient fluidIngredient = new MapFluidIngredient(fluid);
                uniqueFluidIngredients.add(fluidIngredient);
                recipeFluidMap.computeIfAbsent(fluidIngredient, k -> new HashSet<>(1)).add(recipe);
            }

            HashSet<MapItemStackIngredient> uniqueItemIngredients = new HashSet<>();
            for (CountableIngredient item : recipe.getInputs()) {
                Ingredient ingredient = item.getIngredient();
                ItemStack[] itemStacks = ingredient.getMatchingStacks();
                if (itemStacks.length == 0) continue;
                uniqueItemIngredients.add(new MapItemStackIngredient(itemStacks[0].copy()));
                for (ItemStack itemStack : itemStacks) {
                    ItemStack newItemStack = itemStack.copy();
                    recipeItemMap.computeIfAbsent(new MapItemStackIngredient(newItemStack), k -> new HashSet<>(1)).add(recipe);
                }
            }
            byte uniqueIngredients = 0;
            uniqueIngredients += (byte) (uniqueFluidIngredients.size() + uniqueItemIngredients.size());
            recipeIngredientCountMap.put(recipe, uniqueIngredients);
        });
    }
    @Nullable
    public Recipe findRecipe(long voltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, int outputFluidTankCapacity) {
        return this.findRecipe(voltage, GTUtility.itemHandlerToList(inputs), GTUtility.fluidHandlerToList(fluidInputs), outputFluidTankCapacity, MatchingMode.DEFAULT, false);
    }

    @Nullable
    public Recipe findRecipe(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, int outputFluidTankCapacity) {
        return this.findRecipe(voltage, inputs, fluidInputs, outputFluidTankCapacity, MatchingMode.DEFAULT, false);
    }

    @Nullable
    public Recipe findRecipe(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, int outputFluidTankCapacity, boolean useOptimizedRecipeLookUp) {
        return this.findRecipe(voltage, inputs, fluidInputs, outputFluidTankCapacity, MatchingMode.DEFAULT, useOptimizedRecipeLookUp);
    }

    @Nullable
    public Recipe findRecipe(long voltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, int outputFluidTankCapacity, MatchingMode matchingMode) {
        return this.findRecipe(voltage, GTUtility.itemHandlerToList(inputs), GTUtility.fluidHandlerToList(fluidInputs), outputFluidTankCapacity, matchingMode, false);
    }

    @Nullable
    public Recipe findRecipe(long voltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, int outputFluidTankCapacity, boolean useOptimizedRecipeLookUp) {
        return this.findRecipe(voltage, GTUtility.itemHandlerToList(inputs), GTUtility.fluidHandlerToList(fluidInputs), outputFluidTankCapacity, MatchingMode.DEFAULT, useOptimizedRecipeLookUp);
    }

    @Nullable
    public Recipe findRecipe(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, int outputFluidTankCapacity, MatchingMode matchingMode, boolean useOptimizedRecipeLookUp) {

        if (recipeList.isEmpty())
            return null;
        if (minFluidInputs > 0 && GTUtility.amountOfNonNullElements(fluidInputs) < minFluidInputs) {
            return null;
        }
        if (minInputs > 0 && GTUtility.amountOfNonEmptyStacks(inputs) < minInputs) {
            return null;
        }

        if (useOptimizedRecipeLookUp) {
            return findWithHashMap(voltage, inputs, fluidInputs, matchingMode);
        }

        if (maxInputs > 0) {
            return findByInputs(voltage, inputs, fluidInputs, matchingMode);
        } else {
            return findByFluidInputs(voltage, inputs, fluidInputs, matchingMode);
        }
    }

    @Nullable
    private Recipe findByFluidInputs(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, MatchingMode matchingMode) {
        for (FluidStack fluid : fluidInputs) {
            if (fluid == null) continue;
            Collection<Recipe> recipes = recipeFluidMap.get(new MapFluidIngredient(fluid));
            if (recipes == null) continue;
            for (Recipe tmpRecipe : recipes) {
                if (tmpRecipe.matches(false, inputs, fluidInputs, matchingMode)) {
                    return voltage >= tmpRecipe.getEUt() ? tmpRecipe : null;
                }
            }
        }
        return null;
    }

    @Nullable
    private Recipe findByInputs(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, MatchingMode matchingMode) {
        for (Recipe recipe : recipeList) {
            if (recipe.matches(false, inputs, fluidInputs, matchingMode)) {
                return voltage >= recipe.getEUt() ? recipe : null;
            }
        }
        return null;
    }

    @Nullable
    private Recipe findWithHashMap(long voltage, List<ItemStack> inputs, List<FluidStack> fluidInputs, MatchingMode matchingMode) {
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
                    return recipe;
                }
            }
        }
        return null;
    }

}
