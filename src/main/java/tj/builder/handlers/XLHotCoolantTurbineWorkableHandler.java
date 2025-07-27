package tj.builder.handlers;

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
import tj.TJValues;
import tj.capability.IGeneratorInfo;
import tj.capability.TJCapabilities;
import tj.machines.multi.electric.MetaTileEntityXLHotCoolantTurbine;
import tj.machines.multi.electric.MetaTileEntityXLTurbine;

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
        if (this.getMetaTileEntity().getWorld().isRemote || !this.isWorkingEnabled())
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
            this.toggleFastMode(this.isFastMode);
            if (this.extremeTurbine.getNumProblems() >= 6 || !this.isReadyForRecipes() || !this.tryAcquireNewRecipe())
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
        HotCoolantRecipe currentRecipe;
        if (this.previousRecipe != null && this.previousRecipe.matches(this.getMaxVoltage(), fluidStack)) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = this.previousRecipe;
        } else {
            //else, try searching new recipe for given inputs
            currentRecipe = recipeMap.findRecipe(this.getMaxVoltage(), fluidStack);
            //if we found recipe that can be buffered, buffer it
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
            }
        }
        if (currentRecipe != null && this.checkRecipe(currentRecipe)) {
            int fuelAmountToUse = this.calculateFuelAmount(currentRecipe);
            if (fluidStack.amount >= fuelAmountToUse) {
                this.maxProgress = this.calculateRecipeDuration(currentRecipe);
                this.startRecipe(currentRecipe, fuelAmountToUse, this.maxProgress);
                return fuelAmountToUse;
            }
        }
        return 0;
    }

    @Override
    public boolean checkRecipe(HotCoolantRecipe recipe) {
        List<MetaTileEntityRotorHolder> rotorHolders = this.extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER);
        if (++this.rotorCycleLength >= CYCLE_LENGTH) {
            for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
                int damageToBeApplied = (int) Math.round((BASE_ROTOR_DAMAGE * rotorHolder.getRelativeRotorSpeed()) + 1) * this.rotorDamageMultiplier;
                if (!rotorHolder.applyDamageToRotor(damageToBeApplied, false)) {
                    return false;
                }
            }
            this.rotorCycleLength = 0;
        }
        return true;
    }

    @Override
    public long getMaxVoltage() {
        double totalEnergyOutput = 0;
        List<MetaTileEntityRotorHolder> rotorHolders = this.extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER);
        for (MetaTileEntityRotorHolder rotorHolder : rotorHolders) {
            if (rotorHolder.hasRotorInInventory()) {
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + this.getBonusForTurbineType(this.extremeTurbine) * rotorEfficiency);
            }
        }
        return MathHelper.ceil((totalEnergyOutput / rotorHolders.size() * this.fastModeMultiplier) / TURBINE_BONUS);
    }

    protected boolean isReadyForRecipes() {
        int areReadyForRecipes = 0;
        int rotorHolderSize = this.extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).size();
        for (int index = 0; index < rotorHolderSize; index++) {
            MetaTileEntityRotorHolder rotorHolder = this.extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER).get(index);
            if (rotorHolder.isHasRotor())
                areReadyForRecipes++;
        }
        return areReadyForRecipes == rotorHolderSize;
    }

    @Override
    protected long startRecipe(HotCoolantRecipe currentRecipe, int fuelAmountUsed, int recipeDuration) {
        this.addOutputFluids(currentRecipe, fuelAmountUsed);
        return 0L; //energy is added each tick while the rotor speed is >0 RPM
    }

    private void addOutputFluids(HotCoolantRecipe currentRecipe, int fuelAmountUsed) {
        if (this.extremeTurbine.turbineType == MetaTileEntityHotCoolantTurbine.TurbineType.HOT_COOLANT) {
            if (fuelAmountUsed > 0) {
                FluidMaterial material = MetaFluids.getMaterialFromFluid(currentRecipe.getRecipeFluid().getFluid());
                if (material != null) {
                    this.extremeTurbine.exportFluidHandler.fill(material.getFluid(fuelAmountUsed), true);
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
        for (MetaTileEntityRotorHolder rotorHolder : this.extremeTurbine.getAbilities(MetaTileEntityXLTurbine.ABILITY_ROTOR_HOLDER)) {
            if (rotorHolder.getCurrentRotorSpeed() > 0 && rotorHolder.hasRotorInInventory()) {
                double relativeRotorSpeed = rotorHolder.getRelativeRotorSpeed();
                double rotorEfficiency = rotorHolder.getRotorEfficiency();
                totalEnergyOutput += (BASE_EU_OUTPUT + this.getBonusForTurbineType(this.extremeTurbine) * rotorEfficiency) * (relativeRotorSpeed * relativeRotorSpeed);
                totalEnergyOutput /= 1.00 + 0.05 * this.extremeTurbine.getNumProblems();
            }
        }
        return MathHelper.ceil(totalEnergyOutput * this.fastModeMultiplier * TURBINE_BONUS);
    }

    @Override
    protected int calculateFuelAmount(HotCoolantRecipe currentRecipe) {
        return (int) (super.calculateFuelAmount(currentRecipe) / (this.isFastMode ? 1 : 1.5F));
    }

    @Override
    public void writeInitialData(PacketBuffer buf) {
        buf.writeBoolean(this.active);
    }

    @Override
    public void receiveInitialData(PacketBuffer buf) {
        this.active = buf.readBoolean();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 10)
            this.active = buf.readBoolean();
    }

    private void setActive(boolean active) {
        this.active = active;
        if (!this.getMetaTileEntity().getWorld().isRemote) {
            this.writeCustomData(10, buf -> buf.writeBoolean(active));
            this.getMetaTileEntity().markDirty();
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = super.serializeNBT();
        tagCompound.setInteger("CycleLength", this.rotorCycleLength);
        tagCompound.setInteger("FastModeMultiplier", this.fastModeMultiplier);
        tagCompound.setInteger("DamageMultiplier", this.rotorDamageMultiplier);
        tagCompound.setBoolean("IsFastMode", this.isFastMode);
        tagCompound.setInteger("Consumption", this.consumption);
        tagCompound.setInteger("TotalEnergy", this.totalEnergyProduced);
        tagCompound.setInteger("Progress", this.progress);
        tagCompound.setInteger("MaxProgress", this.maxProgress);
        tagCompound.setBoolean("Active", this.active);
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
        this.totalEnergyProduced = compound.getInteger("TotalEnergy");
        this.maxProgress = compound.getInteger("MaxProgress");
        this.progress = compound.getInteger("Progress");
        this.active = compound.getBoolean("Active");
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
        return this.totalEnergyProduced;
    }

    @Override
    public String[] consumptionInfo() {
        int seconds = this.maxProgress / 20;
        String amount = String.valueOf(seconds);
        String s = seconds < 2 ? "second" : "seconds";
        return ArrayUtils.toArray("machine.universal.consumption", "§b ", "suffix", "machine.universal.liters.short",  "§r ", this.fuelName, " ", "every", "§6 ", amount, "§r ", s);
    }

    @Override
    public String[] productionInfo() {
        int tier = GAUtility.getTierByVoltage(this.totalEnergyProduced);
        String voltage = GAValues.VN[tier];
        String color = TJValues.VCC[tier];
        return ArrayUtils.toArray("machine.universal.producing", "§e ", "suffix", "§r ", "machine.universal.eu.tick",
                " ", "§r(§6", color, voltage, "§r)");
    }
}
