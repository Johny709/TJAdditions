package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkEvent;
import com.johny.tj.capability.LinkPosInterDim;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.items.TJMetaItems;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.item.metal.MetalCasing2;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

import static gregicadditions.GAMaterials.Talonite;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.unification.material.Materials.Nitrogen;
import static gregtech.api.unification.material.Materials.RedSteel;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;

public class MetaTileEntityLargeWirelessEnergyEmitter extends TJMultiblockDisplayBase implements LinkPosInterDim<BlockPos>, LinkEvent, IParallelController {

    protected TransferType transferType;
    private long energyPerTick;
    private long totalEnergyPerTick;
    private int fluidConsumption;
    private boolean isActive = false;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer inputEnergyContainer;
    private int tier;
    private BlockPos[] entityLinkBlockPos;
    private int[] entityLinkWorld;
    private int[] entityEnergyAmps;
    private int linkedWorldsCount;
    private final int pageSize = 4;
    private int pageIndex;
    private NBTTagCompound linkData;
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_FLUIDS, INPUT_ENERGY, OUTPUT_ENERGY, MAINTENANCE_HATCH};

    public MetaTileEntityLargeWirelessEnergyEmitter(ResourceLocation metaTileEntityId, TransferType transferType) {
        super(metaTileEntityId);
        this.transferType = transferType;
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWirelessEnergyEmitter(metaTileEntityId, transferType);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_wireless_energy_emitter.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            Style style = new Style().setColor(TextFormatting.GREEN);
            textList.add(hasEnoughEnergy(totalEnergyPerTick) ? new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", getMaxEUt())
                    .appendText("\n")
                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.sum", totalEnergyPerTick))
                    .appendText("\n")
                    .appendSibling(new TextComponentTranslation("machine.universal.tooltip.voltage_tier")
                            .appendText(" ")
                            .appendSibling(new TextComponentString(String.valueOf(getVoltageTier())).setStyle(style))
                            .appendText(" (")
                            .appendSibling(new TextComponentString(String.valueOf(GAValues.VN[GTUtility.getGATierByVoltage(getVoltageTier())])).setStyle(style))
                            .appendText(")"))
                    : new TextComponentTranslation("gregtech.multiblock.not_enough_energy")
                    .setStyle(new Style().setColor(TextFormatting.RED)));
            textList.add(isWorkingEnabled ? (isActive ? new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN))
                    : new TextComponentTranslation("gregtech.multiblock.idling"))
                    : new TextComponentTranslation("gregtech.multiblock.work_paused").setStyle(new Style().setColor(TextFormatting.YELLOW)));
            textList.add(hasEnoughFluid(fluidConsumption) || fluidConsumption == 0 ? new TextComponentTranslation("tj.multiblock.enough_fluid")
                    .appendText(" ")
                    .appendSibling(new TextComponentTranslation(Nitrogen.getPlasma(fluidConsumption).getUnlocalizedName())
                            .setStyle(new Style().setColor(TextFormatting.DARK_AQUA)))
                    .appendText(" ")
                    .appendText("§3" + fluidConsumption)
                    .appendText("§7 L/t")
                     : new TextComponentTranslation("tj.multiblock.not_enough_fluid").setStyle(new Style().setColor(TextFormatting.RED)));
        } else {
            super.addDisplayText(textList);
        }
    }

    private void addDisplayLinkedEntitiesText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.linked")
                .setStyle(new Style().setBold(true).setUnderlined(true)));
        textList.add(new TextComponentString(":")
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[<]"), "leftPage"))
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[>]"), "rightPage")));

        for (int i = pageIndex, linkedEntitiesPos = i + 1; i < pageIndex + pageSize; i++, linkedEntitiesPos++) {
            if (i < entityLinkBlockPos.length && entityLinkBlockPos[i] != null) {

                WorldServer world = DimensionManager.getWorld(entityLinkWorld[i]);
                TileEntity getTileEntity = world.getTileEntity(entityLinkBlockPos[i]);
                MetaTileEntity getMetaTileEntity = BlockMachine.getMetaTileEntity(world, entityLinkBlockPos[i]);
                boolean isTileEntity = getTileEntity != null;
                boolean isMetaTileEntity = getMetaTileEntity != null;
                IEnergyStorage RFContainer = isTileEntity ? getTileEntity.getCapability(ENERGY, null) : null;
                long RFStored = RFContainer != null ? RFContainer.getEnergyStored() : 0;
                long RFCapacity = RFContainer != null ? RFContainer.getMaxEnergyStored() : 0;
                IEnergyContainer EUContainer = isMetaTileEntity ? getMetaTileEntity.getCapability(CAPABILITY_ENERGY_CONTAINER, null) : null;
                long EUStored = EUContainer != null ? EUContainer.getEnergyStored() : 0;
                long EUCapacity = EUContainer != null ? EUContainer.getEnergyCapacity() : 0;

                textList.add(new TextComponentString(": [" + linkedEntitiesPos + "] ")
                        .appendSibling(new TextComponentTranslation(isMetaTileEntity ? getMetaTileEntity.getMetaFullName()
                                : isTileEntity ? getTileEntity.getBlockType().getTranslationKey() + ".name"
                                : "machine.universal.linked.entity.null")).setStyle(new Style()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(isMetaTileEntity ? getMetaTileEntity.getMetaFullName()
                                        : isTileEntity ? getTileEntity.getBlockType().getTranslationKey() + ".name"
                                        : "machine.universal.linked.entity.null")
                                        .appendText("\n")
                                        .appendSibling(new TextComponentTranslation("machine.universal.linked.dimension", world.provider.getDimensionType().getName(), world.provider.getDimensionType().getId()))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentTranslation("machine.universal.energy.stored", isMetaTileEntity ? EUStored : RFStored, isMetaTileEntity ? EUCapacity : RFCapacity))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("X: ").appendSibling(new TextComponentTranslation(entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(entityLinkBlockPos[i].getX()))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("Y: ").appendSibling(new TextComponentTranslation(entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(entityLinkBlockPos[i].getY()))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("Z: ").appendSibling(new TextComponentTranslation(entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(entityLinkBlockPos[i].getZ()))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true))))))
                        .appendText(" ")
                        .appendSibling(new TextComponentTranslation("machine.universal.energy.amps", entityEnergyAmps[i])
                                .appendText(" ")
                                .appendSibling(withButton(new TextComponentString("[+]"), "increment" + i))
                                .appendText(" ")
                                .appendSibling(withButton(new TextComponentString("[-]"), "decrement" + i)))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove" + i)));

            }
        }
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        WidgetGroup widgetLinkedEntitiesGroup = new WidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.linked_entities_display", TJMetaItems.LINKING_DEVICE.getStackForm(), linkedEntitiesDisplayTab(widget -> {widgetLinkedEntitiesGroup.addWidget(widget); return widgetLinkedEntitiesGroup;})));
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        return widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    private AbstractWidgetGroup linkedEntitiesDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addDisplayLinkedEntitiesText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
    }

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        Arrays.fill(entityLinkBlockPos, null);
        Arrays.fill(entityLinkWorld, Integer.MIN_VALUE);
        Arrays.fill(entityEnergyAmps, 0);
        linkData = null;
        updateTotalEnergyPerTick();
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        if (componentData.equals("leftPage") && pageIndex > 0) {
            pageIndex -= pageSize;
            return;
        }
        if (componentData.equals("rightPage") && pageIndex < entityLinkBlockPos.length - pageSize) {
            pageIndex += pageSize;
            return;
        }
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (componentData.equals("increment" + i)) {
                entityEnergyAmps[i] = MathHelper.clamp(entityEnergyAmps[i] + 1, 0, 256);
                updateTotalEnergyPerTick();
                break;
            }
            if (componentData.equals("decrement" + i)) {
                entityEnergyAmps[i] = MathHelper.clamp(entityEnergyAmps[i] - 1, 0, 256);
                updateTotalEnergyPerTick();
                break;
            }
            if (componentData.equals("remove" + i)) {
                int index = linkData.getInteger("I");
                linkData.setInteger("I", index + 1);
                entityLinkBlockPos[i] = null;
                entityLinkWorld[i] = Integer.MIN_VALUE;
                entityEnergyAmps[i] = 0;
                updateTotalEnergyPerTick();
                break;
            }
        }
    }

    @Override
    protected void updateFormedValid() {
        if (!isWorkingEnabled || !hasEnoughEnergy(totalEnergyPerTick) || getNumProblems() >= 6) {
            if (isActive)
                setActive(false);
            return;
        }
        if (!isActive)
            setActive(true);
        calculateMaintenance(1);
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (entityLinkBlockPos[i] == null)
                continue;
            if (getWorld().provider.getDimension() != entityLinkWorld[i]) {
                if (!hasEnoughFluid(fluidConsumption))
                    continue;
                int fluidToConsume = fluidConsumption / linkedWorldsCount;
                importFluidHandler.drain(Nitrogen.getPlasma(fluidToConsume), true);
            }
            WorldServer world = DimensionManager.getWorld(entityLinkWorld[i]);
            Chunk chunk = world.getChunk(entityLinkBlockPos[i]);
            if (!chunk.isLoaded())
                chunk.onLoad();
            TileEntity tileEntity = world.getTileEntity(entityLinkBlockPos[i]);
            MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, entityLinkBlockPos[i]);
            long energyToAdd = energyPerTick * entityEnergyAmps[i];
            if (tileEntity != null) {
                IEnergyStorage RFContainer = tileEntity.getCapability(ENERGY, null);
                transferRF((int) energyToAdd, RFContainer);
            }
            if (metaTileEntity != null) {
                IEnergyContainer EUContainer = metaTileEntity.getCapability(CAPABILITY_ENERGY_CONTAINER, null);
                transferEU(energyToAdd, EUContainer);
            }
        }
    }

    protected void transferRF(int energyToAdd, IEnergyStorage RFContainer) {
        if (RFContainer == null)
            return;
        int energyRemainingToFill = RFContainer.getMaxEnergyStored() - RFContainer.getEnergyStored();
        if (RFContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            int energyInserted = RFContainer.receiveEnergy(Math.min(Integer.MAX_VALUE, energyToAdd * 4), false);
            inputEnergyContainer.removeEnergy(energyInserted / 4);
        }
    }

    protected void transferEU(long energyToAdd, IEnergyContainer EUContainer) {
        if (EUContainer == null)
            return;
        long energyRemainingToFill = EUContainer.getEnergyCapacity() - EUContainer.getEnergyStored();
        if (EUContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            long energyInserted = EUContainer.addEnergy(Math.min(energyToAdd, energyRemainingToFill));
            inputEnergyContainer.removeEnergy(energyInserted);
        }
    }

    private void updateTotalEnergyPerTick() {
        int amps = Arrays.stream(entityEnergyAmps).sum();
        totalEnergyPerTick = (long) (Math.pow(4, tier) * 8) * amps;
    }

    protected boolean hasEnoughEnergy(long amount) {
        return inputEnergyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(Nitrogen.getPlasma(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return transferType == null ? null : FactoryBlockPattern.start()
                .aisle("~HHH~", "~HHH~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "HHFHH", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "HFIFH", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("HHHHH", "HHFHH", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("~HHH~", "~HSH~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState(transferType)))
                .where('H', statePredicate(getCasingState(transferType)).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', statePredicate(getFrameState(transferType)))
                .where('I', frameworkPredicate().or(frameworkPredicate2()))
                .where('~', tile -> true)
                .build();
    }

    public IBlockState getCasingState(TransferType transferType) {
        if (transferType == TransferType.INPUT)
            return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TALONITE);
        else
            return GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.RED_STEEL);
    }

    public IBlockState getFrameState(TransferType transferType) {
        if (transferType == TransferType.INPUT)
            return MetaBlocks.FRAMES.get(Talonite).getDefaultState();
        else
            return MetaBlocks.FRAMES.get(RedSteel).getDefaultState();
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        inputEnergyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        int framework = context.getOrDefault("framework", GAMultiblockCasing.CasingType.TIERED_HULL_LV).getTier();
        int framework2 = context.getOrDefault("framework2", GAMultiblockCasing2.CasingType.TIERED_HULL_UHV.getTier());
        tier = Math.min(framework, framework2);
        entityLinkBlockPos = entityLinkBlockPos != null ? Arrays.copyOf(entityLinkBlockPos, tier) : new BlockPos[tier];
        entityEnergyAmps = entityEnergyAmps != null ? Arrays.copyOf(entityEnergyAmps, tier) : new int[tier];
        if (entityLinkWorld != null) {
            entityLinkWorld = Arrays.copyOf(entityLinkWorld, tier);
        } else {
            entityLinkWorld = new int[tier];
            Arrays.fill(entityLinkWorld, getWorld().provider.getDimension());
        }
        energyPerTick = (long) (Math.pow(4, tier) * 8);
        updateTotalEnergyPerTick();
        int dimensionID = getWorld().provider.getDimension();
        linkedWorldsCount = (int) Arrays.stream(entityLinkWorld).filter(id -> id != dimensionID && id != Integer.MIN_VALUE).count();
        fluidConsumption = 10 * linkedWorldsCount;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.TALONITE_CASING;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (entityLinkBlockPos[i] != null) {
                data.setDouble("EntityLinkX" + i, entityLinkBlockPos[i].getX());
                data.setDouble("EntityLinkY" + i, entityLinkBlockPos[i].getY());
                data.setDouble("EntityLinkZ" + i, entityLinkBlockPos[i].getZ());
                data.setInteger("EntityWorld" + i, entityLinkWorld[i]);
                data.setInteger("EntityEnergyAmps" + i, entityEnergyAmps[i]);
            }
        }
        data.setLong("EnergyPerTick", totalEnergyPerTick);
        data.setInteger("BlockPosSize", entityLinkBlockPos.length);
        if (linkData != null)
            data.setTag("Link.XYZ", linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        linkData = data.hasKey("Link.XYZ") ? data.getCompoundTag("Link.XYZ") : null;
        totalEnergyPerTick = data.getLong("EnergyPerTick");
        entityLinkBlockPos = new BlockPos[data.getInteger("BlockPosSize")];
        entityLinkWorld = new int[data.getInteger("BlockPosSize")];
        entityEnergyAmps = new int[data.getInteger("BlockPosSize")];
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (data.hasKey("EntityLinkX" + i) && data.hasKey("EntityLinkY" + i) && data.hasKey("EntityLinkY" + i)) {
                entityLinkBlockPos[i] = new BlockPos(data.getDouble("EntityLinkX" + i), data.getDouble("EntityLinkY" + i), data.getDouble("EntityLinkZ" + i));
                entityLinkWorld[i] = data.getInteger("EntityWorld" + i);
                entityEnergyAmps[i] = data.getInteger("EntityEnergyAmps" + i);
            } else {
                entityLinkWorld[i] = Integer.MIN_VALUE;
            }
        }
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
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public long getMaxEUt() {
        return inputEnergyContainer.getInputVoltage();
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergy() {
        return totalEnergyPerTick;
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[tier];
    }

    @Override
    public RecipeMap<?> getMultiblockRecipe() {
        return null;
    }

    @Override
    public void onLink() {
        updateTotalEnergyPerTick();
        int dimensionID = getWorld().provider.getDimension();
        linkedWorldsCount = (int) Arrays.stream(entityLinkWorld).filter(id -> id != dimensionID && id != Integer.MIN_VALUE).count();
        fluidConsumption = 10 * linkedWorldsCount;
    }

    @Override
    public int dimensionID() {
        return getWorld().provider.getDimension();
    }

    @Override
    public void setDimension(IntSupplier dimensionID, int index) {
        entityLinkWorld[index] = dimensionID.getAsInt();
    }

    @Override
    public int getDimension(int index) {
        return entityLinkWorld[index];
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return entityLinkBlockPos.length;
    }

    @Override
    public BlockPos getPos(int i) {
        return entityLinkBlockPos[i];
    }

    @Override
    public void setPos(BlockPos pos, int index) {
        entityEnergyAmps[index] = 1;
        entityLinkBlockPos[index] = pos;
    }

    @Override
    public World world() {
        return getWorld();
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.linkData = linkData;
    }

    @Override
    public NBTTagCompound getLinkData() {
        return linkData;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_LINK_POS_INTERDIM)
            return TJCapabilities.CAPABILITY_LINK_POS_INTERDIM.cast(this);
        if (capability == TJCapabilities.CAPABILITY_LINK_POS)
            return TJCapabilities.CAPABILITY_LINK_POS.cast(this);
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }

    public enum TransferType {
        INPUT,
        OUTPUT
    }
}
