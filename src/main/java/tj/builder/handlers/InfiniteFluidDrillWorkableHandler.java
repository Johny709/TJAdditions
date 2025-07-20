package tj.builder.handlers;

import gregicadditions.GAValues;
import gregicadditions.worldgen.PumpjackHandler;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static gregicadditions.GAMaterials.DrillingMud;
import static gregicadditions.GAMaterials.UsedDrillingMud;

public class InfiniteFluidDrillWorkableHandler extends AbstractWorkableHandler<IItemHandlerModifiable, IMultipleTankHandler> implements IItemFluidHandlerInfo {

    private Fluid veinFluid;
    private FluidStack veinFluidStack;
    private int outputVeinFluidAmount;
    private int drillingMudAmount;
    private final List<FluidStack> fluidInputsList = new ArrayList<>();
    private final List<FluidStack> fluidOutputsList = new ArrayList<>();

    public InfiniteFluidDrillWorkableHandler(MetaTileEntity metaTileEntity, Supplier<IMultipleTankHandler> importFluids, Supplier<IMultipleTankHandler> exportFluids, Supplier<IEnergyContainer> energyInputs, LongSupplier maxVoltage) {
        super(metaTileEntity, null, null, importFluids, exportFluids, energyInputs, null, maxVoltage, null);
    }

    @Override
    public void initialize(int tier) {
        super.initialize(tier);
        this.outputVeinFluidAmount = (int) Math.min(Integer.MAX_VALUE, Math.pow(4, (tier - GAValues.EV)) * 4000);
        this.drillingMudAmount = (int) Math.pow(4, (tier - GAValues.EV)) * 10;

        World world = this.metaTileEntity.getWorld();
        BlockPos pos = this.metaTileEntity.getPos();
        this.energyPerTick = this.maxVoltage.getAsLong();
        this.veinFluid = PumpjackHandler.getFluid(world, world.getChunk(this.metaTileEntity.getPos()).x, world.getChunk(pos).z);
        this.maxProgress = 20;
        if (this.veinFluid != null)
            this.veinFluidStack = new FluidStack(this.veinFluid, this.outputVeinFluidAmount);
    }

    @Override
    protected boolean startRecipe() {
        FluidStack drillingMud = DrillingMud.getFluid(this.drillingMudAmount);
        FluidStack usedDrillingMud = UsedDrillingMud.getFluid(this.drillingMudAmount);
        if (this.hasEnoughFluid(drillingMud, this.drillingMudAmount) && this.canOutputFluid(usedDrillingMud, this.drillingMudAmount)) {
            this.fluidInputsList.add(this.importFluids.get().drain(DrillingMud.getFluid(this.drillingMudAmount), true));
            int outputAmount = this.exportFluids.get().fill(UsedDrillingMud.getFluid(this.drillingMudAmount), true);
            this.fluidOutputsList.add(new FluidStack(UsedDrillingMud.getFluid(outputAmount), outputAmount));
            this.fluidOutputsList.add(this.veinFluidStack);
            this.wasActiveAndNeedsUpdate = false;
            this.progress = 0;
            if (!this.isActive)
                this.setActive(true);
            return true;
        }
        return false;
    }

    @Override
    protected boolean completeRecipe() {
        if (this.canOutputFluid(this.veinFluidStack, this.outputVeinFluidAmount)) {
            this.exportFluids.get().fill(this.veinFluidStack, true);
            this.fluidInputsList.clear();
            this.fluidOutputsList.clear();
            if (this.metaTileEntity instanceof TJMultiblockDisplayBase)
                ((TJMultiblockDisplayBase) this.metaTileEntity).calculateMaintenance(this.maxProgress);
            return true;
        }
        return false;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagList fluidInputsList = new NBTTagList(),
                fluidOutputsList = new NBTTagList();
        NBTTagCompound compound = super.serializeNBT();
        for (FluidStack fluid : this.fluidInputsList)
            fluidInputsList.appendTag(fluid.writeToNBT(new NBTTagCompound()));
        for (FluidStack fluid : this.fluidOutputsList)
            fluidOutputsList.appendTag(fluid.writeToNBT(new NBTTagCompound()));
        compound.setTag("fluidInputsList", fluidInputsList);
        compound.setTag("fluidOutputsList", fluidOutputsList);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        NBTTagList fluidInputsList = compound.getTagList("fluidInputsList", 10),
            fluidOutputsList = compound.getTagList("fluidOutputsList", 10);
        for (int i = 0; i < fluidInputsList.tagCount(); i++)
            this.fluidInputsList.add(FluidStack.loadFluidStackFromNBT(fluidInputsList.getCompoundTagAt(i)));
        for (int i = 0; i < fluidOutputsList.tagCount(); i++)
            this.fluidOutputsList.add(FluidStack.loadFluidStackFromNBT(fluidOutputsList.getCompoundTagAt(i)));
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability);
    }

    public int getDrillingMudAmount() {
        return this.drillingMudAmount;
    }

    public int getOutputVeinFluidAmount() {
        return this.outputVeinFluidAmount;
    }

    public Fluid getVeinFluid() {
        return this.veinFluid;
    }

    public FluidStack getVeinFluidStack() {
        return this.veinFluidStack;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return this.fluidInputsList;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return this.fluidOutputsList;
    }
}
