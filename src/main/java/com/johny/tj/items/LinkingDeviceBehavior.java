package com.johny.tj.items;

import com.johny.tj.machines.LinkPos;
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
                        MetaTileEntity linkedGTTE = BlockMachine.getMetaTileEntity(world, new BlockPos(x, y, z));
                        if (linkedGTTE instanceof LinkPos) {
                            LinkPos linkPos = (LinkPos)linkedGTTE;
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
                            if (xDiff <= 64 && xDiff >= -64) {
                                if (yDiff <= 64 && yDiff >= -64) {
                                    if (zDiff <= 64 && zDiff >= -64) {
                                        if (linkI > 0) {
                                            BlockPos targetPos = linkPos.getBlockPos(linkI - 1);
                                            if (targetPos == null || targetPos.getX() != linkX && targetPos.getY() != linkY && targetPos.getZ() != linkZ) {
                                                linkPos.setBlockPos(linkX, linkY, linkZ, true, linkI - 1);
                                                nbt.setInteger("I", linkI - 1);
                                                player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.success"));
                                                player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.remaining")
                                                        .appendSibling(new TextComponentString(" " + (linkI - 1))));
                                            } else {
                                                player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.occupied")
                                                        .appendSibling(new TextComponentString(" " + I18n.format(linkedGTTE.getMetaFullName())).setStyle(new Style().setColor(TextFormatting.YELLOW))));
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
                        if (name.equals("Null")) {
                            nbt.setString("Name", targetGTTE.getMetaFullName());
                            nbt.setDouble("X", targetGTTE.getPos().getX());
                            nbt.setDouble("Y", targetGTTE.getPos().getY());
                            nbt.setDouble("Z", targetGTTE.getPos().getZ());
                            nbt.setInteger("I", linkPos.getBlockPosSize());
                        } else {
                            linkPos.setBlockPos(0, 0, 0, false, 0);
                            nbt.setString("Name", "Null");
                            nbt.setDouble("X", 0);
                            nbt.setDouble("Y", 0);
                            nbt.setDouble("Z", 0);
                            nbt.setInteger("I", 0);
                        }
                        ITextComponent textComponent = new TextComponentTranslation(name.equals("Null") ? "metaitem.linking.device.message.link" : "metaitem.linking.device.message.unlink");
                        player.sendMessage(textComponent.appendSibling(new TextComponentString(" " + I18n.format(targetGTTE.getMetaFullName())).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentString("\nX: " + targetGTTE.getPos().getX()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentString("\nY: " + targetGTTE.getPos().getY()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                                .appendSibling(new TextComponentString("\nZ: " + targetGTTE.getPos().getZ() + "\n").setStyle(new Style().setColor(TextFormatting.YELLOW))));

                                if (name.equals("Null")) {
                                    textComponent.appendSibling(new TextComponentTranslation("metaitem.linking.device.message.remaining").appendSibling(new TextComponentString(" " + linkPos.getBlockPosSize()))
                                            .setStyle(new Style().setColor(TextFormatting.YELLOW)));
                                }
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
        String name = nbt.hasKey("Name") ? nbt.getString("Name") : "Null";
        lines.add(I18n.format("metaitem.linking.device.name", name));
        lines.add(I18n.format("metaitem.linking.device.x", x));
        lines.add(I18n.format("metaitem.linking.device.y", y));
        lines.add(I18n.format("metaitem.linking.device.z", z));
        lines.add(I18n.format("metaitem.linking.device.message.remaining", linkI));
    }
}
