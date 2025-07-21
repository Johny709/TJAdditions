package tj.util;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;


public class ItemStackHelper {

    /**
     * Tries to insert ItemStack into player's main inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInMainInventory(InventoryPlayer inventory, ItemStack stack) {
        return insertInPlayerInventory(inventory, stack, true, false, false);
    }

    /**
     * Tries to insert ItemStack into player's armor slot inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInArmorSlots(InventoryPlayer inventory, ItemStack stack) {
        return insertInPlayerInventory(inventory, stack, false, true, false);
    }

    /**
     * Tries to insert ItemStack into player's Off-Hand slot inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInOffHand(InventoryPlayer inventory, ItemStack stack) {
        return insertInPlayerInventory(inventory, stack, false, false, true);
    }

    /**
     * Tries to insert ItemStack into player inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @param mainInventory if you should insert in main inventory
     * @param armor if you should insert in armor slots
     * @param offHand if you should insert in off-Hand slot
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInPlayerInventory(InventoryPlayer inventory, ItemStack stack, boolean mainInventory, boolean armor, boolean offHand) {
        if (inventory == null || stack.isEmpty())
            return stack;
        if (mainInventory)
            insertToAvailableSlots(inventory.mainInventory, stack);
        if (armor)
            insertToAvailableSlots(inventory.armorInventory, stack);
        if (offHand)
            insertToAvailableSlots(inventory.offHandInventory, stack);
        return stack;
    }

    private static void insertToAvailableSlots(NonNullList<ItemStack> stackList, ItemStack stack) {
        for (int i = 0; i < stackList.size(); i++) {
            ItemStack inventoryStack = stackList.get(i);
            if (inventoryStack.isEmpty()) {
                int shrink = Math.min(stack.getCount(), 64);
                ItemStack newStack = stack.copy();
                newStack.setCount(shrink);
                stackList.set(i, newStack);
                stack.shrink(shrink);
            } else if (inventoryStack.isItemEqual(stack)) {
                int reminder = inventoryStack.getMaxStackSize() - inventoryStack.getCount();
                int shrink = Math.min(reminder, stack.getCount());
                inventoryStack.grow(shrink);
                stack.shrink(shrink);
            }
        }
    }

    /**
     * Tries to insert into container inventory or item handler
     * @param itemHandler container inventory
     * @param stack the ItemStack to insert
     * @param simulate test to see if the item can be inserted without actually inserting the item for real.
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertIntoItemHandler(IItemHandler itemHandler, @Nonnull ItemStack stack, boolean simulate) {
        if (itemHandler == null || stack.isEmpty())
            return stack;

        stack = simulate ? stack.copy() : stack;
        for (int i = 0; i < itemHandler.getSlots() && !stack.isEmpty(); i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            int maxStackSize = itemHandler.getSlotLimit(i);
            if (slotStack.isEmpty()) {
                stack = itemHandler.insertItem(i, stack, simulate);
            } else if (slotStack.isItemEqual(stack) && ItemStack.areItemStackShareTagsEqual(slotStack, stack)) {
                int reminder = Math.max(0, maxStackSize - slotStack.getCount());
                int extracted = Math.min(stack.getCount(), reminder);
                stack.shrink(extracted);
                if (!simulate)
                    slotStack.grow(extracted);
            }
        }
        return stack;
    }
}
