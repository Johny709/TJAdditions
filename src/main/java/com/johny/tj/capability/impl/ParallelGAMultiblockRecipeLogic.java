package com.johny.tj.capability.impl;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import gregicadditions.GAUtility;
import gregicadditions.utils.GALog;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.GTFluidUtils;
import gregtech.api.util.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;
import java.util.stream.Collectors;


public class ParallelGAMultiblockRecipeLogic extends ParallelMultiblockRecipeLogic {

    private final int EUtPercentage;
    private final int durationPercentage;
    private final int chancePercentage;
    private final int stack;

    public ParallelGAMultiblockRecipeLogic(ParallelRecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
        super(tileEntity, TJConfig.machines.recipeCacheCapacity);
        this.EUtPercentage = EUtPercentage;
        this.durationPercentage = durationPercentage;
        this.chancePercentage = chancePercentage;
        this.stack = stack;
    }

    public int getEUtPercentage() {
        return EUtPercentage;
    }

    public int getDurationPercentage() {
        return durationPercentage;
    }

    public int getChancePercentage() {
        return chancePercentage;
    }

    public int getStack() {
        return stack;
    }

    public boolean isBatching() {
        return true;
    }

    public RecipeMap<?> getRecipeMap() {
        return this.controller.getMultiblockRecipe();
    }

