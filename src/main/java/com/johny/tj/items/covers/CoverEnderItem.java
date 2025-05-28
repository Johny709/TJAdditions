package com.johny.tj.items.covers;

import com.johny.tj.gui.widgets.PopUpWidgetGroup;
import com.johny.tj.items.handlers.LargeItemStackHandler;
import com.johny.tj.textures.TJSimpleOverlayRenderer;
import com.johny.tj.util.EnderWorldData;
import gregicadditions.GAValues;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.util.function.BooleanConsumer;
import gregtech.common.covers.filter.SimpleItemFilter;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.johny.tj.gui.TJGuiTextures.*;
import static com.johny.tj.textures.TJTextures.PORTAL_OVERLAY;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.ProgressWidget.MoveType.VERTICAL;
import static gregtech.common.covers.CoverPump.PumpMode.IMPORT;

public class CoverEnderItem extends AbstractCoverEnder<String, LargeItemStackHandler> {

    private final IItemHandler itemInventory;
    private final SimpleItemFilter itemFilter;
    private BooleanConsumer enableItemPopUp;
    private final int capacity;
    private final int tier;
    private boolean isFilterBlacklist = true;

    public CoverEnderItem(ICoverable coverHolder, EnumFacing attachedSide, int tier) {
        super(coverHolder, attachedSide);
        this.tier = tier;
        this.itemInventory = this.coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        this.capacity = (int) Math.min(Math.pow(4, tier) * 10, Integer.MAX_VALUE);
        this.maxTransferRate = (int) Math.min(Math.round(Math.pow(4, tier) / 20), Integer.MAX_VALUE);
        this.itemFilter = new SimpleItemFilter() {
            @Override
            public void initUI(Consumer<Widget> widgetGroup) {
                for (int i = 0; i < 9; i++) {
                    widgetGroup.accept(new PhantomSlotWidget(itemFilterSlots, i, 3 + 18 * (i % 3), 3 + 18 * (i / 3)).setBackgroundTexture(SLOT));
                }
                widgetGroup.accept(new ToggleButtonWidget(21, 57, 18, 18, BUTTON_FILTER_DAMAGE,
                        () -> ignoreDamage, this::setIgnoreDamage).setTooltipText("cover.item_filter.ignore_damage"));
                widgetGroup.accept(new ToggleButtonWidget(39, 57, 18, 18, BUTTON_FILTER_NBT,
                        () -> ignoreNBT, this::setIgnoreNBT).setTooltipText("cover.item_filter.ignore_nbt"));
            }
        };
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.attachedSide) != null;
    }

    @Override
    protected TJSimpleOverlayRenderer getOverlay() {
        return PORTAL_OVERLAY;
    }

    @Override
    protected int getPortalColor() {
        return 0xff3e00;
    }

    @Override
    public int getTier() {
        return this.tier;
    }

    @Override
    protected Map<String, LargeItemStackHandler> getMap() {
        return EnderWorldData.getItemChestMap();
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        PopUpWidgetGroup popUpWidgetGroup = new PopUpWidgetGroup(112, 61, 60, 78, BORDERED_BACKGROUND);
        popUpWidgetGroup.addWidget(new ToggleButtonWidget(3, 57, 18, 18, BUTTON_BLACKLIST, this::isFilterBlacklist, this::setFilterBlacklist)
                .setTooltipText("cover.filter.blacklist"));
        popUpWidgetGroup.setEnabled(this.isFilterPopUp);
        this.itemFilter.initUI(popUpWidgetGroup::addWidget);
        this.enableItemPopUp = popUpWidgetGroup::setEnabled;
        widget.accept(popUpWidgetGroup);
        widget.accept(new LabelWidget(30, 4, "metaitem.ender_item_cover_" + GAValues.VN[this.tier].toLowerCase() + ".name"));
        widget.accept(new ToggleButtonWidget(151, 161, 18, 18, TOGGLE_BUTTON_BACK, this::isFilterPopUp, this::setFilterPopUp)
                .setTooltipText("machine.universal.toggle.filter.open"));
        widget.accept(new ImageWidget(151, 161, 18, 18, ITEM_FILTER));
        widget.accept(new ProgressWidget(this::getItemsStored, 7, 38, 18, 18) {
            private int itemStored;
            private int itemCapacity;

            @Override
            public void drawInForeground(int mouseX, int mouseY) {
                if(isMouseOverElement(mouseX, mouseY)) {
                    List<String> hoverList = Collections.singletonList(I18n.format("machine.universal.item.stored", this.itemStored, this.itemCapacity));
                    drawHoveringText(ItemStack.EMPTY, hoverList, 300, mouseX, mouseY);
                }
            }

            @Override
            public void detectAndSendChanges() {
                super.detectAndSendChanges();
                LargeItemStackHandler itemStackHandler = getMap().get(text);
                if (itemStackHandler != null) {
                    int itemStored = itemStackHandler.getStackInSlot(0).getCount();
                    int itemCapacity = itemStackHandler.getCapacity();
                    writeUpdateInfo(1, buffer -> buffer.writeInt(itemStored));
                    writeUpdateInfo(2, buffer -> buffer.writeInt(itemCapacity));
                }
            }

            @Override
            public void readUpdateInfo(int id, PacketBuffer buffer) {
                super.readUpdateInfo(id, buffer);
                if (id == 1) {
                    this.itemStored = buffer.readInt();
                }
                if (id == 2) {
                    this.itemCapacity = buffer.readInt();
                }
            }

        }.setProgressBar(BAR_STEEL, BAR_HEAT, VERTICAL));
    }

    private void setFilterBlacklist(boolean isFilterBlacklist) {
        this.isFilterBlacklist = isFilterBlacklist;
        markAsDirty();
    }

    private boolean isFilterBlacklist() {
        return this.isFilterBlacklist;
    }

    @Override
    protected void setFilterPopUp(boolean isFilterPopUp) {
        this.enableItemPopUp.apply(isFilterPopUp);
        super.setFilterPopUp(isFilterPopUp);
    }

    private double getItemsStored() {
        LargeItemStackHandler itemHandler = getMap().get(this.text);
        if (itemHandler == null)
            return 0;
        return (double) itemHandler.getStackInSlot(0).getCount() / itemHandler.getCapacity();
    }

    @Override
    protected void onAddEntry(Widget.ClickData clickData) {
        EnderWorldData.getItemChestMap().putIfAbsent(this.text, new LargeItemStackHandler(1, this.capacity));
    }

    @Override
    protected void onClear(Widget.ClickData clickData) {
        EnderWorldData.getItemChestMap().put(this.text, new LargeItemStackHandler(1, this.capacity));
    }

    @Override
    public void update() {
        if (this.isWorkingEnabled) {
            LargeItemStackHandler itemStackHandler = getMap().get(this.text);
            if (itemStackHandler == null)
                return;
            if (this.pumpMode == IMPORT) {
                moveInventoryItems(this.itemInventory, itemStackHandler);
            } else {
                moveInventoryItems(itemStackHandler, this.itemInventory);
            }
        }
    }

    private void moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory) {
        for (int srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            ItemStack sourceStack = sourceInventory.extractItem(srcIndex, Math.min(this.transferRate, this.maxTransferRate), true);
            boolean isFilterStack = this.itemFilter.matchItemStack(sourceStack) != null;
            if (sourceStack.isEmpty() || this.isFilterBlacklist == isFilterStack) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInventory, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();
            if (amountToInsert > 0) {
                sourceStack = sourceInventory.extractItem(srcIndex, amountToInsert, false);
                ItemHandlerHelper.insertItemStacked(targetInventory, sourceStack, false);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        itemFilter.writeToNBT(data);
        data.setBoolean("FilterBlacklist", isFilterBlacklist);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        itemFilter.readFromNBT(data);
        isFilterBlacklist = data.getBoolean("FilterBlacklist");
    }
}
