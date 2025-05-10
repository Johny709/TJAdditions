package com.johny.tj.builder.multicontrollers;

import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJHorizontoalTabListRenderer;
import com.johny.tj.gui.TJTabGroup;
import com.johny.tj.gui.TJWidgetGroup;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.GAFueledMultiblockController;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.recipes.machines.FuelRecipeMap;
import gregtech.api.util.Position;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TJFueledMultiblockController extends GAFueledMultiblockController {

    private boolean doStructureCheck;

    public TJFueledMultiblockController(ResourceLocation metaTileEntityId, FuelRecipeMap recipeMap, long maxVoltage) {
        super(metaTileEntityId, recipeMap, maxVoltage);
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();
        return maintenanceCount == 1;
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

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.recipeMapWorkable(textList, isStructureFormed(), workableHandler);
        MultiblockDisplaysUtility.isInvalid(textList, isStructureFormed());
    }

    protected void addMaintenanceDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.mufflerDisplay(textList, !hasMufflerHatch() || isMufflerFaceFree());
        MultiblockDisplaysUtility.maintenanceDisplay(textList, maintenance_problems, hasProblems());
    }

    public boolean isWorkingEnabled() {
        return this.workableHandler.isWorkingEnabled();
    }

    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.workableHandler.setWorkingEnabled(isActivationAllowed);
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
}
