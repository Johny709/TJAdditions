package com.johny.tj.capability;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface IItemHandlerInfo {

    List<ItemStack> getItemInputs();

    List<ItemStack> getItemOutputs();
}
