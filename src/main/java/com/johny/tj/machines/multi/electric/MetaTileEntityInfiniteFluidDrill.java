package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.worldgen.PumpjackHandler;
import gregtech.api.capability.IMultipleTankHandler;
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
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gregicadditions.GAMaterials.DrillingMud;
import static gregicadditions.GAMaterials.UsedDrillingMud;
import static gregtech.api.unification.material.Materials.Tritanium;
import static gregtech.api.util.GTFluidUtils.simulateFluidStackMerge;

public class MetaTileEntityInfiniteFluidDrill extends TJMultiblockDisplayBase {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS,
            MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private Fluid veinFluid;
    private boolean isActive;
    private long maxVoltage;
    private int tier;
    private int outputVeinFluidAmount;
    private int drillingMudAmount;
    private IMultipleTankHandler outputFluid;
    private IMultipleTankHandler inputFluid;
    private EnergyContainerList energyContainer;

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
        int fluidOutputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();

        return fluidInputsCount >= 1 &&
                fluidOutputsCount >= 1 &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        int drillingMud = (int) Math.pow(4, (9 - GAValues.EV)) * 20;
        int outputFluid = (int) Math.pow(4, (9 - GAValues.EV)) * 1000;
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.1"));
        tooltip.add(I18n.format("tj.multiblock.drilling_rig.voltage", GAValues.VN[9], GAValues.VN[14]));
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.void.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.4", outputFluid, GAValues.VN[9]));
        tooltip.add(I18n.format("tj.multiblock.drilling_rig.tooltip.drilling_mud", drillingMud, drillingMud));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (!isStructureFormed())
            return;

        if (veinFluid == null) {
            textList.add(new TextComponentTranslation("gtadditions.multiblock.drilling_rig.no_fluid").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }

        textList.add(hasEnoughEnergy(maxVoltage) ? new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage)
                : new TextComponentTranslation("gregtech.multiblock.not_enough_energy")
                .setStyle(new Style().setColor(TextFormatting.RED)));
        textList.add(hasEnoughFluid(drillingMudAmount) ? new TextComponentTranslation("tj.multiblock.drilling_rig_drilling_mud.input", drillingMudAmount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid", DrillingMud.getFluid(drillingMudAmount).getLocalizedName()));
        textList.add(canFillFluidExport(outputVeinFluidAmount, drillingMudAmount) ? new TextComponentTranslation("gtadditions.multiblock.drilling_rig.rig_production", outputVeinFluidAmount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", DrillingMud.fluid.getName()));
        textList.add(canFillFluidExport(outputVeinFluidAmount, drillingMudAmount) ? new TextComponentTranslation("tj.multiblock.drilling_rig_drilling_mud.output", drillingMudAmount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", veinFluid.getName()));
        textList.add(new TextComponentTranslation("gtadditions.multiblock.drilling_rig.fluid", veinFluid.getName()));
        textList.add(isWorkingEnabled ? (isActive ? new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN))
                : new TextComponentTranslation("gregtech.multiblock.idling"))
                : new TextComponentTranslation("gregtech.multiblock.work_paused").setStyle(new Style().setColor(TextFormatting.YELLOW)));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        veinFluid = PumpjackHandler.getFluid(getWorld(), getWorld().getChunk(getPos()).x, getWorld().getChunk(getPos()).z);
        energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        inputFluid = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        outputFluid = new FluidTankList(false, getAbilities(MultiblockAbility.EXPORT_FLUIDS));

        int motorTier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int pumpTier = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        tier = Math.min(motorTier, pumpTier);

        maxVoltage = (long) (Math.pow(4, tier) * 8);
        outputVeinFluidAmount = (int) Math.pow(4, (tier - GAValues.EV)) * 1000;
        drillingMudAmount = (int) Math.pow(4, (tier - GAValues.EV)) * 10;
    }


    @Override
    protected void updateFormedValid() {
        if (!isWorkingEnabled && tier >= GAValues.UHV) {
            if (isActive)
                setActive(false);
            return;
        }

        if (hasEnoughEnergy(maxVoltage) && canFillFluidExport(outputVeinFluidAmount, drillingMudAmount) && hasEnoughFluid(drillingMudAmount)) {
            if (!isActive) {
                setActive(true);
            }
            energyContainer.removeEnergy(maxVoltage);
            if (getOffsetTimer() % 20 == 0) {
                inputFluid.drain(DrillingMud.getFluid(drillingMudAmount), true);
                outputFluid.fill(new FluidStack(veinFluid, outputVeinFluidAmount), true);
                outputFluid.fill(UsedDrillingMud.getFluid(drillingMudAmount), true);
            }
        } else {
            if (isActive) {
                setActive(false);
            }
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
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Tritanium).getDefaultState()))
                .where('T', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('M', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('P', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.TRITANIUM);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.TRITANIUM_CASING;
    }

    private boolean hasEnoughEnergy(long amount) {
        return energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = inputFluid.drain(DrillingMud.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean canFillFluidExport(int veinAmount, int drillingMudAmount) {
        if (veinFluid == null) {
            return false;
        }
        FluidStack veinStack = new FluidStack(veinFluid, veinAmount);
        FluidStack drillMudStack = UsedDrillingMud.getFluid(drillingMudAmount);
        return simulateFluidStackMerge(Arrays.asList(veinStack, drillMudStack), outputFluid);
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
}
