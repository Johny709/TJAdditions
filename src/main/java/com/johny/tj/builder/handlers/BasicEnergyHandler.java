package com.johny.tj.builder.handlers;


import net.minecraft.nbt.NBTTagCompound;

public class BasicEnergyHandler {

    private long stored;
    private long capacity;

    public BasicEnergyHandler(long capacity) {
        this.capacity = capacity;
    }

    public long changeEnergy(long differenceAmount) {
        long oldEnergyStored = getStored();
        long newEnergyStored = (capacity - oldEnergyStored < differenceAmount) ? capacity : (oldEnergyStored + differenceAmount);
        if (newEnergyStored < 0)
            newEnergyStored = 0;
        this.stored = newEnergyStored;
        return newEnergyStored - oldEnergyStored;
    }

    public long addEnergy(long energyToAdd) {
        return changeEnergy(energyToAdd);
    }

    public long removeEnergy(long energyToRemove) {
        return changeEnergy(-energyToRemove);
    }

    public long getCapacity() {
        return capacity;
    }

    public long getStored() {
        return stored;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("Stored", stored);
        nbt.setLong("Capacity", capacity);
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        capacity = nbt.getLong("Capacity");
        stored = nbt.getLong("Stored");
    }
}
