package tj.items.covers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import tj.capability.IEnderNotifiable;

import java.util.*;

public class EnderCoverProfile<V> {

    private final UUID owner;
    private final Set<UUID> allowedUsers = new HashSet<>();
    private final Map<String, Set<IEnderNotifiable<V>>> notifyMap = new Object2ObjectOpenHashMap<>();
    private final Map<String, V> entries;
    private boolean isPublic = true;

    public EnderCoverProfile(UUID owner, Map<String, V> entries) {
        this.owner = owner;
        this.entries = entries;
        this.allowedUsers.add(this.owner);
        for (String key : entries.keySet())
            this.notifyMap.put(key, new HashSet<>());
    }

    public static <V> EnderCoverProfile<V> fromNBT(NBTTagCompound nbt, Map<String, V> entries) {
        NBTTagCompound compound = nbt.getCompoundTag("coverProfile");
        UUID uuid = compound.hasUniqueId("owner") ? compound.getUniqueId("owner") : null;
        EnderCoverProfile<V> enderCoverProfile = new EnderCoverProfile<>(uuid, entries);
        enderCoverProfile.readFromNBT(nbt);
        return enderCoverProfile;
    }

    public void addToNotifiable(String key, IEnderNotifiable<V> notifiable) {
        Set<IEnderNotifiable<V>> set = this.notifyMap.get(key);
        if (set != null)
            set.add(notifiable);
    }

    public void removeFromNotifiable(String key, IEnderNotifiable<V> notifiable) {
        this.notifyMap.getOrDefault(key, new HashSet<>()).remove(notifiable);
    }

    public boolean containsEntry(String key) {
        return this.entries.containsKey(key);
    }

    public void removeEntry(String key) {
        Set<IEnderNotifiable<V>> set = this.notifyMap.remove(key);
        this.entries.remove(key);
        for (IEnderNotifiable<V> notifiable : set) {
            notifiable.setEntry(null);
            notifiable.setHandler(null);
            notifiable.markToDirty();
        }
    }

    public void editEntry(String key, V handler) {
        for (IEnderNotifiable<V> cover : this.notifyMap.get(key))
            cover.setHandler(handler);
    }

    public void editEntry(String oldKey, String newKey) {
        Set<IEnderNotifiable<V>> set = this.notifyMap.remove(oldKey);
        this.entries.put(newKey, this.entries.remove(oldKey));
        this.notifyMap.put(newKey, set);
        for (IEnderNotifiable<V> notifiable : set) {
            notifiable.setEntry(newKey);
            notifiable.markToDirty();
        }
    }

    public void addEntry(String key, V handler) {
        this.entries.putIfAbsent(key, handler);
        this.notifyMap.putIfAbsent(key, new HashSet<>());
    }

    public void editChannel(String key) {
        for (Map.Entry<String, Set<IEnderNotifiable<V>>> entry : this.notifyMap.entrySet()) {
            for (IEnderNotifiable<V> notifiable : entry.getValue()) {
                notifiable.setChannel(key);
                notifiable.markToDirty();
            }
        }
    }

    public void removeChannel() {
        for (Map.Entry<String, Set<IEnderNotifiable<V>>> entry : this.notifyMap.entrySet()) {
            for (IEnderNotifiable<V> notifiable : entry.getValue()) {
                notifiable.setEntry(null);
                notifiable.setHandler(null);
                notifiable.setChannel(null);
                notifiable.markToDirty();
            }
        }
    }

    public void setPublic(boolean isPublic) {
        if (this.owner != null)
            this.isPublic = isPublic;
    }

    public Map<String, V> getEntries() {
        return this.entries;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public Set<UUID> getAllowedUsers() {
        return this.allowedUsers;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList userList = new NBTTagList();
        for (UUID id : this.allowedUsers) {
            if (id != null) {
                NBTTagCompound compound1 = new NBTTagCompound();
                compound1.setUniqueId("user", id);
                userList.appendTag(compound1);
            }
        }
        if (this.owner != null)
            compound.setUniqueId("owner", this.owner);
        compound.setBoolean("public", this.isPublic);
        compound.setTag("userList", userList);
        nbt.setTag("coverProfile", compound);
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagCompound compound = nbt.getCompoundTag("coverProfile");
        this.isPublic = compound.getBoolean("public");
        NBTTagList userList = compound.getTagList("userList", 10);
        for (int i = 0; i < userList.tagCount(); i++) {
            this.allowedUsers.add(userList.getCompoundTagAt(i).getUniqueId("user"));
        }
    }
}
