package com.johny.tj.machines.multi.electric;

import com.johny.tj.builder.logic.XLHotCoolantTurbineWorkableHandler;
import com.johny.tj.gui.TJGuiTextures;
import gregicadditions.item.GAMetaItems;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.multi.impl.HotCoolantRecipeLogic;
import gregicadditions.machines.multi.impl.MetaTileEntityRotorHolderForNuclearCoolant;
import gregicadditions.machines.multi.nuclear.MetaTileEntityHotCoolantTurbine;
import gregtech.api.capability.impl.FluidTankList;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static gregicadditions.client.ClientHandler.MARAGING_STEEL_250_CASING;
import static gregicadditions.item.GAMetaBlocks.METAL_CASING_1;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public class MetaTileEntityXLHotCoolantTurbine extends MetaTileEntityHotCoolantTurbine {

    public final MetaTileEntityHotCoolantTurbine.TurbineType turbineType;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    private int pageIndex = 0;
    private final int pageSize = 6;
    private XLHotCoolantTurbineWorkableHandler xlHotCoolantTurbineWorkableHandler;
    protected boolean doStructureCheck = false;

    public MetaTileEntityXLHotCoolantTurbine(ResourceLocation metaTileEntityId, MetaTileEntityHotCoolantTurbine.TurbineType turbineType) {
        super(metaTileEntityId, turbineType);
        this.turbineType = turbineType;
        reinitializeStructurePattern();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.turbine.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", turbineType.recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", 12));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency"));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.normal", (int) XLHotCoolantTurbineWorkableHandler.getTurbineBonus() + "%"));
        tooltip.add(I18n.format("tj.multiblock.turbine.tooltip.efficiency.fast",  100 + "%"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            ITextComponent totalEnergy = new TextComponentTranslation("gregtech.multiblock.extreme_turbine.energy");
            totalEnergy.appendText(" ");
            totalEnergy.appendSibling(new TextComponentString(xlHotCoolantTurbineWorkableHandler.getTotalEnergyProduced() + " EU/t"));
            textList.add(totalEnergy);

            ITextComponent toggleFastMode = new TextComponentTranslation("gregtech.multiblock.extreme_turbine.fast_mode");
            toggleFastMode.appendText(" ");

            if (xlHotCoolantTurbineWorkableHandler.getFastModeToggle())
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

            int rotorHolderSize = getAbilities(ABILITY_ROTOR_HOLDER).size();
            for (int i = pageIndex, rotorIndex = i + 1; i < pageIndex + pageSize; i++, rotorIndex++) {
                if (i < rotorHolderSize) {
                    MetaTileEntityRotorHolderForNuclearCoolant rotorHolder = getAbilities(ABILITY_ROTOR_HOLDER).get(i);

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
                    turbineText.appendSibling(new TextComponentString("[" + rotorIndex + "] " + (rotorName.equals("Air") ? I18n.format("gregtech.multiblock.extreme_turbine.insertrotor") : rotorName))
                                    .setStyle(new Style().setColor(colorFormatting)))
                            .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("gregtech.multiblock.extreme_turbine.name").appendSibling(new TextComponentString(rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName().equals("Air") ? " " + I18n.format("gregtech.multiblock.extreme_turbine.norotor") + "\n" :
                                            " " + rotorHolder.getRotorInventory().getStackInSlot(0).getDisplayName() + "\n"))
                                    .appendSibling(new TextComponentTranslation("gregtech.multiblock.extreme_turbine.speed").appendSibling(new TextComponentString(" " + rotorHolder.getCurrentRotorSpeed() + " / " + rotorHolder.getMaxRotorSpeed() + "\n")))
                                    .appendSibling(new TextComponentTranslation("gregtech.multiblock.extreme_turbine.efficiency").appendSibling(new TextComponentString(" " + (int) efficencyToInt + "%\n")))
                                    .appendSibling(new TextComponentTranslation("gregtech.multiblock.extreme_turbine.durability").appendSibling(new TextComponentString(" " + (int) durabilityToInt + "%").setStyle(new Style().setColor(colorFormatting))))
                            )));
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

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "false":
                xlHotCoolantTurbineWorkableHandler.toggleFastMode(true);
                break;
            case "true":
                xlHotCoolantTurbineWorkableHandler.toggleFastMode(false);
                break;
            case "leftPage":
                if (pageIndex > 0)
                    pageIndex -= pageSize;
                break;
            default:
                if (pageIndex < getAbilities(ABILITY_ROTOR_HOLDER).size() - pageSize)
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
    protected HotCoolantRecipeLogic createWorkable(long maxVoltage) {
        XLHotCoolantTurbineWorkableHandler xlHotCoolantTurbineWorkableHandler = new XLHotCoolantTurbineWorkableHandler(this, recipeMap, () -> energyContainer, () -> importFluidHandler);
        this.xlHotCoolantTurbineWorkableHandler = xlHotCoolantTurbineWorkableHandler;
        return xlHotCoolantTurbineWorkableHandler;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityXLHotCoolantTurbine(metaTileEntityId, turbineType);
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

    public boolean isActive() {
        return isTurbineFaceFree() && workableHandler.isActive() && workableHandler.isWorkingEnabled();
    }

    @Override
    protected void updateFormedValid() {
        super.updateFormedValid();
        if (isStructureFormed()) {
            if (getOffsetTimer() % 20 == 0) {
                ItemStack rotorReplacementStack;
                for (MetaTileEntityRotorHolderForNuclearCoolant rotorHolder : getAbilities(ABILITY_ROTOR_HOLDER)) {
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
    public boolean isTurbineFaceFree() {
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

    @Override
    public MultiblockAbility[] getAllowedAbilities() {
        return turbineType.hasOutputHatch ?
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY} :
                new MultiblockAbility[]{MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.OUTPUT_ENERGY};
    }

    public IBlockState getCasingState() {
        return METAL_CASING_1.getState(MetalCasing1.CasingType.MARAGING_STEEL_250);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return MARAGING_STEEL_250_CASING;
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
        return this.xlHotCoolantTurbineWorkableHandler.isWorkingEnabled();
    }

    protected void setToggleRunning(boolean running) {
        this.xlHotCoolantTurbineWorkableHandler.setWorkingEnabled(running);
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
