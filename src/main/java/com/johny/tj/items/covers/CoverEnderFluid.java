package com.johny.tj.items.covers;

import com.johny.tj.gui.widgets.PopUpWidgetGroup;
import com.johny.tj.gui.widgets.TJTankWidget;
import com.johny.tj.textures.TJSimpleOverlayRenderer;
import com.johny.tj.util.EnderWorldData;
import gregicadditions.GAValues;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.PhantomFluidWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.common.covers.CoverPump;
import gregtech.common.covers.filter.SimpleFluidFilter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Map;
import java.util.function.Consumer;

import static com.johny.tj.gui.TJGuiTextures.FLUID_FILTER;
import static com.johny.tj.textures.TJTextures.PORTAL_OVERLAY;
import static gregtech.api.gui.GuiTextures.*;

public class CoverEnderFluid extends AbstractCoverEnder<String, FluidTank> {

    private final IFluidHandler fluidTank;
    private final SimpleFluidFilter fluidFilter;
    private final int capacity;
    private final int tier;
    private boolean isFilterPopUp;
    private boolean isFilterBlacklist;

    public CoverEnderFluid(ICoverable coverHolder, EnumFacing attachedSide, int tier) {
        super(coverHolder, attachedSide);
        this.tier = tier;
        this.fluidTank = this.coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        this.capacity = (int) Math.min(Math.pow(4, tier) * 1000, Integer.MAX_VALUE);
        this.maxTransferRate = (int) Math.min(Math.pow(4, tier) * 16, Integer.MAX_VALUE);
        this.fluidFilter = new SimpleFluidFilter() {
            @Override
            public void initUI(Consumer<Widget> widgetGroup) {
                for (int i = 0; i < 9; ++i) {
                    int index = i;
                    widgetGroup.accept(new PhantomFluidWidget(3 + 18 * (i % 3), 3 + 18 * (i / 3), 18, 18,
                            () -> getFluidInSlot(index), (newFluid) -> setFluidInSlot(index, newFluid))
                            .setBackgroundTexture(SLOT));
                }
            }
        };
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.attachedSide) != null;
    }

    @Override
    protected TJSimpleOverlayRenderer getOverlay() {
        return PORTAL_OVERLAY;
    }

    @Override
    protected int getPortalColor() {
        return 0x00a6ff;
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    protected Map<String, FluidTank> getMap() {
        return EnderWorldData.getFluidTankMap();
    }

    @Override
    protected void onAddEntry(Widget.ClickData clickData) {
        EnderWorldData.getFluidTankMap().putIfAbsent(text, new FluidTank(capacity));
    }

    @Override
    protected void onClear(Widget.ClickData clickData) {
        EnderWorldData.getFluidTankMap().put(text, new FluidTank(capacity));
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        PopUpWidgetGroup popUpWidgetGroup = new PopUpWidgetGroup(112, 61, 60, 78, BORDERED_BACKGROUND);
        popUpWidgetGroup.addWidget(new ToggleButtonWidget(3, 57, 18, 18, BUTTON_BLACKLIST, this::isFilterBlacklist, this::setFilterBlacklist)
                .setTooltipText("cover.filter.blacklist"));
        fluidFilter.initUI(popUpWidgetGroup::addWidget);
        widget.accept(popUpWidgetGroup.setEnabled(this::isFilterPopUp));
        widget.accept(new LabelWidget(30, 4, "metaitem.ender_fluid_cover_" + GAValues.VN[tier].toLowerCase() + ".name"));
        widget.accept(new ToggleButtonWidget(151, 145, 18, 18, TOGGLE_BUTTON_BACK, this::isFilterPopUp, this::setFilterPopUp)
                .setTooltipText("machine.universal.toggle.filter.open"));
        widget.accept(new ImageWidget(151, 145, 18, 18, FLUID_FILTER));
        widget.accept(new TJTankWidget(this::getFluidTank, 7, 38, 18, 18)
                .setBackgroundTexture(FLUID_SLOT)
                .setContainerClicking(true, true));
    }

    private void setFilterBlacklist(boolean isFilterBlacklist) {
        this.isFilterBlacklist = isFilterBlacklist;
        markAsDirty();
    }

    private boolean isFilterBlacklist() {
        return isFilterBlacklist;
    }

    private void setFilterPopUp(boolean isFilterPopUp) {
        this.isFilterPopUp = isFilterPopUp;
        markAsDirty();
    }

    private boolean isFilterPopUp() {
        return isFilterPopUp;
    }

    private IFluidTank getFluidTank() {
        IFluidTank tank = EnderWorldData.getFluidTankMap().get(text);
        return tank != null ? tank : EnderWorldData.getFluidTankMap().get("default");
    }

    @Override
    public void update() {
        if (isWorkingEnabled) {
            if (pumpMode == CoverPump.PumpMode.EXPORT) {
                FluidStack enderStack = getFluidTank().drain(transferRate, false);
                if (enderStack != null && fluidTank.fill(enderStack, false) > 0) {
                    if (!isFilterBlacklist == fluidFilter.testFluid(enderStack))
                        getFluidTank().drain(fluidTank.fill(enderStack, true), true);
                }
            } else {
                FluidStack fluidStack = fluidTank.drain(transferRate, false);
                if (fluidStack != null && getFluidTank().fill(fluidStack, false) > 0) {
                    if (!isFilterBlacklist == fluidFilter.testFluid(fluidStack))
                        fluidTank.drain(getFluidTank().fill(fluidStack, true), true);
                }
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        fluidFilter.writeToNBT(data);
        data.setBoolean("FilterBlacklist", isFilterBlacklist);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        fluidFilter.readFromNBT(data);
        isFilterBlacklist = data.getBoolean("FilterBlacklist");
    }
}
