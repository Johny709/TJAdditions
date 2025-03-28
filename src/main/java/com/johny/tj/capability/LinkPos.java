package com.johny.tj.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface LinkPos {

    int getRange();

    int getBlockPosSize();

    BlockPos getBlockPos(int i);

    void setBlockPos(double x, double y, double z, boolean connect, int i);

    World world();

    int getPageIndex();

    int getPageSize();

    void setLinkData(NBTTagCompound linkData);

    NBTTagCompound getLinkData();
}
