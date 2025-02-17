package com.johny.tj.capability.impl;

import com.johny.tj.builder.multicontrollers.MultipleRecipeMapMultiblockController;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;

public class MultiblockMultiRecipeLogic extends MultiAbstractRecipeLogic {

    public MultiblockMultiRecipeLogic(MultipleRecipeMapMultiblockController tileEntity, int recipeCacheSize) {
        super(tileEntity, recipeCacheSize);
    }

    public IEnergyContainer getEnergyContainer() {
        MultipleRecipeMapMultiblockController controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
        return controller.getEnergyContainer();
    }

    @Override
    protected IItemHandlerModifiable getInputInventory() {
        MultipleRecipeMapMultiblockController controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
        return controller.getInputInventory();
    }

    @Override
    protected IItemHandlerModifiable getOutputInventory() {
        MultipleRecipeMapMultiblockController controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
        return controller.getOutputInventory();
    }

    @Override
    protected IMultipleTankHandler getInputTank() {
        MultipleRecipeMapMultiblockController controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
        return controller.getInputFluidInventory();
    }

    @Override
    protected IMultipleTankHandler getOutputTank() {
        MultipleRecipeMapMultiblockController controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
        return controller.getOutputFluidInventory();
    }

    @Override
    protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
        MultipleRecipeMapMultiblockController controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
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
}
