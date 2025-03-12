package com.johny.tj.machines;

import net.minecraft.util.math.BlockPos;

public interface LinkPos {

    int getRange();

    int getBlockPosSize();

    BlockPos getBlockPos(int i);

    void setBlockPos(double x, double y, double z, boolean connect, int i);
}
