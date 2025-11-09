package tj.items.covers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CoverEnderProfile {

    private final UUID owner;
    private final Set<UUID> allowedUsers = new HashSet<>();
    private boolean isPublic = true;

    public CoverEnderProfile(UUID owner) {
       this.owner = owner;
       this.allowedUsers.add(this.owner);
    }

    public static CoverEnderProfile fromNBT(NBTTagCompound nbt) {
        NBTTagCompound compound = nbt.getCompoundTag("coverProfile");
        UUID uuid = compound.hasUniqueId("owner") ? compound.getUniqueId("owner") : null;
        CoverEnderProfile coverEnderProfile = new CoverEnderProfile(uuid);
        coverEnderProfile.readFromNBT(nbt);
        return coverEnderProfile;
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
