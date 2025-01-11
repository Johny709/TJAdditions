package com.johny.tj.builder.handlers;

import com.johny.tj.machines.multi.electric.MetaTileEntityXLHotCoolantTurbine;
import gregicadditions.machines.multi.impl.HotCoolantRecipeLogic;
import gregicadditions.machines.multi.impl.MetaTileEntityRotorHolderForNuclearCoolant;
import gregicadditions.machines.multi.nuclear.MetaTileEntityHotCoolantTurbine;
import gregicadditions.recipes.impl.nuclear.HotCoolantRecipe;
import gregicadditions.recipes.impl.nuclear.HotCoolantRecipeMap;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.unification.material.type.FluidMaterial;
import gregtech.common.ConfigHolder;
import gregtech.common.MetaFluids;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Supplier;

public class XLHotCoolantTurbineWorkableHandler extends HotCoolantRecipeLogic {

    private static final float TURBINE_BONUS = 1.5f;
    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = (int) (11 * TURBINE_BONUS);
    private static final int BASE_EU_OUTPUT = 2048;

    private int totalEnergyProduced = 0;
    private int fastModeMultiplier = 1;
    private int rotorDamageMultiplier = 1;
    private float efficiencyPenalty = 1.5f;
    private boolean fastMode = false;

    private final MetaTileEntityXLHotCoolantTurbine extremeTurbine;
    private int rotorCycleLength = CYCLE_LENGTH;

    public XLHotCoolantTurbineWorkableHandler(MetaTileEntity metaTileEntity, HotCoolantRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.extremeTurbine = (MetaTileEntityXLHotCoolantTurbine) metaTileEntity;
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
        if (extremeTurbine.getOffsetTimer() % 20 == 0) {
            totalEnergyProduced = (int) getRecipeOutputVoltage();
        }
        if (totalEnergyProduced > 0) {
            energyContainer.get().addEnergy(totalEnergyProduced);
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
    public boolean checkRecipe(HotCoolantRecipe recipe) {
        int index = 0;
        for (MetaTileEntityRotorHolderForNuclearCoolant rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLHotCoolantTurbine.ABILITY_ROTOR_HOLDER)) {
            index++;
            if (++rotorCycleLength >= CYCLE_LENGTH) {
                int damageToBeApplied = (int) Math.round(((BASE_ROTOR_DAMAGE * rotorHolder.getRelativeRotorSpeed()) + 1) * rotorDamageMultiplier);
                if (rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                    if (index < extremeTurbine.getAbilities(MetaTileEntityXLHotCoolantTurbine.ABILITY_ROTOR_HOLDER).size())
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
        for (MetaTileEntityRotorHolderForNuclearCoolant rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLHotCoolantTurbine.ABILITY_ROTOR_HOLDER)) {
            index++;
            if (rotorHolder.hasRotorInInventory()) {
                totalEnergyMultiplier++;
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                double totalEnergyOutput = (BASE_EU_OUTPUT + getBonusForTurbineType(extremeTurbine) * rotorEfficiency);
                if (index < extremeTurbine.getAbilities(MetaTileEntityXLHotCoolantTurbine.ABILITY_ROTOR_HOLDER).size())
                    continue;
                return MathHelper.ceil((((totalEnergyOutput / totalEnergyMultiplier) * TURBINE_BONUS) * efficiencyPenalty) * fastModeMultiplier);
            }
        }
        return BASE_EU_OUTPUT;
    }

    @Override
    protected long startRecipe(HotCoolantRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        addOutputFluids(currentRecipe, fuelAmountUsed);
        return 0L; //energy is added each tick while the rotor speed is >0 RPM
    }

    private void addOutputFluids(HotCoolantRecipe currentRecipe, int fuelAmountUsed) {
        if (extremeTurbine.turbineType == MetaTileEntityHotCoolantTurbine.TurbineType.HOT_COOLANT) {
            if (fuelAmountUsed > 0) {
                FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
                if (material != null) {
                    extremeTurbine.exportFluidHandler.fill(material.getFluid(fuelAmountUsed), true);
                }
            }
        }
    }

    private int getBonusForTurbineType(MetaTileEntityXLHotCoolantTurbine turbine) {
        if (turbine.turbineType == MetaTileEntityHotCoolantTurbine.TurbineType.HOT_COOLANT) {
            return ConfigHolder.steamTurbineBonusOutput * 130 / 100;
        }
        return 1;
    }

    @Override
    public long getRecipeOutputVoltage() {
        double totalEnergyOutput = 0;
        int index = 0;
        for (MetaTileEntityRotorHolderForNuclearCoolant rotorHolder : extremeTurbine.getAbilities(MetaTileEntityXLHotCoolantTurbine.ABILITY_ROTOR_HOLDER)) {
            index++;
            double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
            if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + getBonusForTurbineType(extremeTurbine) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
                if (index < extremeTurbine.getAbilities(MetaTileEntityXLHotCoolantTurbine.ABILITY_ROTOR_HOLDER).size())
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
        tagCompound.setInteger("TotalEnergy", totalEnergyProduced);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        rotorCycleLength = compound.getInteger("CycleLength");
        fastModeMultiplier = compound.getInteger("FastModeMultiplier");
        rotorDamageMultiplier = compound.getInteger("DamageMultiplier");
        fastMode = compound.getBoolean("FastMode");
        efficiencyPenalty = compound.getFloat("EfficiencyBonus");
        totalEnergyProduced = compound.getInteger("TotalEnergy");
    }

    @Override
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
    }
}
