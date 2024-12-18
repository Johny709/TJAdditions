package com.johny.tj.builder.logic;

import com.johny.tj.machines.multi.electric.MetaTileEntityXLTurbine;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.api.unification.material.Materials;
import gregtech.api.unification.material.type.FluidMaterial;
import gregtech.common.ConfigHolder;
import gregtech.common.MetaFluids;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Supplier;

public class XLTurbineWorkableHandler extends FuelRecipeLogic {

    private static final float TURBINE_BONUS = 1.5f;
    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = (int) (22 * TURBINE_BONUS);
    private static final int BASE_EU_OUTPUT = 2048;

    private int totalEnergyProduced = 0;
    private int fastModeMultiplier = 1;
    private int rotorDamageMultiplier = 1;
    private float efficiencyPenalty = 1.5f;
    private boolean fastMode = false;

    private final MetaTileEntityXLTurbine extremeTurbine;
    private int rotorCycleLength = CYCLE_LENGTH;

    public XLTurbineWorkableHandler(MetaTileEntity metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.extremeTurbine = (MetaTileEntityXLTurbine) metaTileEntity;
    }

    public int getTotalEnergyProduced() {
        return totalEnergyProduced;
    }

    public boolean getFastModeToggle() {
        return fastMode;
    }

    public static float getTurbineBonus() {
        float castTurbineBonus = 100 * TURBINE_BONUS;
        return (int) castTurbineBonus;
    }


    @Override
    public void update() {
        super.update();
        long totalEnergyOutput = getRecipeOutputVoltage();
        if (totalEnergyOutput > 0) {
            energyContainer.get().addEnergy(totalEnergyOutput);

        }
    }

    public void toggleFastMode(boolean toggle) {
        if (toggle) {
            fastModeMultiplier = 3;
            rotorDamageMultiplier = 16;
            efficiencyPenalty = 1.5f;
            fastMode = true;
        }
        else {
            fastModeMultiplier = 1;
            rotorDamageMultiplier = 1;
            efficiencyPenalty = 1.0f;
            fastMode = false;
        }
    }

    public FluidStack getFuelStack() {
        if (previousRecipe == null)
            return null;
        FluidStack fuelStack = previousRecipe.getRecipeFluid();
        return fluidTank.get().drain(new FluidStack(fuelStack.getFluid(), Integer.MAX_VALUE), false);
    }

    @Override
    public boolean checkRecipe(FuelRecipe recipe) {
        int index = 0;
        for (MetaTileEntityRotorHolder rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER)) {
            index++;
            int baseRotorDamage = BASE_ROTOR_DAMAGE;
            if (++rotorCycleLength >= CYCLE_LENGTH) {
                if (extremeTurbine.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM) baseRotorDamage = 200;
                int damageToBeApplied = (int) Math.round(((baseRotorDamage * rotorHolder.getRelativeRotorSpeed()) + 1) * rotorDamageMultiplier);
                if (rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                    if (index < extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).size())
                        continue;
                    this.rotorCycleLength = 0;
                    return true;
                } else return false;
            }
        }
        return true;
    }

    @Override
    public long getMaxVoltage() {
        int totalEnergyMultiplier = 0;
        int index = 0;
        for (MetaTileEntityRotorHolder rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER)) {
            index++;
            if (rotorHolder.hasRotorInInventory()) {
                totalEnergyMultiplier++;
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                double totalEnergyOutput = (BASE_EU_OUTPUT + getBonusForTurbineType(extremeTurbine) * rotorEfficiency);
                if (index < extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).size())
                    continue;
                return MathHelper.ceil((((totalEnergyOutput / totalEnergyMultiplier) * TURBINE_BONUS) * efficiencyPenalty) * fastModeMultiplier);
            }
        }
        return (long) (((BASE_EU_OUTPUT + (float) getBonusForTurbineType(extremeTurbine) / totalEnergyMultiplier) * TURBINE_BONUS) * efficiencyPenalty) * fastModeMultiplier;
    }

    @Override
    protected boolean isReadyForRecipes() {
        int areReadyForRecipes = 0;
        int rotorHolderSize = extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).size();
        for (int index = 0; index < rotorHolderSize; index++) {
            MetaTileEntityRotorHolder rotorHolder = extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).get(index);
            if (rotorHolder.isHasRotor())
                areReadyForRecipes++;
        }
        return areReadyForRecipes == 12;
    }

    @Override
    protected long startRecipe(FuelRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        addOutputFluids(currentRecipe, fuelAmountUsed);
        return 0L; //energy is added each tick while the rotor speed is >0 RPM
    }

    private void addOutputFluids(FuelRecipe currentRecipe, int fuelAmountUsed) {
        if (extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM) {
            int waterFluidAmount = fuelAmountUsed / 15;
            if (waterFluidAmount > 0) {
                FluidStack waterStack = Materials.Water.getFluid(waterFluidAmount);
                extremeTurbine.exportFluidHandler.fill(waterStack, true);
            }
        } else if (extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.PLASMA) {
            FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
            if (material != null) {
                extremeTurbine.exportFluidHandler.fill(material.getFluid(fuelAmountUsed), true);
            }
        }
    }

    private int getBonusForTurbineType(MetaTileEntityXLTurbine turbine) {
        switch (turbine.turbineType) {
            case GAS: return ConfigHolder.gasTurbineBonusOutput;
            case PLASMA: return ConfigHolder.plasmaTurbineBonusOutput;
            case STEAM: return ConfigHolder.steamTurbineBonusOutput;
            default: return 1;
        }
    }
    @Override
    public long getRecipeOutputVoltage() {
        double totalEnergyOutput = 0;
        int index = 0;
        for (MetaTileEntityRotorHolder rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER)) {
            index++;
            double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
            if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + getBonusForTurbineType(extremeTurbine) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
                totalEnergyProduced = (int) (totalEnergyOutput * TURBINE_BONUS) * fastModeMultiplier;
                if (index < extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).size())
                    continue;
                return MathHelper.ceil((totalEnergyOutput * TURBINE_BONUS) * fastModeMultiplier);
            }
        }
        return 0L;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", rotorDamageMultiplier);
        tagCompound.setBoolean("FastMode", fastMode);
        tagCompound.setFloat("EfficiencyBonus", efficiencyPenalty);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.rotorCycleLength = compound.getInteger("CycleLength");
        this.fastModeMultiplier = compound.getInteger("FastModeMultiplier");
        this.rotorDamageMultiplier = compound.getInteger("DamageMultiplier");
        this.fastMode = compound.getBoolean("FastMode");
        this.efficiencyPenalty = compound.getFloat("EfficiencyBonus");
    }

    @Override
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
    }
}

