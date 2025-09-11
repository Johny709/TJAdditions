package tj.gui.widgets;

import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.function.BooleanSupplier;

public class TJSlotWidget extends SlotWidget {

    private BooleanSupplier takeItemsPredicate;
    private BooleanSupplier putItemsPredicate;

    public TJSlotWidget(IItemHandler itemHandler, int slotIndex, int xPosition, int yPosition, boolean canTakeItems, boolean canPutItems) {
        super(itemHandler, slotIndex, xPosition, yPosition, canTakeItems, canPutItems);
    }

    public TJSlotWidget setTakeItemsPredicate(BooleanSupplier takeItemsPredicate) {
        this.takeItemsPredicate = takeItemsPredicate;
        return this;
    }

    public TJSlotWidget setPutItemsPredicate(BooleanSupplier putItemsPredicate) {
        this.putItemsPredicate = putItemsPredicate;
        return this;
    }

    @Override
    public boolean canPutStack(ItemStack stack) {
        return this.putItemsPredicate.getAsBoolean() && super.canPutStack(stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return this.takeItemsPredicate.getAsBoolean() && super.canTakeStack(player);
    }
}
