package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IFluidHandlerInfo;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.textures.TJTextures;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.worldgen.PumpjackHandler;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

import static gregicadditions.GAMaterials.*;
import static net.minecraft.util.text.TextFormatting.*;

public class MetaTileEntityInfiniteFluidDrill extends TJMultiblockDisplayBase implements IWorkable, IFluidHandlerInfo {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS,
            MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private Fluid veinFluid;
    private boolean isActive;
    private long maxVoltage;
    private int tier;
    private int outputVeinFluidAmount;
    private int drillingMudAmount;
    private int progress;
    private final int maxProgress = 20;
    private IMultipleTankHandler outputFluid;
    private IMultipleTankHandler inputFluid;
    private EnergyContainerList energyContainer;
    private final List<FluidStack> fluidInputs = new ArrayList<>();
    private final List<FluidStack> fluidOutputs = new ArrayList<>();

    public MetaTileEntityInfiniteFluidDrill(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityInfiniteFluidDrill(metaTileEntityId);
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        int fluidInputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();
        int fluidOutputsCount = abilities.getOrDefault(MultiblockAbility.EXPORT_FLUIDS, Collections.emptyList()).size();

        return fluidInputsCount >= 1 &&
                fluidOutputsCount >= 1 &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY) &&
                abilities.containsKey(GregicAdditionsCapabilities.MAINTENANCE_HATCH);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        int drillingMud = (int) Math.pow(4, (9 - GAValues.EV)) * 10;
        int outputFluid = (int) Math.pow(4, (9 - GAValues.EV)) * 4000;
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.1"));
        tooltip.add(I18n.format("tj.multiblock.drilling_rig.voltage", GAValues.VN[9], GAValues.VN[14]));
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.void.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.4", outputFluid, GAValues.VN[9]));
        tooltip.add(I18n.format("tj.multiblock.drilling_rig.tooltip.drilling_mud", drillingMud, drillingMud));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.1"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (!isStructureFormed())
            return;

        if (veinFluid == null) {
            textList.add(new TextComponentTranslation("gtadditions.multiblock.drilling_rig.no_fluid").setStyle(new Style().setColor(RED)));
            return;
        }

        String drillingMudName = DrillingMud.getFluid(drillingMudAmount).getLocalizedName();
        String usedDrillingMudName= UsedDrillingMud.getFluid(drillingMudAmount).getLocalizedName();
        String veinName = veinFluid.getName();
        int currentProgress = (int) Math.floor(progress / (maxProgress * 1.0) * 100);
        boolean hasEnoughEnergy = hasEnoughEnergy(maxVoltage);
        boolean hasEnoughDrillingMud = hasEnoughFluid(drillingMudAmount);
        boolean canFillDrillingMudOutput = canOutputUsedDrillingMud(drillingMudAmount);
        boolean canFillFluidOutput = canOutputVeinFluid(outputVeinFluidAmount);

