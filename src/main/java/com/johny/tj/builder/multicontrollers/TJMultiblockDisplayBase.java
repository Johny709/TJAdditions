package com.johny.tj.builder.multicontrollers;

import com.johny.tj.gui.TJHorizontoalTabListRenderer;
import com.johny.tj.gui.TJTabGroup;
import com.johny.tj.gui.TJWidgetGroup;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.GAMultiblockWithDisplayBase;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.util.Position;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.johny.tj.gui.TJGuiTextures.*;
import static com.johny.tj.gui.TJHorizontoalTabListRenderer.HorizontalStartCorner.LEFT;
import static com.johny.tj.gui.TJHorizontoalTabListRenderer.VerticalLocation.BOTTOM;

public abstract class TJMultiblockDisplayBase extends GAMultiblockWithDisplayBase implements IControllable {

    protected boolean doStructureCheck = false;
    protected boolean isWorkingEnabled = true;

    public TJMultiblockDisplayBase(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer player) {
        return this.createUI(player, 0);
    }

    protected ModularUI.Builder createUI(EntityPlayer player, int extended) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(-10, 0, 195, 217 + extended, extended == 0 ? NEW_MULTIBLOCK_DISPLAY : NEW_MULTIBLOCK_DISPLAY_EXTENDED);
        builder.bindPlayerInventory(player.inventory, GuiTextures.SLOT ,-3, 134 + extended);
        builder.widget(new LabelWidget(0, 7, getMetaFullName(), 0xFFFFFF));

        TJTabGroup tabGroup = new TJTabGroup(() -> new TJHorizontoalTabListRenderer(LEFT, BOTTOM), new Position(-10, 1 + extended));
        List<Triple<String, ItemStack, AbstractWidgetGroup>> tabList = new ArrayList<>();
        this.addNewTabs(tabList::add, extended);
        tabList.forEach(tabs -> tabGroup.addTab(new ItemTabInfo(tabs.getLeft(), tabs.getMiddle()), tabs.getRight()));
        builder.widget(tabGroup);
        return builder;
    }

    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs, int extended) {
        TJWidgetGroup widgetDisplayGroup = new TJWidgetGroup(), widgetMaintenanceGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.display", this.getStackForm(), this.mainDisplayTab(widgetDisplayGroup::addWidgets, extended)));
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.maintenance", GATileEntities.MAINTENANCE_HATCH[0].getStackForm(), this.maintenanceTab(widgetMaintenanceGroup::addWidgets, extended)));
    }

    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup, int extended) {
        widgetGroup.apply(new AdvancedTextWidget(10, 18 - extended, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleDisplayClick));
        widgetGroup.apply(new ToggleButtonWidget(172, 169, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                .setTooltipText("machine.universal.toggle.run.mode"));
        return widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
    }

    protected AbstractWidgetGroup maintenanceTab(Function<Widget, WidgetGroup> widgetGroup, int extended) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18 - extended, this::addMaintenanceDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.isInvalid(textList, isStructureFormed());
    }

    protected void addMaintenanceDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.mufflerDisplay(textList, !hasMufflerHatch() || isMufflerFaceFree());
        MultiblockDisplaysUtility.maintenanceDisplay(textList, maintenance_problems, hasProblems());
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.isWorkingEnabled = isActivationAllowed;
        this.markDirty();
    }

    protected boolean getDoStructureCheck() {
        if (this.isStructureFormed())
            this.doStructureCheck = false;
        return this.doStructureCheck;
    }

    protected void setDoStructureCheck(boolean check) {
        if (this.isStructureFormed()) {
            this.doStructureCheck = true;
            this.invalidateStructure();
            this.structurePattern = createStructurePattern();
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        return super.getCapability(capability, side);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("IsWorking", this.isWorkingEnabled);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.isWorkingEnabled = data.getBoolean("IsWorking");
    }
}
