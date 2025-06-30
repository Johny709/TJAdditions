package tj.builder.handlers;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.machines.multi.IMaintenance;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.recipes.recipes.FuelRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.apache.commons.lang3.ArrayUtils;
import tj.TJValues;
import tj.builder.multicontrollers.TJFueledMultiblockController;
import tj.capability.IGeneratorInfo;
import tj.capability.TJCapabilities;

import java.util.function.Supplier;

public class TJFuelRecipeLogic extends FuelRecipeLogic implements IWorkable, IGeneratorInfo {

    private int progress;
    private int maxProgress;
    private long energyProduced;
    private int consumption;
    private String fuelName;

    public TJFuelRecipeLogic(MetaTileEntity metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank, long maxVoltage) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, maxVoltage);
    }

    public FluidStack getFuelStack() {
        if (this.previousRecipe == null)
            return null;
        FluidStack fuelStack = this.previousRecipe.getRecipeFluid();
        return this.fluidTank.get().drain(new FluidStack(fuelStack.getFluid(), Integer.MAX_VALUE), false);
    }

    @Override
    public void update() {
        if (getMetaTileEntity().getWorld().isRemote || !isWorkingEnabled())
            return;

        if (!shouldVoidExcessiveEnergy() && energyContainer.get().getEnergyCanBeInserted() < energyProduced) {
            if (isActive())
                setActive(false);
            return;
        }

        energyContainer.get().addEnergy(energyProduced);

        if (progress > 0 && !isActive())
            setActive(true);

        if (progress >= maxProgress) {
            if (metaTileEntity instanceof TJFueledMultiblockController)
                ((TJFueledMultiblockController) metaTileEntity).calculateMaintenance(this.maxProgress);
            progress = 0;
            setActive(false);
        }

        if (progress <= 0) {
            boolean problems = false;
            if (metaTileEntity instanceof IMaintenance)
                problems = ((IMaintenance) metaTileEntity).getNumProblems() >= 6;
            if (problems || !isReadyForRecipes() || !this.tryAcquireNewRecipe())
                return;
            progress = 1;
            setActive(true);
        } else {
            progress++;
        }
    }

    protected boolean tryAcquireNewRecipe() {
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

    protected int tryAcquireNewRecipe(FluidStack fluidStack) {
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
                energyProduced = startRecipe(currentRecipe, fuelAmountToUse, maxProgress);
                return fuelAmountToUse;
            }
        }
        return 0;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("Progress", progress);
        tagCompound.setInteger("MaxProgress", maxProgress);
        tagCompound.setInteger("Consumption", consumption);
        tagCompound.setString("FuelName", fuelName);
        tagCompound.setLong("Energy", energyProduced);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        consumption = compound.getInteger("Consumption");
        fuelName = compound.getString("FuelName");
        energyProduced = compound.getLong("Energy");
        maxProgress = compound.getInteger("MaxProgress");
        progress = compound.getInteger("Progress");
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
        return energyProduced;
    }

    @Override
    public String[] consumptionInfo() {
        int seconds = maxProgress / 20;
        String amount = String.valueOf(seconds);
        String s = seconds < 2 ? "second" : "seconds";
        return ArrayUtils.toArray("machine.universal.consumption", "§7 ", "suffix", "machine.universal.liters.short",  "§r ", fuelName, " ", "every", "§6 ", amount, "§r ", s);
    }

    @Override
    public String[] productionInfo() {
        int tier = GAUtility.getTierByVoltage(energyProduced);
        String voltage = GAValues.VN[tier];
        String color = TJValues.VCC[tier];
        return ArrayUtils.toArray("machine.universal.producing", "§e ", "suffix", "§r ", "machine.universal.eu.tick",
                " ", "§r(§6", color, voltage, "§r)");
    }
}
