package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.*;
import com.johny.tj.gui.TJGuiTextures;
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
import gregtech.api.pipenet.block.material.TileEntityMaterialPipeBase;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
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
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.unification.material.Materials.UUMatter;

public class MetaTileEntityLargeWorldAccelerator extends TJMultiblockDisplayBase implements AcceleratorBlacklist, LinkPos<BlockPos>, LinkEvent, IParallelController {

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
    private NBTTagCompound linkData;

    public MetaTileEntityLargeWorldAccelerator(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.acceleratorMode = AcceleratorMode.RANDOM_TICK;
        this.isWorkingEnabled = false;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWorldAccelerator(metaTileEntityId);
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
        if (isStructureFormed()) {
            Style style = new Style().setColor(TextFormatting.GREEN);
            textList.add(hasEnoughEnergy(energyPerTick) ? new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", getMaxEUt())
                    .appendText("\n")
                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.sum", energyPerTick))
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

            switch (acceleratorMode) {
                case TILE_ENTITY:
                    textList.add(new TextComponentTranslation("gregtech.machine.world_accelerator.mode.tile"));
                    break;

                case GT_TILE_ENTITY:
                    textList.add(hasEnoughFluid(fluidConsumption) || fluidConsumption == 0 ? new TextComponentTranslation("tj.multiblock.enough_fluid")
                            .appendText(" ")
                            .appendSibling(new TextComponentTranslation(UUMatter.getUnlocalizedName())
                                    .setStyle(new Style().setColor(TextFormatting.LIGHT_PURPLE)))
                            .appendText(" ")
                            .appendText("§d" + fluidConsumption)
                            .appendText("§7 L/t"):
                            new TextComponentTranslation("tj.multiblock.not_enough_fluid").setStyle(new Style().setColor(TextFormatting.RED)));
                    textList.add(new TextComponentTranslation("tj.multiblock.large_world_accelerator.mode.GT"));
                    break;

                case RANDOM_TICK:
                    textList.add(new TextComponentTranslation("gregtech.machine.world_accelerator.mode.entity"));
            }
        } else {
            super.addDisplayText(textList);
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
        for (int i = pageIndex, linkedEntitiesPos = i + 1; i < pageIndex + pageSize; i++, linkedEntitiesPos++) {
            if (i < entityLinkBlockPos.length && entityLinkBlockPos[i] != null) {

                TileEntity tileEntity = world.getTileEntity(entityLinkBlockPos[i]);
                MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, entityLinkBlockPos[i]);
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
                                        isMetaTileEntity && metaTileEntity instanceof MetaTileEntityAcceleratorAnchorPoint ? tier : 0,
                                        isMetaTileEntity && metaTileEntity instanceof MetaTileEntityAcceleratorAnchorPoint ? tier : 0))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString("X: ").appendSibling(new TextComponentTranslation(entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(entityLinkBlockPos[i].getX()))
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString("Y: ").appendSibling(new TextComponentTranslation(entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(entityLinkBlockPos[i].getY()))
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString("Z: ").appendSibling(new TextComponentTranslation(entityLinkBlockPos[i] == null ? "machine.universal.linked.entity.empty" : String.valueOf(entityLinkBlockPos[i].getZ()))
                                        .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true))))))
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove" + i))
                );
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

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        Arrays.fill(entityLinkBlockPos, null);
        linkData = null;
        updateEnergyPerTick();
    }

    private AbstractWidgetGroup linkedEntitiesDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addDisplayLinkedEntities, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
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
            if (componentData.equals("remove" + i)) {
                int index = linkData.getInteger("I");
                linkData.setInteger("I", index + 1);
                entityLinkBlockPos[i] = null;
                updateEnergyPerTick();
                break;
            }
        }
    }

    @Override
    protected void updateFormedValid() {
        if (getOffsetTimer() > 100) {
            if (!isWorkingEnabled) {
                if (isActive)
                    setActive(false);
                return;
            }
            if (getNumProblems() < 6) {
                if (getOffsetTimer() % (1 + getNumProblems()) == 0) {
                    if (hasEnoughEnergy(energyPerTick)) {
                        if (!isActive)
                            setActive(true);

                        calculateMaintenance(1 + getNumProblems());
                        energyContainer.removeEnergy(energyPerTick);
                        WorldServer world = (WorldServer) this.getWorld();
                        switch (acceleratorMode) {

                            case TILE_ENTITY:
                                for (BlockPos pos : entityLinkBlockPos) {
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
                                        IntStream.range(0, (int) Math.pow(2, tier)).forEach(value -> ((ITickable) targetTE).update());
                                    }
                                }
                                return;

                            case GT_TILE_ENTITY:
                                if (gtAcceleratorTier < 1) {
                                    return;
                                }
                                if (hasEnoughFluid(fluidConsumption)) {
                                    importFluidHandler.drain(UUMatter.getFluid(fluidConsumption), true);
                                    if (entityLinkBlockPos[0] != null) {
                                        MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, entityLinkBlockPos[0]);
                                        if (targetGTTE == null || targetGTTE instanceof AcceleratorBlacklist) {
                                            return;
                                        }
                                        IntStream.range(0, (int) Math.pow(4, gtAcceleratorTier)).forEach(value -> targetGTTE.update());
                                    }
                                }
                                return;

                            case RANDOM_TICK:
                                for (BlockPos blockPos : entityLinkBlockPos) {
                                    if (blockPos == null)
                                        continue;
                                    MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, blockPos);
                                    if (targetGTTE instanceof MetaTileEntityAcceleratorAnchorPoint) {
                                        if (((MetaTileEntityAcceleratorAnchorPoint) targetGTTE).isRedStonePowered())
                                            continue;
                                    }
                                    BlockPos upperConner = blockPos.north(tier).east(tier);
                                    for (int x = 0; x < getArea(); x++) {
                                        BlockPos row = upperConner.south(x);
                                        for (int y = 0; y < getArea(); y++) {
                                            BlockPos cell = row.west(y);
                                            IBlockState targetBlock = world.getBlockState(cell);
                                            IntStream.range(0, (int) Math.pow(2, tier)).forEach(value -> {
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
                    } else {
                        if (isActive)
                            setActive(false);
                    }
                }
            }
        }
    }

    private void updateEnergyPerTick() {
        long count = Arrays.stream(entityLinkBlockPos).filter(Objects::nonNull).count();
        energyPerTick = (long) ((Math.pow(4, tier) * 8) * energyMultiplier) * count;
    }

    private boolean hasEnoughEnergy(long amount) {
        return energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(UUMatter.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    public int getArea() {
        return (tier * 2) + 1;
    }

    static Class clazz;

    static {
        try {
            clazz = Class.forName("cofh.core.block.TileCore");
        } catch (Exception ignored) {

        }
    }

    private void setLinkedEntitiesPos(MetaTileEntity metaTileEntity) {
        if (entityLinkBlockPos != null)
            Arrays.stream(entityLinkBlockPos)
                    .filter(Objects::nonNull)
                    .map(blockPos -> BlockMachine.getMetaTileEntity(getWorld(), blockPos))
                    .filter(entity -> entity instanceof LinkSet)
                    .forEach(entity -> ((LinkSet) entity).setLink(() -> metaTileEntity));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        FieldGenCasing.CasingType fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV);
        EmitterCasing.CasingType emitter = context.getOrDefault("Emitter", EmitterCasing.CasingType.EMITTER_LV);
        tier = Math.min(fieldGen.getTier(), emitter.getTier());
        gtAcceleratorTier = tier - GAValues.UHV;
        if (acceleratorMode == AcceleratorMode.GT_TILE_ENTITY) {
            entityLinkBlockPos = entityLinkBlockPos != null ? entityLinkBlockPos : new BlockPos[1];
        } else {
            entityLinkBlockPos = entityLinkBlockPos != null ? Arrays.copyOf(entityLinkBlockPos, tier) : new BlockPos[tier];
        }
        fluidConsumption = (int) Math.pow(4, gtAcceleratorTier - 1) * 1000;
        setLinkedEntitiesPos(this);
        updateEnergyPerTick();
    }

    @Override
    public void onRemoval() {
        if (!getWorld().isRemote)
            setLinkedEntitiesPos(null);
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
        switch (acceleratorMode) {
            case RANDOM_TICK:
                acceleratorMode = AcceleratorMode.TILE_ENTITY;
                energyMultiplier = 1;
                entityLinkBlockPos = new BlockPos[tier];
                tileMode = "gregtech.machine.world_accelerator.mode.tile";
                break;
             case TILE_ENTITY:
                 acceleratorMode = AcceleratorMode.GT_TILE_ENTITY;
                 energyMultiplier = 64;
                 entityLinkBlockPos = new BlockPos[1];
                 tileMode = "tj.multiblock.large_world_accelerator.mode.GT";
                 break;
            case GT_TILE_ENTITY:
                acceleratorMode = AcceleratorMode.RANDOM_TICK;
                energyMultiplier = 1;
                entityLinkBlockPos = new BlockPos[tier];
                tileMode = "gregtech.machine.world_accelerator.mode.entity";
        }
        energyPerTick = (long) (Math.pow(4, tier) * 8) * energyMultiplier;
        if (getWorld().isRemote) {
            playerIn.sendStatusMessage(new TextComponentTranslation(tileMode), false);
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (entityLinkBlockPos[i] != null) {
                data.setDouble("EntityLinkX" + i, entityLinkBlockPos[i].getX());
                data.setDouble("EntityLinkY" + i, entityLinkBlockPos[i].getY());
                data.setDouble("EntityLinkZ" + i, entityLinkBlockPos[i].getZ());
            }
        }
        data.setLong("EnergyPerTick", energyPerTick);
        data.setInteger("EnergyMultiplier", energyMultiplier);
        data.setInteger("AcceleratorMode", acceleratorMode.ordinal());
        data.setInteger("BlockPosSize", entityLinkBlockPos.length);
        if (linkData != null)
            data.setTag("Link.XYZ", linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        linkData = data.hasKey("Link.XZY") ? data.getCompoundTag("Link.XYZ") : null;
        energyMultiplier = data.getInteger("EnergyMultiplier");
        energyPerTick = data.getLong("EnergyPerTick");
        acceleratorMode = AcceleratorMode.values()[data.getInteger("AcceleratorMode")];
        entityLinkBlockPos = new BlockPos[data.getInteger("BlockPosSize")];
        for (int i = 0; i < entityLinkBlockPos.length; i++) {
            if (data.hasKey("EntityLinkX" + i) && data.hasKey("EntityLinkY" + i) && data.hasKey("EntityLinkY" + i)) {
                entityLinkBlockPos[i] = new BlockPos(data.getDouble("EntityLinkX" + i), data.getDouble("EntityLinkY" + i), data.getDouble("EntityLinkZ" + i));
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
    public int getRange() {
        return TJConfig.largeWorldAccelerator.range;
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
    public void onLink() {
        updateEnergyPerTick();
    }

    @Override
    public long getMaxEUt() {
        return energyContainer.getInputVoltage();
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergy() {
        return energyPerTick;
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
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_LINK_POS) {
            return TJCapabilities.CAPABILITY_LINK_POS.cast(this);
        }
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER) {
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        }
        return super.getCapability(capability, side);
    }

    public enum AcceleratorMode {
        RANDOM_TICK,
        TILE_ENTITY,
        GT_TILE_ENTITY
    }

}
