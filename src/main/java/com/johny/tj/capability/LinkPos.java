package com.johny.tj.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface LinkPos extends IPageCapable {

    int getRange();

    int getBlockPosSize();

    BlockPos getBlockPos(int i);

    World world();

    void setBlockPos(double x, double y, double z, boolean connect, int i);;

    NBTTagCompound getPosLinkData();

    void setPosLinkData(NBTTagCompound linkData);
}
