package tj.capability.impl;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.machines.multi.IMaintenance;
import gregtech.api.capability.*;
import gregtech.api.capability.impl.FluidFuelInfo;
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

import java.util.*;
import java.util.function.Supplier;

public class TJFuelRecipeLogic extends FuelRecipeLogic implements IWorkable, IGeneratorInfo {

    private final Set<FluidStack> lastSearchedFluid = new HashSet<>();
    private long energyProduced;
    private int progress;
    private int maxProgress;
    private int consumption;
    private int searchCount;
    private String fuelName;

    public TJFuelRecipeLogic(MetaTileEntity metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> fluidTank, long maxVoltage) {
        super(metaTileEntity, recipeMap, energyContainer, fluidTank, maxVoltage);
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
            if (stack == null) continue;
            if (fuelStack == null) {
                if (this.lastSearchedFluid.contains(stack)) continue;
                fuelStack = stack.copy();
                this.lastSearchedFluid.add(fuelStack);
            } else if (fuelStack.isFluidEqual(stack)) {
                long amount = fuelStack.amount + stack.amount;
                fuelStack.amount = (int) Math.min(Integer.MAX_VALUE, amount);
            }
        }
        fuelStack = this.tryAcquireNewRecipe(fuelStack);
        if (fuelStack != null && fuelStack.amount > 0) {
            FluidStack fluidStack = this.fluidTank.get().drain(fuelStack, true);
            this.consumption = fluidStack.amount;
            this.fuelName = fluidStack.getUnlocalizedName();
            this.lastSearchedFluid.clear();
            return true; //recipe is found and ready to use
        }
        if (++this.searchCount >= this.fluidTank.get().getTanks()) {
            this.lastSearchedFluid.clear();
            this.searchCount = 0;
        }
        return false;
    }

    protected FluidStack tryAcquireNewRecipe(FluidStack fuelStack) {
        FuelRecipe currentRecipe;
        if (this.previousRecipe != null && this.previousRecipe.matches(this.getMaxVoltage(), fuelStack)) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = this.previousRecipe;
        } else {
            //else, try searching new recipe for given inputs
            currentRecipe = this.recipeMap.findRecipe(this.getMaxVoltage(), fuelStack);
            //if we found recipe that can be buffered, buffer it
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
            }
        }
        if (currentRecipe != null && checkRecipe(currentRecipe)) {
            int fuelAmountToUse = this.calculateFuelAmount(currentRecipe);
            if (fuelStack.amount >= fuelAmountToUse) {
                this.maxProgress = this.calculateRecipeDuration(currentRecipe);
                this.energyProduced = this.startRecipe(currentRecipe, fuelAmountToUse, this.maxProgress);
                FluidStack recipeFluid = currentRecipe.getRecipeFluid();
                recipeFluid.amount = fuelAmountToUse;
                return recipeFluid;
            }
        }
        return null;
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
    public Collection<IFuelInfo> getFuels() {
        final IMultipleTankHandler fluidTanks = this.fluidTank.get();
        if (fluidTanks == null)
            return Collections.emptySet();

        final LinkedHashMap<String, IFuelInfo> fuels = new LinkedHashMap<>();
        // Fuel capacity is all tanks
        int fuelCapacity = 0;
        for (IFluidTank fluidTank : fluidTanks) {
            fuelCapacity += fluidTank.getCapacity();
        }

        for (IFluidTank fluidTank : fluidTanks) {
            final FluidStack tankContents = fluidTank.drain(Integer.MAX_VALUE, false);
            if (tankContents == null || tankContents.amount <= 0)
                continue;
            int fuelRemaining = tankContents.amount;
            FuelRecipe recipe = this.recipeMap.findRecipe(this.maxVoltage, tankContents);
            if (recipe == null)
                continue;
            int amountPerRecipe = this.calculateFuelAmount(recipe);
            int duration = this.calculateRecipeDuration(recipe);
            long fuelBurnTime = ((long) duration * fuelRemaining) / amountPerRecipe;

            FluidFuelInfo fuelInfo = (FluidFuelInfo) fuels.get(tankContents.getUnlocalizedName());
            if (fuelInfo == null) {
                fuelInfo = new FluidFuelInfo(tankContents, fuelRemaining, fuelCapacity, amountPerRecipe, fuelBurnTime);
                fuels.put(tankContents.getUnlocalizedName(), fuelInfo);
            } else {
                fuelInfo.addFuelRemaining(fuelRemaining);
                fuelInfo.addFuelBurnTime(fuelBurnTime);
            }
        }
        return fuels.values();
    }

    @Override
    protected boolean shouldVoidExcessiveEnergy() {
        return true;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("progress", this.progress);
        tagCompound.setInteger("maxProgress", this.maxProgress);
        tagCompound.setInteger("consumption", this.consumption);
        tagCompound.setLong("energy", this.energyProduced);
        if (this.fuelName != null)
            tagCompound.setString("fuelName", this.fuelName);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.consumption = compound.getInteger("consumption");
        this.energyProduced = compound.getLong("energy");
        this.maxProgress = compound.getInteger("maxProgress");
        this.progress = compound.getInteger("progress");
        if (compound.hasKey("fuelName"))
            this.fuelName = compound.getString("fuelName");
    }

    public FluidStack getFuelStack() {
        if (this.previousRecipe == null)
            return null;
        FluidStack fuelStack = this.previousRecipe.getRecipeFluid();
        return this.fluidTank.get().drain(new FluidStack(fuelStack.getFluid(), Integer.MAX_VALUE), false);
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
