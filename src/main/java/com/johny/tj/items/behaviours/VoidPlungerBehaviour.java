package com.johny.tj.items.behaviours;

import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VoidPlungerBehaviour implements IItemBehaviour {

    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        NBTTagCompound nbt = player.getHeldItem(hand).getOrCreateSubCompound("Plunger");
        boolean voiding = nbt.getBoolean("Void");
        MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(world, pos);
        if (!world.isRemote) {
            IItemHandlerModifiable importItems, exportItems;
            IMultipleTankHandler importFluids, exportFluids;
            if (metaTileEntity != null) {
                importItems = metaTileEntity.getImportItems();
                importFluids = metaTileEntity.getImportFluids();
                exportItems = metaTileEntity.getExportItems();
                exportFluids = metaTileEntity.getExportFluids();
            } else {
                player.sendMessage(new TextComponentTranslation("metaitem.void_plunger.message.fail"));
                return EnumActionResult.SUCCESS;
            }
            if (metaTileEntity instanceof RecipeMapMultiblockController) {
                RecipeMapMultiblockController controller = (RecipeMapMultiblockController) metaTileEntity;
                importItems = controller.getInputInventory();
                importFluids = controller.getInputFluidInventory();
                exportItems = controller.getOutputInventory();
                exportFluids = controller.getOutputFluidInventory();
            } else if (metaTileEntity instanceof ParallelRecipeMapMultiblockController) {
                ParallelRecipeMapMultiblockController controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
                importItems = controller.getInputInventory();
                importFluids = controller.getInputFluidInventory();
                exportItems = controller.getOutputInventory();
                exportFluids = controller.getOutputFluidInventory();
            }
            List<ItemStack> importItemList = new ArrayList<>();
            List<FluidStack> importFluidList = new ArrayList<>();
            List<ItemStack> exportItemList = new ArrayList<>();
            List<FluidStack> exportFluidList = new ArrayList<>();

            for (int i = 0; i < importItems.getSlots(); i++) {
                ItemStack item = importItems.getStackInSlot(i);
                if (voiding || player.inventory.addItemStackToInventory(item)) {
                    addItemsToList(importItemList, item);
                    importItems.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            for (int i = 0; i < importFluids.getTanks(); i++) {
                FluidStack fluid = importFluids.getTankAt(i).getFluid();
                addFluidsToList(importFluidList, fluid);
                importFluids.getTankAt(i).drain(Integer.MAX_VALUE, true);
            }
            for (int i = 0; i < exportItems.getSlots(); i++) {
                ItemStack item = importItems.getStackInSlot(i);
                if (voiding || player.inventory.addItemStackToInventory(item)) {
                    addItemsToList(exportItemList, item);
                    importItems.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            for (int i = 0; i < exportFluids.getTanks(); i++) {
                FluidStack fluid = importFluids.getTankAt(i).getFluid();
                addFluidsToList(exportFluidList, fluid);
                importFluids.getTankAt(i).drain(Integer.MAX_VALUE, true);
            }

            ITextComponent importItemText = new TextComponentTranslation("metaitem.void_plunger.message.void.item.input");
            importItemList.stream()
                    .filter(item -> !item.isEmpty())
                    .forEach(item -> importItemText.appendText("\n").appendSibling(new TextComponentTranslation(item.getTranslationKey() + ".name")
                                    .setStyle(new Style().setColor(TextFormatting.GOLD))
                    .appendText(" ").appendSibling(new TextComponentString(String.valueOf(item.getCount())))));

            ITextComponent importFluidText = new TextComponentTranslation("metaitem.void_plunger.message.void.fluid.input");
            importFluidList.stream()
                    .filter(Objects::nonNull)
                    .forEach(fluid -> importFluidText.appendText("\n").appendSibling(new TextComponentTranslation(fluid.getUnlocalizedName())
                            .setStyle(new Style().setColor(TextFormatting.AQUA))
                    .appendText(" ").appendSibling(new TextComponentString(String.valueOf(fluid.amount)))));

            ITextComponent exportItemText = new TextComponentTranslation("metaitem.void_plunger.message.void.item.output");
            exportItemList.stream()
                    .filter(item -> !item.isEmpty())
                    .forEach(item -> exportItemText.appendText("\n").appendSibling(new TextComponentTranslation(item.getTranslationKey() + ".name")
                            .setStyle(new Style().setColor(TextFormatting.GOLD))
                    .appendText(" ").appendSibling(new TextComponentString(String.valueOf(item.getCount())))));

            ITextComponent exportFluidText = new TextComponentTranslation("metaitem.void_plunger.message.void.fluid.output");
            exportFluidList.stream()
                    .filter(Objects::nonNull)
                    .forEach(fluid -> exportFluidText.appendText("\n").appendSibling(new TextComponentTranslation(fluid.getUnlocalizedName())
                            .setStyle(new Style().setColor(TextFormatting.AQUA))
                    .appendText(" ").appendSibling(new TextComponentString(String.valueOf(fluid.amount)))));

            player.sendMessage(new TextComponentTranslation("metaitem.void_plunger.message.success")
                    .appendText("\n").appendSibling(importItemText)
                    .appendText("\n").appendSibling(importFluidText)
                    .appendText("\n").appendSibling(exportItemText)
                    .appendText("\n").appendSibling(exportFluidText));
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.SUCCESS;
    }

    private void addItemsToList(List<ItemStack> list, ItemStack item) {
        if (item.isEmpty())
            return;
        for (ItemStack getItem : list) {
            String nameToAdd = item.getTranslationKey();
            String nameFromList = getItem.getTranslationKey();
            if (nameToAdd.equals(nameFromList)) {
                int addItemCount = item.getCount() + getItem.getCount();
                getItem.setCount(addItemCount);
                return;
            }
        }
        list.add(item);
    }

    private void addFluidsToList(List<FluidStack> list, FluidStack fluid) {
        if (fluid == null)
            return;
        for (FluidStack getFluid : list) {
            String nameToAdd = fluid.getUnlocalizedName();
            String nameFromList = getFluid.getUnlocalizedName();
            if (nameToAdd.equals(nameFromList)) {
                getFluid.amount += fluid.amount;
                return;
            }
        }
        list.add(fluid);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        NBTTagCompound nbt = player.getHeldItem(hand).getOrCreateSubCompound("Plunger");
        boolean voiding = nbt.getBoolean("Void");
        nbt.setBoolean("Void", !voiding);
        if (world.isRemote)
            player.sendMessage(new TextComponentTranslation("metaitem.void_plunger.mode", !voiding));
        return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        NBTTagCompound nbt = itemStack.getOrCreateSubCompound("Plunger");
        boolean voiding = nbt.getBoolean("Void");
        lines.add(I18n.format("metaitem.void_plunger.description"));
        lines.add(I18n.format("metaitem.void_plunger.mode", voiding));
    }
}
