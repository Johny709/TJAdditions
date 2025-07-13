package tj.builder.handlers;

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
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class LargeArchitectWorkbenchWorkableHandler extends MTETrait implements IWorkable {

    private final Supplier<ItemHandlerList> itemInputs;
    private final Supplier<ItemHandlerList> itemOutputs;
    private final Supplier<IEnergyContainer> energyInputs;
    private BooleanSupplier findInputs = this::findInputs;
    private final LongSupplier maxVoltage;
    private final IntSupplier parallel;
    private ItemStack catalyst;
    private ItemStack input;
    private ItemStack output;
    private boolean isWorking;
    private boolean isActive;
    private boolean isDistinct;
    private int progress = -1;
    private int maxProgress = 100;

    public LargeArchitectWorkbenchWorkableHandler(MetaTileEntity metaTileEntity, Supplier<ItemHandlerList> itemInputs, Supplier<ItemHandlerList> itemOutputs, Supplier<IEnergyContainer> energyInputs, LongSupplier maxVoltage, IntSupplier parallel) {
        super(metaTileEntity);
        this.itemInputs = itemInputs;
        this.itemOutputs = itemOutputs;
        this.energyInputs = energyInputs;
        this.maxVoltage = maxVoltage;
        this.parallel = parallel;
    }

    @Override
    public void update() {
        if (!this.isWorking) {
            if (this.isActive)
                this.setActive(false);
            return;
        }

        if (this.progress < 0 && !this.startRecipe()) {
            return;
        }

        if (this.progress < this.maxProgress) {
            this.progressRecipe();
        } else this.completeRecipe();
    }

    private boolean startRecipe() {
        if (this.findCatalyst() && this.findInputs.getAsBoolean()) {
            this.output = new ItemStack(Item.getByNameOrId("architecturecraft:shape"), this.input.getCount(), this.input.getMetadata());
            NBTTagCompound compound = this.catalyst.getTagCompound();
            compound.setString("BaseName", Item.REGISTRY.getNameForObject(this.input.getItem()).toString());
            this.output.setTagCompound(compound);
            this.setActive(true);
            return true;
        }
        return false;
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
        this.progress = -1;
        this.catalyst = null;
        this.input = null;
        this.output = null;
        if (this.isActive)
            this.setActive(false);
    }

    private boolean findCatalyst() {
        ItemHandlerList itemInputs = this.itemInputs.get();
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

    private boolean findInputs() {
        ItemHandlerList itemInputs = this.itemInputs.get();
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
            this.input.setCount(count);
            stack.shrink(reminder);
        }
        return this.input != null;
    }

    private boolean findInputsDistinct() {
        return false;
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
        this.findInputs = this.isDistinct ? this::findInputsDistinct : this::findInputs;
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
        return capability == GregtechTileCapabilities.CAPABILITY_WORKABLE ? GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this) : null;
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
    public boolean isWorkingEnabled() {
        return this.isWorking;
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        this.isWorking = isWorking;
        this.metaTileEntity.markDirty();
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        if (this.metaTileEntity.getWorld().isRemote) {
            this.writeCustomData(1, buffer -> buffer.writeBoolean(isActive));
            this.metaTileEntity.markDirty();
        }
    }
}
