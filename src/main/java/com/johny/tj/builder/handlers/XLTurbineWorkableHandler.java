package com.johny.tj.builder.handlers;

import com.johny.tj.capability.IGeneratorInfo;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.machines.multi.electric.MetaTileEntityXLTurbine;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.function.Supplier;

public class XLTurbineWorkableHandler extends FuelRecipeLogic implements IWorkable, IGeneratorInfo {

    private static final float TURBINE_BONUS = 1.5f;
    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 220;
    private static final int BASE_EU_OUTPUT = 2048;

    private int totalEnergyProduced ;
    private int fastModeMultiplier = 1;
    private int rotorDamageMultiplier = 1;
    private boolean isFastMode;
    private int progress;
    private int maxProgress;

    private final MetaTileEntityXLTurbine extremeTurbine;
    private int rotorCycleLength = CYCLE_LENGTH;

    public XLTurbineWorkableHandler(MetaTileEntity metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.extremeTurbine = (MetaTileEntityXLTurbine) metaTileEntity;
    }

    public static float getTurbineBonus() {
        float castTurbineBonus = 100 * TURBINE_BONUS;
        return (int) castTurbineBonus;
    }

    @Override
    public void update() {
        if (getMetaTileEntity().getWorld().isRemote || !isWorkingEnabled())
            return;

        if (extremeTurbine.getOffsetTimer() % 20 == 0) {
            totalEnergyProduced = (int) getRecipeOutputVoltage();
        }

        if (totalEnergyProduced > 0) {
            energyContainer.get().addEnergy(totalEnergyProduced);
        }

        extremeTurbine.calculateMaintenance(rotorDamageMultiplier);
        if (progress > 0 && !isActive())
            setActive(true);

        if (progress >= maxProgress) {
            progress = 0;
            setActive(false);
        }

        if (progress <= 0) {
            if (extremeTurbine.getNumProblems() >= 6 || !isReadyForRecipes() || !this.tryAcquireNewRecipe())
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
        List<MetaTileEntityRotorHolder> rotorHolders = extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER);
        if (++rotorCycleLength >= CYCLE_LENGTH) {
            for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
                int baseRotorDamage = BASE_ROTOR_DAMAGE;
                if (extremeTurbine.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM)
                    baseRotorDamage = 150;
                int damageToBeApplied = (int) Math.round((baseRotorDamage * rotorHolder.getRelativeRotorSpeed()) + 1) * rotorDamageMultiplier;
                if (!rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                    return false;
                }
            }
            rotorCycleLength = 0;
        }
        return true;
    }

    @Override
    public long getMaxVoltage() {
        double totalEnergyOutput = 0;
        List<MetaTileEntityRotorHolder> rotorHolders = extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER);
        for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
            if (rotorHolder.hasRotorInInventory()) {
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + getBonusForTurbineType(extremeTurbine) * rotorEfficiency);
            }
        }
        return MathHelper.ceil((totalEnergyOutput / rotorHolders.size() * fastModeMultiplier) / TURBINE_BONUS);
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
        return areReadyForRecipes == rotorHolderSize;
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
        for (MetaTileEntityRotorHolder rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER)) {
            if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
                double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + getBonusForTurbineType(extremeTurbine) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
                totalEnergyOutput /= 1.00 + 0.05 * extremeTurbine.getNumProblems();
            }
        }
        return MathHelper.ceil(totalEnergyOutput * fastModeMultiplier * TURBINE_BONUS);
    }

    @Override
    protected int calculateFuelAmount(FuelRecipe currentRecipe) {
        int durationMultiplier = extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? 10 : 1;
        return (int) ((super.calculateFuelAmount(currentRecipe) * durationMultiplier) / (isFastMode ? 1 : 1.5F));
    }

    @Override
    protected int calculateRecipeDuration(FuelRecipe currentRecipe) {
        int durationMultiplier = extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? 10 : 1;
        return super.calculateRecipeDuration(currentRecipe) * durationMultiplier;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", rotorDamageMultiplier);
        tagCompound.setBoolean("IsFastMode", isFastMode);
        tagCompound.setInteger("TotalEnergy", totalEnergyProduced);
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
        totalEnergyProduced = compound.getInteger("TotalEnergy");
        progress = compound.getInteger("Progress");
        maxProgress = compound.getInteger("MaxProgress");
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
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
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
        return totalEnergyProduced;
    }

    @Override
    public String[] productionInfo() {
        return ArrayUtils.toArray("machine.universal.producing", "machine.universal.eu.tick");
    }
}

