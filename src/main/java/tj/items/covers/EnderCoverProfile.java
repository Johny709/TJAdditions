package tj.items.covers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import tj.capability.IEnderNotifiable;

import java.util.*;

public class EnderCoverProfile<V> {

    private final UUID owner;
    /**
     * Permission Index:
     * 0 -> can see entries: true = 1 / false = 0
     * 1 -> can modify entries: true = 1 / false = 0
     * 2 -> can use entry: true = 1 / false = 0
     * 3 -> can see channels: true = 1 / false = 0
     * 4 -> can modify channels: true = 1 / false = 0
     * 5 -> can use channel: true = 1 / false = 0
     * 6 -> max throughput: 0 - max long
     */
    private final Map<UUID, long[]> allowedUsers = new Object2ObjectOpenHashMap<>();
    private final Map<String, Set<IEnderNotifiable<V>>> notifyMap = new Object2ObjectOpenHashMap<>();
    private final Map<String, V> entries = new Object2ObjectOpenHashMap<>();
    private boolean isPublic = true;

    public EnderCoverProfile(UUID owner, Map<String, V> entries) {
        this.owner = owner;
        this.entries.putAll(entries);
        this.allowedUsers.put(this.owner, new long[]{1, 1, 1, 1, 1, 1, Long.MAX_VALUE});
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

    public void removeEntry(String key, String id) {
        UUID uuid = UUID.fromString(id);
        if (this.owner != null && (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[1] != 1))
            return;
        Set<IEnderNotifiable<V>> set = this.notifyMap.remove(key);
        this.entries.remove(key);
        for (IEnderNotifiable<V> notifiable : set) {
            notifiable.setEntry(null);
            notifiable.setHandler(null);
            notifiable.markToDirty();
        }
    }

    public boolean setEntry(String key, String lastEntry, String id, IEnderNotifiable<V> notifiable) {
        UUID uuid = UUID.fromString(id);
        if (this.owner != null && (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[2] != 1))
            return false;
        this.removeFromNotifiable(lastEntry, notifiable);
        this.addToNotifiable(key, notifiable);
        return true;
    }

    public void editEntry(String key, String id, V handler) {
        UUID uuid = UUID.fromString(id);
        if (!this.entries.containsKey(key))
            return;
        if (this.owner != null && (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[1] != 1))
            return;
        for (IEnderNotifiable<V> cover : this.notifyMap.get(key))
            cover.setHandler(handler);
    }

    public void editEntry(String newKey, String id) {
        int index = id.lastIndexOf(":");
        String oldKey = id.substring(0, index);
        UUID uuid = UUID.fromString(id.substring(index + 1));
        if (!this.entries.containsKey(oldKey))
            return;
        if (this.owner != null && (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[1] != 1))
            return;
        Set<IEnderNotifiable<V>> set = this.notifyMap.remove(oldKey);
        this.entries.put(newKey, this.entries.remove(oldKey));
        this.notifyMap.put(newKey, set);
        for (IEnderNotifiable<V> notifiable : set) {
            notifiable.setEntry(newKey);
            notifiable.markToDirty();
        }
    }

    public void addEntry(String key, String id, V handler) {
        UUID uuid = UUID.fromString(id);
        if (key == null)
            return;
        if (this.owner != null && (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[1] != 1))
            return;
        this.entries.putIfAbsent(key, handler);
        this.notifyMap.putIfAbsent(key, new HashSet<>());
    }

    public boolean editChannel(String key, UUID uuid) {
        if (this.owner == null || (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[4] != 1))
            return false;
        for (Map.Entry<String, Set<IEnderNotifiable<V>>> entry : this.notifyMap.entrySet()) {
            for (IEnderNotifiable<V> notifiable : entry.getValue()) {
                notifiable.setChannel(key);
                notifiable.markToDirty();
            }
        }
        return true;
    }

    public boolean removeChannel(String id) {
        UUID uuid = UUID.fromString(id);
        if (this.owner == null || (this.allowedUsers.get(uuid) == null || this.allowedUsers.get(uuid)[4] != 1))
            return false;
        for (Map.Entry<String, Set<IEnderNotifiable<V>>> entry : this.notifyMap.entrySet()) {
            for (IEnderNotifiable<V> notifiable : entry.getValue()) {
                notifiable.setEntry(null);
                notifiable.setHandler(null);
                notifiable.setChannel(null);
                notifiable.markToDirty();
            }
        }
        return true;
    }

    public boolean addUser(UUID uuid, UUID owner) {
        if (this.allowedUsers.get(owner) != null && this.allowedUsers.get(owner)[4] == 1 && !this.allowedUsers.containsKey(uuid)) {
            this.allowedUsers.put(uuid, new long[]{0, 0, 0, 0, 0, 0, 0});
            return true;
        } else return false;
    }

    public void removeUser(UUID uuid, UUID owner) {
        if (this.allowedUsers.get(owner) != null && this.allowedUsers.get(owner)[4] == 1)
            this.allowedUsers.remove(uuid);
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

    public Map<UUID, long[]> getAllowedUsers() {
        return this.allowedUsers;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList userList = new NBTTagList();
        for (Map.Entry<UUID, long[]> entry : this.allowedUsers.entrySet()) {
            if (entry.getKey() != null) {
                NBTTagCompound compound1 = new NBTTagCompound();
                NBTTagList permissionList = new NBTTagList();
                for (long permission : entry.getValue()) {
                    permissionList.appendTag(new NBTTagLong(permission));
                }
                compound1.setUniqueId("user", entry.getKey());
                compound1.setTag("permissionList", permissionList);
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
            NBTTagList permissionList = userList.getCompoundTagAt(i).getTagList("permissionList", 4);
            long[] permissions = new long[permissionList.tagCount()];
            for (int j = 0; j < permissionList.tagCount(); j++) {
                permissions[j] = ((NBTTagLong) permissionList.get(j)).getLong();
            }
            this.allowedUsers.put(userList.getCompoundTagAt(i).getUniqueId("user"), permissions);
        }
    }
}
