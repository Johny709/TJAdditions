package tj.gui.widgets;

import gregtech.api.gui.INativeWidget;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.IScissored;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.function.Supplier;

public class TJSlotWidget extends Widget implements INativeWidget {

    protected Slot slotReference;
    protected boolean isEnabled = true;

    protected boolean canTakeItems;
    protected boolean canPutItems;
    protected SlotLocationInfo locationInfo = new SlotLocationInfo(false, false);

    protected TextureArea[] backgroundTexture;
    protected Runnable changeListener;

    protected Rectangle scissor;

    public TJSlotWidget(Supplier<IItemHandler> itemHandler, int slotIndex, int xPosition, int yPosition, int width, int height, boolean canTakeItems, boolean canPutItems) {
        super(new Position(xPosition, yPosition), new Size(width, height));
        this.canTakeItems = canTakeItems;
        this.canPutItems = canPutItems;
        this.slotReference = createSlot(itemHandler, slotIndex);
    }

    public TJSlotWidget(Supplier<IItemHandler> itemHandler, int slotIndex, int xPosition, int yPosition, boolean canTakeItems, boolean canPutItems) {
        this(itemHandler, slotIndex, xPosition, yPosition, 18, 18, canTakeItems, canPutItems);
    }

    public TJSlotWidget(Supplier<IItemHandler> itemHandler, int slotIndex, int xPosition, int yPosition) {
        this(itemHandler, slotIndex, xPosition, yPosition, 18, 18, true, true);
    }

    protected Slot createSlot(Supplier<IItemHandler> itemHandler, int index) {
        return new TJWidgetSlotItemHandler(itemHandler, index, 0, 0);
    }

    public TJSlotWidget setSize(int width, int height) {
        super.setSize(new Size(width, height));
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        if (isEnabled() && backgroundTexture != null) {
            Position pos = getPosition();
            Size size = getSize();
            for (TextureArea backgroundTexture : this.backgroundTexture) {
                backgroundTexture.draw(pos.x, pos.y, size.width, size.height);
            }
        }
    }

    @Override
    protected void onPositionUpdate() {
        if (slotReference != null && sizes != null) {
            Position position = getPosition();
            this.slotReference.xPos = position.x + 1 - sizes.getGuiLeft();
            this.slotReference.yPos = position.y + 1 - sizes.getGuiTop();
        }
    }

    public TJSlotWidget setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public void applyScissor(final int parentX, final int parentY, final int parentWidth, final int parentHeight) {
        this.scissor = new Rectangle(parentX, parentY, parentWidth, parentHeight);
    }

    /**
     * Sets array of background textures used by slot
     * they are drawn on top of each other
     */
    public TJSlotWidget setBackgroundTexture(TextureArea... backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
        return this;
    }

    public TJSlotWidget setLocationInfo(boolean isPlayerInventory, boolean isHotbarSlot) {
        this.locationInfo = new SlotLocationInfo(isPlayerInventory, isHotbarSlot);
        return this;
    }

    @Override
    public SlotLocationInfo getSlotLocationInfo() {
        return locationInfo;
    }

    public boolean canPutStack(ItemStack stack) {
        return isEnabled() && canPutItems;
    }

    public boolean canTakeStack(EntityPlayer player) {
        return isEnabled() && canTakeItems;
    }

    public boolean isEnabled() {
        if (!this.isEnabled) {
            return false;
        }
        if (this.scissor == null) {
            return true;
        }
        return scissor.intersects(toRectangleBox());
    }

    @Override
    public boolean canMergeSlot(ItemStack stack) {
        return isEnabled();
    }

    public void onSlotChanged() {
        gui.holder.markAsDirty();
    }

    @Override
    public ItemStack slotClick(int dragType, ClickType clickTypeIn, EntityPlayer player) {
        return INativeWidget.VANILLA_LOGIC;
    }

    @Override
    public final Slot getHandle() {
        return slotReference;
    }

