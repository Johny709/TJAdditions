package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.metatileentity.MTETrait;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.builder.handlers.LargeWirelessEnergyWorkableHandler;
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
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static tj.gui.TJGuiTextures.SPACES_BUTTON;
import static gregicadditions.GAMaterials.Talonite;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.unification.material.Materials.Nitrogen;
import static gregtech.api.unification.material.Materials.RedSteel;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;

public class MetaTileEntityLargeWirelessEnergyEmitter extends TJMultiblockDisplayBase implements LinkPos, LinkEvent, IParallelController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_FLUIDS, INPUT_ENERGY, OUTPUT_ENERGY, MAINTENANCE_HATCH};
    protected final LargeWirelessEnergyWorkableHandler workableHandler = new LargeWirelessEnergyWorkableHandler(this);
    private final int pageSize = 4;

    protected TransferType transferType;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer inputEnergyContainer;
    private int tier;
    private int pageIndex;

    public MetaTileEntityLargeWirelessEnergyEmitter(ResourceLocation metaTileEntityId, TransferType transferType) {
        super(metaTileEntityId);
        this.transferType = transferType;
        this.workableHandler.setImportFluidsSupplier(this::getImportFluidHandler)
                .setImportEnergySupplier(this::getInputEnergyContainer)
                .setTierSupplier(this::getTier);
        this.reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWirelessEnergyEmitter(this.metaTileEntityId, this.transferType);
    }

    @Override
    @SideOnly(Side.CLIENT)
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
                    .energyInput(hasEnoughEnergy(this.workableHandler.getEnergyPerTick()), this.workableHandler.getEnergyPerTick(), this.workableHandler.getMaxProgress())
                    .fluidInput(hasEnoughFluid(this.workableHandler.getFluidConsumption()), Nitrogen.getPlasma(this.workableHandler.getFluidConsumption()))
                    .isWorking(this.workableHandler.isWorkingEnabled(), this.workableHandler.isActive(), this.workableHandler.getProgress(), this.workableHandler.getMaxProgress());

    }

    private void addDisplayLinkedEntitiesText(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + net.minecraft.util.text.translation.I18n.translateToLocal("tj.multiblock.large_world_accelerator.linked") + "§r(§e" + this.searchResults + "§r/§e" + this.entityLinkName.length + "§r)"));
    }

    private void addDisplayLinkedEntitiesText2(List<ITextComponent> textList) {
        int searchResults = 0;
        for (int i = 0; i < this.workableHandler.getEntityLinkName().length; i++) {
            String name = this.workableHandler.getEntityLinkName()[i] != null ? this.workableHandler.getEntityLinkName()[i] : net.minecraft.util.text.translation.I18n.translateToLocal("machine.universal.empty");
            String result = name, result2 = name;

            if (!this.isCaseSensitive) {
                result = result.toLowerCase();
                result2 = result2.toUpperCase();
            }

            if (!this.hasSpaces) {
                result = result.replace(" ", "");
                result2 = result2.replace(" ", "");
            }

            if (!result.isEmpty() && !result.contains(this.searchPrompt) && !result2.contains(this.searchPrompt))
                continue;

            BlockPos pos = this.workableHandler.getEntityLinkBlockPos()[i] != null ? this.workableHandler.getEntityLinkBlockPos()[i] : TJValues.DUMMY_POS;
            WorldServer world = DimensionManager.getWorld(this.workableHandler.getEntityLinkWorld()[i]);
            TileEntity getTileEntity = world != null ? world.getTileEntity(pos) : null;
            MetaTileEntity getMetaTileEntity = world != null ? BlockMachine.getMetaTileEntity(world, pos) : null;
            boolean isTileEntity = getTileEntity != null;
            boolean isMetaTileEntity = getMetaTileEntity != null;
            IEnergyStorage RFContainer = isTileEntity ? getTileEntity.getCapability(ENERGY, null) : null;
            long RFStored = RFContainer != null ? RFContainer.getEnergyStored() : 0;
            long RFCapacity = RFContainer != null ? RFContainer.getMaxEnergyStored() : 0;
            IEnergyContainer EUContainer = isMetaTileEntity ? getMetaTileEntity.getCapability(CAPABILITY_ENERGY_CONTAINER, null) : null;
            long EUStored = EUContainer != null ? EUContainer.getEnergyStored() : 0;
            long EUCapacity = EUContainer != null ? EUContainer.getEnergyCapacity() : 0;

            textList.add(new TextComponentString(": [§a" + (++searchResults) + "§r] ")
                    .appendSibling(new TextComponentString(name)).setStyle(new Style()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(name)
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.energy.stored", isMetaTileEntity ? EUStored : RFStored, isMetaTileEntity ? EUCapacity : RFCapacity)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.dimension", world != null ? world.provider.getDimensionType().getName() : "N/A", world != null ? world.provider.getDimension() : 0)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.pos", pos.getX(), pos.getY(), pos.getZ()))))))
                    .appendText("\n")
                    .appendSibling(new TextComponentTranslation("machine.universal.energy.amps", this.workableHandler.getEntityEnergyAmps()[i])
                            .appendText(" ")
                            .appendSibling(withButton(new TextComponentString("[+]"), "increment:" + i))
                            .appendText(" ")
                            .appendSibling(withButton(new TextComponentString("[-]"), "decrement:" + i)))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:" + i))
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
            scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addDisplayLinkedEntitiesText2, 0xFFFFFF)
                    .addClickHandler(this::handleLinkedDisplayClick)
                    .setMaxWidthLimit(1000));
            linkedEntitiesDisplayTab.addWidget(new AdvancedTextWidget(10, -20, this::addDisplayLinkedEntitiesText, 0xFFFFFF));
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
        widgetGroup.addWidget(new TJTextFieldWidget(33, 117, 136, 18, false, () -> String.valueOf(this.workableHandler.getMaxProgress()), maxProgress -> this.workableHandler.setMaxProgress(maxProgress.isEmpty() ? 1 : Integer.parseInt(maxProgress)))
                .setTooltipText("machine.universal.tick.speed")
                .setTooltipFormat(() -> ArrayUtils.toArray(String.valueOf(this.workableHandler.getMaxProgress())))
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new ClickButtonWidget(7, 112, 18, 18, "+", click -> this.workableHandler.setMaxProgress(MathHelper.clamp(this.workableHandler.getMaxProgress() * 2, 1, Integer.MAX_VALUE))));
        widgetGroup.addWidget(new ClickButtonWidget(172, 112, 18, 18, "-", click -> this.workableHandler.setMaxProgress(MathHelper.clamp(this.workableHandler.getMaxProgress() / 2, 1, Integer.MAX_VALUE))));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, () -> false, this.workableHandler::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    protected void handleLinkedDisplayClick(String componentData, String textId, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.equals("leftPage") && this.pageIndex > 0) {
            this.pageIndex -= this.pageSize;

        } else if (componentData.equals("rightPage") && this.pageIndex < this.workableHandler.getEntityLinkBlockPos().length - this.pageSize) {
            this.pageIndex += this.pageSize;

        } else  if (componentData.startsWith("increment")) {
            String[] increment = componentData.split(":");
            int i = Integer.parseInt(increment[1]);
            this.workableHandler.getEntityEnergyAmps()[i] = MathHelper.clamp(this.workableHandler.getEntityEnergyAmps()[i] + 1, 0, 256);
            this.workableHandler.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("decrement")) {
            String[] decrement = componentData.split(":");
            int i = Integer.parseInt(decrement[1]);
            this.workableHandler.getEntityEnergyAmps()[i] = MathHelper.clamp(this.workableHandler.getEntityEnergyAmps()[i] - 1, 0, 256);
            this.workableHandler.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("remove")) {
            String[] remove = componentData.split(":");
            int i = Integer.parseInt(remove[1]);
            int j = this.workableHandler.getLinkData().getInteger("I");
            this.workableHandler.getLinkData().setInteger("I", j + 1);
            this.workableHandler.getEntityLinkName()[i] = null;
            this.workableHandler.getEntityLinkBlockPos()[i] = null;
            this.workableHandler.getEntityLinkWorld()[i] = Integer.MIN_VALUE;
            this.workableHandler.getEntityEnergyAmps()[i] = 0;
            this.workableHandler.updateTotalEnergyPerTick();

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
        this.workableHandler.update();
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
        else return GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.RED_STEEL);
    }

    public IBlockState getFrameState(TransferType transferType) {
        if (transferType == TransferType.INPUT)
            return MetaBlocks.FRAMES.get(Talonite).getDefaultState();
        else return MetaBlocks.FRAMES.get(RedSteel).getDefaultState();
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int framework = 0, framework2 = 0;
        if (context.get("framework") instanceof GAMultiblockCasing.CasingType) {
            framework = ((GAMultiblockCasing.CasingType) context.get("framework")).getTier();
        }
        if (context.get("framework2") instanceof GAMultiblockCasing2.CasingType) {
            framework2 = ((GAMultiblockCasing2.CasingType) context.get("framework2")).getTier();
        }
        this.tier = Math.max(framework, framework2);
        this.inputEnergyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.workableHandler.initialize(this.transferType.ordinal());
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.TALONITE_CASING;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), this.workableHandler.isActive());
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
        return this.workableHandler.getEnergyPerTick();
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
    }

    @Override
    public void onLink(MetaTileEntity tileEntity) {
        this.workableHandler.onLink(tileEntity);
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
        return this.workableHandler.getEntityLinkWorld()[index];
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return this.workableHandler.getEntityLinkBlockPos().length;
    }

    @Override
    public BlockPos getPos(int i) {
        return this.workableHandler.getEntityLinkBlockPos()[i];
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        name = this.checkDuplicateNames(name, 1);
        this.workableHandler.getEntityLinkName()[index] = name;
        this.workableHandler.getEntityLinkWorld()[index] = world.provider.getDimension();
        this.workableHandler.getEntityEnergyAmps()[index] = 1;
        this.workableHandler.getEntityLinkBlockPos()[index] = pos;
    }

    private String checkDuplicateNames(String name, int count) {
        if (!Arrays.asList(this.workableHandler.getEntityLinkName()).contains(name))
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
        this.workableHandler.setLinkData(linkData);
        this.markDirty();
    }

    @Override
    public NBTTagCompound getLinkData() {
        return this.workableHandler.getLinkData();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_LINK_POS)
            return TJCapabilities.CAPABILITY_LINK_POS.cast(this);
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.workableHandler.setWorkingEnabled(isActivationAllowed);
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.workableHandler.isWorkingEnabled();
    }

    private IEnergyContainer getInputEnergyContainer() {
        return this.inputEnergyContainer;
    }

    private IMultipleTankHandler getImportFluidHandler() {
        return this.importFluidHandler;
    }

    public int getTier() {
        return this.tier;
    }

    public enum TransferType {
        INPUT,
        OUTPUT
    }
}
