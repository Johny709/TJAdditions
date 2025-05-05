package com.johny.tj.machines.multi.electric;

import com.johny.tj.builder.handlers.XLTurbineWorkableHandler;
import com.johny.tj.builder.multicontrollers.MultiblockDisplaysUtility;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJHorizontoalTabListRenderer;
import com.johny.tj.gui.TJTabGroup;
import com.johny.tj.gui.TJWidgetGroup;
import gregicadditions.GAConfig;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GAMetaItems;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.IMaintenance;
import gregicadditions.machines.multi.multiblockpart.MetaTileEntityMaintenanceHatch;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.FuelRecipeLogic;
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
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.util.Position;
import gregtech.api.util.XSTR;
import gregtech.api.util.function.BooleanConsumer;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import gregtech.common.metatileentities.multi.electric.generator.RotorHolderMultiblockController;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static gregicadditions.capabilities.MultiblockDataCodes.STORE_TAPED;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;


public class MetaTileEntityXLTurbine extends RotorHolderMultiblockController implements IMaintenance {

    public final MetaTileEntityLargeTurbine.TurbineType turbineType;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    private int pageIndex;
    private final int pageSize = 6;
    private XLTurbineWorkableHandler xlTurbineWorkableHandler;
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

    public MetaTileEntityXLTurbine(ResourceLocation metaTileEntityId, MetaTileEntityLargeTurbine.TurbineType turbineType) {
        super(metaTileEntityId, turbineType.recipeMap, GTValues.V[4]);
        this.turbineType = turbineType;
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityXLTurbine(metaTileEntityId, turbineType);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.turbine.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", turbineType.recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", 12));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency"));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.normal", (int) XLTurbineWorkableHandler.getTurbineBonus() + "%"));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.fast", 100 + "%"));
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
        if (isStructureFormed()) {
            ITextComponent totalEnergy = new TextComponentTranslation("tj.multiblock.extreme_turbine.energy");
            totalEnergy.appendText(" ");
            totalEnergy.appendSibling(new TextComponentString(xlTurbineWorkableHandler.getTotalEnergyProduced() + " EU/t"));
            textList.add(totalEnergy);

            ITextComponent toggleFastMode = new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode");
            toggleFastMode.appendText(" ");

            if (xlTurbineWorkableHandler.isFastMode())
                toggleFastMode.appendSibling(withButton(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode.true"), "true"));
            else
                toggleFastMode.appendSibling(withButton(new TextComponentTranslation("tj.multiblock.extreme_turbine.fast_mode.false"), "false"));

            FluidStack fuelStack = xlTurbineWorkableHandler.getFuelStack();
            int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

            ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
            textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.fuel_amount", fuelAmount, fuelName));

            textList.add(toggleFastMode);

            ITextComponent page = new TextComponentString(":");
            page.appendText(" ");
            page.appendSibling(withButton(new TextComponentString("[<]"), "leftPage"));
            page.appendText(" ");
            page.appendSibling(withButton(new TextComponentString("[>]"), "rightPage"));
            textList.add(page);

            int rotorHolderSize = getRotorHolders().size();
            for (int i = pageIndex, rotorIndex = i + 1; i < pageIndex + pageSize; i++, rotorIndex++) {
                if (i < rotorHolderSize) {
                    MetaTileEntityRotorHolder rotorHolder = getAbilities(ABILITY_ROTOR_HOLDER).get(i);

                    double durabilityToInt = rotorHolder.getRotorDurability() * 100;
                    double efficencyToInt = rotorHolder.getRotorEfficiency() * 100;

                    ITextComponent turbineText;
                    TextFormatting colorFormatting;

                    if (rotorHolder.hasRotorInInventory()) {
                        if (durabilityToInt <= 10) {
                            colorFormatting = TextFormatting.RED;
                        }
                        else if (durabilityToInt <= 25) {
                            colorFormatting = TextFormatting.YELLOW;
                        }
                        else {
                            colorFormatting = TextFormatting.GREEN;
                        }
                    } else {
                        colorFormatting = TextFormatting.WHITE;
                    }

                    String rotorName = getShortenRotorName(rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName());
                    turbineText = new TextComponentString("-");
                    turbineText.appendText(" ");
                    turbineText.appendSibling(new TextComponentString("[" + rotorIndex + "] " + (rotorName.equals("Air") ? I18n.format("tj.multiblock.extreme_turbine.insertrotor") : rotorName))
                                    .setStyle(new Style().setColor(colorFormatting)))
                            .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("tj.multiblock.extreme_turbine.name").appendSibling(new TextComponentString(rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName().equals("Air") ? " " + I18n.format("gregtech.multiblock.extreme_turbine.norotor") + "\n" :
                                            " " + rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName() + "\n"))
                                    .appendSibling(new TextComponentTranslation("tj.multiblock.extreme_turbine.speed").appendSibling(new TextComponentString(" " + rotorHolder.getCurrentRotorSpeed() + " / " + rotorHolder.getMaxRotorSpeed() + "\n")))
                                    .appendSibling(new TextComponentTranslation("tj.multiblock.extreme_turbine.efficiency").appendSibling(new TextComponentString(" " + (int) efficencyToInt + "%\n")))
                                    .appendSibling(new TextComponentTranslation("tj.multiblock.extreme_turbine.durability").appendSibling(new TextComponentString(" " + (int) durabilityToInt + "%").setStyle(new Style().setColor(colorFormatting))))
                            )));
                    textList.add(turbineText);
                }
            }
        } else {
            MultiblockDisplaysUtility.isInvalid(textList, isStructureFormed());
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "false":
                fastModeConsumer.apply(true);
                break;
            case "true":
                fastModeConsumer.apply(false);
                break;
            case "leftPage":
                if (pageIndex > 0)
                    pageIndex -= pageSize;
                break;
            default:
                if (pageIndex < getRotorHolders().size() - pageSize)
                    pageIndex += pageSize;
        }
    }

    private String getShortenRotorName(String name) {
        return name
                .replace("Small", "")
                .replace("Medium", "")
                .replace("Large", "")
                .replace("Huge", "")
                .replace("Turbine", "")
                .replace("Rotor", "");
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        XLTurbineWorkableHandler xlTurbineWorkableHandler = new XLTurbineWorkableHandler(this, recipeMap, () -> energyContainer, () -> importFluidHandler);
        this.xlTurbineWorkableHandler = xlTurbineWorkableHandler;
        this.fastModeConsumer = xlTurbineWorkableHandler::setFastMode;
        return xlTurbineWorkableHandler;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.exportFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.importItemHandler = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        if (getAbilities(GregicAdditionsCapabilities.MAINTENANCE_HATCH).isEmpty())
            return;
        MetaTileEntityMaintenanceHatch maintenanceHatch = getAbilities(GregicAdditionsCapabilities.MAINTENANCE_HATCH).get(0);
        if (maintenanceHatch.getType() == 2 || !GAConfig.GT5U.enableMaintenance) {
            this.maintenance_problems = 0b111111;
        } else {
            readMaintenanceData(maintenanceHatch);
            if (maintenanceHatch.getType() == 0 && storedTaped) {
                maintenanceHatch.setTaped(true);
                storeTaped(false);
            }
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
        if (isStructureFormed()) {
            if (getOffsetTimer() % 20 == 0) {
                ItemStack rotorReplacementStack;
                for (MetaTileEntityRotorHolder rotorHolder : getAbilities(ABILITY_ROTOR_HOLDER)) {
                    if (rotorHolder.hasRotorInInventory())
                        continue;
                    rotorReplacementStack = checkAndConsumeItem();
                    if (!(rotorReplacementStack == null)) {
                        rotorHolder.getRotorInventory().setStackInSlot(0, rotorReplacementStack);
                    }
                }
            }
        }
    }

    private ItemStack checkAndConsumeItem() {
        int getItemSlots = importItemHandler.getSlots();
        for (int slotIndex = 0; slotIndex < getItemSlots; slotIndex++) {
            ItemStack item = importItemHandler.getStackInSlot(slotIndex);
            boolean hugeRotorStack = GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean largeRotorStack = GAMetaItems.LARGE_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean mediumRotorStack = GAMetaItems.MEDIUM_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean smallRotorStack = GAMetaItems.SMALL_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);

            // check if slot has either small, medium, large, huge rotor. if not then skip to next slot
            if(!hugeRotorStack && !largeRotorStack && !mediumRotorStack && !smallRotorStack)
                continue;

            ItemStack getItemFromSlot = item.getItem().getContainerItem(item);
            item.setCount(0); // sets stacksize to 0. effectively voiding the item
            importItemHandler.setStackInSlot(slotIndex, item);
            return getItemFromSlot;
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
        return turbineType == null ? null :
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
                        .where('S', selfPredicate())
                        .where('#', isAirPredicate())
                        .where('C', statePredicate(getCasingState()))
                        .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(getAllowedAbilities())))
                        .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
                        .build();
    }

    public MultiblockAbility[] getAllowedAbilities() {
        return turbineType.hasOutputHatch ?
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH} :
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    }
    @Override
    public boolean canShare(){
        return false;
    }

    public IBlockState getCasingState() {
        return turbineType.casingState;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return turbineType.casingRenderer;
    }

    @Deprecated
    public boolean isTurbineFaceFree() {
        return isRotorFaceFree();
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return turbineType.frontOverlay;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setByte("Maintenance", maintenance_problems);
        data.setInteger("ActiveTimer", timeActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        maintenance_problems = data.getByte("Maintenance");
        timeActive = data.getInteger("ActiveTimer");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeByte(maintenance_problems);
        buf.writeInt(timeActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        maintenance_problems = buf.readByte();
        timeActive = buf.readInt();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == STORE_TAPED) {
            storedTaped = buf.readBoolean();
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

    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        TJWidgetGroup widgetDisplayGroup = new TJWidgetGroup(), widgetMaintenanceGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.display", this.getStackForm(), mainDisplayTab(widgetDisplayGroup::addWidgets)));
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.maintenance", GATileEntities.MAINTENANCE_HATCH[0].getStackForm(), maintenanceTab(widgetMaintenanceGroup::addWidgets)));
    }

    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
        widgetGroup.apply(new ToggleButtonWidget(172, 169, 18, 18, TJGuiTextures.POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                .setTooltipText("machine.universal.toggle.run.mode"));
        return widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, TJGuiTextures.CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
    }

    protected AbstractWidgetGroup maintenanceTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addMaintenanceDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    protected void addMaintenanceDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.maintenanceDisplay(textList, maintenance_problems, hasProblems());
    }

    public boolean isWorkingEnabled() {
        return xlTurbineWorkableHandler.isWorkingEnabled();
    }

    public void setWorkingEnabled(boolean isWorking) {
        xlTurbineWorkableHandler.setWorkingEnabled(isWorking);
        this.markDirty();
    }

    protected boolean getDoStructureCheck() {
        if (isStructureFormed())
            this.doStructureCheck = false;
        return this.doStructureCheck;
    }

    protected void setDoStructureCheck(boolean check) {
        if (isStructureFormed()) {
            this.doStructureCheck = true;
            invalidateStructure();
            this.structurePattern = createStructurePattern();
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
        return maintenance_problems;
    }

    /**
     *
     * @return the amount of maintenance problems the multiblock has
     */
    public int getNumProblems() {
        return 6 - Integer.bitCount(maintenance_problems);
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

        timeActive += duration;
        if (minimumMaintenanceTime - timeActive <= 0)
            if(XSTR_RAND.nextFloat() - 0.75f >= 0) {
                causeProblems();
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
        writeCustomData(STORE_TAPED, buf -> buf.writeBoolean(isTaped));
    }

}
