package tj.builder.handlers;

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
import org.apache.commons.lang3.ArrayUtils;
import tj.capability.IGeneratorInfo;
import tj.capability.TJCapabilities;
import tj.machines.multi.electric.MetaTileEntityLargeAtmosphereCollector;

import java.util.function.Supplier;

import static gregtech.api.unification.material.Materials.Air;

public class LargeAtmosphereCollectorWorkableHandler extends FuelRecipeLogic implements IWorkable, IGeneratorInfo {

    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 22;
    private static final long BASE_EU_OUTPUT = 512;

    private final MetaTileEntityLargeAtmosphereCollector airCollector;
    private int rotorCycleLength = CYCLE_LENGTH;

    private int totalAirProduced;
    private int consumption;
    private String fuelName;
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

        this.totalAirProduced = (int) getRecipeOutputVoltage();

        if (this.totalAirProduced > 0) {
            this.airCollector.exportFluidHandler.fill(Air.getFluid(this.totalAirProduced), true);
        }

        if (this.progress > 0 && !this.isActive())
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            this.airCollector.calculateMaintenance(this.rotorDamageMultiplier * this.maxProgress);
            this.progress = 0;
            this.setActive(false);
        }

        if (this.progress <= 0) {
            toggleFastMode(this.isFastMode);
            if (this.airCollector.getNumProblems() >= 6 || !this.isReadyForRecipes() || !this.tryAcquireNewRecipe())
                return;
            this.progress = 1;
            this.setActive(true);
        } else {
            this.progress++;
        }
    }

    public void setFastMode(boolean isFastMode) {
        this.isFastMode = isFastMode;
        this.getMetaTileEntity().markDirty();
    }

    public boolean isFastMode() {
        return this.isFastMode;
    }

    private void toggleFastMode(boolean toggle) {
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
        IMultipleTankHandler fluidTanks = this.fluidTank.get();
        for (IFluidTank fluidTank : fluidTanks) {
            FluidStack tankContents = fluidTank.getFluid();
            if (tankContents != null && tankContents.amount > 0) {
                int fuelAmountUsed = this.tryAcquireNewRecipe(tankContents);
                if (fuelAmountUsed > 0) {
                    FluidStack fluidStack = fluidTank.drain(fuelAmountUsed, true);
                    this.consumption = fluidStack.amount;
                    this.fuelName = fluidStack.getUnlocalizedName();
                    return true; //recipe is found and ready to use
                }
            }
        }
        return false;
    }

    private int tryAcquireNewRecipe(FluidStack fluidStack) {
        FuelRecipe currentRecipe;
        if (this.previousRecipe != null && this.previousRecipe.matches(getMaxVoltage(), fluidStack)) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = this.previousRecipe;
        } else {
            //else, try searching new recipe for given inputs
            currentRecipe = this.recipeMap.findRecipe(getMaxVoltage(), fluidStack);
            //if we found recipe that can be buffered, buffer it
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
            }
        }
        if (currentRecipe != null && this.checkRecipe(currentRecipe)) {
            int fuelAmountToUse = this.calculateFuelAmount(currentRecipe);
            if (fluidStack.amount >= fuelAmountToUse) {
                this.maxProgress = calculateRecipeDuration(currentRecipe);
                this.startRecipe(currentRecipe, fuelAmountToUse, this.maxProgress);
                return fuelAmountToUse;
            }
        }
        return 0;
    }

    @Override
    public boolean checkRecipe(FuelRecipe recipe) {
        MetaTileEntityRotorHolder rotorHolder = this.airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        int baseRotorDamage = BASE_ROTOR_DAMAGE;
        if (++this.rotorCycleLength >= CYCLE_LENGTH) {
            if (this.airCollector.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM) baseRotorDamage = 150;
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
        MetaTileEntityRotorHolder rotorHolder = this.airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        if (rotorHolder.hasRotorInInventory()) {
            double rotorEfficiency = rotorHolder.getRotorEfficiency();
            double totalEnergyOutput = (BASE_EU_OUTPUT + getBonusForTurbineType(this.airCollector) * rotorEfficiency);
            return MathHelper.ceil(totalEnergyOutput * this.fastModeMultiplier);
        }
        return BASE_EU_OUTPUT + this.getBonusForTurbineType(this.airCollector);
    }

    @Override
    protected boolean isReadyForRecipes() {
        MetaTileEntityRotorHolder rotorHolder = this.airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        return rotorHolder.isHasRotor();
    }

    @Override
    protected long startRecipe(FuelRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        this.addOutputFluids(currentRecipe, fuelAmountUsed);
        return 0L; //energy is added each tick while the rotor speed is >0 RPM
    }

    private void addOutputFluids(FuelRecipe currentRecipe, int fuelAmountUsed) {
        if (this.airCollector.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM) {
            int waterFluidAmount = fuelAmountUsed / 15;
            if (waterFluidAmount > 0) {
                FluidStack waterStack = Materials.Water.getFluid(waterFluidAmount);
                if (this.airCollector.exportFluidHandler != null) {
                    this.airCollector.exportFluidHandler.fill(waterStack, true);
                }
            }
        } else if (this.airCollector.turbineType == MetaTileEntityLargeTurbine.TurbineType.PLASMA) {
            FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
            if (material != null) {
                if (this.airCollector.exportFluidHandler != null) {
                    this.airCollector.exportFluidHandler.fill(material.getFluid(fuelAmountUsed), true);
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
        MetaTileEntityRotorHolder rotorHolder = this.airCollector.getAbilities(MetaTileEntityLargeAtmosphereCollector.ABILITY_ROTOR_HOLDER).get(0);
        double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
        if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
            double rotorEfficiency = rotorHolder.getRotorEfficiency();
            totalEnergyOutput = ((BASE_EU_OUTPUT + this.getBonusForTurbineType(this.airCollector)) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
            totalEnergyOutput /= 1.00 + 0.05 * this.airCollector.getNumProblems();
            return MathHelper.ceil(totalEnergyOutput * this.fastModeMultiplier);
        }
        return 0L;
    }

    @Override
    protected int calculateFuelAmount(FuelRecipe currentRecipe) {
        int durationMultiplier = this.airCollector.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? 2 : 1;
        return super.calculateFuelAmount(currentRecipe) * durationMultiplier;
    }

    @Override
    protected int calculateRecipeDuration(FuelRecipe currentRecipe) {
        int durationMultiplier = this.airCollector.turbineType == MetaTileEntityLargeTurbine.TurbineType.STEAM ? 2 : 1;
        return super.calculateRecipeDuration(currentRecipe) * durationMultiplier;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", this.rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", this.fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", this.rotorDamageMultiplier);
        tagCompound.setBoolean("IsFastMode", this.isFastMode);
        tagCompound.setInteger("Consumption", this.consumption);
        tagCompound.setInteger("TotalAir", this.totalAirProduced);
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
        this.consumption = compound.getInteger("Consumption");
        this.totalAirProduced = compound.getInteger("TotalAir");
        this.progress = compound.getInteger("Progress");
        this.maxProgress = compound.getInteger("MaxProgress");
        if (compound.hasKey("FuelName"))
            this.fuelName = compound.getString("FuelName");
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
        return this.totalAirProduced;
    }

    @Override
    public String[] consumptionInfo() {
        int seconds = this.maxProgress / 20;
        String amount = String.valueOf(seconds);
        String s = seconds < 2 ? "second" : "seconds";
        return ArrayUtils.toArray("machine.universal.consumption", "§7 ", "suffix", "machine.universal.liters.short",  "§r ", this.fuelName, " ", "every", "§6 ", amount, "§r ", s);
    }

    @Override
    public String[] productionInfo() {
        return ArrayUtils.toArray("machine.universal.producing", "§b ", "suffix", "machine.universal.liters.short", "§r ", Air.getUnlocalizedName(), "machine.universal.tick");
    }
}
