package com.johny.tj.capability.impl;

import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.recipes.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ParallelMultiblockRecipeLogic extends ParallelAbstractRecipeLogic {

    // Field used for maintenance
    protected int previousRecipeDuration;

    // Fields used for distinct mode
    protected int[] lastRecipeIndex;
    protected ItemStack[][] lastItemInputsMatrix;

    public ParallelMultiblockRecipeLogic(ParallelRecipeMapMultiblockController tileEntity, int recipeCacheSize) {
        super(tileEntity, recipeCacheSize);
        this.lastRecipeIndex = new int[1];
    }

    @Override
    public void setLayer(int i, boolean remove) {
        super.setLayer(i, remove);
        this.lastRecipeIndex = Arrays.copyOf(lastRecipeIndex, getSize());
    }

    public IEnergyContainer getEnergyContainer() {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        return controller.getEnergyContainer();
    }

    @Override
    protected IItemHandlerModifiable getInputInventory() {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        return controller.getInputInventory();
    }

    @Override
    protected IItemHandlerModifiable getOutputInventory() {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        return controller.getOutputInventory();
    }

    @Override
    protected IMultipleTankHandler getInputTank() {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        return controller.getInputFluidInventory();
    }

    @Override
    protected IMultipleTankHandler getOutputTank() {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        return controller.getOutputFluidInventory();
    }

    @Override
    protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        if (controller.checkRecipe(recipe, false) &&
                super.setupAndConsumeRecipeInputs(recipe)) {
            controller.checkRecipe(recipe, true);
            return true;
        } else return false;
    }

    @Override
    protected long getEnergyStored() {
        return getEnergyContainer().getEnergyStored();
    }

    @Override
    protected long getEnergyCapacity() {
        return getEnergyContainer().getEnergyCapacity();
    }

    @Override
    protected boolean drawEnergy(int recipeEUt) {
        long resultEnergy = getEnergyStored() - recipeEUt;
        if (resultEnergy >= 0L && resultEnergy <= getEnergyCapacity()) {
            getEnergyContainer().changeEnergy(-recipeEUt);
            return true;
        } else return false;
    }

    @Override
    protected long getMaxVoltage() {
        return Math.max(getEnergyContainer().getInputVoltage(), getEnergyContainer().getOutputVoltage());
    }

    /**
     * Used to reset cached values after multiblock structure deforms
     */
    public void invalidate() {
        Arrays.fill(lastRecipeIndex, 0);
    }

    protected List<IItemHandlerModifiable> getInputBuses() {
        return controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
    }

    @Override
    protected int[] calculateOverclock(int EUt, long voltage, int duration) {
        int numMaintenanceProblems = (this.metaTileEntity instanceof ParallelRecipeMapMultiblockController) ?
                ((ParallelRecipeMapMultiblockController) metaTileEntity).getNumProblems() : 0;

        double maintenanceDurationMultiplier = 1.0 + (0.2 * numMaintenanceProblems);
        int durationModified = (int) (duration * maintenanceDurationMultiplier);

        if (!allowOverclocking) {
            return new int[]{EUt, durationModified};
        }
        boolean negativeEU = EUt < 0;
        int tier = getOverclockingTier(voltage);
        if (GAValues.V[tier] <= EUt || tier == 0)
            return new int[]{EUt, durationModified};
        if (negativeEU)
            EUt = -EUt;
        int resultEUt = EUt;
        double resultDuration = durationModified;
        //do not overclock further if duration is already too small
        while (resultDuration >= 1 && resultEUt <= GAValues.V[tier - 1]) {
            resultEUt *= 4;
            resultDuration /= 2.8;
        }
        previousRecipeDuration = (int) resultDuration;
        return new int[]{negativeEU ? -resultEUt : resultEUt, (int) Math.ceil(resultDuration)};
    }

    @Override
    protected int getOverclockingTier(long voltage) {
        return GAUtility.getTierByVoltage(voltage);
    }

    @Override
    protected void completeRecipe(int i) {
        super.completeRecipe(i);
        if (metaTileEntity instanceof ParallelRecipeMapMultiblockController) {
            ParallelRecipeMapMultiblockController gaController = (ParallelRecipeMapMultiblockController)metaTileEntity;
            //if (gaController.hasMufflerHatch()) {
            //    gaController.outputRecoveryItems();
            //}
            if (gaController.hasMaintenanceHatch()) {
                gaController.calculateMaintenance(previousRecipeDuration);
                previousRecipeDuration = 0;
            }
        }
    }

    @Override
    protected boolean trySearchNewRecipe(int i) {
        if (metaTileEntity instanceof ParallelRecipeMapMultiblockController) {
            ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController)metaTileEntity;
            if (controller.getNumProblems() > 5)
                return false;

            if (controller.isDistinctBus())
                return trySearchNewRecipeDistinct(i);

        }
        return trySearchNewRecipeCombined(i);

    }

    // TODO May need to do more here
    protected boolean trySearchNewRecipeCombined(int i) {
        return super.trySearchNewRecipe(i);
    }

    protected boolean trySearchNewRecipeDistinct(int i) {
        long maxVoltage = getMaxVoltage();
        Recipe currentRecipe = null;
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
                foundRecipe = this.previousRecipe.get(importInventory.get(lastRecipeIndex[i]), importFluids);
            else
                foundRecipe = this.previousRecipe.get(importInventory.get(lastRecipeIndex[i]), importFluids, i, occupiedRecipes);
            if (foundRecipe != null) {
                currentRecipe = foundRecipe;
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
            if (!setupAndConsumeRecipeInputs(currentRecipe, j)) {
                continue;
            }
            lastRecipeIndex[i] = j;
            setupRecipe(currentRecipe, i);
            return true;
        }
        return false;
    }

    // Replacing this for optimization reasons
    protected boolean checkRecipeInputsDirty(IItemHandler inputs, IMultipleTankHandler fluidInputs, int index) {
        boolean shouldRecheckRecipe = false;

        if (lastItemInputsMatrix == null || lastItemInputsMatrix.length != getInputBuses().size()) {
            lastItemInputsMatrix = new ItemStack[getInputBuses().size()][];
        }
        if (lastItemInputsMatrix[index] == null || lastItemInputsMatrix[index].length != inputs.getSlots()) {
            this.lastItemInputsMatrix[index] = new ItemStack[inputs.getSlots()];
            Arrays.fill(lastItemInputsMatrix[index], ItemStack.EMPTY);
        }
        if (lastFluidInputs == null || lastFluidInputs.length != fluidInputs.getTanks()) {
            this.lastFluidInputs = new FluidStack[fluidInputs.getTanks()];
        }
        for (int j = 0; j < lastItemInputsMatrix[index].length; j++) {
            ItemStack currentStack = inputs.getStackInSlot(j);
            ItemStack lastStack = lastItemInputsMatrix[index][j];
            if (!areItemStacksEqual(currentStack, lastStack)) {
                this.lastItemInputsMatrix[index][j] = currentStack.isEmpty() ? ItemStack.EMPTY : currentStack.copy();
                shouldRecheckRecipe = true;
            } else if (currentStack.getCount() != lastStack.getCount()) {
                lastStack.setCount(currentStack.getCount());
                shouldRecheckRecipe = true;
            }
        }
        for (int j = 0; j < lastFluidInputs.length; j++) {
            FluidStack currentStack = fluidInputs.getTankAt(j).getFluid();
            FluidStack lastStack = lastFluidInputs[j];
            if ((currentStack == null && lastStack != null) ||
                    (currentStack != null && !currentStack.isFluidEqual(lastStack))) {
                this.lastFluidInputs[j] = currentStack == null ? null : currentStack.copy();
                shouldRecheckRecipe = true;
            } else if (currentStack != null && lastStack != null &&
                    currentStack.amount != lastStack.amount) {
                lastStack.amount = currentStack.amount;
                shouldRecheckRecipe = true;
            }
        }
        return shouldRecheckRecipe;
    }

    protected boolean setupAndConsumeRecipeInputs(Recipe recipe, int index) {
        ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        if (controller.checkRecipe(recipe, false)) {

            int[] resultOverclock = calculateOverclock(recipe.getEUt(), recipe.getDuration());
            int totalEUt = resultOverclock[0] * resultOverclock[1];
            IItemHandlerModifiable importInventory = getInputBuses().get(index);
            IItemHandlerModifiable exportInventory = getOutputInventory();
            IMultipleTankHandler importFluids = getInputTank();
            IMultipleTankHandler exportFluids = getOutputTank();
            boolean setup = (totalEUt >= 0 ? getEnergyStored() >= (totalEUt > getEnergyCapacity() / 2 ? resultOverclock[0] : totalEUt) :
                    (getEnergyStored() - resultOverclock[0] <= getEnergyCapacity())) &&
                    MetaTileEntity.addItemsToItemHandler(exportInventory, true, recipe.getAllItemOutputs(exportInventory.getSlots())) &&
                    MetaTileEntity.addFluidsToFluidHandler(exportFluids, true, recipe.getFluidOutputs()) &&
                    recipe.matches(true, importInventory, importFluids);

            if (setup) {
                controller.checkRecipe(recipe, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.lastRecipeIndex = new int[getSize()];
    }
}
