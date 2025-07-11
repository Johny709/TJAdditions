package tj.util;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;


public class ItemStackHelper {

    /**
     * Tries to insert ItemStack into player's main inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInMainInventory(InventoryPlayer inventory, ItemStack stack) {
        return insertInInventory(inventory, stack, true, false, false);
    }

    /**
     * Tries to insert ItemStack into player's armor slot inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInArmorSlots(InventoryPlayer inventory, ItemStack stack) {
        return insertInInventory(inventory, stack, false, true, false);
    }

    /**
     * Tries to insert ItemStack into player's Off-Hand slot inventory. Make sure to return a new copy of ItemStack before inserting an item from ItemHandler
     * @param inventory player inventory
     * @param stack the ItemStack to insert
     * @return ItemStack reminder. returns empty when ItemStack is fully inserted. returns the stack unmodified when unable to insert at all.
     */
    public static ItemStack insertInOffHand(InventoryPlayer inventory, ItemStack stack) {
        return insertInInventory(inventory, stack, false, false, true);
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
    public static ItemStack insertInInventory(InventoryPlayer inventory, ItemStack stack, boolean mainInventory, boolean armor, boolean offHand) {
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
}
