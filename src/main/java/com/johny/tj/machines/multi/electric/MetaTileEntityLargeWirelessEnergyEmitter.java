package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.MultiblockDisplayBuilder;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkEvent;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJWidgetGroup;
import com.johny.tj.gui.uifactory.IPlayerUI;
import com.johny.tj.gui.uifactory.PlayerHolder;
import com.johny.tj.gui.widgets.OnTextFieldWidget;
import com.johny.tj.gui.widgets.TJAdvancedTextWidget;
import com.johny.tj.gui.widgets.TJClickButtonWidget;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import com.johny.tj.items.TJMetaItems;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.item.metal.MetalCasing2;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.MetaTileEntityUIFactory;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static gregicadditions.GAMaterials.Talonite;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
import static gregtech.api.gui.GuiTextures.BORDERED_BACKGROUND;
import static gregtech.api.gui.GuiTextures.DISPLAY;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.unification.material.Materials.Nitrogen;
import static gregtech.api.unification.material.Materials.RedSteel;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;

public class MetaTileEntityLargeWirelessEnergyEmitter extends TJMultiblockDisplayBase implements LinkPos, LinkEvent, IParallelController, IWorkable, IPlayerUI {

    protected TransferType transferType;
    private long energyPerTick;
    private long totalEnergyPerTick;
    private int fluidConsumption;
    private boolean isActive = false;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer inputEnergyContainer;
    private int tier;
    private String[] entityLinkName;
    private BlockPos[] entityLinkBlockPos;
    private int[] entityLinkWorld;
    private int[] entityEnergyAmps;
    private int linkedWorldsCount;
    private final int pageSize = 4;
    private int pageIndex;
    private int progress;
    private int maxProgress = 1;
    private String renamePrompt = "";
    private NBTTagCompound linkData;
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_FLUIDS, INPUT_ENERGY, OUTPUT_ENERGY, MAINTENANCE_HATCH};

    public MetaTileEntityLargeWirelessEnergyEmitter(ResourceLocation metaTileEntityId, TransferType transferType) {
        super(metaTileEntityId);
        this.transferType = transferType;
        this.reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWirelessEnergyEmitter(this.metaTileEntityId, this.transferType);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_wireless_energy_emitter.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed())
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.inputEnergyContainer)
                    .voltageTier(this.tier)
                    .energyStored(this.getEnergyStored(), this.getEnergyCapacity())
                    .energyInput(hasEnoughEnergy(this.totalEnergyPerTick), this.totalEnergyPerTick, this.maxProgress)
                    .fluidInput(hasEnoughFluid(this.fluidConsumption), Nitrogen.getPlasma(this.fluidConsumption), this.fluidConsumption)
                    .isWorking(this.isWorkingEnabled, this.isActive, this.progress, this.maxProgress);

    }

    private void addDisplayLinkedEntitiesText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.linked")
                .setStyle(new Style().setBold(true).setUnderlined(true)));
        textList.add(new TextComponentString(":")
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[<]"), "leftPage"))
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[>]"), "rightPage")));

        for (int i = this.pageIndex, linkedEntitiesPos = i + 1; i < this.pageIndex + this.pageSize; i++, linkedEntitiesPos++) {
            if (i < this.entityLinkBlockPos.length && this.entityLinkBlockPos[i] != null) {

                WorldServer world = DimensionManager.getWorld(this.entityLinkWorld[i]);
                TileEntity getTileEntity = world != null ? world.getTileEntity(this.entityLinkBlockPos[i]) : null;
                MetaTileEntity getMetaTileEntity = world != null ? BlockMachine.getMetaTileEntity(world, this.entityLinkBlockPos[i]) : null;
                boolean isTileEntity = getTileEntity != null;
                boolean isMetaTileEntity = getMetaTileEntity != null;
                IEnergyStorage RFContainer = isTileEntity ? getTileEntity.getCapability(ENERGY, null) : null;
                long RFStored = RFContainer != null ? RFContainer.getEnergyStored() : 0;
                long RFCapacity = RFContainer != null ? RFContainer.getMaxEnergyStored() : 0;
                IEnergyContainer EUContainer = isMetaTileEntity ? getMetaTileEntity.getCapability(CAPABILITY_ENERGY_CONTAINER, null) : null;
                long EUStored = EUContainer != null ? EUContainer.getEnergyStored() : 0;
                long EUCapacity = EUContainer != null ? EUContainer.getEnergyCapacity() : 0;
                String name = this.entityLinkName[i];

                textList.add(new TextComponentString(": [" + linkedEntitiesPos + "] ")
                        .appendSibling(new TextComponentString(name)).setStyle(new Style()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(name)
                                        .appendText("\n")
                                        .appendSibling(new TextComponentTranslation("machine.universal.linked.dimension", world != null ? world.provider.getDimensionType().getName() : "N/A", world != null ? world.provider.getDimensionType().getId() : 0))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentTranslation("machine.universal.energy.stored", isMetaTileEntity ? EUStored : RFStored, isMetaTileEntity ? EUCapacity : RFCapacity))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("X: ").appendSibling(new TextComponentTranslation(this.entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(this.entityLinkBlockPos[i].getX()))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("Y: ").appendSibling(new TextComponentTranslation(this.entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(this.entityLinkBlockPos[i].getY()))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("Z: ").appendSibling(new TextComponentTranslation(this.entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(this.entityLinkBlockPos[i].getZ()))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true))))))
                        .appendText(" ")
                        .appendSibling(new TextComponentTranslation("machine.universal.energy.amps", this.entityEnergyAmps[i])
                                .appendText(" ")
                                .appendSibling(withButton(new TextComponentString("[+]"), "increment:" + i))
                                .appendText(" ")
                                .appendSibling(withButton(new TextComponentString("[-]"), "decrement:" + i)))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:" + i))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "rename:" + name)));

            }
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer player) {
        return this.createUI(player, 18);
    }

    @Override
    public ModularUI createUI(PlayerHolder holder, EntityPlayer player) {
        ModularUI.Builder builder = ModularUI.builder(BORDERED_BACKGROUND, 176, 80);
        builder.widget(new ImageWidget(10, 10, 156, 18, DISPLAY));
        OnTextFieldWidget onTextFieldWidget = new OnTextFieldWidget(15, 15, 151, 18, false, this::getRename, this::setRename);
        onTextFieldWidget.setTooltipText("machine.universal.set.name");
        onTextFieldWidget.setBackgroundText("machine.universal.set.name");
        onTextFieldWidget.setValidator(str -> Pattern.compile(".*").matcher(str).matches());
        builder.widget(onTextFieldWidget);
        builder.widget(new TJClickButtonWidget(10, 38, 156, 18, "OK", onTextFieldWidget::onResponder)
                .setClickHandler(this::onPlayerPressed));
        return builder.build(holder, player);
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs, int extended) {
        super.addNewTabs(tabs, extended);
        TJWidgetGroup widgetLinkedEntitiesGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.linked_entities_display", TJMetaItems.LINKING_DEVICE.getStackForm(), linkedEntitiesDisplayTab(widgetLinkedEntitiesGroup::addWidgets)));
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup, int extended) {
        super.mainDisplayTab(widgetGroup, extended);
        widgetGroup.apply(new ImageWidget(28, 112, 141, 18, DISPLAY));
        widgetGroup.apply(new TJTextFieldWidget(33, 117, 136, 18, false, this::getTickSpeed, this::setTickSpeed)
                .setTooltipText("machine.universal.tick.speed")
                .setTooltipFormat(this::getTickSpeedFormat)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.apply(new ClickButtonWidget(7, 112, 18, 18, "+", this::onIncrement));
        widgetGroup.apply(new ClickButtonWidget(172, 112, 18, 18, "-", this::onDecrement));
        return widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    private AbstractWidgetGroup linkedEntitiesDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new TJAdvancedTextWidget(10, 0, this::addDisplayLinkedEntitiesText, 0xFFFFFF)
                .setMaxWidthLimit(1000).setClickHandler(this::handleDisplayClick));
    }

    private String getRename() {
        return this.renamePrompt;
    }

    private void setRename(String name) {
        IntStream.range(0, this.entityLinkName.length)
                .filter(i -> this.entityLinkName[i].equals(this.renamePrompt))
                .forEach(i -> this.entityLinkName[i] = name);
    }

    private void onPlayerPressed(Widget.ClickData clickData, EntityPlayer player) {
        MetaTileEntityUIFactory.INSTANCE.openUI(this.getHolder(), (EntityPlayerMP) player);
    }

    private String[] getTickSpeedFormat() {
        return ArrayUtils.toArray(String.valueOf(this.maxProgress));
    }

    private void onIncrement(Widget.ClickData clickData) {
        this.maxProgress = MathHelper.clamp(this.maxProgress * 2, 1, Integer.MAX_VALUE);
        this.markDirty();
    }

    private void onDecrement(Widget.ClickData clickData) {
        this.maxProgress = MathHelper.clamp(this.maxProgress / 2, 1, Integer.MAX_VALUE);
        this.markDirty();
    }

    private String getTickSpeed() {
        return String.valueOf(this.maxProgress);
    }

    private void setTickSpeed(String maxProgress) {
        this.maxProgress = maxProgress.isEmpty() ? 1 : Integer.parseInt(maxProgress);
        this.markDirty();
    }

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        Arrays.fill(this.entityLinkName, null);
        Arrays.fill(this.entityLinkBlockPos, null);
        Arrays.fill(this.entityLinkWorld, Integer.MIN_VALUE);
        Arrays.fill(this.entityEnergyAmps, 0);
        this.linkData.setInteger("I", getPosSize());
        this.updateTotalEnergyPerTick();
    }

    protected void handleDisplayClick(String componentData, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.equals("leftPage") && this.pageIndex > 0) {
            this.pageIndex -= this.pageSize;

        } else if (componentData.equals("rightPage") && this.pageIndex < this.entityLinkBlockPos.length - this.pageSize) {
            this.pageIndex += this.pageSize;

        } else  if (componentData.startsWith("increment")) {
            String[] increment = componentData.split(":");
            int i = Integer.parseInt(increment[1]);
            this.entityEnergyAmps[i] = MathHelper.clamp(this.entityEnergyAmps[i] + 1, 0, 256);
            this.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("decrement")) {
            String[] decrement = componentData.split(":");
            int i = Integer.parseInt(decrement[1]);
            this.entityEnergyAmps[i] = MathHelper.clamp(this.entityEnergyAmps[i] - 1, 0, 256);
            this.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("remove")) {
            String[] remove = componentData.split(":");
            int i = Integer.parseInt(remove[1]);
            int j = linkData.getInteger("I");
            this.linkData.setInteger("I", j + 1);
            this.entityLinkName[i] = null;
            this.entityLinkBlockPos[i] = null;
            this.entityLinkWorld[i] = Integer.MIN_VALUE;
            this.entityEnergyAmps[i] = 0;
            this.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("rename")) {
            String[] rename = componentData.split(":");
            this.renamePrompt = rename[1];
            PlayerHolder holder = new PlayerHolder(player, this);
            holder.openUI();
        }
    }

    @Override
    protected void updateFormedValid() {
        if (!this.isWorkingEnabled || !this.hasEnoughEnergy(this.totalEnergyPerTick) || this.getNumProblems() >= 6 || this.maxProgress < 1) {
            if (this.isActive)
                this.setActive(false);
            return;
        }

        this.calculateMaintenance(1);
        if (this.progress > 0 && !this.isActive)
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            for (int i = 0; i < this.entityLinkBlockPos.length; i++) {
                if (this.entityLinkBlockPos[i] == null)
                    continue;
                if (getWorld().provider.getDimension() != this.entityLinkWorld[i]) {
                    if (!hasEnoughFluid(this.fluidConsumption))
                        continue;
                    int fluidToConsume = this.fluidConsumption / this.linkedWorldsCount;
                    this.importFluidHandler.drain(Nitrogen.getPlasma(fluidToConsume), true);
                }
                WorldServer world = DimensionManager.getWorld(this.entityLinkWorld[i]);
                if (world == null)
                    continue;
                Chunk chunk = world.getChunk(this.entityLinkBlockPos[i]);
                if (!chunk.isLoaded())
                    chunk.onLoad();
                TileEntity tileEntity = world.getTileEntity(this.entityLinkBlockPos[i]);
                MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, this.entityLinkBlockPos[i]);
                long energyToAdd = this.energyPerTick * this.entityEnergyAmps[i];
                if (tileEntity != null) {
                    IEnergyStorage RFContainer = tileEntity.getCapability(ENERGY, null);
                    this.transferRF((int) energyToAdd, RFContainer);
                }
                if (metaTileEntity != null) {
                    IEnergyContainer EUContainer = metaTileEntity.getCapability(CAPABILITY_ENERGY_CONTAINER, null);
                    this.transferEU(energyToAdd, EUContainer);
                }
            }
            this.progress = 0;
            if (this.isActive)
                this.setActive(false);
        }

        if (this.progress <= 0) {
            this.progress = 1;
            if (!this.isActive)
                this.setActive(true);
        } else {
            this.progress++;
        }
    }

    protected void transferRF(int energyToAdd, IEnergyStorage RFContainer) {
        if (RFContainer == null)
            return;
        int energyRemainingToFill = RFContainer.getMaxEnergyStored() - RFContainer.getEnergyStored();
        if (RFContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            int energyInserted = RFContainer.receiveEnergy(Math.min(Integer.MAX_VALUE, energyToAdd * 4), false);
            this.inputEnergyContainer.removeEnergy(energyInserted / 4);
        }
    }

    protected void transferEU(long energyToAdd, IEnergyContainer EUContainer) {
        if (EUContainer == null)
            return;
        long energyRemainingToFill = EUContainer.getEnergyCapacity() - EUContainer.getEnergyStored();
        if (EUContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            long energyInserted = EUContainer.addEnergy(Math.min(energyToAdd, energyRemainingToFill));
            this.inputEnergyContainer.removeEnergy(energyInserted);
        }
    }

    private void updateTotalEnergyPerTick() {
        int amps = Arrays.stream(this.entityEnergyAmps).sum();
        this.totalEnergyPerTick = (long) (Math.pow(4, this.tier) * 8) * amps;
    }

    protected boolean hasEnoughEnergy(long amount) {
        return this.inputEnergyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(Nitrogen.getPlasma(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return this.transferType == null ? null : FactoryBlockPattern.start()
                .aisle("~HHH~", "~HHH~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "HHFHH", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "HFIFH", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("HHHHH", "HHFHH", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("~HHH~", "~HSH~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState(this.transferType)))
                .where('H', statePredicate(this.getCasingState(this.transferType)).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', statePredicate(this.getFrameState(this.transferType)))
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
        this.inputEnergyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        int framework = 0, framework2 = 0;
        if (context.get("framework") instanceof GAMultiblockCasing.CasingType) {
            framework = ((GAMultiblockCasing.CasingType) context.get("framework")).getTier();
        }
        if (context.get("framework2") instanceof GAMultiblockCasing2.CasingType) {
            framework2 = ((GAMultiblockCasing2.CasingType) context.get("framework2")).getTier();
        }
        this.tier = Math.max(framework, framework2);
        int linkAmount = tier * 2;
        this.entityLinkName = this.entityLinkName != null ? Arrays.copyOf(this.entityLinkName, linkAmount) : new String[linkAmount];
        this.entityLinkBlockPos = this.entityLinkBlockPos != null ? Arrays.copyOf(this.entityLinkBlockPos, linkAmount) : new BlockPos[linkAmount];
        this.entityEnergyAmps = this.entityEnergyAmps != null ? Arrays.copyOf(this.entityEnergyAmps, linkAmount) : new int[linkAmount];
        if (this.entityLinkWorld != null) {
            this.entityLinkWorld = Arrays.copyOf(this.entityLinkWorld, linkAmount);
        } else {
            this.entityLinkWorld = new int[linkAmount];
            Arrays.fill(this.entityLinkWorld, getWorld().provider.getDimension());
        }
        this.energyPerTick = (long) (Math.pow(4, tier) * 8);
        this.updateTotalEnergyPerTick();
        int dimensionID = getWorld().provider.getDimension();
        this.linkedWorldsCount = (int) Arrays.stream(this.entityLinkWorld).filter(id -> id != dimensionID && id != Integer.MIN_VALUE).count();
        this.fluidConsumption = 10 * this.linkedWorldsCount;
        if (this.linkData != null) {
            int size = this.linkData.getInteger("Size") - this.entityLinkBlockPos.length;
            int remaining = Math.max(0, (this.linkData.getInteger("I") - size));
            this.linkData.setInteger("Size", this.entityLinkBlockPos.length);
            this.linkData.setInteger("I", remaining);
        }
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.TALONITE_CASING;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), this.isActive);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        for (int i = 0; i < this.entityLinkBlockPos.length; i++) {
            if (this.entityLinkBlockPos[i] != null) {
                data.setDouble("EntityLinkX" + i, this.entityLinkBlockPos[i].getX());
                data.setDouble("EntityLinkY" + i, this.entityLinkBlockPos[i].getY());
                data.setDouble("EntityLinkZ" + i, this.entityLinkBlockPos[i].getZ());
                data.setInteger("EntityWorld" + i, this.entityLinkWorld[i]);
                data.setInteger("EntityEnergyAmps" + i, this.entityEnergyAmps[i]);
                data.setString("EntityName" + i, this.entityLinkName[i]);
            }
        }
        data.setLong("EnergyPerTick", this.totalEnergyPerTick);
        data.setInteger("Progress", this.progress);
        data.setInteger("MaxProgress", this.maxProgress);
        data.setInteger("BlockPosSize", this.entityLinkBlockPos.length);
        if (this.linkData != null)
            data.setTag("Link.XYZ", this.linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.totalEnergyPerTick = data.getLong("EnergyPerTick");
        this.entityLinkName = new String[data.getInteger("BlockPosSize")];
        this.entityLinkBlockPos = new BlockPos[data.getInteger("BlockPosSize")];
        this.entityLinkWorld = new int[data.getInteger("BlockPosSize")];
        this.entityEnergyAmps = new int[data.getInteger("BlockPosSize")];
        this.maxProgress = data.hasKey("MaxProgress") ? data.getInteger("MaxProgress") : 1;
        this.progress = data.getInteger("Progress");
        for (int i = 0; i < this.entityLinkBlockPos.length; i++) {
            if (data.hasKey("EntityLinkX" + i) && data.hasKey("EntityLinkY" + i) && data.hasKey("EntityLinkY" + i)) {
                this.entityLinkBlockPos[i] = new BlockPos(data.getDouble("EntityLinkX" + i), data.getDouble("EntityLinkY" + i), data.getDouble("EntityLinkZ" + i));
                this.entityLinkWorld[i] = data.getInteger("EntityWorld" + i);
                this.entityEnergyAmps[i] = data.getInteger("EntityEnergyAmps" + i);
                this.entityLinkName[i] = data.getString("EntityName" + i);
            } else {
                this.entityLinkWorld[i] = Integer.MIN_VALUE;
            }
        }
        if (data.hasKey("Link.XYZ"))
            this.linkData = data.getCompoundTag("Link.XYZ");
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        this.markDirty();
        if (!this.getWorld().isRemote) {
            this.writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            this.getHolder().scheduleChunkForRenderUpdate();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public long getEnergyStored() {
        return this.inputEnergyContainer.getEnergyStored();
    }

    @Override
    public long getEnergyCapacity() {
        return this.inputEnergyContainer.getEnergyCapacity();
    }

    @Override
    public long getMaxEUt() {
        return this.inputEnergyContainer.getInputVoltage();
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergyConsumption() {
        return this.totalEnergyPerTick;
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
    }

    @Override
    public void onLink(MetaTileEntity tileEntity) {
        updateTotalEnergyPerTick();
        int dimensionID = getWorld().provider.getDimension();
        this.linkedWorldsCount = (int) Arrays.stream(this.entityLinkWorld).filter(id -> id != dimensionID && id != Integer.MIN_VALUE).count();
        this.fluidConsumption = 10 * this.linkedWorldsCount;
    }

    @Override
    public boolean isInterDimensional() {
        return true;
    }

    @Override
    public int dimensionID() {
        return getWorld().provider.getDimension();
    }

    @Override
    public int getDimension(int index) {
        return this.entityLinkWorld[index];
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return this.entityLinkBlockPos.length;
    }

    @Override
    public BlockPos getPos(int i) {
        return this.entityLinkBlockPos[i];
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        this.entityLinkName[index] = name;
        this.entityLinkWorld[index] = world.provider.getDimensionType().getId();
        this.entityEnergyAmps[index] = 1;
        this.entityLinkBlockPos[index] = pos;
    }

    @Override
    public World world() {
        return this.getWorld();
    }

    @Override
    public int getPageIndex() {
        return this.pageIndex;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.linkData = linkData;
    }

    @Override
    public NBTTagCompound getLinkData() {
        return this.linkData;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_LINK_POS)
            return TJCapabilities.CAPABILITY_LINK_POS.cast(this);
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }

    public enum TransferType {
        INPUT,
        OUTPUT
    }
}
