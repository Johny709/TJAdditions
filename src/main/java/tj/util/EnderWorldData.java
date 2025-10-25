package tj.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import tj.builder.handlers.BasicEnergyHandler;
import tj.items.handlers.LargeItemStackHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.util.Map;

public class EnderWorldData extends WorldSavedData {

    private static final Map<String, FluidTank> FLUID_TANK_MAP = new Object2ObjectOpenHashMap<>();
    private static final Map<String, LargeItemStackHandler> ITEM_CHEST_MAP = new Object2ObjectOpenHashMap<>();
    private static final Map<String, BasicEnergyHandler> ENERGY_CONTAINER_MAP = new Object2ObjectOpenHashMap<>();
    private static EnderWorldData INSTANCE;

    public EnderWorldData(String name) {
        super(name);
    }

    public static Map<String, FluidTank> getFluidTankMap() {
        return FLUID_TANK_MAP;
    }

    public static Map<String, LargeItemStackHandler> getItemChestMap() {
        return ITEM_CHEST_MAP;
    }

    public static Map<String, BasicEnergyHandler> getEnergyContainerMap() {
        return ENERGY_CONTAINER_MAP;
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList tankList = new NBTTagList(),
                tankIDList = new NBTTagList(),
                tankCapacityList = new NBTTagList(),
                chestList = new NBTTagList(),
                chestIDList = new NBTTagList(),
                chestCapacityList = new NBTTagList(),
                containerList = new NBTTagList(),
                containerIDList = new NBTTagList();

        int i = 0, j = 0, k = 0;
        for (Map.Entry<String, FluidTank> tank : FLUID_TANK_MAP.entrySet()) {
            NBTTagCompound tankCompound = new NBTTagCompound(), tankIDCompound = new NBTTagCompound(), tankCapacity = new NBTTagCompound();
            tankCompound.setTag("Tank" + i, tank.getValue().writeToNBT(new NBTTagCompound()));
            tankIDCompound.setString("ID" + i, tank.getKey());
            tankCapacity.setInteger("Capacity" + i, tank.getValue().getCapacity());

            tankList.appendTag(tankCompound);
            tankIDList.appendTag(tankIDCompound);
            tankCapacityList.appendTag(tankCapacity);
            i++;
        }

        for (Map.Entry<String, LargeItemStackHandler> chest : ITEM_CHEST_MAP.entrySet()) {
            NBTTagCompound chestCompound = new NBTTagCompound(), chestIDCompound = new NBTTagCompound(), chestCapacity = new NBTTagCompound();
            chestCompound.setTag("Chest" + j, chest.getValue().getStackInSlot(0).writeToNBT(new NBTTagCompound()));
            chestIDCompound.setString("ID" + j, chest.getKey());
            chestCapacity.setInteger("Capacity" + j, chest.getValue().getCapacity());

            chestList.appendTag(chestCompound);
            chestIDList.appendTag(chestIDCompound);
            chestCapacityList.appendTag(chestCapacity);
            j++;
        }

        for (Map.Entry<String, BasicEnergyHandler> container : ENERGY_CONTAINER_MAP.entrySet()) {
            NBTTagCompound containerCompound = new NBTTagCompound(), containerIDCompound = new NBTTagCompound();
            containerCompound.setTag("Container" + k, container.getValue().writeToNBT(new NBTTagCompound()));
            containerIDCompound.setString("ID" + k, container.getKey());

            containerList.appendTag(containerCompound);
            containerIDList.appendTag(containerIDCompound);
            k++;
        }
        nbt.setTag("TankList", tankList);
        nbt.setTag("TankIDList", tankIDList);
        nbt.setTag("TankCapacityList", tankCapacityList);
        nbt.setTag("ChestList", chestList);
        nbt.setTag("ChestIDList", chestIDList);
        nbt.setTag("ChestCapacityList", chestCapacityList);
        nbt.setTag("ContainerList", containerList);
        nbt.setTag("ContainerIDList", containerIDList);
        return nbt;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList tankList = nbt.getTagList("TankList", Constants.NBT.TAG_COMPOUND),
                tankIDList = nbt.getTagList("TankIDList", Constants.NBT.TAG_COMPOUND),
                tankCapacityList = nbt.getTagList("TankCapacityList", Constants.NBT.TAG_COMPOUND),
                chestList = nbt.getTagList("ChestList", Constants.NBT.TAG_COMPOUND),
                chestIDList = nbt.getTagList("ChestIDList", Constants.NBT.TAG_COMPOUND),
                chestCapacityList = nbt.getTagList("ChestCapacityList", Constants.NBT.TAG_COMPOUND),
                containerList = nbt.getTagList("ContainerList", Constants.NBT.TAG_COMPOUND),
                containerIDList = nbt.getTagList("ContainerIDList", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < tankList.tagCount(); i++) {
            NBTTagCompound tankCompound = tankList.getCompoundTagAt(i).getCompoundTag("Tank" + i);
            String tankIDCompound = tankIDList.getCompoundTagAt(i).getString("ID" + i);
            int tankCapacity = tankCapacityList.getCompoundTagAt(i).getInteger("Capacity" + i);

            FluidTank tank = new FluidTank(tankCapacity);
            tank.readFromNBT(tankCompound);
            FLUID_TANK_MAP.put(tankIDCompound, tank);
        }

        for (int i = 0; i < chestList.tagCount(); i++) {
            NBTTagCompound chestCompound = chestList.getCompoundTagAt(i).getCompoundTag("Chest" + i);
            String chestIDCompound = chestIDList.getCompoundTagAt(i).getString("ID" + i);
            int chestCapacity = chestCapacityList.getCompoundTagAt(i).getInteger("Capacity" + i);

            LargeItemStackHandler chest = new LargeItemStackHandler(1, chestCapacity);
            chest.setStackInSlot(0, new ItemStack(chestCompound));
            ITEM_CHEST_MAP.put(chestIDCompound, chest);
        }

        for (int i = 0; i < containerList.tagCount(); i++) {
            NBTTagCompound containerCompound = containerList.getCompoundTagAt(i).getCompoundTag("Container" + i);
            String containerIDCompound = containerIDList.getCompoundTagAt(i).getString("ID" + i);

            BasicEnergyHandler container = new BasicEnergyHandler(0);
            container.readFromNBT(containerCompound);
            ENERGY_CONTAINER_MAP.put(containerIDCompound, container);
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
