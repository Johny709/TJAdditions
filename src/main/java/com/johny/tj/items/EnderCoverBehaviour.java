package com.johny.tj.items;

import gregtech.api.items.metaitem.stats.IItemBehaviour;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.util.List;

public class EnderCoverBehaviour implements IItemBehaviour {

    private final int transferRate;

    public EnderCoverBehaviour(int tier) {
        this.transferRate = (int) Math.min(Math.pow(4, tier) * 16, Integer.MAX_VALUE);
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        lines.add(I18n.format("metaitem.ender_fluid_cover.transfer", transferRate));
    }
}
