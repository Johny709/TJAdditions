package tj.capability.impl;

import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IWorkable;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.function.BooleanConsumer;
import gregtech.common.ConfigHolder;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.function.*;

/**
 *
 * @param <I> type of item handler impl
 * @param <F> type of fluid tank impl
 */
public abstract class AbstractWorkableHandler<I, F> extends MTETrait implements IWorkable {

    protected Supplier<I> importItems;
    protected Supplier<I> exportItems;
    protected Supplier<F> importFluids;
    protected Supplier<F> exportFluids;
    protected Supplier<IEnergyContainer> importEnergy;
    protected IntFunction<I> inputBus;
    protected LongSupplier maxVoltage;
    protected IntSupplier tier;
    protected IntSupplier parallel;
    private BooleanConsumer activeConsumer;
    protected boolean isWorking = true;
    protected boolean isActive;
    protected boolean wasActiveAndNeedsUpdate;
    protected boolean isDistinct;
    protected boolean hasProblem;
    protected long energyPerTick;
    protected int progress;
    protected int maxProgress;
    protected int lastInputIndex;
    protected int busCount;
    protected int sleepTimer;
    protected int sleepTime = 1;
    protected int failCount;

    public AbstractWorkableHandler(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    /**
     * this should be called to initialize some stuff before this workable handler starts running!
     * @param busCount amount of item input buses
     */
    public void initialize(int busCount) {
        this.lastInputIndex = 0;
        this.busCount = busCount;
    }

    /**
     * @param importItems Item input supplier
     */
    public void setImportItems(Supplier<I> importItems) {
        this.importItems = importItems;
    }

    /**
     * @param exportItems Item output supplier
     */
    public void setExportItems(Supplier<I> exportItems) {
        this.exportItems = exportItems;
    }

    /**
     * @param importFluids Fluid input supplier
     */
    public void setImportFluids(Supplier<F> importFluids) {
        this.importFluids = importFluids;
    }

    /**
     * @param exportFluids Fluid output supplier
     */
    public void setExportFluids(Supplier<F> exportFluids) {
        this.exportFluids = exportFluids;
    }

    /**
     * @param importEnergy Energy Input supplier
     */
    public void setImportEnergy(Supplier<IEnergyContainer> importEnergy) {
        this.importEnergy = importEnergy;
    }

    /**
     * @param inputBus For Input bus distinct mode
     */
    public void setInputBus(IntFunction<I> inputBus) {
        this.inputBus = inputBus;
    }

    /**
     * @param maxVoltage Voltage long supplier
     */
    public void setMaxVoltage(LongSupplier maxVoltage) {
        this.maxVoltage = maxVoltage;
    }

    /**
     * @param tier tier int supplier
     */
    public void setTier(IntSupplier tier) {
        this.tier = tier;
    }

    /**
     * @param parallel amount of parallels int supplier
     */
    public void setParallel(IntSupplier parallel) {
        this.parallel = parallel;
    }

    /**
     * @param activeConsumer isActive boolean consumer
     */
    public void setActive(BooleanConsumer activeConsumer) {
        this.activeConsumer = activeConsumer;
    }

    /**
     * Don't forget to set {@Link gregtech.api.metatileentity.MetaTileEntity#shouldUpdate(MTETrait trait) shouldUpdate} to return false for the MetaTileEntity using this workable handler.
     */
    @Override
    public void update() {
        if (!this.isWorking) {
            this.stopRecipe();
            return;
        }

        if (this.wasActiveAndNeedsUpdate && this.isActive)
            this.setActive(false);

        if (this.progress < 1) {
            if (this.sleepTimer > 0) {
                this.sleepRecipe();
                return;
            }
            boolean canStart = this.startRecipe();
            if (canStart) {
                this.progress = 1;
                this.sleepTime = 1;
                this.progressRecipe(this.progress);
                if (!this.isActive)
                    this.setActive(true);
            } else this.failRecipe();
            this.wasActiveAndNeedsUpdate = !canStart;
        } else this.progressRecipe(this.progress);

        if (this.progress > this.maxProgress) {
            if (this.completeRecipe()) {
                this.progress = 0;
                this.energyPerTick = 0;
                if (this.hasProblem)
                    this.setProblem(false);
            } else {
                this.progress--;
                if (!this.hasProblem)
                    this.setProblem(true);
            }
        }
    }

    /**
     * @return true if the recipe can start
     */
    protected boolean startRecipe() {
        return false;
    }

    /**
     * For every tick the workable handler is on sleep timer
     */
    @OverridingMethodsMustInvokeSuper
    protected void sleepRecipe() {
        this.sleepTimer--;
    }

    /**
     * For every tick the workable handler is stopped
     */
    protected void stopRecipe() {}

    /**
     * When recipe was unable to start. Recommended to invoke super method to utilize sleep timer for performance
     */
    @OverridingMethodsMustInvokeSuper
    protected void failRecipe() {
        if (this.failCount > 4) {
            this.sleepTime = Math.min(this.sleepTime * 2, Math.min(ConfigHolder.maxSleepTime, 400));
            this.failCount = 0;
        } else this.failCount++;
        this.sleepTimer = this.sleepTime;
    }

    protected void progressRecipe(int progress) {
        if (this.importEnergy.get().getEnergyStored() >= this.energyPerTick) {
            this.importEnergy.get().removeEnergy(this.energyPerTick);
            this.progress++;
        } else if (this.progress > 1)
            this.progress--;
    }

    /**
     * @return true if the recipe can be completed and then will start the next recipe.
     */
    protected boolean completeRecipe() {
        return false;
    }

    protected int calculateOverclock(long baseEnergy, int duration, float multiplier) {
        long voltage = this.maxVoltage.getAsLong();
        baseEnergy *= 4;
        while (duration > 1 && baseEnergy <= voltage) {
            duration /= multiplier;
            baseEnergy *= 4;
        }
        this.energyPerTick = baseEnergy / 4;
        return Math.max(1, duration);
    }

    public boolean hasEnoughFluid(FluidStack fluid, int amount) {
        if (this.importFluids.get() instanceof IFluidHandler) {
            FluidStack fluidStack = ((IFluidHandler) this.importFluids.get()).drain(fluid, false);
            return fluidStack != null && fluidStack.amount == amount || amount == 0;
        }
        return false;
    }

    public boolean canOutputFluid(FluidStack fluid, int amount) {
        if (this.exportFluids.get() instanceof IFluidHandler) {
            int fluidStack = ((IFluidHandler) this.exportFluids.get()).fill(fluid, false);
            return fluidStack == amount || amount == 0;
        }
        return false;
    }

    @Override
    public void receiveCustomData(int id, PacketBuffer buffer) {
        super.receiveCustomData(id, buffer);
        switch (id) {
            case 1: this.isActive = buffer.readBoolean(); break;
            case 2: this.hasProblem = buffer.readBoolean(); break;
            case 3: this.isWorking = buffer.readBoolean(); break;
        }
        this.metaTileEntity.scheduleRenderUpdate();
    }

    @Override
    public void writeInitialData(PacketBuffer buffer) {
        buffer.writeBoolean(this.isActive);
        buffer.writeBoolean(this.hasProblem);
        buffer.writeBoolean(this.isWorking);
    }

    @Override
    public void receiveInitialData(PacketBuffer buffer) {
        this.isActive = buffer.readBoolean();
        this.hasProblem = buffer.readBoolean();
        this.isWorking = buffer.readBoolean();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("progress", this.progress);
        compound.setInteger("maxProgress", this.maxProgress);
        compound.setLong("energyPerTick", this.energyPerTick);
        compound.setBoolean("isWorking", this.isWorking);
        compound.setBoolean("isDistinct", this.isDistinct);
        compound.setBoolean("isActive", this.isActive);
        compound.setBoolean("wasActiveAndNeedsUpdate", this.wasActiveAndNeedsUpdate);
        compound.setBoolean("hasProblem", this.hasProblem);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        this.maxProgress = compound.getInteger("maxProgress");
        this.progress = compound.getInteger("progress");
        this.energyPerTick = compound.getLong("energyPerTick");
        this.isWorking = compound.getBoolean("isWorking");
        this.isDistinct = compound.getBoolean("isDistinct");
        this.isActive = compound.getBoolean("isActive");
        this.wasActiveAndNeedsUpdate = compound.getBoolean("wasActiveAndNeedsUpdate");
        this.hasProblem = compound.getBoolean("hasProblem");
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE)
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        return capability == GregtechTileCapabilities.CAPABILITY_WORKABLE ? GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this) : null;
    }

    @Override
    public String getName() {
        return "RecipeWorkable";
    }

    @Override
    public int getNetworkID() {
        return TraitNetworkIds.TRAIT_ID_WORKABLE;
    }

    public void setDistinct(boolean distinct) {
        this.isDistinct = distinct;
        this.metaTileEntity.markDirty();
    }

    public boolean isDistinct() {
        return this.isDistinct;
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }

    public double getProgressPercent() {
        return this.getMaxProgress() == 0 ? 0.0 : this.getProgress() / (this.getMaxProgress() * 1.0);
    }

    public boolean hasNotEnoughEnergy() {
        return this.isActive && this.importEnergy.get().getEnergyStored() < this.energyPerTick;
    }

    public long getEUt() {
        return this.energyPerTick;
    }

    @Override
    public boolean isActive() {
        return this.isActive;
    }

    public boolean hasProblem() {
        return this.hasProblem;
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.isWorking;
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        this.isWorking = isWorking;
        if (!this.metaTileEntity.getWorld().isRemote) {
            this.writeCustomData(3, buffer -> buffer.writeBoolean(isWorking));
            this.metaTileEntity.markDirty();
        }
    }

    public void setProblem(boolean hasProblem) {
        this.hasProblem = hasProblem;
        if (!this.metaTileEntity.getWorld().isRemote) {
            this.writeCustomData(2, buffer -> buffer.writeBoolean(hasProblem));
            this.metaTileEntity.markDirty();
        }
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        if (!this.metaTileEntity.getWorld().isRemote) {
            if (this.activeConsumer != null)
                this.activeConsumer.apply(isActive);
            this.writeCustomData(1, buffer -> buffer.writeBoolean(isActive));
            this.metaTileEntity.markDirty();
        }
    }
}
