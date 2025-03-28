package com.johny.tj.items;

import com.johny.tj.capability.LinkEvent;
import com.johny.tj.capability.LinkInterDimPos;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.LinkSet;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
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
                        if (linkedGTTE instanceof LinkPos) {
                            LinkPos linkPos = (LinkPos)linkedGTTE;
                            if (linkPos.getLinkData() != null) {
                                nbt = linkPos.getLinkData();
                                player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", nbt);
                            }
                            double linkX = 0;
                            double linkY = 0;
                            double linkZ = 0;
                            if (targetGTTE != null) {
                                linkX = targetGTTE.getPos().getX();
                                linkY = targetGTTE.getPos().getY();
                                linkZ = targetGTTE.getPos().getZ();
                            }
                            if (targetTE != null) {
                                linkX = targetTE.getPos().getX();
                                linkY = targetTE.getPos().getY();
                                linkZ = targetTE.getPos().getZ();
                            }
                            double xDiff = linkX - x;
                            double yDiff = linkY - y;
                            double zDiff = linkZ - z;
                            if (xDiff <= linkPos.getRange() && xDiff >= -linkPos.getRange()) {
                                if (yDiff <= linkPos.getRange() && yDiff >= -linkPos.getRange()) {
                                    if (zDiff <= linkPos.getRange() && zDiff >= -linkPos.getRange()) {
                                        if (linkI > 0) {
                                            for (int i = 0; i < linkPos.getBlockPosSize(); i++) {
                                                BlockPos targetPos = linkPos.getBlockPos(i);
                                                if (targetPos != null && targetPos.getX() == linkX && targetPos.getY() == linkY && targetPos.getZ() == linkZ) {
                                                    player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.occupied")
                                                            .appendText(" ")
                                                            .appendSibling(new TextComponentTranslation(linkedGTTE.getMetaFullName()).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                                                    return EnumActionResult.SUCCESS;
                                                }
                                                if (targetPos == null) {
                                                    linkPos.setBlockPos(linkX, linkY, linkZ, true, Math.abs(linkI - linkPos.getBlockPosSize()));
                                                    nbt.setInteger("I", linkI - 1);
                                                    linkPos.setLinkData(nbt);
                                                    break;
                                                }
                                            }
                                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.success"));
                                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.remaining")
                                                    .appendSibling(new TextComponentString(" " + (linkI - 1))));
                                            if (linkedGTTE instanceof LinkInterDimPos) {
                                                ((LinkInterDimPos) linkedGTTE).setDimension(world.provider::getDimension, Math.abs(linkI - linkPos.getBlockPosSize()));
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
                    if (targetGTTE instanceof LinkPos) {
                        LinkPos linkPos = (LinkPos)targetGTTE;
                        if (linkPos.getLinkData() != null) {
                            nbt = linkPos.getLinkData();
                            player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", nbt);
                        } else {
                            nbt.setString("Name", targetGTTE.getMetaFullName());
                            nbt.setDouble("X", targetGTTE.getPos().getX());
                            nbt.setDouble("Y", targetGTTE.getPos().getY());
                            nbt.setDouble("Z", targetGTTE.getPos().getZ());
                            nbt.setInteger("I", linkPos.getBlockPosSize());
                            nbt.setInteger("Range", linkPos.getRange());
                            nbt.removeTag("DimensionID");
                            if (targetGTTE instanceof LinkInterDimPos)
                                nbt.setInteger("DimensionID", ((LinkInterDimPos) targetGTTE).dimensionID());
                        }

                        ITextComponent textComponent = new TextComponentTranslation(linkPos.getLinkData() != null ? "metaitem.linking.device.message.link.continue" : "metaitem.linking.device.message.link").appendText(" ");
                        player.sendMessage(textComponent.appendSibling(new TextComponentTranslation(targetGTTE.getMetaFullName()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentString("\nX: " + targetGTTE.getPos().getX()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentString("\nY: " + targetGTTE.getPos().getY()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentString("\nZ: " + targetGTTE.getPos().getZ() + "\n").setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentTranslation("metaitem.linking.device.message.remaining").appendSibling(new TextComponentString(" " + nbt.getInteger("I")))
                                            .setStyle(new Style().setColor(TextFormatting.YELLOW))));

                    } else {
                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.capable"));
                    }
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        return EnumActionResult.PASS;
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        NBTTagCompound nbt = itemStack.getOrCreateSubCompound("Link.XYZ");
        double x = nbt.hasKey("X") ? nbt.getDouble("X") : 0;
        double y = nbt.hasKey("Y") ? nbt.getDouble("Y") : 0;
        double z = nbt.hasKey("Z") ? nbt.getDouble("Z") : 0;
        int linkI = nbt.hasKey("I") ? nbt.getInteger("I") : 0;
        int range = nbt.hasKey("Range") ? nbt.getInteger("Range") : 0;
        String name = nbt.hasKey("Name") ? nbt.getString("Name") : "Null";
        lines.add(I18n.format("metaitem.linking.device.description"));
        lines.add(I18n.format("metaitem.linking.device.name") + I18n.format(name));
        lines.add(I18n.format("metaitem.linking.device.x", x));
        lines.add(I18n.format("metaitem.linking.device.y", y));
        lines.add(I18n.format("metaitem.linking.device.z", z));
        lines.add(I18n.format("metaitem.linking.device.message.remaining", linkI));
        lines.add(I18n.format("metaitem.linking.device.range", range));
    }
}