    protected class TJWidgetSlotItemHandler extends Slot implements IScissored {

        private static final IInventory emptyInventory = new InventoryBasic("[Null]", true, 0);
        private final Supplier<IItemHandler> itemHandler;
        private final int index;

        public TJWidgetSlotItemHandler(Supplier<IItemHandler> itemHandler, int index, int xPosition, int yPosition) {
            super(emptyInventory, index, xPosition, yPosition);
            this.itemHandler = itemHandler;
            this.index = index;
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            if (stack.isEmpty() || !itemHandler.get().isItemValid(index, stack))
                return false;

            IItemHandler handler = this.getItemHandler();
            ItemStack remainder;
            if (handler instanceof IItemHandlerModifiable) {
                IItemHandlerModifiable handlerModifiable = (IItemHandlerModifiable) handler;
                ItemStack currentStack = handlerModifiable.getStackInSlot(index);

                handlerModifiable.setStackInSlot(index, ItemStack.EMPTY);

                remainder = handlerModifiable.insertItem(index, stack, true);

                handlerModifiable.setStackInSlot(index, currentStack);
            } else {
                remainder = handler.insertItem(index, stack, true);
            }
            return TJSlotWidget.this.canPutStack(stack) && remainder.getCount() < stack.getCount();
        }

        @Override
        @Nonnull
        public ItemStack getStack() {
            return this.getItemHandler().getStackInSlot(index);
        }

        @Override
        public boolean canTakeStack(@Nonnull EntityPlayer playerIn) {
            return TJSlotWidget.this.canTakeStack(playerIn) && !this.getItemHandler().extractItem(index, 1, true).isEmpty();
        }

        @Override
        public void putStack(@Nonnull ItemStack stack) {
            ((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(index, stack);
            this.onSlotChanged();

            if (changeListener != null) {
                changeListener.run();
            }
        }

        @Override
        public int getSlotStackLimit() {
            return this.itemHandler.get().getSlotLimit(this.index);
        }

        @Override
        public int getItemStackLimit(@Nonnull ItemStack stack) {
            ItemStack maxAdd = stack.copy();
            int maxInput = stack.getMaxStackSize();
            maxAdd.setCount(maxInput);

            IItemHandler handler = this.getItemHandler();
            ItemStack currentStack = handler.getStackInSlot(index);
            if (handler instanceof IItemHandlerModifiable) {
                IItemHandlerModifiable handlerModifiable = (IItemHandlerModifiable) handler;

                handlerModifiable.setStackInSlot(index, ItemStack.EMPTY);

                ItemStack remainder = handlerModifiable.insertItem(index, maxAdd, true);

                handlerModifiable.setStackInSlot(index, currentStack);

                return maxInput - remainder.getCount();
            } else {
                ItemStack remainder = handler.insertItem(index, maxAdd, true);

                int current = currentStack.getCount();
                int added = maxInput - remainder.getCount();
                return current + added;
            }
        }

        @Override
        @Nonnull
        public ItemStack decrStackSize(int amount) {
            return this.getItemHandler().extractItem(index, amount, false);
        }

        public IItemHandler getItemHandler() {
            return itemHandler.get();
        }

        @Override
        public boolean isSameInventory(@Nonnull Slot other) {
            return other instanceof SlotItemHandler && ((SlotItemHandler) other).getItemHandler() == itemHandler.get();
        }

        @Override
        @Nonnull
        public final ItemStack onTake(@Nonnull EntityPlayer thePlayer, @Nonnull ItemStack stack) {
            return onItemTake(thePlayer, super.onTake(thePlayer, stack), false);
        }

        @Override
        public void onSlotChanged() {
            TJSlotWidget.this.onSlotChanged();
        }

        @Override
        public boolean isEnabled() {
            return TJSlotWidget.this.isEnabled();
        }

        @Override
        public Rectangle getScissor() {
            return TJSlotWidget.this.scissor;
        }
    }
}