        ITextComponent energyText = hasEnoughEnergy ? new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, GAValues.VN[tier])
                : new TextComponentTranslation("gregtech.multiblock.not_enough_energy");
        ITextComponent drillingMudInputText = hasEnoughDrillingMud ? new TextComponentTranslation("machine.universal.fluid.input.sec", drillingMudName, drillingMudAmount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid", drillingMudName, drillingMudAmount);
        ITextComponent drillingMudOutputText = canFillDrillingMudOutput ? new TextComponentTranslation("machine.universal.fluid.output.sec", usedDrillingMudName, drillingMudAmount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", usedDrillingMudName, drillingMudAmount);
        ITextComponent fluidOutputText = canFillFluidOutput ? new TextComponentTranslation("gtadditions.multiblock.drilling_rig.rig_production", outputVeinFluidAmount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", veinName, outputVeinFluidAmount);
        ITextComponent currentFluidVeinText = new TextComponentTranslation("gtadditions.multiblock.drilling_rig.fluid", veinName);
        ITextComponent isWorkingText = !isWorkingEnabled ? new TextComponentTranslation("gregtech.multiblock.work_paused")
                : !isActive ? new TextComponentTranslation("gregtech.multiblock.idling")
                : new TextComponentTranslation("gregtech.multiblock.running");

        energyText.getStyle().setColor(hasEnoughEnergy ? WHITE : RED);
        drillingMudInputText.getStyle().setColor(hasEnoughDrillingMud ? WHITE : RED);
        drillingMudOutputText.getStyle().setColor(canFillDrillingMudOutput ? WHITE : RED);
        fluidOutputText.getStyle().setColor(canFillFluidOutput ? WHITE : RED);
        isWorkingText.getStyle().setColor(!isWorkingEnabled ? YELLOW : !isActive ? WHITE : GREEN);

        textList.addAll(Arrays.asList(energyText, drillingMudInputText, drillingMudOutputText, fluidOutputText, currentFluidVeinText, isWorkingText));
        if (isActive)
            textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        veinFluid = PumpjackHandler.getFluid(getWorld(), getWorld().getChunk(getPos()).x, getWorld().getChunk(getPos()).z);
        energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        inputFluid = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        outputFluid = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));

        int motorTier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int pumpTier = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        tier = Math.min(motorTier, pumpTier);

        maxVoltage = (long) (Math.pow(4, tier) * 8);
        outputVeinFluidAmount = (int) Math.pow(4, (tier - GAValues.EV)) * 4000;
        drillingMudAmount = (int) Math.pow(4, (tier - GAValues.EV)) * 10;
    }


    @Override
    protected void updateFormedValid() {
        if (!isWorkingEnabled || tier < GAValues.UHV || getNumProblems() >= 6) {
            if (isActive)
                setActive(false);
            return;
        }

        calculateMaintenance(1);
        if (progress > 0 && !isActive)
            setActive(true);

        if (progress >= maxProgress) {
            if (canOutputVeinFluid(outputVeinFluidAmount)) {
                outputFluid.fill(new FluidStack(veinFluid, outputVeinFluidAmount), true);
                fluidInputs.clear();
                fluidOutputs.clear();
                progress = 0;
                if (isActive)
                    setActive(false);
            } else {
                progress--;
            }
        }

        if (hasEnoughEnergy(maxVoltage)) {
            if (progress <= 0) {
                if (hasEnoughFluid(drillingMudAmount) && canOutputUsedDrillingMud(drillingMudAmount)) {
                    fluidInputs.add(inputFluid.drain(DrillingMud.getFluid(drillingMudAmount), true));
                    int outputAmount = outputFluid.fill(UsedDrillingMud.getFluid(drillingMudAmount), true);
                    fluidOutputs.add(new FluidStack(UsedDrillingMud.getFluid(outputAmount), outputAmount));
                    fluidOutputs.add(new FluidStack(veinFluid, outputVeinFluidAmount));
                    progress = 1;
                    if (!isActive)
                        setActive(true);
                } else {
                    return; // prevent energy consumption if fluid is not consumed
                }
            } else {
                progress++;
            }
            energyContainer.removeEnergy(maxVoltage);
        } else {
            if (progress > 1)
                progress--;
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("CF~FC", "CF~FC", "CCCCC", "~XXX~", "~~C~~", "~~C~~", "~~C~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("F~~~F", "F~~~F", "CCMCC", "X###X", "~C#C~", "~C#C~", "~C#C~", "~FCF~", "~FFF~", "~FFF~", "~~F~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("~~~~~", "~~~~~", "CMPMC", "X#T#X", "C#T#C", "C#T#C", "C#T#C", "~CCC~", "~FCF~", "~FFF~", "~FFF~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("F~~~F", "F~~~F", "CCMCC", "X###X", "~C#C~", "~C#C~" ,"~C#C~", "~FCF~", "~FFF~", "~FFF~", "~~F~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CF~FC", "CF~FC", "CCCCC", "~XSX~", "~~C~~", "~~C~~", "~~C~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('X', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Seaborgium).getDefaultState()))
                .where('T', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('M', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('P', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.SEABORGIUM_CASING);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return TJTextures.SEABORGIUM;
    }

    private boolean hasEnoughEnergy(long amount) {
        return energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = inputFluid.drain(DrillingMud.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean canOutputVeinFluid(int amount) {
        int fluidStack = outputFluid.fill(new FluidStack(veinFluid, amount), false);
        return fluidStack == amount;
    }

    private boolean canOutputUsedDrillingMud(int amount) {
        int fluidStack = outputFluid.fill(UsedDrillingMud.getFluid(amount), false);
        return fluidStack == amount;
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        markDirty();
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            getHolder().scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_FLUID_HANDLING.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Progress", progress);
        data.setBoolean("Active", isActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        progress = data.getInteger("Progress");
        isActive = data.getBoolean("Active");
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return maxProgress;
    }
}
