package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.MultiblockDisplayBuilder;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkEvent;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJWidgetGroup;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import com.johny.tj.items.TJMetaItems;
import com.johny.tj.machines.AcceleratorBlacklist;
import com.johny.tj.machines.singleblock.MetaTileEntityAcceleratorAnchorPoint;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
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
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static gregtech.api.gui.GuiTextures.DISPLAY;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.unification.material.Materials.UUMatter;

public class MetaTileEntityLargeWorldAccelerator extends TJMultiblockDisplayBase implements AcceleratorBlacklist, LinkPos, LinkEvent, IParallelController, IWorkable {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.INPUT_ENERGY, MultiblockAbility.IMPORT_FLUIDS, GregicAdditionsCapabilities.MAINTENANCE_HATCH};

    private long energyPerTick;
    private boolean isActive = false;
    private AcceleratorMode acceleratorMode;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer energyContainer;
    private int tier;
    private int gtAcceleratorTier;
    private int energyMultiplier = 1;
    private BlockPos[] entityLinkBlockPos;
    private int fluidConsumption;
    private final int pageSize = 4;
    private int pageIndex;
    private int progress;
    private int maxProgress = 1;
    private NBTTagCompound linkData;

    public MetaTileEntityLargeWorldAccelerator(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.acceleratorMode = AcceleratorMode.RANDOM_TICK;
        this.isWorkingEnabled = false;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWorldAccelerator(this.metaTileEntityId);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_world_accelerator.description"));
        tooltip.add(I18n.format("metaitem.item.linking.device.link.from"));
        tooltip.add("§f§n" + I18n.format("gregtech.machine.world_accelerator.mode.entity") + "§r§e§l -> §r§7" + I18n.format("tj.multiblock.world_accelerator.mode.entity.description"));
        tooltip.add("§f§n" + I18n.format("gregtech.machine.world_accelerator.mode.tile") + "§r§e§l -> §r§7" + I18n.format("tj.multiblock.world_accelerator.mode.tile.description"));
        tooltip.add("§f§n" + I18n.format("tj.multiblock.large_world_accelerator.mode.GT") + "§r§e§l -> §r§7" + I18n.format("tj.multiblock.large_world_accelerator.mode.GT.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            boolean randomTick = this.acceleratorMode == AcceleratorMode.RANDOM_TICK;
            boolean tileEntity = this.acceleratorMode == AcceleratorMode.TILE_ENTITY;
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyContainer)
                    .voltageTier(this.tier)
                    .energyInput(hasEnoughEnergy(this.energyPerTick), this.energyPerTick)
                    .fluidInput(hasEnoughFluid(this.fluidConsumption), UUMatter.getFluid(this.fluidConsumption), this.fluidConsumption)
                    .custom(text -> text.add(randomTick ? new TextComponentTranslation("gregtech.machine.world_accelerator.mode.entity")
                            : tileEntity ? new TextComponentTranslation("gregtech.machine.world_accelerator.mode.tile")
                            : new TextComponentTranslation("tj.multiblock.large_world_accelerator.mode.GT")))
                    .isWorking(this.isWorkingEnabled, this.isActive, this.progress, this.maxProgress);
        }
    }

    private void addDisplayLinkedEntities(List<ITextComponent> textList) {
        WorldServer world = (WorldServer) this.getWorld();
        textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.linked")
                .setStyle(new Style().setBold(true).setUnderlined(true)));
        textList.add(new TextComponentString(":")
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[<]"), "leftPage"))
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[>]"), "rightPage")));
        for (int i = this.pageIndex, linkedEntitiesPos = i + 1; i < this.pageIndex + this.pageSize; i++, linkedEntitiesPos++) {
            if (i < this.entityLinkBlockPos.length && this.entityLinkBlockPos[i] != null) {

                TileEntity tileEntity = world.getTileEntity(this.entityLinkBlockPos[i]);
                MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, this.entityLinkBlockPos[i]);
                boolean isTileEntity = tileEntity != null;
                boolean isMetaTileEntity = metaTileEntity != null;

                textList.add(new TextComponentString(": [" + linkedEntitiesPos + "] ")
                        .appendSibling(new TextComponentTranslation(isMetaTileEntity ? metaTileEntity.getMetaFullName()
                                : isTileEntity ? tileEntity.getBlockType().getTranslationKey() + ".name"
                                : "machine.universal.linked.entity.null")).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(isMetaTileEntity ? metaTileEntity.getMetaFullName()
                                    : isTileEntity ? tileEntity.getBlockType().getTranslationKey() + ".name"
                                    : "machine.universal.linked.entity.null")
                                    .appendText("\n")
                                    .appendSibling(new TextComponentTranslation("machine.universal.linked.entity.radius",
                                        isMetaTileEntity && metaTileEntity instanceof MetaTileEntityAcceleratorAnchorPoint ? this.tier : 0,
                                        isMetaTileEntity && metaTileEntity instanceof MetaTileEntityAcceleratorAnchorPoint ? this.tier : 0))
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
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove" + i))
                );
            }
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer player) {
        return this.createUI(player, 18);
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
        Arrays.fill(this.entityLinkBlockPos, null);
        this.linkData.setInteger("I", getPosSize());
        this.updateEnergyPerTick();
    }

    private AbstractWidgetGroup linkedEntitiesDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 0, this::addDisplayLinkedEntities, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        if (componentData.equals("leftPage") && this.pageIndex > 0) {
            this.pageIndex -= this.pageSize;
            return;
        }
        if (componentData.equals("rightPage") && this.pageIndex < this.entityLinkBlockPos.length - this.pageSize) {
            this.pageIndex += this.pageSize;
            return;
        }
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (componentData.equals("remove" + i)) {
                int index = linkData.getInteger("I");
                this.linkData.setInteger("I", index + 1);
                this.entityLinkBlockPos[i] = null;
                this.updateEnergyPerTick();
                break;
            }
        }
    }

    @Override
    protected void updateFormedValid() {
        if (getOffsetTimer() > 100) {
            if (!this.isWorkingEnabled || this.getNumProblems() >= 6 || this.maxProgress < 1) {
                if (this.isActive)
                    this.setActive(false);
                return;
            }

            this.calculateMaintenance(1);
            if (this.progress > 0 && !this.isActive)
                this.setActive(true);

            if (this.progress >= this.maxProgress) {
                if (this.hasEnoughEnergy(this.energyPerTick)) {
                    WorldServer world = (WorldServer) this.getWorld();
                    switch (this.acceleratorMode) {
                        case TILE_ENTITY:
                            for (BlockPos pos : this.entityLinkBlockPos) {
                                if (pos == null) {
                                    continue;
                                }
                                TileEntity targetTE = world.getTileEntity(pos);
                                if (targetTE == null || targetTE instanceof TileEntityMaterialPipeBase || targetTE instanceof MetaTileEntityHolder) {
                                    continue;
                                }
                                boolean horror = false;
                                if (clazz != null && targetTE instanceof ITickable) {
                                    horror = clazz.isInstance(targetTE);
                                }
                                if (targetTE instanceof ITickable && (!horror || !world.isRemote)) {
                                    IntStream.range(0, (int) Math.pow(2, this.tier)).forEach(value -> ((ITickable) targetTE).update());
                                }
                            }
                            break;

                        case GT_TILE_ENTITY:
                            if (this.gtAcceleratorTier < 1) {
                                return;
                            }
                            if (this.hasEnoughFluid(this.fluidConsumption)) {
                                this.importFluidHandler.drain(UUMatter.getFluid(this.fluidConsumption), true);
                                if (this.entityLinkBlockPos[0] != null) {
                                    MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, this.entityLinkBlockPos[0]);
                                    if (targetGTTE == null || targetGTTE instanceof AcceleratorBlacklist) {
                                        return;
                                    }
                                    IntStream.range(0, (int) Math.pow(4, this.gtAcceleratorTier)).forEach(value -> targetGTTE.update());
                                }
                            }
                            break;

                        case RANDOM_TICK:
                            for (BlockPos blockPos : this.entityLinkBlockPos) {
                                if (blockPos == null)
                                    continue;
                                MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, blockPos);
                                if (targetGTTE instanceof MetaTileEntityAcceleratorAnchorPoint) {
                                    if (((MetaTileEntityAcceleratorAnchorPoint) targetGTTE).isRedStonePowered())
                                        continue;
                                }
                                BlockPos upperConner = blockPos.north(this.tier).east(this.tier);
                                for (int x = 0; x < getArea(); x++) {
                                    BlockPos row = upperConner.south(x);
                                    for (int y = 0; y < getArea(); y++) {
                                        BlockPos cell = row.west(y);
                                        IBlockState targetBlock = world.getBlockState(cell);
                                        IntStream.range(0, (int) Math.pow(2, this.tier)).forEach(value -> {
                                            if (world.rand.nextInt(100) == 0) {
                                                if (targetBlock.getBlock().getTickRandomly()) {
                                                    targetBlock.getBlock().randomTick(world, cell, targetBlock, world.rand);
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                    }
                    this.energyContainer.removeEnergy(this.energyPerTick);
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
    }

    private void updateEnergyPerTick() {
        long count = Arrays.stream(this.entityLinkBlockPos).filter(Objects::nonNull).count();
        this.energyPerTick = (long) ((Math.pow(4, this.tier) * 8) * this.energyMultiplier) * count;
    }

    private boolean hasEnoughEnergy(long amount) {
        return this.energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(UUMatter.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    public int getArea() {
        return (this.tier * 2) + 1;
    }

    static Class clazz;

    static {
        try {
            clazz = Class.forName("cofh.core.block.TileCore");
        } catch (Exception ignored) {

        }
    }

    private void setLinkedEntitiesPos(MetaTileEntity metaTileEntity) {
        if (this.entityLinkBlockPos != null)
            Arrays.stream(this.entityLinkBlockPos)
                    .filter(Objects::nonNull)
                    .map(blockPos -> BlockMachine.getMetaTileEntity(getWorld(), blockPos))
                    .filter(entity -> entity instanceof LinkEvent)
                    .forEach(entity -> ((LinkEvent) entity).onLink(metaTileEntity));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        FieldGenCasing.CasingType fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV);
        EmitterCasing.CasingType emitter = context.getOrDefault("Emitter", EmitterCasing.CasingType.EMITTER_LV);
        this.tier = Math.min(fieldGen.getTier(), emitter.getTier());
        this.gtAcceleratorTier = this.tier - GAValues.UHV;
        if (this.acceleratorMode == AcceleratorMode.GT_TILE_ENTITY) {
            this.entityLinkBlockPos = this.entityLinkBlockPos != null ? this.entityLinkBlockPos : new BlockPos[1];
        } else {
            this.entityLinkBlockPos = this.entityLinkBlockPos != null ? Arrays.copyOf(this.entityLinkBlockPos, this.tier) : new BlockPos[this.tier];
        }
        this.fluidConsumption = (int) Math.pow(4, this.gtAcceleratorTier - 1) * 1000;
        this.setLinkedEntitiesPos(this);
        this.updateEnergyPerTick();
        if (this.linkData != null) {
            int size = this.linkData.getInteger("Size") - this.entityLinkBlockPos.length;
            int remaining = Math.max(0, (this.linkData.getInteger("I") - size));
            this.linkData.setInteger("Size", this.entityLinkBlockPos.length);
            this.linkData.setInteger("I", remaining);
        }
    }

    @Override
    public void onRemoval() {
        if (!this.getWorld().isRemote)
            this.setLinkedEntitiesPos(null);
        super.onRemoval();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("#C#", "CEC", "#C#")
                .aisle("CEC", "EFE", "CEC")
                .aisle("#C#", "CSC", "#C#")
                .where('S', selfPredicate())
                .where('C', statePredicate(GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.TRITANIUM)).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', LargeSimpleRecipeMapMultiblockController.fieldGenPredicate())
                .where('E', LargeSimpleRecipeMapMultiblockController.emitterPredicate())
                .where('#', (tile) -> true)
                .build();
    }


    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.TRITANIUM_CASING;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.AMPLIFAB_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        String tileMode = "null";
        switch (this.acceleratorMode) {
            case RANDOM_TICK:
                this.acceleratorMode = AcceleratorMode.TILE_ENTITY;
                this.energyMultiplier = 1;
                this.entityLinkBlockPos = new BlockPos[this.tier];
                tileMode = "gregtech.machine.world_accelerator.mode.tile";
                break;
             case TILE_ENTITY:
                 this.acceleratorMode = AcceleratorMode.GT_TILE_ENTITY;
                 this.energyMultiplier = 64;
                 this.entityLinkBlockPos = new BlockPos[1];
                 tileMode = "tj.multiblock.large_world_accelerator.mode.GT";
                 break;
            case GT_TILE_ENTITY:
                this.acceleratorMode = AcceleratorMode.RANDOM_TICK;
                this.energyMultiplier = 1;
                this.entityLinkBlockPos = new BlockPos[this.tier];
                tileMode = "gregtech.machine.world_accelerator.mode.entity";
        }
        this.energyPerTick = (long) (Math.pow(4, this.tier) * 8) * this.energyMultiplier;
        if (this.linkData != null) {
            this.linkData.setInteger("Size", this.entityLinkBlockPos.length);
            this.linkData.setInteger("I", this.entityLinkBlockPos.length);
        }
        if (this.getWorld().isRemote) {
            playerIn.sendStatusMessage(new TextComponentTranslation(tileMode), false);
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        for (int i = 0; i < this.entityLinkBlockPos.length; i++) {
            if (this.entityLinkBlockPos[i] != null) {
                data.setDouble("EntityLinkX" + i, this.entityLinkBlockPos[i].getX());
                data.setDouble("EntityLinkY" + i, this.entityLinkBlockPos[i].getY());
                data.setDouble("EntityLinkZ" + i, this.entityLinkBlockPos[i].getZ());
            }
        }
        data.setLong("EnergyPerTick", this.energyPerTick);
        data.setInteger("Progress", this.progress);
        data.setInteger("MaxProgress", this.maxProgress);
        data.setInteger("EnergyMultiplier", this.energyMultiplier);
        data.setInteger("AcceleratorMode", this.acceleratorMode.ordinal());
        data.setInteger("BlockPosSize", this.entityLinkBlockPos.length);
        if (this.linkData != null)
            data.setTag("Link.XYZ", this.linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.energyMultiplier = data.getInteger("EnergyMultiplier");
        this.energyPerTick = data.getLong("EnergyPerTick");
        this.acceleratorMode = AcceleratorMode.values()[data.getInteger("AcceleratorMode")];
        this.entityLinkBlockPos = new BlockPos[data.getInteger("BlockPosSize")];
        this.maxProgress = data.hasKey("MaxProgress") ? data.getInteger("MaxProgress") : 1;
        this.progress = data.getInteger("Progress");
        for (int i = 0; i < this.entityLinkBlockPos.length; i++) {
            if (data.hasKey("EntityLinkX" + i) && data.hasKey("EntityLinkY" + i) && data.hasKey("EntityLinkY" + i)) {
                this.entityLinkBlockPos[i] = new BlockPos(data.getDouble("EntityLinkX" + i), data.getDouble("EntityLinkY" + i), data.getDouble("EntityLinkZ" + i));
            }
        }
        if (data.hasKey("Link.XYZ"))
            this.linkData = data.getCompoundTag("Link.XYZ");
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        this.markDirty();
        if (!getWorld().isRemote) {
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
    public int getRange() {
        return TJConfig.largeWorldAccelerator.baseRange + TJConfig.largeWorldAccelerator.additionalRange * tier;
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
        this.entityLinkBlockPos[index] = pos;
    }

    @Override
    public World world() {
        return getWorld();
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
    public void onLink(MetaTileEntity tileEntity) {
        this.updateEnergyPerTick();
    }

    @Override
    public long getMaxEUt() {
        return this.energyContainer.getInputVoltage();
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergyConsumption() {
        return this.energyPerTick;
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
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

    public enum AcceleratorMode {
        RANDOM_TICK,
        TILE_ENTITY,
        GT_TILE_ENTITY
    }

}
