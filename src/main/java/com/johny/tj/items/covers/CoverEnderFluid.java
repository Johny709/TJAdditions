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
import gregtech.api.util.function.BooleanConsumer;
import gregtech.common.covers.CoverPump;
import gregtech.common.covers.filter.SimpleFluidFilter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Map;
import java.util.function.Consumer;

import static com.johny.tj.TJValues.DUMMY_TANK;
import static com.johny.tj.gui.TJGuiTextures.FLUID_FILTER;
import static com.johny.tj.textures.TJTextures.PORTAL_OVERLAY;
import static gregtech.api.gui.GuiTextures.*;

public class CoverEnderFluid extends AbstractCoverEnder<String, FluidTank> {

    private final IFluidHandler fluidTank;
    private final SimpleFluidFilter fluidFilter;
    private BooleanConsumer enableFluidPopUp;
    private final int capacity;
    private final int tier;
    private boolean isFilterBlacklist = true;

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
        return this.tier;
    }

    @Override
    protected Map<String, FluidTank> getMap() {
        return EnderWorldData.getFluidTankMap();
    }

    @Override
    protected void onAddEntry(Widget.ClickData clickData) {
        this.getMap().putIfAbsent(this.text, new FluidTank(this.capacity));
    }

    @Override
    protected void onClear(Widget.ClickData clickData) {
        if (this.getMap().containsKey(this.text)) {
            this.getMap().put(this.text, new FluidTank(this.capacity));
        }
    }

    @Override
    protected void addEntryText(ITextComponent keyEntry, String key, FluidTank value) {
        FluidStack fluid = value.getFluid();
        boolean empty = fluid == null;
        String name = !empty ? fluid.getUnlocalizedName() : I18n.translateToLocal("metaitem.fluid_cell.empty");
        int capacity = !empty ? value.getCapacity() : 0;
        int amount = !empty ? fluid.amount : 0;
        keyEntry.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(name)
                .appendText("§b ")
                .appendText(amount + "L")
                .appendText(" §r/§b ")
                .appendText(capacity + "L")));
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        PopUpWidgetGroup popUpWidgetGroup = new PopUpWidgetGroup(112, 61, 60, 78, BORDERED_BACKGROUND);
        popUpWidgetGroup.addWidget(new ToggleButtonWidget(3, 57, 18, 18, BUTTON_BLACKLIST, this::isFilterBlacklist, this::setFilterBlacklist)
                .setTooltipText("cover.filter.blacklist"));
        popUpWidgetGroup.setEnabled(this.isFilterPopUp);
        this.enableFluidPopUp = popUpWidgetGroup::setEnabled;
        this.fluidFilter.initUI(popUpWidgetGroup::addWidget);
        widget.accept(popUpWidgetGroup);
        widget.accept(new LabelWidget(30, 4, "metaitem.ender_fluid_cover_" + GAValues.VN[this.tier].toLowerCase() + ".name"));
        widget.accept(new ToggleButtonWidget(151, 161, 18, 18, TOGGLE_BUTTON_BACK, this::isFilterPopUp, this::setFilterPopUp)
                .setTooltipText("machine.universal.toggle.filter.open"));
        widget.accept(new ImageWidget(151, 161, 18, 18, FLUID_FILTER));
        widget.accept(new TJTankWidget(this::getFluidTank, 7, 38, 18, 18)
                .setBackgroundTexture(FLUID_SLOT)
                .setContainerClicking(true, true));
    }

    private void setFilterBlacklist(boolean isFilterBlacklist) {
        this.isFilterBlacklist = isFilterBlacklist;
        this.markAsDirty();
    }

    private boolean isFilterBlacklist() {
        return this.isFilterBlacklist;
    }

    @Override
    protected void setFilterPopUp(boolean isFilterPopUp) {
        this.enableFluidPopUp.apply(isFilterPopUp);
        super.setFilterPopUp(isFilterPopUp);
    }

    private IFluidTank getFluidTank() {
        return this.handler != null ? this.handler : DUMMY_TANK;
    }

    @Override
    public void update() {
        if (this.isWorkingEnabled) {
            this.handler = this.getMap().get(this.text);
            if (this.handler == null) {
                return;
            }
            if (this.pumpMode == CoverPump.PumpMode.EXPORT) {
                FluidStack enderStack = this.getFluidTank().drain(this.transferRate, false);
                if (enderStack != null && this.fluidTank.fill(enderStack, false) > 0) {
                    if (!this.isFilterBlacklist == this.fluidFilter.testFluid(enderStack))
                        this.getFluidTank().drain(this.fluidTank.fill(enderStack, true), true);
                }
            } else {
                FluidStack fluidStack = this.fluidTank.drain(this.transferRate, false);
                if (fluidStack != null && this.getFluidTank().fill(fluidStack, false) > 0) {
                    if (!this.isFilterBlacklist == this.fluidFilter.testFluid(fluidStack))
                        this.fluidTank.drain(this.getFluidTank().fill(fluidStack, true), true);
                }
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.fluidFilter.writeToNBT(data);
        data.setBoolean("FilterBlacklist", this.isFilterBlacklist);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.fluidFilter.readFromNBT(data);
        this.isFilterBlacklist = data.getBoolean("FilterBlacklist");
    }
}
