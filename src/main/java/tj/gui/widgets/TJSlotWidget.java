package tj.gui.widgets;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.lwjgl.input.Keyboard;
import tj.util.ItemStackHelper;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

public class TJSlotWidget extends Widget implements IItemSlotHandler {

    private final IItemHandler itemHandler;
    private final int slotIndex;
    private BooleanSupplier takeItemsPredicate;
    private BooleanSupplier putItemsPredicate;
    private TextureArea[] backgroundTexture;
    private IWidgetGroup widgetGroup;

    @SideOnly(Side.CLIENT)
    private boolean isDragging;

    @SideOnly(Side.CLIENT)
    private boolean slotModified;

    public TJSlotWidget(IItemHandler itemHandler, int slotIndex, int x, int y) {
        super(new Position(x, y), new Size(18, 18));
        this.itemHandler = itemHandler;
        this.slotIndex = slotIndex;
    }

    public TJSlotWidget setWidgetGroup(IWidgetGroup widgetGroup) {
        this.widgetGroup = widgetGroup;
        return this;
    }

    public TJSlotWidget setTakeItemsPredicate(BooleanSupplier takeItemsPredicate) {
        this.takeItemsPredicate = takeItemsPredicate;
        return this;
    }

    public TJSlotWidget setPutItemsPredicate(BooleanSupplier putItemsPredicate) {
        this.putItemsPredicate = putItemsPredicate;
        return this;
    }

    public TJSlotWidget setBackgroundTexture(TextureArea... backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        ItemStack stack;
        if (this.isMouseOverElement(mouseX, mouseY) && this.itemHandler != null && !(stack = this.itemHandler.getStackInSlot(this.slotIndex)).isEmpty()) {
            List<String> tooltip = getItemToolTip(stack);
            String itemStoredText = I18n.format("gregtech.item_list.item_stored", stack.getCount());
            tooltip.add(TextFormatting.GRAY + itemStoredText);
            drawHoveringText(stack, tooltip, -1, mouseX, mouseY);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        Position pos = this.getPosition();
        int stackX = pos.getX() + 1;
        int stackY = pos.getY() + 1;
        if (this.backgroundTexture != null)
            for (TextureArea textureArea : this.backgroundTexture) {
                textureArea.draw(pos.getX(), pos.getY(), 18, 18);
            }
        if (this.itemHandler != null) {
            ItemStack stack = this.itemHandler.getStackInSlot(this.slotIndex);
            if (!stack.isEmpty()) {
                drawItemStack(stack, stackX, stackY, null);
            }
            if (this.isMouseOverElement(mouseX, mouseY))
                drawSelectionOverlay(stackX, stackY, 16, 16);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        this.isDragging = true;
        if (this.isMouseOverElement(mouseX, mouseY)) {
            this.slotModified = true;
            if (this.widgetGroup == null || this.widgetGroup.getTimer() < 1) {
                boolean isCtrlKeyPressed = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                boolean isShiftKeyPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
                this.writeClientAction(1, buffer -> {
                    buffer.writeBoolean(isCtrlKeyPressed);
                    buffer.writeBoolean(isShiftKeyPressed);
                    buffer.writeInt(button);
                });
            } else this.writeClientAction(3, buffer -> buffer.writeInt(64 - this.gui.entityPlayer.inventory.getItemStack().getCount()));
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseDragged(int mouseX, int mouseY, int button, long timeDragged) {
        if (this.isDragging && this.isMouseOverElement(mouseX, mouseY)) {
            if (!this.slotModified) {
                this.slotModified = true;
                this.writeClientAction(2, buffer -> buffer.writeInt(button));
            }
        } else this.slotModified = false;
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        this.isDragging = false;
        return false;
    }

    @Override
    public void detectAndSendChanges() {
        if (this.itemHandler != null)
            this.writeUpdateInfo(1, buffer -> buffer.writeItemStack(this.itemHandler.getStackInSlot(this.slotIndex)));
    }

    @Override
    public ItemStack insertStack(int slot, ItemStack stack, boolean simulate) {
        ItemStack inventoryStack = this.itemHandler.getStackInSlot(slot);
        if (inventoryStack.isEmpty() || inventoryStack.isItemEqual(stack) && ItemStack.areItemStackShareTagsEqual(inventoryStack, stack))
            return this.itemHandler.insertItem(slot, stack, simulate);
        else return stack;
    }

    private void insertStackAmount(int slot, ItemStack stack, int amount) {
        ItemStack oneStack = stack.copy();
        oneStack.setCount(amount);
        stack.shrink(amount - this.insertStack(slot, oneStack, false).getCount());
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        EntityPlayer player = this.gui.entityPlayer;
        ItemStack handStack = player.inventory.getItemStack();
        ItemStack newStack = handStack;
        if (id == 1) {
            boolean isCtrlKeyPressed = buffer.readBoolean();
            boolean isShiftKeyPressed = buffer.readBoolean();
            int button = buffer.readInt();
            if (button == 0) {
                if (handStack.isEmpty())
                    if (this.takeItemsPredicate == null || this.takeItemsPredicate.getAsBoolean()) {
                        int amount = isCtrlKeyPressed ? Integer.MAX_VALUE : 64;
                        newStack = this.itemHandler.extractItem(this.slotIndex, amount, false);
                        if (isShiftKeyPressed)
                            newStack = ItemStackHelper.insertInMainInventory(player.inventory, newStack);
                        if (this.widgetGroup != null)
                            this.writeUpdateInfo(3, buffer1 -> buffer1.writeInt(5));
                    } else return;
                else if (this.putItemsPredicate == null || this.putItemsPredicate.getAsBoolean())
                    newStack = this.insertStack(this.slotIndex, handStack, false);
                else return;
            } else if (button == 1) {
                if (handStack.isEmpty()) {
                    if (this.takeItemsPredicate == null || this.takeItemsPredicate.getAsBoolean()) {
                        ItemStack stack = this.itemHandler.getStackInSlot(this.slotIndex);
                        newStack = this.itemHandler.extractItem(this.slotIndex, Math.max(1, stack.getCount() / 2), false);
                    } else return;
                } else {
                    if (this.putItemsPredicate == null || this.putItemsPredicate.getAsBoolean()) {
                        this.insertStackAmount(this.slotIndex, handStack, 1);
                    } else return;
                }
            } else if (button == 2) {
                if (player.isCreative()) {
                    newStack = this.itemHandler.getStackInSlot(this.slotIndex).copy();
                    newStack.setCount(64);
                }
            }
        } else if (id == 2) {
            int button = buffer.readInt();
            if (button == 1)
                this.insertStackAmount(this.slotIndex, newStack, 1);
        } else if (id == 3) {
            int amount = buffer.readInt();
            newStack = ItemStackHelper.extractFromItemHandler(this.itemHandler, newStack, amount, false);
        }
        final ItemStack finalStack = newStack;
        player.inventory.setItemStack(finalStack);
        this.writeUpdateInfo(2, buffer1 -> buffer1.writeItemStack(finalStack));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        if (id == 1) {
            try {
                ItemStack stack = buffer.readItemStack();
                if (this.itemHandler instanceof IItemHandlerModifiable)
                    ((IItemHandlerModifiable) this.itemHandler).setStackInSlot(this.slotIndex, stack);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (id == 2) {
            try {
                ItemStack stack = buffer.readItemStack();
                this.gui.entityPlayer.inventory.setItemStack(stack);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (id == 3) {
            if (this.widgetGroup != null)
                this.widgetGroup.setTimer(buffer.readInt());
        }
    }
}
