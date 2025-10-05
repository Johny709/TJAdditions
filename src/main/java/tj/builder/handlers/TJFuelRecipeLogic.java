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
import tj.builder.multicontrollers.TJFueledMultiblockControllerBase;
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
        if (this.getMetaTileEntity().getWorld().isRemote || !this.isWorkingEnabled())
            return;

        if (!this.shouldVoidExcessiveEnergy() && this.energyContainer.get().getEnergyCanBeInserted() < this.energyProduced) {
            if (this.isActive())
                this.setActive(false);
            return;
        }

        this.energyContainer.get().addEnergy(this.energyProduced);

        if (this.progress > 0 && !this.isActive())
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            if (this.metaTileEntity instanceof TJFueledMultiblockControllerBase)
                ((TJFueledMultiblockControllerBase) this.metaTileEntity).calculateMaintenance(this.maxProgress);
            this.progress = 0;
            this.setActive(false);
        }

        if (this.progress <= 0) {
            boolean problems = false;
            if (this.metaTileEntity instanceof IMaintenance)
                problems = ((IMaintenance) this.metaTileEntity).getNumProblems() >= 6;
            if (problems || !this.isReadyForRecipes() || !this.tryAcquireNewRecipe())
                return;
            this.progress = 1;
            this.setActive(true);
        } else {
            this.progress++;
        }
    }

    protected boolean tryAcquireNewRecipe() {
        FluidStack fuelStack = null;
        for (int i = 0; i < this.fluidTank.get().getTanks(); i++) {
            IFluidTank tank = this.fluidTank.get().getTankAt(i);
            FluidStack stack = tank.getFluid();
            if (stack == null)
                continue;
            if (fuelStack == null)
                fuelStack = stack.copy();
            else if (fuelStack.isFluidEqual(stack)) {
                long amount = fuelStack.amount + stack.amount;
                fuelStack.amount = (int) Math.min(Integer.MAX_VALUE, amount);
            }
        }
        int fuelAmountUsed = this.tryAcquireNewRecipe(fuelStack);
        if (fuelAmountUsed > 0) {
            FluidStack fluidStack = this.fluidTank.get().drain(fuelAmountUsed, true);
            this.consumption = fluidStack.amount;
            this.fuelName = fluidStack.getUnlocalizedName();
            return true; //recipe is found and ready to use
        }
        return false;
    }

    protected int tryAcquireNewRecipe(FluidStack fluidStack) {
        FuelRecipe currentRecipe;
        if (this.previousRecipe != null && this.previousRecipe.matches(this.getMaxVoltage(), fluidStack)) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = this.previousRecipe;
        } else {
            //else, try searching new recipe for given inputs
            currentRecipe = this.recipeMap.findRecipe(this.getMaxVoltage(), fluidStack);
            //if we found recipe that can be buffered, buffer it
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
            }
        }
        if (currentRecipe != null && checkRecipe(currentRecipe)) {
            int fuelAmountToUse = this.calculateFuelAmount(currentRecipe);
            if (fluidStack.amount >= fuelAmountToUse) {
                this.maxProgress = this.calculateRecipeDuration(currentRecipe);
                this.energyProduced = this.startRecipe(currentRecipe, fuelAmountToUse, this.maxProgress);
                return fuelAmountToUse;
            }
        }
        return 0;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("Progress", this.progress);
        tagCompound.setInteger("MaxProgress", this.maxProgress);
        tagCompound.setInteger("Consumption", this.consumption);
        tagCompound.setString("FuelName", this.fuelName);
        tagCompound.setLong("Energy", this.energyProduced);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.consumption = compound.getInteger("Consumption");
        this.fuelName = compound.getString("FuelName");
        this.energyProduced = compound.getLong("Energy");
        this.maxProgress = compound.getInteger("MaxProgress");
        this.progress = compound.getInteger("Progress");
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
        return this.fuelName;
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }

    @Override
    public long getConsumption() {
        return this.consumption;
    }

    @Override
    public long getProduction() {
        return this.energyProduced;
    }

    @Override
    public String[] consumptionInfo() {
        int seconds = this.maxProgress / 20;
        String amount = String.valueOf(seconds);
        String s = seconds < 2 ? "second" : "seconds";
        return ArrayUtils.toArray("machine.universal.consumption", "§7 ", "suffix", "machine.universal.liters.short",  "§r§7(§b", this.fuelName, "§7)§r ", "every", "§b ", amount, "§r ", s);
    }

    @Override
    public String[] productionInfo() {
        int tier = GAUtility.getTierByVoltage(this.energyProduced);
        String voltage = GAValues.VN[tier];
        String color = TJValues.VCC[tier];
        return ArrayUtils.toArray("machine.universal.producing", "§e ", "suffix", "§r ", "machine.universal.eu.tick",
                " ", "§7(§6", color, voltage, "§7)");
    }
}
