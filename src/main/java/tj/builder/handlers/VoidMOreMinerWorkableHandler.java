package tj.builder.handlers;

import gregicadditions.machines.multi.IMaintenance;
import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IHeatInfo;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;
import tj.util.ItemStackHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gregicadditions.GAMaterials.*;
import static gregicadditions.GAMaterials.UsedDrillingMud;
import static gregicadditions.recipes.categories.handlers.VoidMinerHandler.ORES_3;

public class VoidMOreMinerWorkableHandler extends AbstractWorkableHandler<IItemHandlerModifiable, IMultipleTankHandler> implements IHeatInfo, IItemFluidHandlerInfo {

    private static final int CONSUME_START = 100;
    private boolean overheat;
    private long maxTemperature;
    private long temperature;
    private double currentDrillingFluid = CONSUME_START;
    private final List<FluidStack> fluidInputsList = new ArrayList<>();
    private final List<FluidStack> fluidOutputsList = new ArrayList<>();
    private final List<ItemStack> oreOutputs = new ArrayList<>();

    public VoidMOreMinerWorkableHandler(MetaTileEntity metaTileEntity, Supplier<IItemHandlerModifiable> itemInputs, Supplier<IItemHandlerModifiable> itemOutputs, Supplier<IMultipleTankHandler> fluidImports, Supplier<IMultipleTankHandler> fluidExports,
                                        Supplier<IEnergyContainer> energyInputs, IntFunction<IItemHandlerModifiable> inputBus, LongSupplier maxVoltage, IntSupplier parallel) {
        super(metaTileEntity, itemInputs, itemOutputs, fluidImports, fluidExports, energyInputs, inputBus, maxVoltage, parallel);
    }

    @Override
    public void initialize(int tier) {
        super.initialize(tier);
        int startTier = tier - GTValues.ZPM;
        int multiplier = (startTier + 2) * 100;
        int multiplier2 = Math.min((startTier + 2) * 10, 40);
        int multiplier3 = startTier > 2 ? (int) Math.pow(2.8, startTier - 2) : 1;
        this.maxTemperature = multiplier * ((long) multiplier2 * multiplier3);
        this.maxProgress = 20;
        this.energyPerTick = this.maxVoltage.getAsLong();
    }

    @Override
    protected boolean startRecipe() {
        if (this.overheat) {
            if (this.temperature < 0) {
                this.overheat = false;
                this.temperature = 0;
            }
            return false;
        }

        boolean canMineOres = false;
        boolean hasEnoughPyrotheum = this.hasEnoughFluid(Pyrotheum.getFluid(this.getCurrentDrillingFluid()), this.currentDrillingFluid);
        boolean hasEnoughCryotheum = this.hasEnoughFluid(Cryotheum.getFluid(this.getCurrentDrillingFluid()), this.currentDrillingFluid);
        if (hasEnoughPyrotheum && hasEnoughCryotheum) {
            this.fluidInputsList.add(this.importFluids.get().drain(Pyrotheum.getFluid(this.getCurrentDrillingFluid()), true));
            this.fluidInputsList.add(this.importFluids.get().drain(Cryotheum.getFluid(this.getCurrentDrillingFluid()), true));
            canMineOres = true;
        } else if (hasEnoughPyrotheum) {
            this.fluidInputsList.add(this.importFluids.get().drain(Pyrotheum.getFluid(this.getCurrentDrillingFluid()), true));
            this.temperature += (long) (this.currentDrillingFluid / 100.0);
            this.currentDrillingFluid *= 1.02;
            canMineOres = true;
        } else if (hasEnoughCryotheum) {
            this.fluidInputsList.add(this.importFluids.get().drain(Cryotheum.getFluid(this.getCurrentDrillingFluid()), true));
            this.currentDrillingFluid /= 1.02;
            this.temperature -= (long) (this.currentDrillingFluid / 100.0);
        } else {
            return false; // prevent energy consumption if either fluids are not consumed
        }

        if (this.currentDrillingFluid < CONSUME_START) {
            this.currentDrillingFluid = CONSUME_START;
        }
        if (this.temperature > this.maxTemperature) {
            this.overheat = true;
            this.currentDrillingFluid = CONSUME_START;
            return false;
        }

        if (this.metaTileEntity instanceof IMaintenance)
            this.currentDrillingFluid += ((IMaintenance) this.metaTileEntity).getNumProblems();

        boolean hasEnoughDrillingMud = this.hasEnoughFluid(DrillingMud.getFluid(this.getCurrentDrillingFluid()), this.currentDrillingFluid);
        boolean canOutputUsedDrillingMud = this.canOutputFluid(UsedDrillingMud.getFluid(this.getCurrentDrillingFluid()), this.currentDrillingFluid);
        if (hasEnoughDrillingMud && canOutputUsedDrillingMud) {
            this.fluidInputsList.add(this.importFluids.get().drain(DrillingMud.getFluid(this.getCurrentDrillingFluid()), true));
            int outputAmount = this.exportFluids.get().fill(UsedDrillingMud.getFluid(this.getCurrentDrillingFluid()), true);
            this.fluidOutputsList.add(new FluidStack(UsedDrillingMud.getFluid(outputAmount), outputAmount));
            long nbOres = this.temperature / 1000;

            if (nbOres != 0 && canMineOres) {
                List<ItemStack> ores = getOres();
                Collections.shuffle(ores);
                this.oreOutputs.addAll(ores.stream()
                        .limit(10)
                        .peek(itemStack -> itemStack.setCount(this.metaTileEntity.getWorld().rand.nextInt((int) (nbOres * nbOres)) + 1))
                        .collect(Collectors.toCollection(ArrayList::new)));
            }
        } else return false;
        this.wasActiveAndNeedsUpdate = false;
        this.progress = 1;
        if (!this.isActive)
            this.setActive(true);
        return true;
    }

