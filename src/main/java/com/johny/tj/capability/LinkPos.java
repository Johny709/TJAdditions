package com.johny.tj.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public interface LinkPos<T> extends IPageCapable {

    int getRange();

    int getPosSize();

    T getPos(int index);

    void setPos(T pos, int index);

    World world();

    NBTTagCompound getLinkData();

    void setLinkData(NBTTagCompound linkData);
}
