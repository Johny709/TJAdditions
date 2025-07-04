package tj.builder.handlers;

import gregicadditions.GAValues;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.recipeproperties.BlastTemperatureProperty;
import tj.TJConfig;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelMultiblockRecipeLogic;

import java.util.function.IntSupplier;

public class ParallelElectricBlastFurnaceRecipeLogic extends ParallelMultiblockRecipeLogic {

    private final IntSupplier temperature;

    public ParallelElectricBlastFurnaceRecipeLogic(ParallelRecipeMapMultiblockController tileEntity, IntSupplier temperature) {
        super(tileEntity, TJConfig.machines.recipeCacheCapacity);
        this.temperature = temperature;
    }

    @Override
    protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
        this.overclockManager.setRecipeProperty(recipe.getRecipePropertyStorage().getRecipePropertyValue(BlastTemperatureProperty.getInstance(), 0));
        return super.setupAndConsumeRecipeInputs(recipe);
    }

    @Override
    protected void calculateOverclock(int EUt, int duration) {
        int numMaintenanceProblems = this.controller.getNumProblems();

        double maintenanceDurationMultiplier = 1.0 + (0.1 * numMaintenanceProblems);
        int durationModified = (int) (duration * maintenanceDurationMultiplier);

        if (!this.allowOverclocking) {
            this.overclockManager.setEUtAndDuration(EUt, durationModified);
        }
        boolean negativeEU = EUt < 0;

        int bonusAmount = Math.max(0, this.temperature.getAsInt() - (Integer) this.overclockManager.getRecipeProperty()) / 900;

        // Apply EUt discount for every 900K above the base recipe temperature
        EUt *= Math.pow(0.95, bonusAmount);

        int tier = getOverclockingTier(this.getMaxVoltage());
        if (GAValues.V[tier] <= EUt || tier == 0)
            this.overclockManager.setEUtAndDuration(EUt, durationModified);
        if (negativeEU)
            EUt = -EUt;
        if (EUt <= 16) {
            int multiplier = EUt <= 8 ? tier : tier - 1;
            int resultEUt = EUt * (1 << multiplier) * (1 << multiplier);
            int resultDuration = durationModified / (1 << multiplier);
            this.overclockManager.setEUtAndDuration(negativeEU ? -resultEUt : resultEUt, resultDuration);
        } else {
            int resultEUt = EUt;
            double resultDuration = durationModified;

            // Do not overclock further if duration is already too small
            // Apply Super Overclocks for every 1800k above the base recipe temperature
            for (int i = bonusAmount; resultEUt <= GAValues.V[tier - 1] && resultDuration >= 3 && i > 0; i--) {
                if (i % 2 == 0) {
                    resultEUt *= 4;
                    resultDuration *= 0.25;
                }
            }

            // Do not overclock further if duration is already too small
            // Apply Regular Overclocking
            while (resultDuration >= 3 && resultEUt <= GAValues.V[tier - 1]) {
                resultEUt *= 4;
                resultDuration /= 2.8;
            }

            this.overclockManager.setEUtAndDuration(resultEUt, (int) Math.round(resultDuration));
        }
    }
}