    @Override
    protected void stopRecipe() {
        if (this.temperature > 0)
            this.temperature--;
    }

    @Override
    protected boolean completeRecipe() {
        this.oreOutputs.forEach(ore -> ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), ore, false));
        this.fluidInputsList.clear();
        this.fluidOutputsList.clear();
        this.oreOutputs.clear();
        if (this.metaTileEntity instanceof TJMultiblockDisplayBase)
            ((TJMultiblockDisplayBase) this.metaTileEntity).calculateMaintenance(this.maxProgress);
        return true;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagList oreList = new NBTTagList(),
                fluidInputList = new NBTTagList(),
                fluidOutputList = new NBTTagList();
        NBTTagCompound compound = super.serializeNBT();
        for (ItemStack item : this.oreOutputs)
            oreList.appendTag(item.serializeNBT());
        for (FluidStack fluid : this.fluidInputsList)
            fluidInputList.appendTag(fluid.writeToNBT(new NBTTagCompound()));
        for (FluidStack fluid : this.fluidOutputsList)
            fluidOutputList.appendTag(fluid.writeToNBT(new NBTTagCompound()));
        compound.setLong("temperature", this.temperature);
        compound.setDouble("currentDrillingFluid", this.currentDrillingFluid);
        compound.setBoolean("overheat", this.overheat);
        compound.setTag("oreList", oreList);
        compound.setTag("fluidInputList", fluidOutputList);
        compound.setTag("fluidOutputList", fluidOutputList);
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        super.deserializeNBT(compound);
        NBTTagList oreList = compound.getTagList("oreList", 10),
                fluidInputList = compound.getTagList("fluidInputList", 10),
                fluidOutputList = compound.getTagList("fluidOutputList", 10);
        for (int i = 0; i < oreList.tagCount(); i++)
            this.oreOutputs.add(new ItemStack(oreList.getCompoundTagAt(i)));
        for (int i = 0; i < fluidInputList.tagCount(); i++)
            this.fluidInputsList.add(FluidStack.loadFluidStackFromNBT(fluidInputList.getCompoundTagAt(i)));
        for (int i = 0; i < fluidOutputList.tagCount(); i++)
            this.fluidOutputsList.add(FluidStack.loadFluidStackFromNBT(fluidOutputList.getCompoundTagAt(i)));
        this.temperature = compound.getLong("temperature");
        this.currentDrillingFluid = compound.getDouble("currentDrillingFluid");
        this.overheat = compound.getBoolean("overheat");
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_HEAT)
            return TJCapabilities.CAPABILITY_HEAT.cast(this);
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability);
    }

    public int getCurrentDrillingFluid() {
        return (int) this.currentDrillingFluid;
    }

    public boolean isOverheat() {
        return this.overheat;
    }

    @Override
    public long heat() {
        return this.temperature;
    }

    @Override
    public long maxHeat() {
        return this.maxTemperature;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return this.oreOutputs;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return this.fluidInputsList;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return this.fluidOutputsList;
    }

    private static List<ItemStack> getOres() {
        return ORES_3;
    }
}
