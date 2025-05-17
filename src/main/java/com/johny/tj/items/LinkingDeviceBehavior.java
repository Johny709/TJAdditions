package com.johny.tj.items;

import com.johny.tj.capability.*;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.List;

public class LinkingDeviceBehavior implements IItemBehaviour {

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        NBTTagCompound nbt = player.getHeldItem(hand).getOrCreateSubCompound("Link.XYZ");
        double x = nbt.hasKey("X") ? nbt.getDouble("X") : 0;
        double y = nbt.hasKey("Y") ? nbt.getDouble("Y") : 0;
        double z = nbt.hasKey("Z") ? nbt.getDouble("Z") : 0;
        int linkI = nbt.hasKey("I") ? nbt.getInteger("I") : 0;
        String name = nbt.hasKey("Name") ? nbt.getString("Name") : "Null";
        MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, pos);
        TileEntity targetTE = world.getTileEntity(pos);
        if (!world.isRemote) {
            if (targetGTTE != null || targetTE != null) {
                if (!player.isSneaking()) {
                    if (!name.equals("Null")) {
                        WorldServer getWorld = nbt.hasKey("DimensionID") ? DimensionManager.getWorld(nbt.getInteger("DimensionID")) : (WorldServer) world;
                        BlockPos worldPos = new BlockPos(x, y, z);
                        getWorld.getChunk(worldPos);
                        MetaTileEntity linkedGTTE = BlockMachine.getMetaTileEntity(getWorld, worldPos);
                        if (linkedGTTE instanceof LinkPos<?>) {
                            LinkPos<BlockPos> linkPos = (LinkPos<BlockPos>) linkedGTTE;
                            if (linkPos.getLinkData() != null) {
                                nbt = linkPos.getLinkData();
                                player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", nbt);
                            }
                            int linkX = 0;
                            int linkY = 0;
                            int linkZ = 0;
                            if (targetTE != null) {
                                linkX = targetTE.getPos().getX();
                                linkY = targetTE.getPos().getY();
                                linkZ = targetTE.getPos().getZ();
                            }
                            if (targetGTTE != null) {
                                linkX = targetGTTE.getPos().getX();
                                linkY = targetGTTE.getPos().getY();
                                linkZ = targetGTTE.getPos().getZ();
                            }
                            int xDiff = (int) (linkX - x);
                            int yDiff = (int) (linkY - y);
                            int zDiff = (int) (linkZ - z);
                            if (xDiff <= linkPos.getRange() && xDiff >= -linkPos.getRange()) {
                                if (yDiff <= linkPos.getRange() && yDiff >= -linkPos.getRange()) {
                                    if (zDiff <= linkPos.getRange() && zDiff >= -linkPos.getRange()) {
                                        if (linkI > 0) {
                                            int index = 0;
                                            for (int i = 0; i < 2; i++) {
                                                for (int j = 0; j < linkPos.getPosSize(); j++) {
                                                    BlockPos targetPos = linkPos.getPos(j);
                                                    if (i == 0 && targetPos != null && targetPos.getX() == linkX && targetPos.getY() == linkY && targetPos.getZ() == linkZ) {
                                                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.occupied")
                                                                .appendText(" ")
                                                                .appendSibling(new TextComponentTranslation(linkedGTTE.getMetaFullName()).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                                                        return EnumActionResult.SUCCESS;
                                                    }
                                                    if (i == 1 && targetPos == null) {
                                                        index = j;
                                                        linkPos.setPos(new BlockPos(linkX, linkY, linkZ), index);
                                                        nbt.setInteger("I", linkI - 1);
                                                        linkPos.setLinkData(nbt);
                                                        break;
                                                    }
                                                }
                                            }

                                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.success"));
                                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.remaining")
                                                    .appendSibling(new TextComponentString(" " + (linkI - 1))));
                                            if (linkedGTTE instanceof LinkPosInterDim) {
                                                ((LinkPosInterDim<?>) linkedGTTE).setDimension(world.provider::getDimension, index);
                                            }
                                            if (targetGTTE instanceof LinkSet) {
                                                ((LinkSet) targetGTTE).setLink(() -> linkedGTTE);
                                            }
                                            if (linkedGTTE instanceof LinkEvent) {
                                                ((LinkEvent) linkedGTTE).onLink();
                                            }
                                            if (targetGTTE instanceof LinkEvent) {
                                                ((LinkEvent) targetGTTE).onLink();
                                            }
                                        } else {
                                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.no_remaining"));
                                        }
                                        return EnumActionResult.SUCCESS;
                                    }
                                }
                            }
                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.far"));
                            return EnumActionResult.SUCCESS;

                        }
                    } else {
                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.nolink"));
                        return EnumActionResult.SUCCESS;
                    }
                } else {
                    if (targetGTTE instanceof LinkPos<?>) {
                        LinkPos<?> linkPos = (LinkPos<?>) targetGTTE;
                        if (linkPos.getLinkData() != null) {
                            nbt = linkPos.getLinkData();
                            player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", nbt);
                        }
                        boolean sameLink = linkPos.getLinkData() != null && linkPos.getLinkData().getCompoundTag("Link.XYZ").equals(nbt.getCompoundTag("Link.XYZ"));
                        int posSize = nbt.hasKey("I") && linkPos.getLinkData() != null ? nbt.getInteger("I") : linkPos.getPosSize();
                        int range = nbt.hasKey("Range") && linkPos.getLinkData() != null ? nbt.getInteger("Range") : linkPos.getRange();
                        setLinkData(nbt, targetGTTE, player, posSize, range, sameLink);
                    } else {
                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.capable"));
                        return EnumActionResult.SUCCESS;
                    }
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        return EnumActionResult.PASS;
    }

    private void setLinkData(NBTTagCompound nbt, MetaTileEntity targetGTTE, EntityPlayer player, int posSize, int range, boolean sameLink) {
        nbt.setString("Name", targetGTTE.getMetaFullName());
        nbt.setDouble("X", targetGTTE.getPos().getX());
        nbt.setDouble("Y", targetGTTE.getPos().getY());
        nbt.setDouble("Z", targetGTTE.getPos().getZ());
        nbt.setInteger("I", posSize);
        nbt.setInteger("Range", range);
        nbt.removeTag("DimensionID");
        if (targetGTTE instanceof LinkPosInterDim<?>)
            nbt.setInteger("DimensionID", ((LinkPosInterDim<?>) targetGTTE).dimensionID());
        if (targetGTTE instanceof LinkEntityInterDim<?>)
            nbt.setInteger("DimensionID", ((LinkEntityInterDim<?>) targetGTTE).dimensionID());

        ITextComponent textComponent = new TextComponentTranslation(sameLink ? "metaitem.linking.device.message.link.continue" : "metaitem.linking.device.message.link").appendText(" ");
        player.sendMessage(textComponent.appendSibling(new TextComponentTranslation(targetGTTE.getMetaFullName()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                .appendSibling(new TextComponentString("\nX: " + targetGTTE.getPos().getX()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                .appendSibling(new TextComponentString("\nY: " + targetGTTE.getPos().getY()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                .appendSibling(new TextComponentString("\nZ: " + targetGTTE.getPos().getZ() + "\n").setStyle(new Style().setColor(TextFormatting.YELLOW)))
                .appendSibling(new TextComponentTranslation("metaitem.linking.device.message.remaining").appendSibling(new TextComponentString(" " + nbt.getInteger("I")))
                        .setStyle(new Style().setColor(TextFormatting.YELLOW))));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        NBTTagCompound nbt = player.getHeldItem(hand).getOrCreateSubCompound("Link.XYZ");
        double x = nbt.hasKey("X") ? nbt.getDouble("X") : 0;
        double y = nbt.hasKey("Y") ? nbt.getDouble("Y") : 0;
        double z = nbt.hasKey("Z") ? nbt.getDouble("Z") : 0;
        int linkI = nbt.hasKey("I") ? nbt.getInteger("I") : 0;
        String name = nbt.hasKey("Name") ? nbt.getString("Name") : "Null";
        if (!world.isRemote) {
            if (!name.equals("Null") && player.isSneaking()) {
                WorldServer getWorld = nbt.hasKey("DimensionID") ? DimensionManager.getWorld(nbt.getInteger("DimensionID")) : (WorldServer) world;
                BlockPos worldPos = new BlockPos(x, y, z);
                getWorld.getChunk(worldPos);
                MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(getWorld, worldPos);
                if (metaTileEntity instanceof LinkEntity<?>) {
                    LinkEntity<Entity> linkEntity = (LinkEntity<Entity>) metaTileEntity;
                    if (linkI > 0) {
                        for (int i = 0; i < linkEntity.getPosSize(); i++) {
                            Entity targetEntity = linkEntity.getPos(i);
                            if (targetEntity != null && targetEntity.isEntityEqual(player)) {
                                player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.occupied"));
                                return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
                            }
                            if (targetEntity == null) {
                                linkEntity.setPos(player, i);
                                nbt.setInteger("I", linkI - 1);
                                linkEntity.setLinkData(nbt);
                                player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.success"));
                                if (metaTileEntity instanceof LinkEntityInterDim<?>)
                                    ((LinkEntityInterDim<Entity>) metaTileEntity).setDimension(player.world.provider::getDimension, i);
                                break;
                            }
                        }
                    } else {
                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.no_remaining"));
                    }
                }
            }
        }
        return ActionResult.newResult(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        NBTTagCompound nbt = itemStack.getOrCreateSubCompound("Link.XYZ");
        double x = nbt.hasKey("X") ? nbt.getDouble("X") : 0;
        double y = nbt.hasKey("Y") ? nbt.getDouble("Y") : 0;
        double z = nbt.hasKey("Z") ? nbt.getDouble("Z") : 0;
        int linkI = nbt.hasKey("I") ? nbt.getInteger("I") : 0;
        int range = nbt.hasKey("Range") ? nbt.getInteger("Range") : 0;
        int dimensionID = nbt.hasKey("DimensionID") ? nbt.getInteger("DimensionID") : 0;
        String dimensionName = DimensionType.getById(dimensionID).getName();
        String name = nbt.hasKey("Name") ? nbt.getString("Name") : "Null";
        lines.add(I18n.format("metaitem.linking.device.description"));
        lines.add(I18n.format("metaitem.linking.device.name") + I18n.format(name));
        lines.add(I18n.format("metaitem.linking.device.x", x));
        lines.add(I18n.format("metaitem.linking.device.y", y));
        lines.add(I18n.format("metaitem.linking.device.z", z));
        lines.add(I18n.format("metaitem.linking.device.message.remaining", linkI));
        lines.add(I18n.format("metaitem.linking.device.range", range));
        lines.add(I18n.format("metaitem.linking.device.dimension", dimensionName, dimensionID));
    }
}
