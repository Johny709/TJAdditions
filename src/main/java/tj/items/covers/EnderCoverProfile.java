package tj.items.covers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.*;

public class EnderCoverProfile<V> {

    private final UUID owner;
    private final Set<UUID> allowedUsers = new HashSet<>();
    private final Map<String, Set<AbstractEnderCover<V>>> covers = new Object2ObjectOpenHashMap<>();
    private final Map<String, V> entries;
    private boolean isPublic = true;

    public EnderCoverProfile(UUID owner, Map<String, V> entries) {
        this.owner = owner;
        this.entries = entries;
        this.allowedUsers.add(this.owner);
        for (String key : entries.keySet())
            this.covers.put(key, new HashSet<>());
    }

    public static <V> EnderCoverProfile<V> fromNBT(NBTTagCompound nbt, Map<String, V> entries) {
        NBTTagCompound compound = nbt.getCompoundTag("coverProfile");
        UUID uuid = compound.hasUniqueId("owner") ? compound.getUniqueId("owner") : null;
        EnderCoverProfile<V> enderCoverProfile = new EnderCoverProfile<>(uuid, entries);
        enderCoverProfile.readFromNBT(nbt);
        return enderCoverProfile;
    }

    public void addCoverToEntry(String key, AbstractEnderCover<V> cover) {
        Set<AbstractEnderCover<V>> set = this.covers.get(key);
        if (set != null)
            set.add(cover);
    }

    public void removeCoverFromEntry(String key, AbstractEnderCover<V> cover) {
        this.covers.getOrDefault(key, new HashSet<>()).remove(cover);
    }

    public boolean containsEntry(String key) {
        return this.entries.containsKey(key);
    }

    public void removeEntry(String key) {
        Set<AbstractEnderCover<V>> set = this.covers.remove(key);
        this.entries.remove(key);
        for (AbstractEnderCover<V> cover : set) {
            cover.setLastEntry(null);
            cover.setHandler(null);
            cover.markAsDirty();
        }
    }

    public void editEntry(String key, V handler) {
        for (AbstractEnderCover<V> cover : this.covers.get(key))
            cover.setHandler(handler);
    }

    public void editEntry(String oldKey, String newKey) {
        Set<AbstractEnderCover<V>> set = this.covers.remove(oldKey);
        this.entries.put(newKey, this.entries.remove(oldKey));
        this.covers.put(newKey, set);
        for (AbstractEnderCover<V> cover : set) {
            cover.setLastEntry(newKey);
            cover.markAsDirty();
        }
    }

    public void addEntry(String key, V handler) {
        this.entries.putIfAbsent(key, handler);
        this.covers.putIfAbsent(key, new HashSet<>());
    }

    public void editChannel(String key) {
        for (Map.Entry<String, Set<AbstractEnderCover<V>>> entry : this.covers.entrySet()) {
            for (AbstractEnderCover<V> cover : entry.getValue()) {
                cover.setChannel(key);
                cover.markAsDirty();
            }
        }
    }

    public void removeChannel() {
        for (Map.Entry<String, Set<AbstractEnderCover<V>>> entry : this.covers.entrySet()) {
            for (AbstractEnderCover<V> cover : entry.getValue()) {
                cover.setLastEntry(null);
                cover.setHandler(null);
                cover.setChannel(null);
                cover.markAsDirty();
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
