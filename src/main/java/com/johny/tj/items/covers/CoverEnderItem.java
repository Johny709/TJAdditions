package com.johny.tj.items.covers;

import com.johny.tj.gui.widgets.PopUpWidgetGroup;
import com.johny.tj.items.handlers.LargeItemStackHandler;
import com.johny.tj.textures.TJSimpleOverlayRenderer;
import com.johny.tj.util.EnderWorldData;
import gregicadditions.GAValues;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
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
import static gregtech.api.gui.GuiTextures.BORDERED_BACKGROUND;
import static gregtech.api.gui.GuiTextures.TOGGLE_BUTTON_BACK;
import static gregtech.api.gui.widgets.ProgressWidget.MoveType.VERTICAL;
import static gregtech.common.covers.CoverPump.PumpMode.IMPORT;

public class CoverEnderItem extends AbstractCoverEnder<String, LargeItemStackHandler> {

    private final IItemHandler itemInventory;
    private final SimpleItemFilter itemFilter;
    private final int capacity;
    private final int tier;
    private boolean isFilterEnabled;

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
                    widgetGroup.accept(new PhantomSlotWidget(itemFilterSlots, i, 3 + 18 * (i % 3), 3 + 18 * (i / 3)).setBackgroundTexture(GuiTextures.SLOT));
                }
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
        return tier;
    }

    @Override
    protected Map<String, LargeItemStackHandler> getMap() {
        return EnderWorldData.getItemChestMap();
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        PopUpWidgetGroup popUpWidgetGroup = new PopUpWidgetGroup(180, 162, 60, 60, BORDERED_BACKGROUND);
        itemFilter.initUI(popUpWidgetGroup::addWidget);
        widget.accept(popUpWidgetGroup.setEnabled(this::isFilterEnabled));
        widget.accept(new LabelWidget(30, 4, "metaitem.ender_item_cover_" + GAValues.VN[tier].toLowerCase() + ".name"));
        widget.accept(new ToggleButtonWidget(151, 145, 18, 18, TOGGLE_BUTTON_BACK, this::isFilterEnabled, this::setFilterEnabled)
                .setTooltipText("machine.universal.toggle.filter"));
        widget.accept(new ImageWidget(151, 145, 18, 18, ITEM_FILTER));
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

    private void setFilterEnabled(boolean isFilterEnabled) {
        this.isFilterEnabled = isFilterEnabled;
        markAsDirty();
    }

    private boolean isFilterEnabled() {
        return isFilterEnabled;
    }

    private double getItemsStored() {
        LargeItemStackHandler itemHandler = getMap().get(text);
        if (itemHandler == null)
            return 0;
        return (double) itemHandler.getStackInSlot(0).getCount() / itemHandler.getCapacity();
    }

    @Override
    protected void onAddEntry(Widget.ClickData clickData) {
        EnderWorldData.getItemChestMap().putIfAbsent(text, new LargeItemStackHandler(1, capacity));
    }

    @Override
    protected void onClear(Widget.ClickData clickData) {
        EnderWorldData.getItemChestMap().put(text, new LargeItemStackHandler(1, capacity));
    }

    @Override
    public void update() {
        if (isWorkingEnabled) {
            LargeItemStackHandler itemStackHandler = getMap().get(text);
            if (itemStackHandler == null)
                return;
            if (pumpMode == IMPORT) {
                moveInventoryItems(itemInventory, itemStackHandler);
            } else {
                moveInventoryItems(itemStackHandler, itemInventory);
            }
        }
    }

    private void moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory) {
        for (int srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            ItemStack sourceStack = sourceInventory.extractItem(srcIndex, Math.min(transferRate, maxTransferRate), true);
            if (sourceStack.isEmpty() || isFilterEnabled && itemFilter.matchItemStack(sourceStack) == -1) {
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
        data.setBoolean("FilterEnabled", isFilterEnabled);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        itemFilter.readFromNBT(data);
        isFilterEnabled = data.getBoolean("FilterEnabled");
    }
}
