package tj.builder.handlers;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.common.ConfigHolder;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.apache.commons.lang3.ArrayUtils;
import tj.TJValues;
import tj.capability.IGeneratorInfo;
import tj.capability.TJCapabilities;
import tj.machines.multi.electric.MetaTileEntityXLTurbine;

import java.util.List;
import java.util.function.Supplier;

import static gregtech.api.unification.material.Materials.DistilledWater;
import static gregtech.common.metatileentities.multi.electric.generator.RotorHolderMultiblockController.ABILITY_ROTOR_HOLDER;

public class XLTurbineWorkableHandler extends FuelRecipeLogic implements IWorkable, IGeneratorInfo {

    private static final float TURBINE_BONUS = 1.5f;
    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 220;
    private static final int BASE_EU_OUTPUT = 2048;
    private static final int BASE_EU_VOLTAGE = 512;

    private final MetaTileEntityXLTurbine extremeTurbine;
    private final Supplier<IMultipleTankHandler> exportFluidTank;

    private int totalEnergyProduced;
    private int consumption;
    private String fuelName;
    private int fastModeMultiplier = 1;
    private int rotorDamageMultiplier = 1;
    private boolean isFastMode;
    private boolean fastMode;
    private int progress;
    private int maxProgress;

    private int rotorCycleLength = CYCLE_LENGTH;

    public XLTurbineWorkableHandler(MetaTileEntity metaTileEntity, FuelRecipeMap recipeMap, Supplier<IEnergyContainer> energyContainer, Supplier<IMultipleTankHandler> importFluidTank, Supplier<IMultipleTankHandler> exportFluidTank) {
        super(metaTileEntity, recipeMap, energyContainer, importFluidTank, 0L);
        this.extremeTurbine = (MetaTileEntityXLTurbine) metaTileEntity;
        this.exportFluidTank = exportFluidTank;
    }

    public static float getTurbineBonus() {
        float castTurbineBonus = 100 * TURBINE_BONUS;
        return (int) castTurbineBonus;
    }

    @Override
    public void update() {
        if (this.getMetaTileEntity().getWorld().isRemote || !isWorkingEnabled())
            return;

        if (this.extremeTurbine.getOffsetTimer() % 20 == 0)
            this.totalEnergyProduced = (int) this.getRecipeOutputVoltage();

        if (this.totalEnergyProduced > 0)
            this.energyContainer.get().addEnergy(this.totalEnergyProduced);

        if (this.progress > 0 && !this.isActive())
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            this.extremeTurbine.calculateMaintenance(this.rotorDamageMultiplier * this.maxProgress);
            this.progress = 0;
            this.setActive(false);
        }

