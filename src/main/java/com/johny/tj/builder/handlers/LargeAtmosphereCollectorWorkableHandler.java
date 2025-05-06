package com.johny.tj.builder.handlers;

import com.johny.tj.capability.IGeneratorInfo;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.machines.multi.electric.MetaTileEntityLargeAtmosphereCollector;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.FuelRecipeLogic;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import java.util.function.Supplier;

import static gregtech.api.unification.material.Materials.Air;

public class LargeAtmosphereCollectorWorkableHandler extends FuelRecipeLogic implements IWorkable, IGeneratorInfo {

    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 22;
    private static final long BASE_EU_OUTPUT = 512;

    private final MetaTileEntityLargeAtmosphereCollector airCollector;
    private int rotorCycleLength = CYCLE_LENGTH;

    private int totalAirProduced;
    private int fastModeMultiplier = 1;
    private int rotorDamageMultiplier = 1;
    private boolean isFastMode;
    private int progress;
    private int maxProgress;

    public LargeAtmosphereCollectorWorkableHandler(MetaTileEntityLargeAtmosphereCollector metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.airCollector = metaTileEntity;
    }

    @Override
    public void update() {
        if (getMetaTileEntity().getWorld().isRemote || !isWorkingEnabled())
            return;

        totalAirProduced = (int) getRecipeOutputVoltage();

        if (totalAirProduced > 0) {
            airCollector.exportFluidHandler.fill(Air.getFluid(totalAirProduced), true);
        }

        airCollector.calculateMaintenance(rotorDamageMultiplier);
        if (progress > 0 && !isActive())
            setActive(true);

        if (progress >= maxProgress) {
            progress = 0;
            setActive(false);
        }

        if (progress <= 0) {
            if (airCollector.getNumProblems() >= 6 || !isReadyForRecipes() || !this.tryAcquireNewRecipe())
                return;
            progress = 1;
            setActive(true);
            toggleFastMode(isFastMode);
        } else {
            progress++;
        }
    }

    public void setFastMode(boolean isFastMode) {
        this.isFastMode = isFastMode;
        getMetaTileEntity().markDirty();
    }

    public boolean isFastMode() {
        return isFastMode;
    }

    private void toggleFastMode(boolean toggle) {
        if (toggle) {
            fastModeMultiplier = 3;
            rotorDamageMultiplier = 16;
        } else {
            fastModeMultiplier = 1;
            rotorDamageMultiplier = 1;
        }
    }

    public FluidStack getFuelStack() {
        if (previousRecipe == null)
            return null;
        FluidStack fuelStack = previousRecipe.getRecipeFluid();
        return fluidTank.get().drain(new FluidStack(fuelStack.getFluid(), Integer.MAX_VALUE), false);
    }

    private boolean tryAcquireNewRecipe() {
        IMultipleTankHandler fluidTanks = this.fluidTank.get();
        for (IFluidTank fluidTank : fluidTanks) {
            FluidStack tankContents = fluidTank.getFluid();
            if (tankContents != null && tankContents.amount > 0) {
                int fuelAmountUsed = this.tryAcquireNewRecipe(tankContents);
                if (fuelAmountUsed > 0) {
                    fluidTank.drain(fuelAmountUsed, true);
                    return true; //recipe is found and ready to use
                }
            }
        }
        return false;
    }

