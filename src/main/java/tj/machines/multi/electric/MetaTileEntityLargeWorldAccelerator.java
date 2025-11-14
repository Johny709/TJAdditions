package tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.metatileentity.MTETrait;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tj.TJConfig;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.builder.handlers.AcceleratorWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IParallelController;
import tj.capability.LinkEvent;
import tj.capability.LinkPos;
import tj.capability.TJCapabilities;
import tj.gui.TJGuiTextures;
import tj.gui.uifactory.PlayerHolder;
import tj.gui.widgets.TJAdvancedTextWidget;
import tj.gui.widgets.TJClickButtonWidget;
import tj.gui.widgets.TJTextFieldWidget;
import tj.items.TJMetaItems;
import tj.machines.AcceleratorBlacklist;
import tj.machines.singleblock.MetaTileEntityAcceleratorAnchorPoint;
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
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static tj.builder.handlers.AcceleratorWorkableHandler.AcceleratorMode.*;
import static tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static tj.gui.TJGuiTextures.SPACES_BUTTON;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.unification.material.Materials.UUMatter;

public class MetaTileEntityLargeWorldAccelerator extends TJMultiblockDisplayBase implements AcceleratorBlacklist, LinkPos, LinkEvent, IParallelController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.INPUT_ENERGY, MultiblockAbility.IMPORT_FLUIDS, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private final AcceleratorWorkableHandler workableHandler = new AcceleratorWorkableHandler(this);

    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer energyContainer;
    private int tier;
    private final int pageSize = 4;
    private int pageIndex;

    public MetaTileEntityLargeWorldAccelerator(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.workableHandler.setImportFluidsSupplier(() -> this.importFluidHandler)
                .setImportEnergySupplier(() -> this.energyContainer);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWorldAccelerator(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
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
            boolean randomTick = this.workableHandler.getAcceleratorMode() == AcceleratorWorkableHandler.AcceleratorMode.RANDOM_TICK;
            boolean tileEntity = this.workableHandler.getAcceleratorMode() == TILE_ENTITY;
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyContainer)
                    .voltageTier(this.tier)
                    .energyInput(this.energyContainer.getEnergyStored() >= this.workableHandler.getEnergyPerTick(), this.workableHandler.getEnergyPerTick(), this.workableHandler.getMaxProgress())
                    .fluidInput(hasEnoughFluid(this.fluidConsumption), UUMatter.getFluid(this.fluidConsumption))
                    .custom(text -> text.add(randomTick ? new TextComponentTranslation("gregtech.machine.world_accelerator.mode.entity")
                            : tileEntity ? new TextComponentTranslation("gregtech.machine.world_accelerator.mode.tile")
                            : new TextComponentTranslation("tj.multiblock.large_world_accelerator.mode.GT")))
                    .isWorking(this.isWorkingEnabled, this.workableHandler.isActive(), this.workableHandler.getProgress(), this.workableHandler.getMaxProgress());
        }
    }

    private void addDisplayLinkedEntities(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + net.minecraft.util.text.translation.I18n.translateToLocal("tj.multiblock.large_world_accelerator.linked") + "§r(§e" + this.searchResults + "§r/§e" + this.entityLinkName.length + "§r)"));
    }

    private void addDisplayLinkedEntities2(List<ITextComponent> textList) {
        int searchResults = 0;
        for (int i = 0; i < this.workableHandler.getEntityLinkName().length; i++) {
            String name = this.workableHandler.getEntityLinkName()[i] != null ? this.workableHandler.getEntityLinkName()[i] : net.minecraft.util.text.translation.I18n.translateToLocal("machine.universal.empty");




            BlockPos pos = this.workableHandler.getEntityLinkBlockPos()[i] != null ? this.workableHandler.getEntityLinkBlockPos()[i] : TJValues.DUMMY_POS;
            MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(this.getWorld(), pos);
            boolean isMetaTileEntity = metaTileEntity != null;

            textList.add(new TextComponentString(": [§a" + ++searchResults+ "§r] ")
                    .appendSibling(new TextComponentString(name).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(name)
                            .appendText("\n")
                            .appendSibling(new TextComponentTranslation("machine.universal.linked.entity.radius",
                                isMetaTileEntity && metaTileEntity instanceof MetaTileEntityAcceleratorAnchorPoint ? this.tier : 0,
                                isMetaTileEntity && metaTileEntity instanceof MetaTileEntityAcceleratorAnchorPoint ? this.tier : 0))
                            .appendText("\n")
                            .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.pos", pos.getX(), pos.getY(), pos.getZ()))))))
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:" + i)))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "rename:" + name)));
        }
        this.searchResults = searchResults;
    }

    @Override
    protected int getExtended() {
        return 18;
    }

    @Override
    protected void addTabs(WidgetTabBuilder tabBuilder) {
        super.addTabs(tabBuilder);
        tabBuilder.addTab("tj.multiblock.tab.linked_entities_display", TJMetaItems.LINKING_DEVICE.getStackForm(), linkedEntitiesDisplayTab -> {
            ScrollableListWidget scrollWidget = new ScrollableListWidget(10, -8, 178, 117) {
                @Override
                public boolean isWidgetClickable(Widget widget) {
                    return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
                }
            };
            scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addDisplayLinkedEntities2, 0xFFFFFF)
                    .addClickHandler(this::handleLinkedDisplayClick)
                    .setMaxWidthLimit(1000));
            linkedEntitiesDisplayTab.addWidget(new AdvancedTextWidget(10, -20, this::addDisplayLinkedEntities, 0xFFFFFF));
            linkedEntitiesDisplayTab.addWidget(scrollWidget);
            linkedEntitiesDisplayTab.addWidget(new ToggleButtonWidget(172, 133, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                    .setTooltipText("machine.universal.case_sensitive"));
            linkedEntitiesDisplayTab.addWidget(new ToggleButtonWidget(172, 151, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                    .setTooltipText("machine.universal.spaces"));
            linkedEntitiesDisplayTab.addWidget(new ImageWidget(7, 112, 162, 18, DISPLAY));
            linkedEntitiesDisplayTab.addWidget(new TJClickButtonWidget(172, 112, 18, 18, "", this::onClear)
                    .setTooltipText("machine.universal.toggle.clear")
                    .setButtonTexture(BUTTON_CLEAR_GRID));
            linkedEntitiesDisplayTab.addWidget(new TJTextFieldWidget(12, 117, 157, 18, false, this::getSearchPrompt, this::setSearchPrompt)
                    .setTextLength(256)
                    .setBackgroundText("machine.universal.search")
                    .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
        });
    }

    @Override
    protected void mainDisplayTab(WidgetGroup widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        widgetGroup.addWidget(new ImageWidget(28, 112, 141, 18, DISPLAY));
        widgetGroup.addWidget(new TJTextFieldWidget(33, 117, 136, 18, false, this::getTickSpeed, this::setTickSpeed)
                .setTooltipText("machine.universal.tick.speed")
                .setTooltipFormat(this.workableHandler::getTickSpeedFormat)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new ClickButtonWidget(7, 112, 18, 18, "+", this::onIncrement));
        widgetGroup.addWidget(new ClickButtonWidget(172, 112, 18, 18, "-", this::onDecrement));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, () -> false, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    protected void handleLinkedDisplayClick(String componentData, String textId, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.equals("leftPage") && this.pageIndex > 0) {
            this.pageIndex -= this.pageSize;

        } else if (componentData.equals("rightPage") && this.pageIndex < this.workableHandler.getEntityLinkBlockPos().length - this.pageSize) {
            this.pageIndex += this.pageSize;

        } else if (componentData.startsWith("remove")) {
            String[] remove = componentData.split(":");
            int i = Integer.parseInt(remove[1]);
            int index = workableHandler.getLinkData().getInteger("I");
            this.linkData.setInteger("I", index + 1);
            this.entityLinkBlockPos[i] = null;
            this.entityLinkName[i] = null;
            this.updateEnergyPerTick();

        } else if (componentData.startsWith("rename")) {
            String[] rename = componentData.split(":");
            this.renamePrompt = rename[1];
            PlayerHolder holder = new PlayerHolder(player, this);
            holder.openUI();
        }
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (getOffsetTimer() > 100)
            this.workableHandler.update();
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(UUMatter.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        FieldGenCasing.CasingType fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV);
        EmitterCasing.CasingType emitter = context.getOrDefault("Emitter", EmitterCasing.CasingType.EMITTER_LV);
        this.tier = Math.min(fieldGen.getTier(), emitter.getTier());
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
    }

    @Override
    public void onRemoval() {
        if (!this.getWorld().isRemote)
            this.workableHandler.setLinkedEntitiesPos(null);
        super.onRemoval();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("#C#", "CEC", "#C#")
                .aisle("CEC", "EFE", "CEC")
                .aisle("#C#", "CSC", "#C#")
                .setAmountAtLeast('L', 2)
                .where('S', selfPredicate())
                .where('L', statePredicate(this.getCasingState()))
                .where('C', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', LargeSimpleRecipeMapMultiblockController.fieldGenPredicate())
                .where('E', LargeSimpleRecipeMapMultiblockController.emitterPredicate())
                .where('#', (tile) -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.TRITANIUM);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.TRITANIUM_CASING;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.AMPLIFAB_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), this.workableHandler.isActive());
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        String tileMode = "null";
        switch (this.workableHandler.getAcceleratorMode()) {
            case RANDOM_TICK:
                this.workableHandler.setAcceleratorMode(TILE_ENTITY);
                this.energyMultiplier = 1;
                this.entityLinkName = new String[this.tier];
                this.entityLinkBlockPos = new BlockPos[this.tier];
                tileMode = "gregtech.machine.world_accelerator.mode.tile";
                break;
             case TILE_ENTITY:
                 this.workableHandler.setAcceleratorMode(GT_TILE_ENTITY);
                 this.energyMultiplier = 64;
                 this.entityLinkName = new String[1];
                 this.entityLinkBlockPos = new BlockPos[1];
                 tileMode = "tj.multiblock.large_world_accelerator.mode.GT";
                 break;
            case GT_TILE_ENTITY:
                this.workableHandler.setAcceleratorMode(RANDOM_TICK);
                this.energyMultiplier = 1;
                this.entityLinkName = new String[this.tier];
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
        name = this.checkDuplicateNames(name, 1);
        this.entityLinkName[index] = name;
        this.entityLinkBlockPos[index] = pos;
    }

    private String checkDuplicateNames(String name, int count) {
        if (!Arrays.asList(this.entityLinkName).contains(name))
            return name;
        if (count > 1) {
            String[] split = name.split(" ");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < split.length - 1; i++) {
                builder.append(split[i]);
                if (i < split.length - 2)
                    builder.append(" ");
            }
            name = builder.toString();
        }
        name = name + " (" + count + ")";
        return this.checkDuplicateNames(name, ++count);
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
        this.workableHandler.setLinkData(linkData);
    }

    @Override
    public NBTTagCompound getLinkData() {
        return this.workableHandler.getLinkData();
    }

    @Override
    public void onLink(MetaTileEntity tileEntity) {
        this.workableHandler.updateEnergyPerTick();
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
        return this.workableHandler.getEnergyPerTick();
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

}
