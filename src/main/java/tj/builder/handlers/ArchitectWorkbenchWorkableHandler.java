package tj.builder.handlers;

import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.capability.impl.AbstractWorkableHandler;
import tj.util.ItemStackHelper;

import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class ArchitectWorkbenchWorkableHandler extends AbstractWorkableHandler {

    private ItemStack catalyst;
    private ItemStack input;
    private ItemStack output;

    public ArchitectWorkbenchWorkableHandler(MetaTileEntity metaTileEntity, Supplier<IItemHandlerModifiable> itemInputs, Supplier<IItemHandlerModifiable> itemOutputs, Supplier<IEnergyContainer> energyInputs, IntFunction<IItemHandlerModifiable> inputBus, LongSupplier maxVoltage, IntSupplier parallel) {
        super(metaTileEntity, itemInputs, itemOutputs, energyInputs, inputBus, maxVoltage, parallel);
    }

    @Override
    protected boolean startRecipe() {
        boolean canStart = false;
        IItemHandlerModifiable itemInputs = this.isDistinct ? this.inputBus.apply(this.lastInputIndex) : this.itemInputs.get();
        if (this.findCatalyst(itemInputs) && this.findInputs(itemInputs)) {
            this.output = new ItemStack(Item.getByNameOrId("architecturecraft:shape"), this.input.getCount());
            NBTTagCompound compound = this.catalyst.getTagCompound().copy();
            compound.setString("BaseName", Item.REGISTRY.getNameForObject(this.input.getItem()).toString());
            compound.setInteger("BaseData", this.input.getMetadata());
            this.output.setTagCompound(compound);
            this.maxProgress = this.calculateOverclock(30, 200, 2.8F);
            this.wasActiveAndNeedsUpdate = false;
            canStart = true;
            if (!this.isActive)
                this.setActive(true);
        }
        if (++this.lastInputIndex == this.busCount)
            this.lastInputIndex = 0;
        return canStart;
    }

    @Override
    protected boolean completeRecipe() {
        if (ItemStackHelper.insertIntoItemHandler(this.itemOutputs.get(), this.output, true).isEmpty()) {
            ItemStackHelper.insertIntoItemHandler(this.itemOutputs.get(), this.output, false);
            this.catalyst = null;
            this.input = null;
            this.output = null;
            return true;
        } else if (this.progress > 1)
            this.progress--;
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
}