    private int tryAcquireNewRecipe(FluidStack fluidStack) {
        FuelRecipe currentRecipe;
        if (previousRecipe != null && previousRecipe.matches(getMaxVoltage(), fluidStack)) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = previousRecipe;
        } else {
            //else, try searching new recipe for given inputs
            currentRecipe = recipeMap.findRecipe(getMaxVoltage(), fluidStack);
            //if we found recipe that can be buffered, buffer it
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
            }
        }
        if (currentRecipe != null && checkRecipe(currentRecipe)) {
            int fuelAmountToUse = calculateFuelAmount(currentRecipe);
            if (fluidStack.amount >= fuelAmountToUse) {
                maxProgress = calculateRecipeDuration(currentRecipe);
                startRecipe(currentRecipe, fuelAmountToUse, maxProgress);
                return fuelAmountToUse;
            }
        }
        return 0;
    }

    @Override
    public boolean checkRecipe(FuelRecipe recipe) {
        MetaTileEntityRotorHolder rotorHolder = airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        int baseRotorDamage = BASE_ROTOR_DAMAGE;
        if (++rotorCycleLength >= CYCLE_LENGTH) {
            if (airCollector.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM) baseRotorDamage = 150;
            int damageToBeApplied = (int) Math.round(baseRotorDamage * rotorHolder.getRelativeRotorSpeed()) + 1;
            if (rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                this.rotorCycleLength = 0;
                return true;
            } else return false;
        }
        return true;
    }

    @Override
    public long getMaxVoltage() {
        MetaTileEntityRotorHolder rotorHolder = airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        if (rotorHolder.hasRotorInInventory()) {
            double rotorEfficiency = rotorHolder.getRotorEfficiency();
            double totalEnergyOutput = (BASE_EU_OUTPUT + getBonusForTurbineType(airCollector) * rotorEfficiency);
            return MathHelper.ceil(totalEnergyOutput * fastModeMultiplier);
        }
        return BASE_EU_OUTPUT + getBonusForTurbineType(airCollector);
    }

    @Override
    protected boolean isReadyForRecipes() {
        MetaTileEntityRotorHolder rotorHolder = airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        return rotorHolder.isHasRotor();
    }

    @Override
    protected long startRecipe(FuelRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        addOutputFluids(currentRecipe, fuelAmountUsed);
        return 0L; //energy is added each tick while the rotor speed is >0 RPM
    }

    private void addOutputFluids(FuelRecipe currentRecipe, int fuelAmountUsed) {
        if (airCollector.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM) {
            int waterFluidAmount = fuelAmountUsed / 15;
            if (waterFluidAmount > 0) {
                FluidStack waterStack = Materials.Water.getFluid(waterFluidAmount);
                if (airCollector.exportFluidHandler != null) {
                    airCollector.exportFluidHandler.fill(waterStack, true);
                }
            }
        } else if (airCollector.turbineType == MetaTileEntityLargeTurbine.TurbineType.PLASMA) {
            FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
            if (material != null) {
                if (airCollector.exportFluidHandler != null) {
                    airCollector.exportFluidHandler.fill(material.getFluid(fuelAmountUsed), true);
                }
            }
        }
    }

    private int getBonusForTurbineType(MetaTileEntityLargeAtmosphereCollector turbine) {
        switch (turbine.turbineType) {
            case GAS: return ConfigHolder.gasTurbineBonusOutput;
            case PLASMA: return ConfigHolder.plasmaTurbineBonusOutput;
            case STEAM: return ConfigHolder.steamTurbineBonusOutput;
            default: return 1;
        }
    }

    @Override
    public long getRecipeOutputVoltage() {
        double totalEnergyOutput;
        MetaTileEntityRotorHolder rotorHolder = airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
        if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
            double rotorEfficiency = rotorHolder.getRotorEfficiency();
            totalEnergyOutput = ((BASE_EU_OUTPUT + getBonusForTurbineType(airCollector)) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
            totalEnergyOutput /= 1.00 + 0.05 * airCollector.getNumProblems();
            return MathHelper.ceil(totalEnergyOutput * fastModeMultiplier);
        }
        return 0L;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", rotorDamageMultiplier);
        tagCompound.setBoolean("IsFastMode", isFastMode);
        tagCompound.setInteger("TotalAir", totalAirProduced);
        tagCompound.setInteger("Progress", progress);
        tagCompound.setInteger("MaxProgress", maxProgress);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        rotorCycleLength = compound.getInteger("CycleLength");
        fastModeMultiplier = compound.getInteger("FastModeMultiplier");
        rotorDamageMultiplier = compound.getInteger("DamageMultiplier");
        isFastMode = compound.getBoolean("IsFastMode");
        totalAirProduced = compound.getInteger("TotalAir");
        progress = compound.getInteger("Progress");
        maxProgress = compound.getInteger("MaxProgress");
    }

    @Override
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_GENERATOR)
            return TJCapabilities.CAPABILITY_GENERATOR.cast(this);
        return super.getCapability(capability);
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return maxProgress;
    }

    @Override
    public long getProduction() {
        return totalAirProduced;
    }

    @Override
    public String prefix() {
        return "machine.universal.producing";
    }

    @Override
    public String suffix() {
        return "tj.multiblock.large_atmosphere_collector.production";
    }
}
