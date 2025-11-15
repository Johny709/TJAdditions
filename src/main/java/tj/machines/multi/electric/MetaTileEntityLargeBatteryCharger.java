package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.CellCasing;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregtech.api.capability.*;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MTETrait;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.ArrayUtils;
import tj.builder.WidgetTabBuilder;
import tj.builder.handlers.BatteryChargerWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IParallelController;
import tj.capability.LinkEntity;
import tj.capability.LinkEvent;
import tj.capability.TJCapabilities;
import tj.gui.TJGuiTextures;
import tj.gui.uifactory.PlayerHolder;
import tj.gui.widgets.TJAdvancedTextWidget;
import tj.gui.widgets.TJClickButtonWidget;
import tj.gui.widgets.TJTextFieldWidget;
import tj.items.TJMetaItems;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static gregicadditions.GAMaterials.Talonite;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.MetaTileEntityBatteryTower.cellPredicate;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.unification.material.Materials.Nitrogen;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;
import static tj.builder.handlers.BatteryChargerWorkableHandler.TransferMode.INPUT;
import static tj.builder.handlers.BatteryChargerWorkableHandler.TransferMode.OUTPUT;
import static tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static tj.gui.TJGuiTextures.SPACES_BUTTON;

public class MetaTileEntityLargeBatteryCharger extends TJMultiblockDisplayBase implements LinkEntity, LinkEvent, IParallelController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, OUTPUT_ENERGY, IMPORT_FLUIDS, MAINTENANCE_HATCH};
    private final BatteryChargerWorkableHandler workableHandler = new BatteryChargerWorkableHandler(this);
    private int tier;
    private final int pageSize = 4;
    private int pageIndex;
    private IItemHandlerModifiable importItemHandler;
    private IItemHandlerModifiable exportItemHandler;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer inputEnergyContainer;
    private IEnergyContainer outputEnergyContainer;

    public MetaTileEntityLargeBatteryCharger(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.workableHandler.setImportItemsSupplier(this::getImportItemHandler)
                .setExportItemsSupplier(this::getExportItemHandler)
                .setImportFluidsSupplier(this::getImportFluidHandler)
                .setImportEnergySupplier(this::getInputEnergyContainer)
                .setExportEnergySupplier(this::getOutputEnergyContainer)
                .setTierSupplier(this::getTier)
                .setResetEnergy(false);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeBatteryCharger(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_battery_charger.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed())
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.inputEnergyContainer)
                    .voltageTier(this.tier)
                    .energyStored(this.getEnergyStored(), this.getEnergyCapacity())
                    .energyInput(this.hasEnoughEnergy(this.workableHandler.getEnergyPerTick()), this.workableHandler.getEnergyPerTick(), this.workableHandler.getMaxProgress())
                    .fluidInput(this.hasEnoughFluid(this.workableHandler.getFluidConsumption()), Nitrogen.getPlasma(this.workableHandler.getFluidConsumption()))
                    .custom(text -> {
                        text.add(new TextComponentTranslation("machine.universal.item.output.transfer")
                                .appendText(" ")
                                .appendSibling(this.workableHandler.isTransferToOutput() ? withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.enabled"), "transferEnabled")
                                        : withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.disabled"), "transferDisabled")));
                        text.add(new TextComponentTranslation("machine.universal.mode.transfer")
                                .appendText(" ")
                                .appendSibling(this.workableHandler.getTransferMode() == INPUT ? withButton(new TextComponentTranslation("machine.universal.mode.transfer.input"), "input")
                                        : withButton(new TextComponentTranslation("machine.universal.mode.transfer.output"), "output")));
                    }).isWorking(this.workableHandler.isWorkingEnabled(), this.workableHandler.isActive(), this.workableHandler.getProgress(), this.workableHandler.getMaxProgress());
    }

    private void addDisplayLinkedPlayersText(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + net.minecraft.util.text.translation.I18n.translateToLocal("machine.universal.linked.players") + "§r(§e" + this.searchResults + "§r/§e" + this.entityLinkName.length + "§r)"));
    }

    private void addDisplayLinkedPlayersText2(List<ITextComponent> textList) {
        int searchResults = 0;
        for (int i = 0; i < this.entityLinkName.length; i++) {
            String name = this.entityLinkName[i] != null ? this.entityLinkName[i] : net.minecraft.util.text.translation.I18n.translateToLocal("machine.universal.empty");
            String result = name, result2 = name;

            EntityPlayer player = this.linkedPlayers[i];
            String dimensionName = player != null ? player.world.provider.getDimensionType().getName() : "";
            int dimensionID = player != null ? this.getDimension(i) : 0;
            int x = player != null ? (int) player.posX : Integer.MIN_VALUE;
            int y = player != null ? (int) player.posY : Integer.MIN_VALUE;
            int z = player != null ? (int) player.posZ : Integer.MIN_VALUE;
            long totalEnergyStored = 0;
            long totalEnergyCapacity = 0;

            if (player != null) {
                for (ItemStack stack : player.inventory.armorInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored() / 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                for (ItemStack stack : player.inventory.mainInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored() / 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                for (ItemStack stack : player.inventory.offHandInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored() / 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }
            }

            textList.add(new TextComponentString(": [§a" + (++searchResults) + "§r] ")
                    .appendSibling(new TextComponentString(name))
                    .setStyle(new Style()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(name)
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.energy.stored", totalEnergyStored, totalEnergyCapacity)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.dimension", dimensionName, dimensionID)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.pos", x, y, z))))))
                    .appendText("\n")
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
        tabBuilder.addTab("tj.multiblock.tab.linked_entities_display", TJMetaItems.LINKING_DEVICE.getStackForm(), linkedPlayersTab -> {
            ScrollableListWidget scrollWidget = new ScrollableListWidget(10, -8, 178, 117) {
                @Override
                public boolean isWidgetClickable(Widget widget) {
                    return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
                }
            };
            scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addDisplayLinkedPlayersText2, 0xFFFFFF)
                    .addClickHandler(this::handleLinkedPlayersClick)
                    .setMaxWidthLimit(1000));
            linkedPlayersTab.addWidget(new AdvancedTextWidget(10, -20, this::addDisplayLinkedPlayersText, 0xFFFFFF));
            linkedPlayersTab.addWidget(scrollWidget);
            linkedPlayersTab.addWidget(new ToggleButtonWidget(172, 133, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                    .setTooltipText("machine.universal.case_sensitive"));
            linkedPlayersTab.addWidget(new ToggleButtonWidget(172, 151, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                    .setTooltipText("machine.universal.spaces"));
            linkedPlayersTab.addWidget(new ImageWidget(7, 112, 162, 18, DISPLAY));
            linkedPlayersTab.addWidget(new TJClickButtonWidget(172, 112, 18, 18, "", this::onClear)
                    .setTooltipText("machine.universal.toggle.clear")
                    .setButtonTexture(BUTTON_CLEAR_GRID));
            linkedPlayersTab.addWidget(new TJTextFieldWidget(12, 117, 157, 18, false, this::getSearchPrompt, this::setSearchPrompt)
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
                .setTooltipFormat(this::getTickSpeedFormat)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new ClickButtonWidget(7, 112, 18, 18, "+", this::onIncrement));
        widgetGroup.addWidget(new ClickButtonWidget(172, 112, 18, 18, "-", this::onDecrement));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, () -> false, this.workableHandler::setReset)
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

    private void handleLinkedPlayersClick(String componentData, String textId, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.equals("leftPage") && this.pageIndex > 0) {
            this.pageIndex -= this.pageSize;

        } else if (componentData.equals("rightPage") && this.pageIndex < this.linkedPlayers.length - this.pageSize) {
            this.pageIndex += this.pageSize;

        } else if (componentData.startsWith("remove")) {
            String[] remove = componentData.split(":");
            int i = Integer.parseInt(remove[1]);
            int index = this.workableHandler.getLinkData().getInteger("I");
            this.workableHandler.getLinkData().setInteger("I", index + 1);
            this.workableHandler.getEntityLinkName()[i] = null;
            this.workableHandler.getLinkedPlayers()[i] = null;
            this.workableHandler.getLinkedPlayersID()[i] = null;
            this.workableHandler.getEntityLinkWorld()[i] = Integer.MIN_VALUE;
            this.workableHandler.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("rename")) {
            String[] rename = componentData.split(":");
            this.renamePrompt = rename[1];
            PlayerHolder holder = new PlayerHolder(player, this);
            holder.openUI();
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "transferEnabled":
                this.workableHandler.setTransferToOutput(false);
                break;
            case "transferDisabled":
                this.workableHandler.setTransferToOutput(true);
                break;
            case "input":
                this.workableHandler.setTransferMode(OUTPUT);
                break;
            default:
                this.workableHandler.setTransferMode(INPUT);
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

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.importItemHandler = new ItemHandlerList(this.getAbilities(IMPORT_ITEMS));
        this.exportItemHandler = new ItemHandlerList(this.getAbilities(EXPORT_ITEMS));
        this.importFluidHandler = new FluidTankList(true, this.getAbilities(IMPORT_FLUIDS));
        this.inputEnergyContainer = new EnergyContainerList(this.getAbilities(INPUT_ENERGY));
        this.outputEnergyContainer = new EnergyContainerList(this.getAbilities(OUTPUT_ENERGY));
        this.tier = context.getOrDefault("CellType", CellCasing.CellType.CELL_EV).getTier();
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
        return FactoryBlockPattern.start()
                .aisle("HHHHH", "~HHH~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH" ,"HHHHH", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "HHHHH", "~BFB~", "~BFB~", "~BFB~", "~BFB~", "~BFB~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("HHHHH", "HHHHH", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "~HSH~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('H', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('B', cellPredicate())
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Talonite).getDefaultState()))
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TALONITE);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.TALONITE_CASING;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.getFrontFacing(), this.workableHandler.isActive());
    }

    @Override
    public long getEnergyStored() {
        return this.inputEnergyContainer != null && this.workableHandler.getTransferMode() == INPUT ? this.inputEnergyContainer.getEnergyStored()
                : this.outputEnergyContainer != null && this.workableHandler.getTransferMode() == OUTPUT ? this.outputEnergyContainer.getEnergyStored()
                : 0;
    }

    @Override
    public long getEnergyCapacity() {
        return this.inputEnergyContainer != null && this.workableHandler.getTransferMode() == INPUT ? this.inputEnergyContainer.getEnergyCapacity()
                : this.outputEnergyContainer != null && this.workableHandler.getTransferMode() == OUTPUT ? this.outputEnergyContainer.getEnergyCapacity()
                : 0;
    }

    @Override
    public long getMaxEUt() {
        return this.inputEnergyContainer != null && this.workableHandler.getTransferMode() == INPUT ? this.inputEnergyContainer.getInputVoltage()
                : this.outputEnergyContainer != null && this.workableHandler.getTransferMode() == OUTPUT ? this.outputEnergyContainer.getInputVoltage()
                : 0;
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergyConsumption() {
        return this.workableHandler.getTotalEnergyPerTick();
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
    }

    @Override
    public int dimensionID() {
        return this.getWorld().provider.getDimension();
    }

    @Override
    public boolean isInterDimensional() {
        return true;
    }

    @Override
    public int getDimension(int index) {
        return this.workableHandler.getLinkedPlayers()[index].world.provider.getDimension();
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return this.workableHandler.getLinkedPlayers().length;
    }

    @Override
    public Entity getEntity(int index) {
        return this.workableHandler.getLinkedPlayers()[index];
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        name = this.checkDuplicateNames(name, 1);
        this.workableHandler.getEntityLinkName()[index] = name;
        this.workableHandler.getEntityLinkWorld()[index] = world.provider.getDimension();
        this.workableHandler.getLinkedPlayers()[index] = player;
        this.workableHandler.getLinkedPlayersID()[index] = player.getUniqueID();
        this.workableHandler.updateTotalEnergyPerTick();
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
    public NBTTagCompound getLinkData() {
        return this.workableHandler.getLinkData();
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.workableHandler.setLinkData(linkData);
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
    public void onLink(MetaTileEntity tileEntity) {
        this.workableHandler.updateTotalEnergyPerTick();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_LINK_ENTITY)
            return TJCapabilities.CAPABILITY_LINK_ENTITY.cast(this);
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }

    private IItemHandlerModifiable getImportItemHandler() {
        return this.importItemHandler;
    }

    private IItemHandlerModifiable getExportItemHandler() {
        return this.exportItemHandler;
    }

    private IMultipleTankHandler getImportFluidHandler() {
        return this.importFluidHandler;
    }

    private IEnergyContainer getInputEnergyContainer() {
        return this.inputEnergyContainer;
    }

    private IEnergyContainer getOutputEnergyContainer() {
        return this.outputEnergyContainer;
    }

    public int getTier() {
        return this.tier;
    }
}
