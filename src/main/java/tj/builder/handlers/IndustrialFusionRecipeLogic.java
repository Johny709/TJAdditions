package tj.builder.handlers;

import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.utils.GALog;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.recipeproperties.FusionEUToStartProperty;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndustrialFusionRecipeLogic extends LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic {

    private final int EUtPercentage;
    private final int durationPercentage;
    private final IFusionProvider fusionReactor;

    public IndustrialFusionRecipeLogic(RecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack, boolean standardOC) {
        super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack);
        this.fusionReactor = (IFusionProvider) tileEntity;
        this.EUtPercentage = EUtPercentage;
        this.durationPercentage = durationPercentage;
        this.recipeMap = tileEntity.recipeMap;
        this.allowOverclocking = standardOC;
    }

    @Override
    protected void completeRecipe() {
        super.completeRecipe();
        this.fusionReactor.setRecipe(0L, null);
    }

    @Override
    protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
        int EUt;
        int duration;
        int minMultiplier = Integer.MAX_VALUE;
        long recipeEnergy = Math.max(160_000_000, matchingRecipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L));

        this.fusionReactor.setRecipe(recipeEnergy, matchingRecipe);
        Map<String, Integer> countFluid = new HashMap<>();
        if (!matchingRecipe.getFluidInputs().isEmpty()) {

            this.findFluid(countFluid, fluidInputs);
            minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, this.fusionReactor.getParallels() * this.fusionReactor.getBatchMode().getAmount()));
        }

        if (minMultiplier == Integer.MAX_VALUE) {
            GALog.logger.error("Cannot calculate ratio of items for large multiblocks");
            return null;
        }
        EUt = matchingRecipe.getEUt();
        duration = matchingRecipe.getDuration();

        float tierDiff = this.allowOverclocking ? 1 : fusionOverclockMultiplier(this.fusionReactor.getEnergyToStart(), recipeEnergy);

        List<FluidStack> newFluidInputs = new ArrayList<>();
        List<FluidStack> outputF = new ArrayList<>();
        multiplyInputsAndOutputs(newFluidInputs, outputF, matchingRecipe, minMultiplier);

        RecipeBuilder<?> newRecipe = this.recipeMap.recipeBuilder();

        newRecipe.fluidInputs(newFluidInputs)
                .fluidOutputs(outputF)
                .EUt((int) Math.max(1, ((EUt * this.EUtPercentage * minMultiplier / 100.0) * tierDiff) / this.fusionReactor.getBatchMode().getAmount()))
                .duration((int) Math.max(1, ((duration * (this.durationPercentage / 100.0)) / tierDiff) * this.fusionReactor.getBatchMode().getAmount()));

        return newRecipe.build().getResult();
    }

    private void multiplyInputsAndOutputs(List<FluidStack> newFluidInputs, List<FluidStack> outputF, Recipe recipe, int multiplier) {
        for (FluidStack fluidS : recipe.getFluidInputs()) {
            FluidStack newFluid = new FluidStack(fluidS.getFluid(), fluidS.amount * multiplier);
            newFluidInputs.add(newFluid);
        }
        for (FluidStack fluid : recipe.getFluidOutputs()) {
            int fluidNum = fluid.amount * multiplier;
            FluidStack fluidCopy = fluid.copy();
            fluidCopy.amount = fluidNum;
            outputF.add(fluidCopy);
        }
    }

    private float fusionOverclockMultiplier(long energyToStart, long recipeEnergy) {
        recipeEnergy = Math.max(160_000_000, recipeEnergy);
        long recipeEnergyOld = recipeEnergy;
        float OCMultiplier = 1;
        while (recipeEnergy <= energyToStart) {
            if (recipeEnergy != recipeEnergyOld)
                OCMultiplier *= recipeEnergy > 640_000_000 ? 4 : 2.8F;
            recipeEnergy *= 2;
        }
        return OCMultiplier;
    }

    @Override
    protected void setActive(boolean active) {
        this.fusionReactor.replaceEnergyPortsAsActive(active);
        super.setActive(active);
    }
}
