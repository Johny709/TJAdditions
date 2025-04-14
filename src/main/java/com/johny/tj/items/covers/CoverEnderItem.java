package com.johny.tj.items.covers;

import com.johny.tj.gui.widgets.TJSlotWidget;
import com.johny.tj.items.handlers.LargeItemStackHandler;
import com.johny.tj.textures.TJSimpleOverlayRenderer;
import com.johny.tj.util.EnderWorldData;
import gregicadditions.GAValues;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.LabelWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Map;
import java.util.function.Consumer;

import static com.johny.tj.textures.TJTextures.COVER_CREATIVE_FLUID;
import static gregtech.api.gui.GuiTextures.SLOT;
import static gregtech.common.covers.CoverPump.PumpMode.IMPORT;

public class CoverEnderItem extends AbstractCoverEnder<String, LargeItemStackHandler> {

    private final IItemHandler itemInventory;
    private final int capacity;
    private final int tier;

    public CoverEnderItem(ICoverable coverHolder, EnumFacing attachedSide, int tier) {
        super(coverHolder, attachedSide);
        this.tier = tier;
        this.itemInventory = this.coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        this.capacity = (int) Math.min(Math.pow(4, tier) * 10, Integer.MAX_VALUE);
        this.maxTransferRate = (int) Math.min(Math.round(Math.pow(4, tier) / 20), Integer.MAX_VALUE);
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.attachedSide) != null;
    }

    @Override
    protected TJSimpleOverlayRenderer getOverlay() {
        return COVER_CREATIVE_FLUID;
    }

    @Override
    protected Map<String, LargeItemStackHandler> getMap() {
        return EnderWorldData.getItemChestMap();
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        widget.accept(new LabelWidget(30, 4, "metaitem.ender_item_cover_" + GAValues.VN[tier].toLowerCase() + ".name"));
        widget.accept(new TJSlotWidget(this::getItemHandler, 0, 7, 38, false, false)
                .setBackgroundTexture(SLOT));
    }

    private IItemHandler getItemHandler() {
        IItemHandler itemHandler = EnderWorldData.getItemChestMap().get(text);
        return itemHandler != null ? itemHandler : EnderWorldData.getItemChestMap().get("default");
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
            if (pumpMode == IMPORT) {
                moveInventoryItems(itemInventory, getItemHandler());
            } else {
                moveInventoryItems(getItemHandler(), itemInventory);
            }
        }
    }

    private void moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory) {
        for (int srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            ItemStack sourceStack = sourceInventory.extractItem(srcIndex, Math.min(transferRate, maxTransferRate), true);
            if (sourceStack.isEmpty()) {
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
}
