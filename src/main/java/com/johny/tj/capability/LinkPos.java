package com.johny.tj.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.IntSupplier;

public interface LinkPos extends IPageCapable {

    default boolean isInterDimensional() {
        return false;
    }

    default int dimensionID() {
        return 0;
    }

    default void setDimension(IntSupplier dimensionID, int index) {}

    default int getDimension(int index) {
        return 0;
    }

    int getRange();

    int getPosSize();

    default BlockPos getPos(int index) {
        return null;
    }

    default void setPos(BlockPos pos, EntityPlayer player, World world, int index) {}

    World world();

    NBTTagCompound getLinkData();

    void setLinkData(NBTTagCompound linkData);
}
