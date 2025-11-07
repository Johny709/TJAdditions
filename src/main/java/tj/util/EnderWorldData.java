package tj.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import tj.builder.handlers.BasicEnergyHandler;
import tj.items.handlers.LargeItemStackHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;
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
        NBTTagList fluidList = new NBTTagList(), itemList = new NBTTagList(), energyList = new NBTTagList();
        for (Map.Entry<String, FluidTank> entry : FLUID_TANK_MAP.entrySet()) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("key", entry.getKey());
            compound.setLong("capacity", entry.getValue().getCapacity());
            entry.getValue().writeToNBT(compound);
            fluidList.appendTag(compound);
        }
        for (Map.Entry<String, LargeItemStackHandler> entry : ITEM_CHEST_MAP.entrySet()) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("key", entry.getKey());
            compound.setLong("capacity", entry.getValue().getCapacity());
            compound.setTag("stack", entry.getValue().serializeNBT());
            itemList.appendTag(compound);
        }
        for (Map.Entry<String, BasicEnergyHandler> entry : ENERGY_CONTAINER_MAP.entrySet()) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("key", entry.getKey());
            entry.getValue().writeToNBT(compound);
            energyList.appendTag(compound);
        }
        nbt.setTag("fluidList", fluidList);
        nbt.setTag("itemList", itemList);
        nbt.setTag("energyList", energyList);
        return nbt;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList fluidList = nbt.getTagList("fluidList", 10), itemList = nbt.getTagList("itemList", 10), energyList = nbt.getTagList("energyList", 10);
        for (int i = 0; i < fluidList.tagCount(); i++) {
            NBTTagCompound compound = fluidList.getCompoundTagAt(i);
            FLUID_TANK_MAP.put(compound.getString("key"), new FluidTank(compound.getInteger("capacity")).readFromNBT(compound));
        }
        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound compound = itemList.getCompoundTagAt(i);
            LargeItemStackHandler itemStackHandler = new LargeItemStackHandler(1, compound.getInteger("capacity"));
            itemStackHandler.deserializeNBT(compound.getCompoundTag("stack"));
            ITEM_CHEST_MAP.put(compound.getString("key"), itemStackHandler);
        }
        for (int i = 0; i < energyList.tagCount(); i++) {
            NBTTagCompound compound = energyList.getCompoundTagAt(i);
            BasicEnergyHandler energyHandler = new BasicEnergyHandler(0);
            energyHandler.readFromNBT(compound);
            ENERGY_CONTAINER_MAP.put(compound.getString("key"), energyHandler);
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
