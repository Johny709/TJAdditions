package com.johny.tj.util;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class EnderWorldData extends WorldSavedData {

    private static final Map<String, FluidTank> fluidTankMap = new HashMap<>();
    private static EnderWorldData INSTANCE;

    public static void init() {
        fluidTankMap.putIfAbsent("default", new FluidTank(Integer.MAX_VALUE));
    }

    public EnderWorldData(String name) {
        super(name);
    }

    public static Map<String, FluidTank> getFluidTankMap() {
        return fluidTankMap;
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList tankList = new NBTTagList(),
                idList = new NBTTagList();

        int i = 0;
        for (Map.Entry<String, FluidTank> tank : fluidTankMap.entrySet()) {
            NBTTagCompound tankCompound = new NBTTagCompound(), colorCompound = new NBTTagCompound();
            tankCompound.setTag("Tank" + i, tank.getValue().writeToNBT(new NBTTagCompound()));
            colorCompound.setString("ID" + i, tank.getKey());

            tankList.appendTag(tankCompound);
            idList.appendTag(colorCompound);
            i++;
        }
        nbt.setTag("TankList", tankList);
        nbt.setTag("TextList", idList);
        return nbt;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList tankList = nbt.getTagList("TankList", Constants.NBT.TAG_COMPOUND),
                idList = nbt.getTagList("TextList", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < tankList.tagCount(); i++) {
            NBTTagCompound tankCompound = tankList.getCompoundTagAt(i).getCompoundTag("Tank" + i);
            String textCompound = idList.getCompoundTagAt(i).getString("ID" + i);

            FluidTank tank = new FluidTank(Integer.MAX_VALUE);
            tank.readFromNBT(tankCompound);
            fluidTankMap.put(textCompound, tank);
        }
    }

    public static void setDirty() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && INSTANCE != null) {
            INSTANCE.markDirty();
        }
    }

    public static void setInstance(EnderWorldData instance) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            INSTANCE = instance;
        }
    }
}
