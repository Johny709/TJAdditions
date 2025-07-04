package tj.machines.multi.electric;

import tj.builder.handlers.XLHotCoolantTurbineWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.MultiblockDisplaysUtility;
import tj.gui.TJGuiTextures;
import tj.gui.TJHorizontoalTabListRenderer;
import tj.gui.TJTabGroup;
import tj.gui.TJWidgetGroup;
import gregicadditions.GAConfig;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GAMetaItems;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.IMaintenance;
import gregicadditions.machines.multi.impl.HotCoolantRecipeLogic;
import gregicadditions.machines.multi.impl.MetaTileEntityRotorHolderForNuclearCoolant;
import gregicadditions.machines.multi.multiblockpart.MetaTileEntityMaintenanceHatch;
import gregicadditions.machines.multi.nuclear.MetaTileEntityHotCoolantTurbine;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.util.Position;
import gregtech.api.util.XSTR;
import gregtech.api.util.function.BooleanConsumer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static gregicadditions.capabilities.MultiblockDataCodes.STORE_TAPED;
import static gregicadditions.client.ClientHandler.MARAGING_STEEL_250_CASING;
import static gregicadditions.item.GAMetaBlocks.METAL_CASING_1;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public class MetaTileEntityXLHotCoolantTurbine extends MetaTileEntityHotCoolantTurbine implements IMaintenance {

    public final MetaTileEntityHotCoolantTurbine.TurbineType turbineType;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    private int pageIndex;
    private final int pageSize = 6;
    private XLHotCoolantTurbineWorkableHandler xlHotCoolantTurbineWorkableHandler;
    protected boolean doStructureCheck;
    private BooleanConsumer fastModeConsumer;

    /**
     * This value stores whether each of the 5 maintenance problems have been fixed.
     * A value of 0 means the problem is not fixed, else it is fixed
     * Value positions correspond to the following from left to right: 0=Wrench, 1=Screwdriver, 2=Soft Hammer, 3=Hard Hammer, 4=Wire Cutter, 5=Crowbar
     */
    protected byte maintenance_problems;

    public static final XSTR XSTR_RAND = new XSTR();
    private int timeActive;
    private static final int minimumMaintenanceTime = 5184000; // 72 real-life hours = 5184000 ticks

    // Used for data preservation with Maintenance Hatch
    private boolean storedTaped = false;

    public MetaTileEntityXLHotCoolantTurbine(ResourceLocation metaTileEntityId, MetaTileEntityHotCoolantTurbine.TurbineType turbineType) {
        super(metaTileEntityId, turbineType);
        this.turbineType = turbineType;
        this.reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityXLHotCoolantTurbine(this.metaTileEntityId, this.turbineType);
    }

    @Override
    protected HotCoolantRecipeLogic createWorkable(long maxVoltage) {
        this.xlHotCoolantTurbineWorkableHandler = new XLHotCoolantTurbineWorkableHandler(this, this.recipeMap, () -> this.energyContainer, () -> this.importFluidHandler);
        this.fastModeConsumer = xlHotCoolantTurbineWorkableHandler::setFastMode;
        return xlHotCoolantTurbineWorkableHandler;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.turbine.description"));
        tooltip.add(I18n.format("tj.multiblock.turbine.fast_mode.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", this.turbineType.recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", 12));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency"));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.normal", (int) XLHotCoolantTurbineWorkableHandler.getTurbineBonus()));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.fast",  100));
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        boolean canForm = super.checkStructureComponents(parts, abilities);
        if (!canForm)
            return false;

        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();

        return maintenanceCount == 1;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (this.isStructureFormed()) {
            MultiblockDisplayBuilder.start(textList)
                    .custom(text -> {
                        text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.consuming.seconds", this.xlHotCoolantTurbineWorkableHandler.getConsumption(),
                                net.minecraft.util.text.translation.I18n.translateToLocal(this.xlHotCoolantTurbineWorkableHandler.getFuelName()),
                                this.xlHotCoolantTurbineWorkableHandler.getMaxProgress() / 20)));
                        FluidStack fuelStack = this.xlHotCoolantTurbineWorkableHandler.getFuelStack();
                        int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

                        ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
                        text.add(new TextComponentTranslation("tj.multiblock.fuel_amount", fuelAmount, fuelName));

                        text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.energy", this.xlHotCoolantTurbineWorkableHandler.getProduction())));

                        text.add(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode").appendText(" ")
                                .appendSibling(this.xlHotCoolantTurbineWorkableHandler.isFastMode() ? withButton(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode.true"), "true")
                                        : withButton(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode.false"), "false")));
                    })
                    .isWorking(this.xlHotCoolantTurbineWorkableHandler.isWorkingEnabled(), this.xlHotCoolantTurbineWorkableHandler.isActive(), this.xlHotCoolantTurbineWorkableHandler.getProgress(), this.xlHotCoolantTurbineWorkableHandler.getMaxProgress());
        } else {
            MultiblockDisplaysUtility.isInvalid(textList, isStructureFormed());
        }
    }

    private void addRotorDisplayText(List<ITextComponent> textList) {
        ITextComponent page = new TextComponentString(":");
        page.appendText(" ");
        page.appendSibling(withButton(new TextComponentString("[<]"), "leftPage"));
        page.appendText(" ");
        page.appendSibling(withButton(new TextComponentString("[>]"), "rightPage"));
        textList.add(page);

        int rotorHolderSize = getAbilities(ABILITY_ROTOR_HOLDER).size();
        for (int i = this.pageIndex, rotorIndex = i + 1; i < this.pageIndex + this.pageSize; i++, rotorIndex++) {
            if (i < rotorHolderSize) {
                MetaTileEntityRotorHolderForNuclearCoolant rotorHolder = this.getAbilities(ABILITY_ROTOR_HOLDER).get(i);

                double durability = rotorHolder.getRotorDurability() * 100;
                double efficiency = rotorHolder.getRotorEfficiency() * 100;

                String colorText = !rotorHolder.hasRotorInInventory() ? "§f"
                        : durability > 25 ? "§a"
                        : durability > 10 ? "§e" : "§c";

                String rotorName = rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName();
                String shortRotorName = rotorName.length() > 27 ? rotorName.substring(0, 27) + "..." : rotorName;
                textList.add(new TextComponentString("-")
                        .appendText(" ")
                        .appendSibling(new TextComponentString(colorText + "[" + rotorIndex + "] " + (shortRotorName.equals("Air") ? net.minecraft.util.text.translation.I18n.translateToLocal("tj.multiblock.extreme_turbine.insertrotor") : shortRotorName)))
                        .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.name", rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName().equals("Air") ?
                                net.minecraft.util.text.translation.I18n.translateToLocal("gregtech.multiblock.extreme_turbine.norotor") :
                                rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName()))
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
            if (this.pageIndex < this.getAbilities(ABILITY_ROTOR_HOLDER).size() - this.pageSize)
                this.pageIndex += this.pageSize;
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        this.fastModeConsumer.apply(componentData.equals("false"));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.exportFluidHandler = new FluidTankList(true, this.getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.importItemHandler = new ItemHandlerList(this.getAbilities(MultiblockAbility.IMPORT_ITEMS));
        if (this.getAbilities(GregicAdditionsCapabilities.MAINTENANCE_HATCH).isEmpty())
            return;
        MetaTileEntityMaintenanceHatch maintenanceHatch = this.getAbilities(GregicAdditionsCapabilities.MAINTENANCE_HATCH).get(0);
        if (maintenanceHatch.getType() == 2 || !GAConfig.GT5U.enableMaintenance) {
            this.maintenance_problems = 0b111111;
        } else {
            this.readMaintenanceData(maintenanceHatch);
            if (maintenanceHatch.getType() == 0 && storedTaped) {
                maintenanceHatch.setTaped(true);
                this.storeTaped(false);
            }
        }
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.exportFluidHandler = null;
        this.importItemHandler = new ItemHandlerList(Collections.emptyList());
    }

    public boolean isActive() {
        return this.isTurbineFaceFree() && this.workableHandler.isActive() && this.workableHandler.isWorkingEnabled();
    }

    @Override
    protected void updateFormedValid() {
        super.updateFormedValid();
        if (this.isStructureFormed()) {
            if (this.getOffsetTimer() % 20 == 0) {
                for (MetaTileEntityRotorHolderForNuclearCoolant rotorHolder : this.getAbilities(ABILITY_ROTOR_HOLDER)) {
                    if (rotorHolder.hasRotorInInventory())
                        continue;
                    ItemStack rotorReplacementStack = this.checkAndConsumeItem();
                    if (rotorReplacementStack != null) {
                        rotorHolder.getRotorInventory().setStackInSlot(0, rotorReplacementStack);
                    }
                }
            }
        }
    }

    private ItemStack checkAndConsumeItem() {
        int getItemSlots = this.importItemHandler.getSlots();
        for (int slotIndex = 0; slotIndex < getItemSlots; slotIndex++) {
            ItemStack item = this.importItemHandler.getStackInSlot(slotIndex);
            boolean hugeRotorStack = GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean largeRotorStack = GAMetaItems.LARGE_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean mediumRotorStack = GAMetaItems.MEDIUM_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean smallRotorStack = GAMetaItems.SMALL_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);

            // check if slot has either small, medium, large, huge rotor. if not then skip to next slot
            if(!hugeRotorStack && !largeRotorStack && !mediumRotorStack && !smallRotorStack)
                continue;

            ItemStack getItemFromSlot = item.getItem().getContainerItem(item);
            item.setCount(0); // sets stacksize to 0. effectively voiding the item
            this.importItemHandler.setStackInSlot(slotIndex, item);
            return getItemFromSlot;
        }
        return null;
    }

    @Override
    public boolean isTurbineFaceFree() {
        return true;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return this.turbineType == null ? null :
                FactoryBlockPattern.start()
                        .aisle("CCCCCCC", "CHHHHHC", "CHHHHHC", "CHHHHHC", "CHHHHHC", "CHHHHHC", "CCCCCCC")
                        .aisle("CHHHHHC", "R#####R", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####R", "CHHHHHC")
                        .aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC")
                        .aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC")
                        .aisle("CHHHHHC", "R#####R", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####R", "CHHHHHC")
                        .aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC")
                        .aisle("CHHHHHC", "CCCCCCC", "CCCCCCC", "HCCCCCH", "CCCCCCC", "CCCCCCC", "CHHHHHC")
                        .aisle("CHHHHHC", "R#####R", "CCCCCCC", "HCCCCCH", "CCCCCCC", "R#####R", "CHHHHHC")
                        .aisle("CCCCCCC", "CHHHHHC", "CHHHHHC", "CHHSHHC", "CHHHHHC", "CHHHHHC", "CCCCCCC")
                        .where('S', this.selfPredicate())
                        .where('#', isAirPredicate())
                        .where('C', statePredicate(this.getCasingState()))
                        .where('H', statePredicate(this.getCasingState()).or(abilityPartPredicate(getAllowedAbilities())))
                        .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
                        .build();
    }

    @Override
    public MultiblockAbility[] getAllowedAbilities() {
        return this.turbineType.hasOutputHatch ?
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH} :
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    }

    public IBlockState getCasingState() {
        return METAL_CASING_1.getState(MetalCasing1.CasingType.MARAGING_STEEL_250);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return MARAGING_STEEL_250_CASING;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setByte("Maintenance", this.maintenance_problems);
        data.setInteger("ActiveTimer", this.timeActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.maintenance_problems = data.getByte("Maintenance");
        this.timeActive = data.getInteger("ActiveTimer");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeByte(this.maintenance_problems);
        buf.writeInt(this.timeActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.maintenance_problems = buf.readByte();
        this.timeActive = buf.readInt();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == STORE_TAPED) {
            this.storedTaped = buf.readBoolean();
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(-10, 0, 195, 217, TJGuiTextures.NEW_MULTIBLOCK_DISPLAY);
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT ,-3, 134);
        builder.widget(new LabelWidget(0, 7, getMetaFullName(), 0xFFFFFF));

        TJTabGroup tabGroup = new TJTabGroup(() -> new TJHorizontoalTabListRenderer(TJHorizontoalTabListRenderer.HorizontalStartCorner.LEFT, TJHorizontoalTabListRenderer.VerticalLocation.BOTTOM), new Position(-10, 1));
        List<Triple<String, ItemStack, AbstractWidgetGroup>> tabList = new ArrayList<>();
        addNewTabs(tabList::add);
        tabList.forEach(tabs -> tabGroup.addTab(new ItemTabInfo(tabs.getLeft(), tabs.getMiddle()), tabs.getRight()));
        builder.widget(tabGroup);
        return builder;
    }

    private void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        TJWidgetGroup widgetDisplayGroup = new TJWidgetGroup(), widgetMaintenanceGroup = new TJWidgetGroup(), rotorWidgetGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.display", this.getStackForm(), mainDisplayTab(widgetDisplayGroup::addWidgets)));
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.maintenance", GATileEntities.MAINTENANCE_HATCH[0].getStackForm(), this.maintenanceTab(widgetMaintenanceGroup::addWidgets)));
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.rotor", GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm(), this.rotorTab(rotorWidgetGroup::addWidgets)));
    }

    private AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
        widgetGroup.apply(new ToggleButtonWidget(172, 169, 18, 18, TJGuiTextures.POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                .setTooltipText("machine.universal.toggle.run.mode"));
        return widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, TJGuiTextures.CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
    }

    private AbstractWidgetGroup maintenanceTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addMaintenanceDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    private AbstractWidgetGroup rotorTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addRotorDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleRotorDisplayClick));
    }

    private void addMaintenanceDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.maintenanceDisplay(textList, this.maintenance_problems, hasProblems());
    }

    public boolean isWorkingEnabled() {
        return this.xlHotCoolantTurbineWorkableHandler.isWorkingEnabled();
    }

    public void setWorkingEnabled(boolean isWorking) {
        this.xlHotCoolantTurbineWorkableHandler.setWorkingEnabled(isWorking);
        this.markDirty();
    }

    private boolean getDoStructureCheck() {
        if (isStructureFormed())
            this.doStructureCheck = false;
        return this.doStructureCheck;
    }

    private void setDoStructureCheck(boolean check) {
        if (isStructureFormed()) {
            this.doStructureCheck = true;
            this.invalidateStructure();
            this.structurePattern = this.createStructurePattern();
        }
    }

    /**
     * Sets the maintenance problem corresponding to index to fixed
     *
     * @param index the index of the maintenance problem
     */
    public void setMaintenanceFixed(int index) {
        this.maintenance_problems |= 1 << index;
    }

    /**
     * Used to cause a single random maintenance problem
     */
    protected void causeProblems() {
        this.maintenance_problems &= ~(1 << ((int) (XSTR_RAND.nextFloat()*5)));
    }

    /**
     *
     * @return the byte value representing the maintenance problems
     */
    public byte getProblems() {
        return this.maintenance_problems;
    }

    /**
     *
     * @return the amount of maintenance problems the multiblock has
     */
    public int getNumProblems() {
        return 6 - Integer.bitCount(this.maintenance_problems);
    }

    /**
     *
     * @return whether the multiblock has any maintenance problems
     */
    public boolean hasProblems() {
        return this.maintenance_problems < 63;
    }

    /**
     * Used to calculate whether a maintenance problem should happen based on machine time active
     * @param duration duration in ticks to add to the counter of active time
     */

    public void calculateMaintenance(int duration) {
        MetaTileEntityMaintenanceHatch maintenanceHatch = getAbilities(GregicAdditionsCapabilities.MAINTENANCE_HATCH).get(0);
        if (maintenanceHatch.getType() == 2 || !GAConfig.GT5U.enableMaintenance) {
            return;
        }

        this.timeActive += duration;
        if (minimumMaintenanceTime - this.timeActive <= 0)
            if(XSTR_RAND.nextFloat() - 0.75f >= 0) {
                this.causeProblems();
                maintenanceHatch.setTaped(false);
            }
    }

    private void readMaintenanceData(MetaTileEntityMaintenanceHatch hatch) {
        if (hatch.hasMaintenanceData()) {
            Tuple<Byte, Integer> data = hatch.readMaintenanceData();
            this.maintenance_problems = data.getFirst();
            this.timeActive = data.getSecond();
        }
    }

    public void storeTaped(boolean isTaped) {
        this.storedTaped = isTaped;
        this.writeCustomData(STORE_TAPED, buf -> buf.writeBoolean(isTaped));
    }

}