        if (this.progress <= 0) {
            if (this.fastMode != this.isFastMode)
                this.toggleFastMode(this.fastMode);
            if (this.extremeTurbine.getNumProblems() >= 6 || !this.isReadyForRecipes() || !this.tryAcquireNewRecipe())
                return;
            this.progress = 1;
            this.setActive(true);
        } else {
            this.progress++;
        }
    }

    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
        this.getMetaTileEntity().markDirty();
    }

    public boolean isFastMode() {
        return this.fastMode;
    }

    private void toggleFastMode(boolean toggle) {
        for (MetaTileEntityRotorHolder rotorHolder : this.extremeTurbine.getAbilities(ABILITY_ROTOR_HOLDER))
            rotorHolder.resetRotorSpeed();
        this.isFastMode = toggle;
        if (toggle) {
            this.fastModeMultiplier = 3;
            this.rotorDamageMultiplier = 16;
        } else {
            this.fastModeMultiplier = 1;
            this.rotorDamageMultiplier = 1;
        }
    }

    public FluidStack getFuelStack() {
        if (this.previousRecipe == null)
            return null;
        FluidStack fuelStack = this.previousRecipe.getRecipeFluid();
        return this.fluidTank.get().drain(new FluidStack(fuelStack.getFluid(), Integer.MAX_VALUE), false);
    }

    private boolean tryAcquireNewRecipe() {
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

    private int tryAcquireNewRecipe(FluidStack fluidStack) {
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
        if (currentRecipe != null && this.checkRecipe(currentRecipe)) {
            int fuelAmountToUse = this.calculateFuelAmount(currentRecipe);
            if (fluidStack.amount >= fuelAmountToUse) {
                this.maxProgress = this.calculateRecipeDuration(currentRecipe);
                if (this.extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.PLASMA)
                    this.exportFluidTank.get().fill(FluidRegistry.getFluidStack(FluidRegistry.getFluidName(currentRecipe.getRecipeFluid()).substring(7), fuelAmountToUse), true);
                else if (this.extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM)
                    this.exportFluidTank.get().fill(DistilledWater.getFluid(fuelAmountToUse / 160), true);
                return fuelAmountToUse;
            }
        }
        return 0;
    }

    @Override
    public boolean checkRecipe(FuelRecipe recipe) {
        List<MetaTileEntityRotorHolder> rotorHolders = this.extremeTurbine.getAbilities(ABILITY_ROTOR_HOLDER);
        if (++this.rotorCycleLength >= CYCLE_LENGTH) {
            for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
                int baseRotorDamage = BASE_ROTOR_DAMAGE;
                if (this.extremeTurbine.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM)
                    baseRotorDamage = 150;
                int damageToBeApplied = (int) Math.round((baseRotorDamage * rotorHolder.getRelativeRotorSpeed()) + 1) * rotorDamageMultiplier;
                if (!rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                    return false;
                }
            }
            this.rotorCycleLength = 0;
        }
        return true;
    }

    @Override
    protected boolean isReadyForRecipes() {
        int areReadyForRecipes = 0;
        int rotorHolderSize = this.extremeTurbine.getAbilities(ABILITY_ROTOR_HOLDER).size();
        for (int index = 0; index < rotorHolderSize; index++) {
            MetaTileEntityRotorHolder rotorHolder = this.extremeTurbine.getAbilities(ABILITY_ROTOR_HOLDER).get(index);
            if (rotorHolder.isHasRotor())
                areReadyForRecipes++;
        }
        return areReadyForRecipes == rotorHolderSize;
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
    public long getMaxVoltage() {
        double totalEnergyOutput = 0;
        List<MetaTileEntityRotorHolder> rotorHolders = this.extremeTurbine.getAbilities(ABILITY_ROTOR_HOLDER);
        for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
            if (rotorHolder.hasRotorInInventory()) {
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += BASE_EU_VOLTAGE + this.getBonusForTurbineType(this.extremeTurbine) * rotorEfficiency;
            }
        }
        return MathHelper.ceil(totalEnergyOutput * this.fastModeMultiplier / 16);
    }

    @Override
    public long getRecipeOutputVoltage() {
        double totalEnergyOutput = 0;
        for (MetaTileEntityRotorHolder rotorHolder : this.extremeTurbine.getAbilities(ABILITY_ROTOR_HOLDER)) {
            if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory() && rotorHolder.isFrontFaceFree()) {
                double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + getBonusForTurbineType(this.extremeTurbine) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
            }
        }
        totalEnergyOutput /= 1.00 + 0.05 * this.extremeTurbine.getNumProblems();
        return MathHelper.ceil(totalEnergyOutput * fastModeMultiplier * TURBINE_BONUS);
    }

    @Override
    protected int calculateFuelAmount(FuelRecipe currentRecipe) {
        int durationMultiplier = this.extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? 20 : 1;
        return (int) ((super.calculateFuelAmount(currentRecipe) * durationMultiplier) / (this.isFastMode ? 1 : TURBINE_BONUS));
    }

    @Override
    protected int calculateRecipeDuration(FuelRecipe currentRecipe) {
        int durationMultiplier = this.extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? 20 : 1;
        return super.calculateRecipeDuration(currentRecipe) * durationMultiplier;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", this.rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", this.fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", this.rotorDamageMultiplier);
        tagCompound.setBoolean("IsFastMode", this.isFastMode);
        tagCompound.setBoolean("FastMode", this.fastMode);
        tagCompound.setInteger("Consumption", this.consumption);
        tagCompound.setInteger("TotalEnergy", this.totalEnergyProduced);
        tagCompound.setInteger("Progress", this.progress);
        tagCompound.setInteger("MaxProgress", this.maxProgress);
        if (this.fuelName != null)
            tagCompound.setString("FuelName", this.fuelName);
        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        this.rotorCycleLength = compound.getInteger("CycleLength");
        this.fastModeMultiplier = compound.getInteger("FastModeMultiplier");
        this.rotorDamageMultiplier = compound.getInteger("DamageMultiplier");
        this.isFastMode = compound.getBoolean("IsFastMode");
        this.fastMode = compound.getBoolean("FastMode");
        this.consumption = compound.getInteger("Consumption");
        this.fuelName = compound.getString("FuelName");
        this.totalEnergyProduced = compound.getInteger("TotalEnergy");
        this.maxProgress = compound.getInteger("MaxProgress");
        this.progress = compound.getInteger("Progress");
        if (compound.hasKey("FuelName"))
            this.fuelName = compound.getString("FuelName");
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
        return this.totalEnergyProduced;
    }

    @Override
    public String[] consumptionInfo() {
        int seconds = this.maxProgress / 20;
        String amount = String.valueOf(seconds);
        String s = seconds < 2 ? "second" : "seconds";
        String color = this.extremeTurbine.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? "§7 " : "§b ";
        return ArrayUtils.toArray("machine.universal.consumption", color, "suffix", "machine.universal.liters.short",  "§r§7(§b", this.fuelName, "§7)§r ", "every", "§b ", amount, "§r ", s);
    }

    @Override
    public String[] productionInfo() {
        int tier = GAUtility.getTierByVoltage(this.totalEnergyProduced);
        String voltage = GAValues.VN[tier];
        String color = TJValues.VCC[tier];
        return ArrayUtils.toArray("machine.universal.producing", "§e ", "suffix", "§r ", "machine.universal.eu.tick",
                " ", "§7(§6", color, voltage, "§7)");
    }
}

