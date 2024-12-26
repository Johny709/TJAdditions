package com.johny.tj.machines.multi.electric;

import com.johny.tj.builder.logic.XLHotCoolantTurbineWorkableHandler;
import com.johny.tj.builder.logic.XLTurbineWorkableHandler;
import com.johny.tj.gui.TJGuiTextures;
import gregicadditions.item.GAMetaItems;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import gregtech.common.metatileentities.multi.electric.generator.RotorHolderMultiblockController;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;


public class MetaTileEntityXLTurbine extends RotorHolderMultiblockController {

    public final MetaTileEntityLargeTurbine.TurbineType turbineType;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    private int pageIndex = 0;
    private final int pageSize = 6;
    private XLTurbineWorkableHandler xlTurbineWorkableHandler;
    protected boolean doStructureCheck = false;

    public MetaTileEntityXLTurbine(ResourceLocation metaTileEntityId, MetaTileEntityLargeTurbine.TurbineType turbineType) {
        super(metaTileEntityId, turbineType.recipeMap, GTValues.V[4]);
        this.turbineType = turbineType;
        reinitializeStructurePattern();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.turbine.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", turbineType.recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", 12));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency", "Normal: " + (int) XLHotCoolantTurbineWorkableHandler.getTurbineBonus() + "%, " + "Fast: " + 100 + "%"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            if (isTurbineFaceFree()) {
                ITextComponent totalEnergy = new TextComponentTranslation("gregtech.multiblock.extreme_turbine.energy");
                totalEnergy.appendText(" ");
                totalEnergy.appendSibling(new TextComponentString(xlTurbineWorkableHandler.getTotalEnergyProduced() + " EU/t"));
                textList.add(totalEnergy);
            }
            else {
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.obstructed")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            }

            ITextComponent toggleFastMode = new TextComponentTranslation("gregtech.multiblock.extreme_turbine.fast_mode");
            toggleFastMode.appendText(" ");

            if (xlTurbineWorkableHandler.getFastModeToggle())
                toggleFastMode.appendSibling(withButton(new TextComponentTranslation("gregtech.multiblock.extreme_turbine.fast_mode.true"), "true"));
            else
                toggleFastMode.appendSibling(withButton(new TextComponentTranslation("gregtech.multiblock.extreme_turbine.fast_mode.false"), "false"));

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

                    ITextComponent turbineText;
                    TextFormatting colorFormatting;

                    StringBuilder rotorInstance = getStringBuilder(rotorHolder);

                    if (rotorHolder.hasRotorInInventory()) {
                        if (durabilityToInt <= 10)
                            colorFormatting = TextFormatting.RED;
                        else if (durabilityToInt <= 25)
                            colorFormatting = TextFormatting.YELLOW;
                        else
                            colorFormatting = TextFormatting.GREEN;
                    } else {
                        colorFormatting = TextFormatting.WHITE;
                    }

                    String rotorName = getShortenRotorName(rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName());
                    turbineText = new TextComponentString("-");
                    turbineText.appendText(" ");
                    turbineText.appendSibling(new TextComponentString("[" + rotorIndex + "] " + (rotorName.equals("Air") ? I18n.format("gregtech.multiblock.extreme_turbine.insertrotor") : rotorName))
                                    .setStyle(new Style().setColor(colorFormatting)))
                            .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(rotorInstance.toString()))));
                    textList.add(turbineText);
                }
            }
        }
        else {
            ITextComponent tooltip = new TextComponentTranslation("gregtech.multiblock.invalid_structure.tooltip");
            tooltip.setStyle(new Style().setColor(TextFormatting.GRAY));
            textList.add(new TextComponentTranslation("gregtech.multiblock.invalid_structure")
                    .setStyle(new Style().setColor(TextFormatting.RED)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
    }

    @NotNull
    private static StringBuilder getStringBuilder(MetaTileEntityRotorHolder rotorHolder) {
        double durabilityToInt = rotorHolder.getRotorDurability() * 100;
        double efficencyToInt = rotorHolder.getRotorEfficiency() * 100;
        StringBuilder rotorInstance = new StringBuilder();

        rotorInstance.append(I18n.format("gregtech.multiblock.extreme_turbine.name"))
            .append(" ")
            .append(rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName().equals("Air") ? I18n.format("gregtech.multiblock.extreme_turbine.norotor") :
                rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName())
            .append("\n");

        rotorInstance.append(I18n.format("gregtech.multiblock.extreme_turbine.speed"))
            .append(" ")
            .append(rotorHolder.getCurrentRotorSpeed())
            .append(" / ")
            .append(rotorHolder.getMaxRotorSpeed())
            .append("\n");

        rotorInstance.append(I18n.format("gregtech.multiblock.extreme_turbine.efficiency"))
            .append(" ")
            .append((int) efficencyToInt)
            .append("%")
            .append("\n");

        rotorInstance.append(I18n.format("gregtech.multiblock.extreme_turbine.durability"))
            .append(" ")
            .append((int) durabilityToInt)
            .append("%");
        return rotorInstance;
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "false":
                xlTurbineWorkableHandler.toggleFastMode(true);
                break;
            case "true":
                xlTurbineWorkableHandler.toggleFastMode(false);
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
        return xlTurbineWorkableHandler;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityXLTurbine(metaTileEntityId, turbineType);
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
        if (isStructureFormed()) {
            ItemStack rotorReplacementStack;
            int index = 0;
            for (MetaTileEntityRotorHolder rotorHolder : getAbilities(ABILITY_ROTOR_HOLDER)) {
                index++;
                if (rotorHolder.hasRotorInInventory())
                    continue;
                rotorReplacementStack = checkAndConsumeItem();
                if (!(rotorReplacementStack == null)) {
                    rotorHolder.getRotorInventory().setStackInSlot(0, rotorReplacementStack);
                }
            }
        }
        if (this.isRotorFaceFree()) {
            super.updateFormedValid();
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
        int numberOfFacesFree = 0;
        for (MetaTileEntityRotorHolder rotorHolder : getAbilities(ABILITY_ROTOR_HOLDER)) {
            if (rotorHolder.isFrontFaceFree())
                numberOfFacesFree++;
        }
        return isStructureFormed() && numberOfFacesFree == 12;
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
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY} :
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY};
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
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(-10, 0, 195, 217, TJGuiTextures.NEW_MULTIBLOCK_DISPLAY);
        builder.label(1, 9, getMetaFullName(), 0xFFFFFF);
        builder.widget(new AdvancedTextWidget(1, 19, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180)
                .setClickHandler(this::handleDisplayClick));
        builder.widget(new ToggleButtonWidget(162, 170, 18, 18, TJGuiTextures.POWER_BUTTON, this::getToggleMode, this::setToggleRunning)
                .setTooltipText("machine.universal.toggle.run.mode"));
        builder.widget(new ToggleButtonWidget(162, 152, 18, 18, TJGuiTextures.CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT ,-3, 134);
        return builder;
    }

    protected boolean getToggleMode() {
        return this.xlTurbineWorkableHandler.isWorkingEnabled();
    }

    protected void setToggleRunning(boolean running) {
        this.xlTurbineWorkableHandler.setWorkingEnabled(running);
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

}
