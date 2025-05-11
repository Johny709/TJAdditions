package com.johny.tj.builder.handlers;

import com.johny.tj.TJValues;
import com.johny.tj.capability.IGeneratorInfo;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.machines.multi.electric.MetaTileEntityXLHotCoolantTurbine;
import com.johny.tj.machines.multi.electric.MetaTileEntityXLTurbine;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.machines.multi.impl.HotCoolantRecipeLogic;
import gregicadditions.machines.multi.nuclear.MetaTileEntityHotCoolantTurbine;
import gregicadditions.recipes.impl.nuclear.HotCoolantRecipe;
import gregicadditions.recipes.impl.nuclear.HotCoolantRecipeMap;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.unification.material.type.FluidMaterial;
import gregtech.common.ConfigHolder;
import gregtech.common.MetaFluids;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.function.Supplier;

public class XLHotCoolantTurbineWorkableHandler extends HotCoolantRecipeLogic implements IWorkable, IGeneratorInfo {

    private static final float TURBINE_BONUS = 1.5f;
    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 11;
    private static final int BASE_EU_OUTPUT = 2048;

    private int totalEnergyProduced;
    private int consumption;
    private String fuelName;
    private int fastModeMultiplier = 1;
    private int rotorDamageMultiplier = 1;
    private boolean isFastMode;
    private int progress;
    private int maxProgress;
    private boolean active;

    private final MetaTileEntityXLHotCoolantTurbine extremeTurbine;
    private int rotorCycleLength = CYCLE_LENGTH;

    public XLHotCoolantTurbineWorkableHandler(MetaTileEntity metaTileEntity, HotCoolantRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, 0L);
        this.extremeTurbine = (MetaTileEntityXLHotCoolantTurbine) metaTileEntity;
    }

    public static float getTurbineBonus() {
        float castTurbineBonus = 100 * TURBINE_BONUS;
        return (int) castTurbineBonus;
    }

    @Override
    public void update() {
        if (getMetaTileEntity().getWorld().isRemote || !isWorkingEnabled())
            return;

        if (extremeTurbine.getOffsetTimer() % 20 == 0)
            totalEnergyProduced = (int) getRecipeOutputVoltage();

        if (totalEnergyProduced > 0)
            energyContainer.get().addEnergy(totalEnergyProduced);

        extremeTurbine.calculateMaintenance(rotorDamageMultiplier);
        if (progress > 0 && !isActive())
            this.setActive(true);

        if (progress >= maxProgress) {
            progress = 0;
            this.setActive(false);
        }

        if (progress <= 0) {
            if (extremeTurbine.getNumProblems() >= 6 || !isReadyForRecipes() || !this.tryAcquireNewRecipe())
                return;
            progress = 1;
            this.setActive(true);
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
                    FluidStack fluidStack = fluidTank.drain(fuelAmountUsed, true);
                    consumption = fluidStack.amount;
                    fuelName = fluidStack.getUnlocalizedName();
                    return true; //recipe is found and ready to use
                }
            }
        }
        return false;
    }

    private int tryAcquireNewRecipe(FluidStack fluidStack) {
        HotCoolantRecipe currentRecipe;
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
    public boolean checkRecipe(HotCoolantRecipe recipe) {
        List<MetaTileEntityRotorHolder> rotorHolders = extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER);
        if (++rotorCycleLength >= CYCLE_LENGTH) {
            for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
                int damageToBeApplied = (int) Math.round((BASE_ROTOR_DAMAGE * rotorHolder.getRelativeRotorSpeed()) + 1) * rotorDamageMultiplier;
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
    protected int calculateFuelAmount(HotCoolantRecipe currentRecipe) {
        return (int) (super.calculateFuelAmount(currentRecipe) / (isFastMode ? 1 : 1.5F));
    }

    @Override
    public void writeInitialData(PacketBuffer buf) {
        buf.writeBoolean(active);
    }

    @Override
    public void receiveInitialData(PacketBuffer buf) {
        active = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 10)
            active = buf.readBoolean();
    }

    private void setActive(boolean active) {
        this.active = active;
        if (!getMetaTileEntity().getWorld().isRemote) {
            writeCustomData(10, buf -> buf.writeBoolean(active));
            getMetaTileEntity().markDirty();
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", rotorDamageMultiplier);
        tagCompound.setBoolean("IsFastMode", isFastMode);
        tagCompound.setInteger("Consumption", consumption);
        tagCompound.setString("FuelName", fuelName);
        tagCompound.setInteger("TotalEnergy", totalEnergyProduced);
        tagCompound.setInteger("Progress", progress);
        tagCompound.setInteger("MaxProgress", maxProgress);
        tagCompound.setBoolean("Active", active);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        rotorCycleLength = compound.getInteger("CycleLength");
        fastModeMultiplier = compound.getInteger("FastModeMultiplier");
        rotorDamageMultiplier = compound.getInteger("DamageMultiplier");
        isFastMode = compound.getBoolean("IsFastMode");
        consumption = compound.getInteger("Consumption");
        fuelName = compound.getString("FuelName");
        totalEnergyProduced = compound.getInteger("TotalEnergy");
        maxProgress = compound.getInteger("MaxProgress");
        progress = compound.getInteger("Progress");
        active = compound.getBoolean("Active");
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

    public String getFuelName() {
        return fuelName;
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
    public long getConsumption() {
        return consumption;
    }

    @Override
    public long getProduction() {
        return totalEnergyProduced;
    }

    @Override
    public String[] consumptionInfo() {
        int seconds = maxProgress / 20;
        String amount = String.valueOf(seconds);
        String s = seconds < 2 ? "second" : "seconds";
        return ArrayUtils.toArray("machine.universal.consumption", "§b ", "suffix", "machine.universal.liters.short",  "§r ", fuelName, " ", "every", "§6 ", amount, "§r ", s);
    }

    @Override
    public String[] productionInfo() {
        int tier = GAUtility.getTierByVoltage(totalEnergyProduced);
        String voltage = GAValues.VN[tier];
        String color = TJValues.VCC[tier];
        return ArrayUtils.toArray("machine.universal.producing", "§e ", "suffix", "§r ", "machine.universal.eu.tick",
                " ", "§r(§6", color, voltage, "§r)");
    }
}
