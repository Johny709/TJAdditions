package tj.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public class PlayerWorldIDData extends WorldSavedData {

    private final Map<UUID, Integer> PLAYER_WORLD_ID_MAP = new Object2ObjectOpenHashMap<>();
    private static PlayerWorldIDData INSTANCE;

    public PlayerWorldIDData(String name) {
        super(name);
        INSTANCE = this;
    }

    public static PlayerWorldIDData getINSTANCE() {
        return INSTANCE;
    }

    public Map<UUID, Integer> getPlayerWorldIdMap() {
        return PLAYER_WORLD_ID_MAP;
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList playerWorldIDList = new NBTTagList();
        for (Map.Entry<UUID, Integer> player : PLAYER_WORLD_ID_MAP.entrySet()) {
            NBTTagCompound playerCompound = new NBTTagCompound();
            playerCompound.setUniqueId("UUID", player.getKey());
            playerCompound.setInteger("WorldID", player.getValue());
            playerWorldIDList.appendTag(playerCompound);
        }

        nbt.setTag("PlayerWorldIDList", playerWorldIDList);
        return nbt;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound nbt) {
        NBTTagList playerWorldIDList = nbt.getTagList("PlayerWorldIDList", Constants.NBT.TAG_COMPOUND);
        for (NBTBase compound : playerWorldIDList) {
            NBTTagCompound tag = (NBTTagCompound) compound;
            UUID uuid = tag.getUniqueId("UUID");
            int worldID = tag.getInteger("WorldID");
            PLAYER_WORLD_ID_MAP.put(uuid, worldID);
        }
    }

    public void setDirty() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && INSTANCE != null) {
            INSTANCE.markDirty();
        }
    }

    public void setInstance(PlayerWorldIDData instance) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            INSTANCE = instance;
        }
    }

}
