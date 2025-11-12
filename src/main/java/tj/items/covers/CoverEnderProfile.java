package tj.items.covers;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.*;

public class CoverEnderProfile {

    private final UUID owner;
    private final Set<UUID> allowedUsers = new HashSet<>();
    private final Map<String, Set<AbstractCoverEnder<?, ?>>> covers = new Object2ObjectOpenHashMap<>();
    private final Map<String, ?> entries;
    private boolean isPublic = true;

    public CoverEnderProfile(UUID owner, Map<String, ?> entries) {
        this.entries = entries;
        this.owner = owner;
        this.allowedUsers.add(this.owner);
        for (String key : entries.keySet())
            this.covers.put(key, new HashSet<>());
    }

    public static CoverEnderProfile fromNBT(NBTTagCompound nbt, Map<String, ?> entries) {
        NBTTagCompound compound = nbt.getCompoundTag("coverProfile");
        UUID uuid = compound.hasUniqueId("owner") ? compound.getUniqueId("owner") : null;
        CoverEnderProfile coverEnderProfile = new CoverEnderProfile(uuid, entries);
        coverEnderProfile.readFromNBT(nbt);
        return coverEnderProfile;
    }

    public void addCover(String key, AbstractCoverEnder<?, ?> cover) {
        Set<AbstractCoverEnder<?, ?>> set = this.covers.get(key);
        if (set != null)
            set.add(cover);
    }

    public void removeCover(String key, AbstractCoverEnder<?, ?> cover) {
        this.covers.getOrDefault(key, new HashSet<>()).remove(cover);
    }

    public void removeEntry(String key) {
        Set<AbstractCoverEnder<?, ?>> set = this.covers.remove(key);
        for (AbstractCoverEnder<?, ?> cover : set) {
            cover.setLastEntry(null);
            cover.setHandler(null);
        }
    }

    public void renameEntry(String oldKey, String newKey) {
        Set<AbstractCoverEnder<?, ?>> set = this.covers.remove(oldKey);
        this.covers.put(newKey, set);
        for (AbstractCoverEnder<?, ?> cover : set)
            cover.setLastEntry(newKey);
    }

    public void addEntry(String key) {
        this.covers.putIfAbsent(key, new HashSet<>());
    }

    public void setPublic(boolean isPublic) {
        if (this.owner != null)
            this.isPublic = isPublic;
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
            if (id == null)
                continue;
            NBTTagCompound compound1 = new NBTTagCompound();
            compound1.setUniqueId("user", id);
            userList.appendTag(compound1);
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
