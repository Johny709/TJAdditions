package tj.capability.impl;

import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IWorkable;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.function.*;

/**
 *
 * @param <I> type of item handler impl
 * @param <F> type of fluid tank impl
 */
public abstract class AbstractWorkableHandler<I, F> extends MTETrait implements IWorkable {

    protected final Supplier<I> importItems;
    protected final Supplier<I> exportItems;
    protected final Supplier<F> importFluids;
    protected final Supplier<F> exportFluids;
    protected final Supplier<IEnergyContainer> energyInputs;
    protected final IntFunction<I> inputBus;
    protected final LongSupplier maxVoltage;
    protected final IntSupplier parallel;
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

    public AbstractWorkableHandler(MetaTileEntity metaTileEntity, Supplier<I> importItems, Supplier<I> exportItems, Supplier<F> importFluids, Supplier<F> exportFluids,
                                   Supplier<IEnergyContainer> energyInputs, IntFunction<I> inputBus, LongSupplier maxVoltage, IntSupplier parallel) {
        super(metaTileEntity);
        this.importItems = importItems;
        this.exportItems = exportItems;
        this.importFluids = importFluids;
        this.exportFluids = exportFluids;
        this.energyInputs = energyInputs;
        this.inputBus = inputBus;
        this.maxVoltage = maxVoltage;
        this.parallel = parallel;
    }

    public void initialize(int busCount) {
        this.lastInputIndex = 0;
        this.busCount = busCount;
    }

    @Override
    public void update() {
        if (this.wasActiveAndNeedsUpdate && this.isActive)
            this.setActive(false);

        if (!this.isWorking || this.progress < 1 && !this.startRecipe()) {
            this.wasActiveAndNeedsUpdate = true;
            return;
        } else this.progressRecipe();

        if (this.progress >= this.maxProgress) {
            if (this.completeRecipe()) {
                this.progress = 0;
                if (this.hasProblem)
                    this.setProblem(false);
            } else if (!this.hasProblem)
                this.setProblem(true);
        } else if (!this.isActive)
            this.setActive(true);
    }

    /**
     * @return true if the recipe can start
     */
    protected boolean startRecipe() {
        return false;
    }

    protected void progressRecipe() {
        if (this.energyInputs.get().getEnergyStored() >= this.energyPerTick) {
            this.energyInputs.get().removeEnergy(this.energyPerTick);
            this.progress++;
        } else if (this.progress > 1)
            this.progress--;
    }

    /**
     * @return true if the recipe can be completed
     */
    protected boolean completeRecipe() {
        return false;
    }

    protected int calculateOverclock(long baseEnergy, int duration, float multiplier) {
        long voltage = this.maxVoltage.getAsLong();
        baseEnergy *= 4;
        for (int i = 1; duration > 1 && baseEnergy <= voltage; i++) {
            duration /= multiplier;
            baseEnergy *= 4;
        }
        this.energyPerTick = baseEnergy;
        return Math.max(1, duration);
    }

    public <N extends Number> boolean hasEnoughFluid(FluidStack fluid, N amount) {
        if (this.importFluids.get() instanceof IFluidHandler) {
            FluidStack fluidStack = ((IFluidHandler) this.importFluids.get()).drain(fluid, false);
            return fluidStack != null && fluidStack.amount == amount.intValue();
        }
        return false;
    }

    public <N extends Number> boolean canOutputFluid(FluidStack fluid, N amount) {
        if (this.exportFluids.get() instanceof IFluidHandler) {
            int fluidStack = ((IFluidHandler) this.exportFluids.get()).fill(fluid, false);
            return fluidStack == amount.intValue();
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
        return this.isActive && this.energyInputs.get().getEnergyStored() < this.energyPerTick;
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
            this.writeCustomData(1, buffer -> buffer.writeBoolean(isActive));
            this.metaTileEntity.markDirty();
        }
    }
}
