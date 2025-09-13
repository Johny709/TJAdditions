package tj.machines.multi.electric;

import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.stats.IMetaItemStats;
import gregtech.common.items.behaviors.TurbineRotorBehavior;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.builder.WidgetTabBuilder;
import tj.builder.handlers.XLTurbineWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJRotorHolderMultiblockControllerBase;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GAMetaItems;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.util.function.BooleanConsumer;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import tj.gui.widgets.TJSlotWidget;
import tj.items.behaviours.TurbineUpgradeBehaviour;
import tj.items.handlers.TurbineUpgradeStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;


public class MetaTileEntityXLTurbine extends TJRotorHolderMultiblockControllerBase {

    public final MetaTileEntityLargeTurbine.TurbineType turbineType;
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH, GregicAdditionsCapabilities.STEAM};
    public static final int BASE_PARALLEL = 12;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    private int pageIndex;
    private final int pageSize = 10;
    private int parallels = BASE_PARALLEL;
    private boolean hasChanged;
    private XLTurbineWorkableHandler xlTurbineWorkableHandler;
    private BooleanConsumer fastModeConsumer;

    public MetaTileEntityXLTurbine(ResourceLocation metaTileEntityId, MetaTileEntityLargeTurbine.TurbineType turbineType) {
        super(metaTileEntityId, turbineType.recipeMap, GTValues.V[4]);
        this.turbineType = turbineType;
        this.reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityXLTurbine(this.metaTileEntityId, this.turbineType);
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        this.xlTurbineWorkableHandler = new XLTurbineWorkableHandler(this, this.recipeMap, () -> this.energyContainer, () -> this.importFluidHandler);
        this.fastModeConsumer = this.xlTurbineWorkableHandler::setFastMode;
        return this.xlTurbineWorkableHandler;
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new TurbineUpgradeStackHandler(this)
                .setOnContentsChanged((stack, insert) -> {
                    if (this.getWorld() != null && !this.getWorld().isRemote && !this.hasChanged) {
                        this.hasChanged = true;
                        this.parallels = BASE_PARALLEL;
                        Item item = stack.getItem();
                        if (insert && item instanceof MetaItem<?>)
                            this.parallels += ((TurbineUpgradeBehaviour) ((MetaItem<?>) item).getItem(stack).getAllStats().get(0)).getExtraParallels();
                        this.writeCustomData(10, buf -> buf.writeInt(this.parallels));
                        this.writeCustomData(11, buf -> buf.writeBoolean(this.hasChanged));
                        if (this.isStructureFormed())
                            this.invalidateStructure();
                        this.structurePattern = this.createStructurePattern();
                    }
                });
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.turbine.description"));
        tooltip.add(I18n.format("tj.multiblock.turbine.fast_mode.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", this.turbineType.recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", 12));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency"));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.normal", (int) XLTurbineWorkableHandler.getTurbineBonus()));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.fast", 100));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed()) {
            MultiblockDisplayBuilder.start(textList)
                    .custom(text -> {
                        text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.consuming.seconds", this.xlTurbineWorkableHandler.getConsumption(),
                                net.minecraft.util.text.translation.I18n.translateToLocal(this.xlTurbineWorkableHandler.getFuelName()),
                                this.xlTurbineWorkableHandler.getMaxProgress() / 20)));
                        FluidStack fuelStack = this.xlTurbineWorkableHandler.getFuelStack();
                        int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

                        ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
                        text.add(new TextComponentTranslation("tj.multiblock.fuel_amount", fuelAmount, fuelName));

                        text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.energy", this.xlTurbineWorkableHandler.getProduction())));

                        text.add(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode").appendText(" ")
                                .appendSibling(this.xlTurbineWorkableHandler.isFastMode() ? withButton(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode.true"), "true")
                                        : withButton(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode.false"), "false")));
                    })
                    .isWorking(this.xlTurbineWorkableHandler.isWorkingEnabled(), this.xlTurbineWorkableHandler.isActive(), this.xlTurbineWorkableHandler.getProgress(), this.xlTurbineWorkableHandler.getMaxProgress());
        }
    }

    private void addRotorDisplayText(List<ITextComponent> textList) {
        ITextComponent page = new TextComponentString(":");
        page.appendText(" ");
        page.appendSibling(withButton(new TextComponentString("[<]"), "leftPage"));
        page.appendText(" ");
        page.appendSibling(withButton(new TextComponentString("[>]"), "rightPage"));
        textList.add(page);

        int rotorHolderSize = getRotorHolders().size();
        for (int i = this.pageIndex, rotorIndex = i + 1; i < this.pageIndex + this.pageSize; i++, rotorIndex++) {
            if (i < rotorHolderSize) {
                MetaTileEntityRotorHolder rotorHolder = this.getAbilities(ABILITY_ROTOR_HOLDER).get(i);

                double durability = rotorHolder.getRotorDurability() * 100;
                double efficiency = rotorHolder.getRotorEfficiency() * 100;

                String colorText = !rotorHolder.hasRotorInInventory() ? "§f"
                        : durability > 25 ? "§a"
                        : durability > 10 ? "§e" : "§c";

                String rotorName = rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName();
                String shortRotorName = rotorName.length() > 26 ? rotorName.substring(0, 26) + "..." : rotorName;
                textList.add(new TextComponentString("-")
                        .appendText(" ")
                        .appendSibling(new TextComponentString(colorText + "[" + rotorIndex + "] " + (shortRotorName.equals("Air") ? net.minecraft.util.text.translation.I18n.translateToLocal("tj.multiblock.extreme_turbine.insertrotor") : shortRotorName)))
                        .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.name", rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName().equals("Air") ?
                                net.minecraft.util.text.translation.I18n.translateToLocal("gregtech.multiblock.extreme_turbine.norotor") :
                                rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName()))
                                .appendText("\n")
                                .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.parallel.status", net.minecraft.util.text.translation.I18n.translateToLocalFormatted(rotorHolder.isFrontFaceFree() ? "tj.multiblock.extreme_turbine.obstructed.not"
                                        : "tj.multiblock.extreme_turbine.obstructed"))))
                                .appendText("\n")
                                .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.speed", rotorHolder.getCurrentRotorSpeed(), rotorHolder.getMaxRotorSpeed())))
                                .appendText("\n")
                                .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.efficiency", (int) efficiency)))
                                .appendText("\n")
                                .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.durability", (int) durability)))))));
            }
        }
    }

    private void handleRotorDisplayClick(String componentData, Widget.ClickData clickData) {
        if (componentData.equals("leftPage")) {
            if (this.pageIndex > 0)
                this.pageIndex -= this.pageSize;
        } else {
            if (this.pageIndex < this.getRotorHolders().size() - this.pageSize)
                this.pageIndex += this.pageSize;
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        this.fastModeConsumer.apply(componentData.equals("false"));
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        boolean hasOutputEnergy = abilities.containsKey(MultiblockAbility.OUTPUT_ENERGY);
        boolean hasInputFluid = abilities.containsKey(MultiblockAbility.IMPORT_FLUIDS);
        boolean hasSteamInput = abilities.containsKey(GregicAdditionsCapabilities.STEAM);

        if (this.turbineType != MetaTileEntityLargeTurbine.TurbineType.STEAM && hasSteamInput)
            return false;

        return super.checkStructureComponents(parts, abilities) && hasOutputEnergy && (hasInputFluid || hasSteamInput);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        List<IFluidTank> fluidTanks = new ArrayList<>();
        fluidTanks.addAll(this.getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        fluidTanks.addAll(this.getAbilities(GregicAdditionsCapabilities.STEAM));

        this.importFluidHandler = new FluidTankList(true, fluidTanks);
        this.exportFluidHandler = new FluidTankList(true, this.getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.importItemHandler = new ItemHandlerList(this.getAbilities(MultiblockAbility.IMPORT_ITEMS));
    }

    @Override
    protected void checkStructurePattern() {
        super.checkStructurePattern();
        if (this.hasChanged) {
            this.hasChanged = false;
            this.writeCustomData(11, buf -> buf.writeBoolean(this.hasChanged));
        }
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.exportFluidHandler = null;
        this.importItemHandler = new ItemHandlerList(Collections.emptyList());
    }

    @Override
    protected void updateFormedValid() {
        super.updateFormedValid();
        if (this.isStructureFormed() && this.getOffsetTimer() % 20 == 0) {
            for (MetaTileEntityRotorHolder rotorHolder : this.getAbilities(ABILITY_ROTOR_HOLDER)) {
                if (rotorHolder.hasRotorInInventory())
                    continue;
                ItemStack rotorStack = this.checkAndConsumeItem();
                if (rotorStack != null) {
                    rotorHolder.getRotorInventory().setStackInSlot(0, rotorStack);
                    rotorHolder.markDirty();
                }
            }
        }
    }

    private ItemStack checkAndConsumeItem() {
        int getItemSlots = this.importItemHandler.getSlots();
        for (int slotIndex = 0; slotIndex < getItemSlots; slotIndex++) {
            ItemStack stack = this.importItemHandler.getStackInSlot(slotIndex);
            Item item = stack.getItem();
            if (item instanceof MetaItem<?>) {
                MetaItem<?>.MetaValueItem metaItem = ((MetaItem<?>) item).getItem(stack);
                if (metaItem != null) {
                    List<IMetaItemStats> stats = metaItem.getAllStats();
                    if (!stats.isEmpty() && stats.get(0) instanceof TurbineRotorBehavior) {
                        this.importItemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                        this.markDirty();
                        return stack;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int getRotorSpeedIncrement() {
        return 3;
    }

    @Override
    public int getRotorSpeedDecrement() {
        return -1;
    }

    @Override
    public boolean isRotorFaceFree() {
        return true;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return this.turbineType == null ? null : this.getStructurePattern();
    }

    private BlockPattern getStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK)
                .aisle("CCCCCCC", "CHHHHHC", "CHHHHHC", "CHHHHHC", "CHHHHHC", "CHHHHHC", "CCCCCCC")
                .aisle("CHHHHHC", "R#####R", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####R", "CHHHHHC")
                .aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC");
        for (int i = 0; i < (this.parallels / 4) - 2; i++) {
            factoryPattern.aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC");
            factoryPattern.aisle("CHHHHHC", "R#####R", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####R", "CHHHHHC");
            factoryPattern.aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC");
        }
        return factoryPattern.aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC")
                .aisle("CHHHHHC", "R#####R", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####R", "CHHHHHC")
                .aisle("CCCCCCC", "CHHHHHC", "CHHHHHC", "CHHSHHC", "CHHHHHC", "CHHHHHC", "CCCCCCC")
                .where('S', this.selfPredicate())
                .where('#', isAirPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('H', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
                .build();
    }

    @Override
    public boolean canShare() {
        return false;
    }

    public IBlockState getCasingState() {
        return this.turbineType.casingState;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return this.turbineType.casingRenderer;
    }

    @Deprecated
    public boolean isTurbineFaceFree() {
        return this.isRotorFaceFree();
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return this.turbineType.frontOverlay;
    }

    @Override
    protected void addTabs(WidgetTabBuilder tabBuilder) {
        super.addTabs(tabBuilder);
        tabBuilder.addTab("tj.multiblock.tab.rotor", GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm(), rotorTab -> rotorTab.addWidget(new AdvancedTextWidget(10, -2, this::addRotorDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleRotorDisplayClick)));
    }

    @Override
    protected void mainDisplayTab(WidgetGroup widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        widgetGroup.addWidget(new TJSlotWidget(this.importItems, 0, 172, 191, true, true)
                .setTakeItemsPredicate(() -> !this.hasChanged)
                .setPutItemsPredicate(() -> !this.hasChanged));
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.xlTurbineWorkableHandler.isWorkingEnabled();
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        this.xlTurbineWorkableHandler.setWorkingEnabled(isWorking);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("parallels", this.parallels);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("parallels")) {
            this.parallels = data.getInteger("parallels");
            this.structurePattern = this.createStructurePattern();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeInt(this.parallels);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.parallels = buf.readInt();
        this.structurePattern = this.createStructurePattern();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 10) {
            this.parallels = buf.readInt();
            this.structurePattern = this.createStructurePattern();
            this.scheduleRenderUpdate();
        } else if (dataId == 11) {
            this.hasChanged = buf.readBoolean();
            this.scheduleRenderUpdate();
        }
    }
}
