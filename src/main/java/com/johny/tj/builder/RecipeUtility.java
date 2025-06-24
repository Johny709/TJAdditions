package com.johny.tj.builder;

import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeUtility {
    public static boolean recipeMatches(Recipe recipe, List<ItemStack> inputs, List<ItemStack> outputs, List<FluidStack> fluidInputs, List<FluidStack> fluidOutputs) {
        Pair<Boolean, Integer[]> inputItems = matchesInputItems(inputs, recipe.getInputs());
        if (!inputItems.getKey()) {
            return false;
        }
        Pair<Boolean, Integer[]> inputFluids = matchesFluids(fluidInputs, recipe.getFluidInputs());
        if (!inputFluids.getKey()) {
            return false;
        }
        List<ItemStack> recipeItemOutputs = new ArrayList<>(recipe.getOutputs());
        recipeItemOutputs.addAll(recipe.getChancedOutputs()
                .stream()
                .map(Recipe.ChanceEntry::getItemStack)
                .collect(Collectors.toCollection(ArrayList::new)));
        Pair<Boolean, Integer[]> outputItems = matchesOutputItems(outputs, recipeItemOutputs);
        if (!outputItems.getKey()) {
            return false;
        }
        Pair<Boolean, Integer[]> outputFluids = matchesFluids(fluidOutputs, recipe.getFluidOutputs());
        return outputFluids.getKey();
    }

    private static Pair<Boolean, Integer[]> matchesInputItems(List<ItemStack> itemStackContainer, List<CountableIngredient> countableIngredients) {
        Integer[] itemAmountInSlot = new Integer[itemStackContainer.size()];

        for (int i = 0; i < itemAmountInSlot.length; i++) {
            ItemStack itemInSlot = itemStackContainer.get(i);
            itemAmountInSlot[i] = itemInSlot.isEmpty() ? 0 : itemInSlot.getCount();
        }

        for (CountableIngredient ingredient : countableIngredients) {
            int ingredientAmount = ingredient.getCount();
            boolean isNotConsumed = false;
            if (ingredientAmount == 0) {
                ingredientAmount = 1;
                isNotConsumed = true;
            }
            for (int i = 0; i < itemStackContainer.size(); i++) {
                ItemStack inputStack = itemStackContainer.get(i);
                if (inputStack.isEmpty() || !ingredient.getIngredient().apply(inputStack))
                    continue;
                int itemAmountToConsume = Math.min(itemAmountInSlot[i], ingredientAmount);
                ingredientAmount -= itemAmountToConsume;
                if (!isNotConsumed) itemAmountInSlot[i] -= itemAmountToConsume;
                if (ingredientAmount == 0) break;
            }
            if (ingredientAmount > 0)
                return Pair.of(false, itemAmountInSlot);
        }

        return Pair.of(true, itemAmountInSlot);
    }

    private static Pair<Boolean, Integer[]> matchesOutputItems(List<ItemStack> itemStackContainer, List<ItemStack> itemStackSearchList) {
        Integer[] itemAmountInSlot = new Integer[itemStackContainer.size()];

        for (int i = 0; i < itemAmountInSlot.length; i++) {
            ItemStack itemInSlot = itemStackContainer.get(i);
            itemAmountInSlot[i] = itemInSlot.isEmpty() ? 0 : itemInSlot.getCount();
        }

        for (ItemStack itemStack : itemStackSearchList) {
            int itemAmount = itemStack.getCount();
            for (int i = 0; i < itemStackContainer.size(); i++) {
                ItemStack outputStack = itemStackContainer.get(i);
                if (outputStack.isEmpty() || !outputStack.isItemEqual(itemStack))
                    continue;
                int itemAmountToConsume = Math.min(itemAmountInSlot[i], itemAmount);
                itemAmount -= itemAmountToConsume;
                if (itemAmount == 0) break;
            }
            if (itemAmount > 0)
                return Pair.of(false, itemAmountInSlot);
        }
        return Pair.of(true, itemAmountInSlot);
    }

    private static Pair<Boolean, Integer[]> matchesFluids(List<FluidStack> fluidStackContainer, List<FluidStack> fluidStackSearchList) {
        Integer[] fluidAmountInTank = new Integer[fluidStackContainer.size()];

        for (int i = 0; i < fluidAmountInTank.length; i++) {
            FluidStack fluidInTank = fluidStackContainer.get(i);
            fluidAmountInTank[i] = fluidInTank == null ? 0 : fluidInTank.amount;
        }

        for (FluidStack fluid : fluidStackSearchList) {
            int fluidAmount = fluid.amount;
            boolean isNotConsumed = false;
            if (fluidAmount == 0) {
                fluidAmount = 1;
                isNotConsumed = true;
            }
            for (int i = 0; i < fluidStackContainer.size(); i++) {
                FluidStack tankFluid = fluidStackContainer.get(i);
                if (tankFluid == null || !tankFluid.isFluidEqual(fluid))
                    continue;
                int fluidAmountToConsume = Math.min(fluidAmountInTank[i], fluidAmount);
                fluidAmount -= fluidAmountToConsume;
                if (!isNotConsumed) fluidAmountInTank[i] -= fluidAmountToConsume;
                if (fluidAmount == 0) break;
            }
            if (fluidAmount > 0)
                return Pair.of(false, fluidAmountInTank);
        }
        return Pair.of(true, fluidAmountInTank);
    }
}
