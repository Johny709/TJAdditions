package com.johny.tj.builder.handlers;

import com.johny.tj.machines.multi.electric.MetaTileEntityLargeAtmosphereCollector;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
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
import net.minecraftforge.fluids.IFluidTank;

import java.util.function.Supplier;

public class LargeAtmosphereCollectorWorkableHandler extends FuelRecipeLogic {

    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 22;
    private static final long BASE_EU_OUTPUT = 512;

    private final MetaTileEntityLargeAtmosphereCollector largeTurbine;
    private int rotorCycleLength = CYCLE_LENGTH;

    public LargeAtmosphereCollectorWorkableHandler(MetaTileEntityLargeAtmosphereCollector metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.largeTurbine = metaTileEntity;
    }

    @Override
    public void update() {
        super.update();
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        if (!rotorHolder.isHasRotor()) {
            setActive(false);
        }
        if (largeTurbine.getOffsetTimer() % 20 == 0) {
            long totalAirOutput = getRecipeOutputVoltage();
            IFluidTank fluidTank = null;
            if (largeTurbine.getAbilities(MultiblockAbility.EXPORT_FLUIDS).size() > 1) {
                fluidTank = largeTurbine.getAbilities(MultiblockAbility.EXPORT_FLUIDS).get(1);
            } else {
                fluidTank = largeTurbine.getAbilities(MultiblockAbility.EXPORT_FLUIDS).get(0);
            }
            if (totalAirOutput > 0) {
                FluidStack fluidStack = Materials.Air.getFluid((int) totalAirOutput);
                if (fluidTank != null) {
                    fluidTank.fill(fluidStack, true);
                }
            }
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
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        int baseRotorDamage = BASE_ROTOR_DAMAGE;
        if (++rotorCycleLength >= CYCLE_LENGTH) {
            if (largeTurbine.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM) baseRotorDamage = 150;
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
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        if (rotorHolder.hasRotorInInventory()) {
            double rotorEfficiency = rotorHolder.getRotorEfficiency();
            double totalEnergyOutput = (BASE_EU_OUTPUT + getBonusForTurbineType(largeTurbine) * rotorEfficiency);
            return MathHelper.ceil(totalEnergyOutput);
        }
        return BASE_EU_OUTPUT + getBonusForTurbineType(largeTurbine);
    }

    @Override
    protected boolean isReadyForRecipes() {
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        return rotorHolder.isHasRotor();
    }

    @Override
    protected long startRecipe(FuelRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        addOutputFluids(currentRecipe, fuelAmountUsed);
        return 0L; //energy is added each tick while the rotor speed is >0 RPM
    }

    private void addOutputFluids(FuelRecipe currentRecipe, int fuelAmountUsed) {
        IFluidTank fluidTank = null;
        if (largeTurbine.getAbilities(MultiblockAbility.EXPORT_FLUIDS).size() > 1) {
            fluidTank = largeTurbine.getAbilities(MultiblockAbility.EXPORT_FLUIDS).get(0);
        }
        if (largeTurbine.turbineType == MetaTileEntityLargeAtmosphereCollector.TurbineType.STEAM) {
            int waterFluidAmount = fuelAmountUsed / 15;
            if (waterFluidAmount > 0) {
                FluidStack waterStack = Materials.Water.getFluid(waterFluidAmount);
                if (fluidTank != null) {
                    fluidTank.fill(waterStack, true);
                }
            }
        } else if (largeTurbine.turbineType == MetaTileEntityLargeAtmosphereCollector.TurbineType.PLASMA) {
            FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
            if (material != null) {
                if (fluidTank != null) {
                    fluidTank.fill(material.getFluid(fuelAmountUsed), true);
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
        MetaTileEntityRotorHolder rotorHolder = largeTurbine.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
        if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
            double rotorEfficiency = rotorHolder.getRotorEfficiency();
            double totalEnergyOutput = ((BASE_EU_OUTPUT + getBonusForTurbineType(largeTurbine)) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
            return MathHelper.ceil(totalEnergyOutput);
        }
        return 0L;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", rotorCycleLength);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.rotorCycleLength = compound.getInteger("CycleLength");
    }

    @Override
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
    }

}
