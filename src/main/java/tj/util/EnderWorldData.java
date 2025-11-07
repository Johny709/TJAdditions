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
import java.util.UUID;

public class EnderWorldData extends WorldSavedData {

    private static EnderWorldData INSTANCE;
    private final Map<UUID, Map<String, FluidTank>> fluidTankPlayerMap = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, Map<String, LargeItemStackHandler>> itemChestPlayerMap = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, Map<String, BasicEnergyHandler>> energyContainerPlayerMap = new Object2ObjectOpenHashMap<>();

    public EnderWorldData(String name) {
        super(name);
        this.fluidTankPlayerMap.putIfAbsent(null, new Object2ObjectOpenHashMap<>());
        this.itemChestPlayerMap.putIfAbsent(null, new Object2ObjectOpenHashMap<>());
        this.energyContainerPlayerMap.putIfAbsent(null, new Object2ObjectOpenHashMap<>());
    }

    public static EnderWorldData getINSTANCE() {
        return INSTANCE;
    }

    public Map<String, FluidTank> getFluidTankMap(UUID value) {
        return this.fluidTankPlayerMap.get(value);
    }

    public Map<String, LargeItemStackHandler> getItemChestMap(UUID value) {
        return this.itemChestPlayerMap.get(value);
    }

    public Map<String, BasicEnergyHandler> getEnergyContainerMap(UUID value) {
        return this.energyContainerPlayerMap.get(value);
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList fluidMap = new NBTTagList();
        for (Map.Entry<UUID, Map<String, FluidTank>> playerEntry : this.fluidTankPlayerMap.entrySet()) {
            NBTTagCompound playerCompound = new NBTTagCompound();
            NBTTagList fluidList = new NBTTagList();
            for (Map.Entry<String, FluidTank> entry : playerEntry.getValue().entrySet()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setString("key", entry.getKey());
                compound.setLong("capacity", entry.getValue().getCapacity());
                entry.getValue().writeToNBT(compound);
                fluidList.appendTag(compound);
            }
            if (playerEntry.getKey() != null)
                playerCompound.setUniqueId("id", playerEntry.getKey());
            playerCompound.setTag("fluidList", fluidList);
            fluidMap.appendTag(playerCompound);
        }
        NBTTagList itemMap = new NBTTagList();
        for (Map.Entry<UUID, Map<String, LargeItemStackHandler>> playerEntry : this.itemChestPlayerMap.entrySet()) {
            NBTTagCompound playerCompound = new NBTTagCompound();
            NBTTagList itemList = new NBTTagList();
            for (Map.Entry<String, LargeItemStackHandler> entry : playerEntry.getValue().entrySet()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setString("key", entry.getKey());
                compound.setLong("capacity", entry.getValue().getCapacity());
                compound.setTag("stack", entry.getValue().serializeNBT());
                itemList.appendTag(compound);
            }
            if (playerEntry.getKey() != null)
                playerCompound.setUniqueId("id", playerEntry.getKey());
            playerCompound.setTag("itemList", itemList);
            itemMap.appendTag(playerCompound);
        }
        NBTTagList energyMap = new NBTTagList();
        for (Map.Entry<UUID, Map<String, BasicEnergyHandler>> playerEntry : this.energyContainerPlayerMap.entrySet()) {
            NBTTagCompound playerCompound = new NBTTagCompound();
            NBTTagList energyList = new NBTTagList();
            for (Map.Entry<String, BasicEnergyHandler> entry : playerEntry.getValue().entrySet()) {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setString("key", entry.getKey());
                entry.getValue().writeToNBT(compound);
                energyList.appendTag(compound);
            }
            if (playerEntry.getKey() != null)
                playerCompound.setUniqueId("id", playerEntry.getKey());
            playerCompound.setTag("energyList", energyList);
            energyMap.appendTag(playerCompound);
        }
        nbt.setTag("fluidMap", fluidMap);
        nbt.setTag("itemMap", itemMap);
        nbt.setTag("energyMap", energyMap);
        return nbt;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList fluidMap = nbt.getTagList("fluidMap", 10);
        for (int i = 0; i < fluidMap.tagCount(); i++) {
            NBTTagCompound playerCompound = fluidMap.getCompoundTagAt(i);
            NBTTagList fluidList = playerCompound.getTagList("fluidList", 10);
            Map<String, FluidTank> fluidTankMap = new Object2ObjectOpenHashMap<>();
            for (int j = 0; j < fluidList.tagCount(); j++) {
                NBTTagCompound compound = fluidList.getCompoundTagAt(j);
                fluidTankMap.put(compound.getString("key"), new FluidTank(compound.getInteger("capacity")).readFromNBT(compound));
            }
            UUID id = playerCompound.hasUniqueId("id") ? playerCompound.getUniqueId("id") : null;
            this.fluidTankPlayerMap.put(id, fluidTankMap);
        }
        NBTTagList itemMap = nbt.getTagList("itemMap", 10);
        for (int i = 0; i < itemMap.tagCount(); i++) {
            NBTTagCompound playerCompound = itemMap.getCompoundTagAt(i);
            NBTTagList itemList = playerCompound.getTagList("itemList", 10);
            Map<String, LargeItemStackHandler> itemChestMap = new Object2ObjectOpenHashMap<>();
            for (int j = 0; j < itemList.tagCount(); j++) {
                NBTTagCompound compound = itemList.getCompoundTagAt(j);
                LargeItemStackHandler itemStackHandler = new LargeItemStackHandler(1, compound.getInteger("capacity"));
                itemStackHandler.deserializeNBT(compound.getCompoundTag("stack"));
                itemChestMap.put(compound.getString("key"), itemStackHandler);
            }
            UUID id = playerCompound.hasUniqueId("id") ? playerCompound.getUniqueId("id") : null;
            this.itemChestPlayerMap.put(id, itemChestMap);
        }
        NBTTagList energyMap = nbt.getTagList("energyMap", 10);
        for (int i = 0; i < energyMap.tagCount(); i++) {
            NBTTagCompound playerCompound = energyMap.getCompoundTagAt(i);
            NBTTagList energyList = playerCompound.getTagList("energyList", 10);
            Map<String, BasicEnergyHandler> energyContainerMap = new Object2ObjectOpenHashMap<>();
            for (int j = 0; j < energyList.tagCount(); j++) {
                NBTTagCompound compound = energyList.getCompoundTagAt(j);
                BasicEnergyHandler energyHandler = new BasicEnergyHandler(0);
                energyHandler.readFromNBT(compound);
                energyContainerMap.put(compound.getString("key"), energyHandler);
            }
            UUID id = playerCompound.hasUniqueId("id") ? playerCompound.getUniqueId("id") : null;
            this.energyContainerPlayerMap.put(id, energyContainerMap);
        }
    }

    public void setDirty() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && INSTANCE != null) {
            INSTANCE.markDirty();
        }
    }

    public void setInstance(EnderWorldData instance) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            INSTANCE = instance;
        }
    }
}
