package com.johny.tj.machines.multi.electric;

import com.johny.tj.builder.handlers.XLTurbineWorkableHandler;
import com.johny.tj.builder.multicontrollers.TJRotorHolderMultiblockController;
import com.johny.tj.gui.TJWidgetGroup;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GAMetaItems;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.WidgetGroup;
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
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static net.minecraft.util.text.TextFormatting.*;


public class MetaTileEntityXLTurbine extends TJRotorHolderMultiblockController {

    public final MetaTileEntityLargeTurbine.TurbineType turbineType;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    private int pageIndex;
    private final int pageSize = 6;
    private XLTurbineWorkableHandler xlTurbineWorkableHandler;
    private BooleanConsumer fastModeConsumer;

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
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
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

            int currentProgress = (int) Math.floor(xlTurbineWorkableHandler.getProgress() / (xlTurbineWorkableHandler.getMaxProgress() * 1.0) * 100);
            textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));

            ITextComponent isWorkingText = !xlTurbineWorkableHandler.isWorkingEnabled() ? new TextComponentTranslation("gregtech.multiblock.work_paused")
                    : !xlTurbineWorkableHandler.isActive() ? new TextComponentTranslation("gregtech.multiblock.idling")
                    : new TextComponentTranslation("gregtech.multiblock.running");

            isWorkingText.getStyle().setColor(!xlTurbineWorkableHandler.isWorkingEnabled() ? YELLOW : !xlTurbineWorkableHandler.isActive() ? WHITE : GREEN);
            textList.add(isWorkingText);
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
    }

    private void handleRotorDisplayClick(String componentData, Widget.ClickData clickData) {
        if (componentData.equals("leftPage")) {
            if (pageIndex > 0)
                pageIndex -= pageSize;
        } else {
            if (pageIndex < getRotorHolders().size() - pageSize)
                pageIndex += pageSize;
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        fastModeConsumer.apply(componentData.equals("false"));
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
    public boolean canShare() {
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
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        TJWidgetGroup rotorWidgetGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.rotor", GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm(), rotorTab(rotorWidgetGroup::addWidgets)));
    }

    private AbstractWidgetGroup rotorTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addRotorDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleRotorDisplayClick));
    }

    @Override
    public boolean isWorkingEnabled() {
        return xlTurbineWorkableHandler.isWorkingEnabled();
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        xlTurbineWorkableHandler.setWorkingEnabled(isWorking);
        this.markDirty();
    }
}
