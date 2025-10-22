package tj.builder.handlers;

import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;
import tj.util.ItemStackHelper;

import java.util.Collections;
import java.util.List;


public class ArchitectWorkbenchWorkableHandler extends AbstractWorkableHandler<ArchitectWorkbenchWorkableHandler> implements IItemFluidHandlerInfo {

    private ItemStack catalyst;
    private ItemStack input;
    private ItemStack output;

    public ArchitectWorkbenchWorkableHandler(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    @Override
    protected boolean startRecipe() {
        boolean canStart = false;
        IItemHandlerModifiable itemInputs = this.isDistinct ? this.inputBus.apply(this.lastInputIndex) : this.importItems.get();
        if (this.findCatalyst(itemInputs) && this.findInputs(itemInputs)) {
            this.output = new ItemStack(Item.getByNameOrId("architecturecraft:shape"), this.input.getCount());
            NBTTagCompound compound = this.catalyst.getTagCompound().copy();
            compound.setString("BaseName", Item.REGISTRY.getNameForObject(this.input.getItem()).toString());
            compound.setInteger("BaseData", this.input.getMetadata());
            this.output.setTagCompound(compound);
            this.maxProgress = this.calculateOverclock(30, 200, 2.8F);
            canStart = true;
        }
        if (++this.lastInputIndex == this.busCount)
            this.lastInputIndex = 0;
        return canStart;
    }

    @Override
    protected boolean completeRecipe() {
        if (ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), this.output, true).isEmpty()) {
            ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), this.output, false);
            if (this.metaTileEntity instanceof TJMultiblockDisplayBase)
                ((TJMultiblockDisplayBase) this.metaTileEntity).calculateMaintenance(this.maxProgress);
            this.catalyst = null;
            this.input = null;
            this.output = null;
            return true;
        }
        return false;
    }

    private boolean findCatalyst(IItemHandlerModifiable itemInputs) {
        for (int i = 0; i < itemInputs.getSlots(); i++) {
            ItemStack stack = itemInputs.getStackInSlot(i);
            if (this.isArchitectureStack(stack.getTagCompound())) {
                this.catalyst = stack;
                return true;
            }
        }
        return false;
    }

    private boolean findInputs(IItemHandlerModifiable itemInputs) {
        int availableParallels = this.parallel.getAsInt();
        int count = 0;
        for (int i = 0; i < itemInputs.getSlots() && availableParallels != 0; i++) {
            ItemStack stack = itemInputs.getStackInSlot(i);
            if (stack.isEmpty() || this.isArchitectureStack(stack.getTagCompound()))
                continue;
            if (this.input == null)
                this.input = stack.copy();
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

    private boolean isArchitectureStack(NBTTagCompound tagCompound) {
        return tagCompound != null && tagCompound.hasKey("Shape") && tagCompound.hasKey("BaseName") && tagCompound.hasKey("BaseData");
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = super.serializeNBT();
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
        super.deserializeNBT(compound);
        if (compound.hasKey("catalyst"))
            this.catalyst = new ItemStack(compound.getCompoundTag("catalyst"));
        if (compound.hasKey("input"))
            this.input = new ItemStack(compound.getCompoundTag("input"));
        if (compound.hasKey("output"))
            this.output = new ItemStack(compound.getCompoundTag("output"));
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability);
    }

    @Override
    public List<ItemStack> getItemInputs() {
        return this.input != null ? Collections.singletonList(this.input) : null;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return this.output != null ? Collections.singletonList(this.output) : null;
    }
}
