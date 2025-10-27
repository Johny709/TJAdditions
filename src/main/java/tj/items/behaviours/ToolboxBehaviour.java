package tj.items.behaviours;

import gregicadditions.item.GAMetaItems;
import gregicadditions.machines.multi.IMaintenance;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.gui.PlayerInventoryHolder;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.items.toolitem.IToolStats;
import gregtech.api.items.toolitem.ToolMetaItem;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.Position;
import gregtech.common.tools.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;
import tj.gui.GuiUtils;
import tj.gui.widgets.TJSlotWidget;
import tj.items.handlers.FilteredItemStackHandler;

public class ToolboxBehaviour implements IItemBehaviour, ItemUIFactory {

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote || hand == EnumHand.OFF_HAND)
            return ActionResult.newResult(EnumActionResult.FAIL, player.getHeldItem(hand));
        PlayerInventoryHolder holder = new PlayerInventoryHolder(player, hand);
        holder.openUI();
        return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, pos);
        if (metaTileEntity instanceof IMaintenance) {

        }
        return EnumActionResult.PASS;
    }

    @Override
    public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer player) {
        ItemStack playerStack = player.getHeldItem(EnumHand.MAIN_HAND);
        if (playerStack.getTagCompound() == null)
            playerStack.setTagCompound(new NBTTagCompound());
        final NBTTagCompound compound = playerStack.getTagCompound();
        ItemStackHandler toolboxInventory = new FilteredItemStackHandler(null, 9)
                .setItemStackPredicate((slot, stack) -> {
                    if (stack.getItem() instanceof ToolMetaItem<?>) {
                        IToolStats toolStats = ((ToolMetaItem<?>) stack.getItem()).getItem(stack).getToolStats();
                        return toolStats instanceof ToolHardHammer || toolStats instanceof ToolSoftHammer || toolStats instanceof ToolWrench || toolStats instanceof ToolScrewdriver || toolStats instanceof ToolWireCutter || toolStats instanceof ToolCrowbar;
                    }
                  return GAMetaItems.INSULATING_TAPE.isItemEqual(stack);
                });
        WidgetGroup widgetGroup = new WidgetGroup(new Position(7, 20));
        for (int i = 0; i < toolboxInventory.getSlots(); i++) {
            widgetGroup.addWidget(new TJSlotWidget(toolboxInventory, i, 18 * i, 0)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        return ModularUI.defaultBuilder()
                .bindOpenListener(() -> toolboxInventory.deserializeNBT(compound.getCompoundTag("inventory")))
                .bindCloseListener(() -> compound.setTag("inventory", toolboxInventory.serializeNBT()))
                .widget(widgetGroup)
                .widget(GuiUtils.bindPlayerInventory(new WidgetGroup(), player.inventory, 7, 84, playerStack))
                .label(7, 5, "metaitem.toolbox.name")
                .build(holder, player);
    }
}