    @Override
    protected boolean trySearchNewRecipeCombined(int i) {
        long maxVoltage = getMaxVoltage();
        Recipe currentRecipe = null;
        IItemHandlerModifiable importInventory = getInputInventory();
        IMultipleTankHandler importFluids = getInputTank();
        Recipe foundRecipe;
        if (lockRecipe[i] && occupiedRecipes[i] != null) {
            if (!occupiedRecipes[i].matches(false, importInventory, importFluids))
                return false;
            foundRecipe = occupiedRecipes[i];
        } else {
            if (!distinct)
                foundRecipe = this.previousRecipe.get(importInventory, importFluids);
            else
                foundRecipe = this.previousRecipe.get(importInventory, importFluids, i, occupiedRecipes);
        }
        if (foundRecipe != null) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = foundRecipe;
        } else {
            boolean dirty = checkRecipeInputsDirty(importInventory, importFluids);
            if (dirty || forceRecipeRecheck[i]) {
                this.forceRecipeRecheck[i] = false;
                //else, try searching new recipe for given inputs
                currentRecipe = findRecipe(maxVoltage, importInventory, importFluids, this.useOptimizedRecipeLookUp);
                if (currentRecipe != null) {
                    this.occupiedRecipes[i] = currentRecipe;
                    this.previousRecipe.put(currentRecipe);
                }
            }
        }
        if (currentRecipe == null) {
            return false;
        }
        if (isBatching()) {
            currentRecipe = createRecipe(maxVoltage, importInventory, importFluids, currentRecipe);
        }
        if (!setupAndConsumeRecipeInputs(currentRecipe)) {
            return false;
        }
        setupRecipe(currentRecipe, i);
        return true;
    }

    @Override
    protected boolean trySearchNewRecipeDistinct(int i) {
        long maxVoltage = getMaxVoltage();
        Recipe currentRecipe;
        List<IItemHandlerModifiable> importInventory = getInputBuses();
        IMultipleTankHandler importFluids = getInputTank();

        // Our caching implementation
        // This guarantees that if we get a recipe cache hit, our efficiency is no different from other machines
        Recipe foundRecipe;
        if (!distinct)
            foundRecipe = this.previousRecipe.get(importInventory.get(lastRecipeIndex[i]), importFluids);
        else
            foundRecipe = this.previousRecipe.get(importInventory.get(lastRecipeIndex[i]), importFluids, i, occupiedRecipes);
        HashSet<Integer> foundRecipeIndex = new HashSet<>();
        if (foundRecipe != null) {
            currentRecipe = foundRecipe;
            currentRecipe = createRecipe(maxVoltage, importInventory.get(lastRecipeIndex[i]), importFluids, currentRecipe);
            if (setupAndConsumeRecipeInputs(currentRecipe, lastRecipeIndex[i])) {
                setupRecipe(currentRecipe, i);
                return true;
            }
            foundRecipeIndex.add(lastRecipeIndex[i]);
        }

        for (int j = 0; j < importInventory.size(); j++) {
            if (j == lastRecipeIndex[i]) {
                continue;
            }
            if (!distinct)
                foundRecipe = this.previousRecipe.get(importInventory.get(j), importFluids);
            else
                foundRecipe = this.previousRecipe.get(importInventory.get(j), importFluids, i, occupiedRecipes);
            if (foundRecipe != null) {
                currentRecipe = foundRecipe;
                currentRecipe = createRecipe(maxVoltage, importInventory.get(j), importFluids, currentRecipe);
                if (setupAndConsumeRecipeInputs(currentRecipe, j)) {
                    setupRecipe(currentRecipe, i);
                    return true;
                }
                foundRecipeIndex.add(j);
            }
        }

        // On a cache miss, our efficiency is much worse, as it will check
        // each bus individually instead of the combined inventory all at once.
        for (int j = 0; j < importInventory.size(); j++) {
            if (foundRecipeIndex.contains(j)) {
                continue;
            }
            IItemHandlerModifiable bus = importInventory.get(j);
            boolean dirty = checkRecipeInputsDirty(bus, importFluids, j);
            if (!dirty && !forceRecipeRecheck[i]) {
                continue;
            }
            this.forceRecipeRecheck[i] = false;
            currentRecipe = findRecipe(maxVoltage, bus, importFluids, this.useOptimizedRecipeLookUp);
            if (currentRecipe == null) {
                continue;
            }
            this.occupiedRecipes[i] = currentRecipe;
            this.previousRecipe.put(currentRecipe);
            if (isBatching()) {
                currentRecipe = createRecipe(maxVoltage, bus, importFluids, currentRecipe);
            }
            if (!setupAndConsumeRecipeInputs(currentRecipe, j)) {
                continue;
            }
            lastRecipeIndex[i] = j;
            setupRecipe(currentRecipe, i);
            return true;
        }
        return false;
    }

    protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
        int maxItemsLimit = getStack();
        int EUt;
        int duration;
        int currentTier = getOverclockingTier(maxVoltage);
        int tierNeeded;
        int minMultiplier = Integer.MAX_VALUE;

        tierNeeded = Math.max(1, GAUtility.getTierByVoltage(matchingRecipe.getEUt()));
        maxItemsLimit *= currentTier - tierNeeded;
        maxItemsLimit = Math.max(1, maxItemsLimit);
        if (maxItemsLimit == 1) {
            return matchingRecipe;
        }

        Set<ItemStack> countIngredients = new HashSet<>();
        if (!matchingRecipe.getInputs().isEmpty()) {
            this.findIngredients(countIngredients, inputs);
            minMultiplier = Math.min(maxItemsLimit, this.getMinRatioItem(countIngredients, matchingRecipe, maxItemsLimit));
        }

        Map<String, Integer> countFluid = new HashMap<>();
        if (!matchingRecipe.getFluidInputs().isEmpty()) {

            this.findFluid(countFluid, fluidInputs);
            minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, maxItemsLimit));
        }

        if (minMultiplier == Integer.MAX_VALUE) {
            GALog.logger.error("Cannot calculate ratio of items for large multiblocks");
            return null;
        }

        EUt = matchingRecipe.getEUt();
        duration = matchingRecipe.getDuration();

        int tierDiff = currentTier - tierNeeded;
        for (int i = 0; i < tierDiff; i++) {
            int attemptItemsLimit = getStack();
            attemptItemsLimit *= tierDiff - i;
            attemptItemsLimit = Math.max(1, attemptItemsLimit);
            attemptItemsLimit = Math.min(minMultiplier, attemptItemsLimit);
            List<CountableIngredient> newRecipeInputs = new ArrayList<>();
            List<FluidStack> newFluidInputs = new ArrayList<>();
            List<ItemStack> outputI = new ArrayList<>();
            List<FluidStack> outputF = new ArrayList<>();
            this.multiplyInputsAndOutputs(newRecipeInputs, newFluidInputs, outputI, outputF, matchingRecipe, attemptItemsLimit);


            RecipeBuilder<?> newRecipe = getRecipeMap().recipeBuilder();
            copyChancedItemOutputs(newRecipe, matchingRecipe, attemptItemsLimit);

            // determine if there is enough room in the output to fit all of this
            // if there isn't, we can't process this recipe.
            List<ItemStack> totalOutputs = newRecipe.getChancedOutputs().stream().map(Recipe.ChanceEntry::getItemStack).collect(Collectors.toList());
            totalOutputs.addAll(outputI);
            boolean canFitOutputs = InventoryUtils.simulateItemStackMerge(totalOutputs, this.getOutputInventory());
            canFitOutputs = canFitOutputs && GTFluidUtils.simulateFluidStackMerge(outputF, this.getOutputTank());
            if (!canFitOutputs) {
                continue;
            }

            newRecipe.inputsIngredients(newRecipeInputs)
                    .fluidInputs(newFluidInputs)
                    .outputs(outputI)
                    .fluidOutputs(outputF)
                    .EUt(Math.max(1, EUt * getEUtPercentage() / 100))
                    .duration((int) Math.max(3, duration * (getDurationPercentage() / 100.0)));

            return newRecipe.build().getResult();
        }
        return matchingRecipe;
    }

    protected void copyChancedItemOutputs(RecipeBuilder<?> newRecipe, Recipe oldRecipe, int multiplier) {
        for (Recipe.ChanceEntry s : oldRecipe.getChancedOutputs()) {
            int chance = Math.min(10000, s.getChance() * getChancePercentage() / 100);
            int boost = s.getBoostPerTier() * getChancePercentage() / 100;
            ItemStack stack = s.getItemStack().copy();
            int count = stack.getCount();
            stack.setCount(count * multiplier);
            newRecipe.chancedOutput(stack, chance, boost);
        }
    }


    protected void findIngredients(Set<ItemStack> countIngredients, IItemHandlerModifiable inputs) {
        for (int slot = 0; slot < inputs.getSlots(); slot++) {
            ItemStack wholeItemStack = inputs.getStackInSlot(slot);
            // skip empty slots
            String name = wholeItemStack.getItem().getUnlocalizedNameInefficiently(wholeItemStack);
            if (name.equals("tile.air"))
                continue;
            boolean found = false;
            for (ItemStack i : countIngredients) {
                if (i.isItemEqual(wholeItemStack)) {
                    i.setCount(i.getCount() + wholeItemStack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                countIngredients.add(wholeItemStack.copy());
            }
        }
    }

    protected int getMinRatioItem(Set<ItemStack> countIngredients, Recipe r, int maxItemsLimit) {
        int minMultiplier = Integer.MAX_VALUE;
        for (CountableIngredient ci : r.getInputs()) {
            if (ci.getCount() == 0) {
                continue;
            }
            for (ItemStack wholeItemStack : countIngredients) {
                if (ci.getIngredient().apply(wholeItemStack)) {
                    int ratio = Math.min(maxItemsLimit, wholeItemStack.getCount() / ci.getCount());
                    if (ratio < minMultiplier) {
                        minMultiplier = ratio;
                    }
                    break;
                }
            }
        }
        return minMultiplier;
    }

    protected int getMinRatioFluid(Map<String, Integer> countFluid, Recipe r, int maxItemsLimit) {
        int minMultiplier = Integer.MAX_VALUE;
        for (FluidStack fs : r.getFluidInputs()) {
            if (fs.amount != 0) { // skip notConsumable fluids
                String name = fs.getFluid().getUnlocalizedName();
                int ratio = Math.min(maxItemsLimit, countFluid.get(name) / fs.amount);
                if (ratio < minMultiplier) {
                    minMultiplier = ratio;
                }
            }
        }
        return minMultiplier;
    }

    protected void findFluid(Map<String, Integer> countFluid, IMultipleTankHandler fluidInputs) {
        for (IFluidTank tank : fluidInputs) {
            if (tank.getFluid() != null) {
                String name = tank.getFluid().getUnlocalizedName();
                if (countFluid.containsKey(name)) {
                    int existingValue = countFluid.get(name);
                    countFluid.put(name, existingValue + tank.getFluidAmount());
                } else {
                    countFluid.put(name, tank.getFluidAmount());
                }
            }
        }
    }

    protected void multiplyInputsAndOutputs(List<CountableIngredient> newRecipeInputs, List<FluidStack> newFluidInputs, List<ItemStack> outputI, List<FluidStack> outputF, Recipe r, int multiplier) {
        for (CountableIngredient ci : r.getInputs()) {
            CountableIngredient newIngredient = new CountableIngredient(ci.getIngredient(), ci.getCount() * multiplier);
            newRecipeInputs.add(newIngredient);
        }
        for (FluidStack fs : r.getFluidInputs()) {
            FluidStack newFluid = new FluidStack(fs.getFluid(), fs.amount * multiplier);
            newFluidInputs.add(newFluid);
        }
        for (ItemStack s : r.getOutputs()) {
            int num = s.getCount() * multiplier;
            ItemStack itemCopy = s.copy();
            itemCopy.setCount(num);
            outputI.add(itemCopy);
        }
        for (FluidStack f : r.getFluidOutputs()) {
            int fluidNum = f.amount * multiplier;
            FluidStack fluidCopy = f.copy();
            fluidCopy.amount = fluidNum;
            outputF.add(fluidCopy);
        }
    }
}
