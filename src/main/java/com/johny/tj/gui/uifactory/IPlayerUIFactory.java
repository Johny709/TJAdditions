package com.johny.tj.gui.uifactory;

import gregtech.api.gui.ModularUI;
import net.minecraft.entity.player.EntityPlayer;

public interface IPlayerUIFactory {

    ModularUI createUI(PlayerHolder holder, EntityPlayer player);
}
