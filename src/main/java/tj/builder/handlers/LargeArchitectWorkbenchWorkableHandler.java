package tj.builder.handlers;

import gregicadditions.GAUtility;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;

import java.util.Collections;
import java.util.List;
import java.util.function.*;

public class LargeArchitectWorkbenchWorkableHandler extends MTETrait implements IWorkable, IItemFluidHandlerInfo {

    private final Supplier<ItemHandlerList> itemInputs;
    private final Supplier<ItemHandlerList> itemOutputs;
    private final Supplier<IEnergyContainer> energyInputs;
    private final IntFunction<IItemHandlerModifiable> inputBus;
    private final LongSupplier maxVoltage;
    private final IntSupplier parallel;
    private ItemStack catalyst;
    private ItemStack input;
    private ItemStack output;
    private boolean isWorking;
    private boolean isActive;
    private boolean wasActiveAndNeedsUpdate;
    private boolean isDistinct;
    private int progress;
    private int maxProgress;
    private int lastInputIndex;
    private int busCount;

    public LargeArchitectWorkbenchWorkableHandler(MetaTileEntity metaTileEntity, Supplier<ItemHandlerList> itemInputs, Supplier<ItemHandlerList> itemOutputs, Supplier<IEnergyContainer> energyInputs, IntFunction<IItemHandlerModifiable> inputBus, LongSupplier maxVoltage, IntSupplier parallel) {
        super(metaTileEntity);
        this.itemInputs = itemInputs;
        this.itemOutputs = itemOutputs;
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
        if (this.wasActiveAndNeedsUpdate)
            this.setActive(false);

        if (this.isWorking && this.progress < 1 && !this.startRecipe()) {
            this.wasActiveAndNeedsUpdate = true;
            return;
        }

        if (this.progress < this.maxProgress) {
            this.progressRecipe();
        } else this.completeRecipe();
    }

    private boolean startRecipe() {
        boolean canStart = false;
        IItemHandlerModifiable itemInputs = this.isDistinct ? this.inputBus.apply(this.lastInputIndex) : this.itemInputs.get();
        if (this.findCatalyst(itemInputs) && this.findInputs(itemInputs)) {
            this.output = new ItemStack(Item.getByNameOrId("architecturecraft:shape"), this.input.getCount());
            NBTTagCompound compound = this.catalyst.getTagCompound().copy();
            compound.setString("BaseName", Item.REGISTRY.getNameForObject(this.input.getItem()).toString());
            compound.setInteger("BaseData", this.input.getMetadata());
            this.output.setTagCompound(compound);
            int recipeTickDuration = Math.round((float) 200L / GAUtility.getTierByVoltage(this.maxVoltage.getAsLong()));
            this.maxProgress = Math.max(1, recipeTickDuration);
            this.wasActiveAndNeedsUpdate = false;
            this.setActive(true);
            canStart = true;
        }
        if (++this.lastInputIndex == this.busCount)
            this.lastInputIndex = 0;
        return canStart;
    }

    private void progressRecipe() {
        if (this.energyInputs.get().getEnergyStored() > this.maxVoltage.getAsLong()) {
            this.energyInputs.get().removeEnergy(this.maxVoltage.getAsLong());
            this.progress++;
        } else if (this.progress > 0)
            this.progress--;
    }

    private void completeRecipe() {
        if (!ItemHandlerHelper.insertItemStacked(this.itemOutputs.get(), this.output, true).isEmpty()) {
            this.progress--;
            return;
        }
        ItemHandlerHelper.insertItemStacked(this.itemOutputs.get(), this.output, false);
        this.progress = 0;
        this.catalyst = null;
        this.input = null;
        this.output = null;
    }

    private boolean findCatalyst(IItemHandlerModifiable itemInputs) {
        for (int i = 0; i < itemInputs.getSlots(); i++) {
            ItemStack stack = itemInputs.getStackInSlot(i);
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (tagCompound != null) {
                if (tagCompound.hasKey("Shape") && tagCompound.hasKey("BaseName") && tagCompound.hasKey("BaseData")) {
                    this.catalyst = stack;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findInputs(IItemHandlerModifiable itemInputs) {
        int availableParallels = this.parallel.getAsInt();
        int count = 0;
        for (int i = 0; i < itemInputs.getSlots() && availableParallels != 0; i++) {
            ItemStack stack = itemInputs.getStackInSlot(i);
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (stack.isEmpty() || tagCompound != null && tagCompound.hasKey("Shape") && tagCompound.hasKey("BaseName") && tagCompound.hasKey("BaseData"))
                continue;
            if (this.input == null) {
                this.input = stack.copy();
            }
            if (!stack.isItemEqual(this.input))
                continue;
            int reminder = Math.min(stack.getCount(), availableParallels);
            availableParallels -= reminder;
            count += reminder;
            stack.shrink(reminder);
            this.input.setCount(count);
        }
        return count > 0;
    }

    @Override
    public void receiveCustomData(int id, PacketBuffer buffer) {
        super.receiveCustomData(id, buffer);
        if (id == 1) {
            this.isActive = buffer.readBoolean();
            this.metaTileEntity.scheduleRenderUpdate();
        }
    }

    @Override
    public void writeInitialData(PacketBuffer buffer) {
        buffer.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialData(PacketBuffer buffer) {
        this.isActive = buffer.readBoolean();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("progress", this.progress);
        compound.setInteger("maxProgress", this.maxProgress);
        compound.setBoolean("isWorking", this.isWorking);
        compound.setBoolean("isDistinct", this.isDistinct);
        compound.setBoolean("isActive", this.isActive);
        compound.setBoolean("wasActiveAndNeedsUpdate", this.wasActiveAndNeedsUpdate);
        if (this.catalyst != null)
            compound.setTag("catalyst", this.catalyst.serializeNBT());
        if (this.input != null)
            compound.setTag("input", this.input.serializeNBT());
        if (this.output != null)
            compound.setTag("output", this.output.serializeNBT());
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        this.maxProgress = compound.getInteger("maxProgress");
        this.progress = compound.getInteger("progress");
        this.isWorking = compound.getBoolean("isWorking");
        this.isDistinct = compound.getBoolean("isDistinct");
        this.isActive = compound.getBoolean("isActive");
        this.wasActiveAndNeedsUpdate = compound.getBoolean("wasActiveAndNeedsUpdate");
        if (compound.hasKey("catalyst"))
            this.catalyst = new ItemStack(compound.getCompoundTag("catalyst"));
        if (compound.hasKey("input"))
            this.input = new ItemStack(compound.getCompoundTag("input"));
        if (compound.hasKey("output"))
            this.output = new ItemStack(compound.getCompoundTag("output"));
    }

    @Override
    public String getName() {
        return "RecipeMapWorkable";
    }

    @Override
    public int getNetworkID() {
        return TraitNetworkIds.TRAIT_ID_WORKABLE;
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return capability == GregtechTileCapabilities.CAPABILITY_WORKABLE ? GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this) : null;
    }

    public void setDistinct(boolean distinct) {
        this.isDistinct = distinct;
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

    @Override
    public boolean isActive() {
        return this.isActive;
    }

    @Override
    public List<ItemStack> getItemInputs() {
        return Collections.singletonList(this.input);
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return Collections.singletonList(this.output);
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.isWorking;
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        this.isWorking = isWorking;
        this.setActive(isWorking);
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        if (!this.metaTileEntity.getWorld().isRemote) {
            this.writeCustomData(1, buffer -> buffer.writeBoolean(isActive));
            this.metaTileEntity.markDirty();
        }
    }
}
