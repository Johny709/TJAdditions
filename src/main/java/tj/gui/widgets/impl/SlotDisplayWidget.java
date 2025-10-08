package tj.gui.widgets.impl;

import gregtech.api.util.GTLog;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import tj.gui.widgets.TJSlotWidget;

import java.io.IOException;
import java.util.function.BiConsumer;

public class SlotDisplayWidget extends TJSlotWidget {

    private BiConsumer<Integer, ItemStack> onPressed;

    public SlotDisplayWidget(IItemHandler itemHandler, int slotIndex, int x, int y) {
        super(itemHandler, slotIndex, x, y);
        this.setTakeItemsPredicate(() -> false);
        this.setPutItemsPredicate(() -> false);
    }

    public SlotDisplayWidget onPressedConsumer(BiConsumer<Integer, ItemStack> onPressed) {
        this.onPressed = onPressed;
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.isMouseOverElement(mouseX, mouseY))
            this.writeClientAction(1, buffer -> {
                buffer.writeInt(button);
                buffer.writeItemStack(this.getItemHandler().getStackInSlot(this.index()));
            });
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseDragged(int mouseX, int mouseY, int button, long timeDragged) {
        return false;
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        if (id == 1) {
            try {
                int index = buffer.readInt();
                ItemStack stack = buffer.readItemStack();
                if (this.onPressed != null)
                    this.onPressed.accept(index, stack);
            } catch (IOException e) {
                GTLog.logger.error(e);
            }
        }
    }
}
