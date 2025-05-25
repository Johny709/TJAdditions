package com.johny.tj.items;

import com.johny.tj.capability.LinkEntity;
import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.LinkSet;
import com.johny.tj.event.MTELinkEvent;
import com.johny.tj.gui.widgets.TJSlotWidget;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import com.johny.tj.util.QuintConsumer;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.gui.PlayerInventoryHolder;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.johny.tj.items.LinkingMode.*;
import static gregtech.api.gui.GuiTextures.BORDERED_BACKGROUND;
import static gregtech.api.gui.GuiTextures.TOGGLE_BUTTON_BACK;
import static net.minecraft.util.EnumHand.MAIN_HAND;
import static net.minecraft.util.EnumHand.OFF_HAND;

public class LinkingDeviceBehavior implements IItemBehaviour, ItemUIFactory {

    private final IItemHandlerModifiable itemSlot = new ItemStackHandler(1);
    private ItemStack item;
    private QuintConsumer<String, BlockPos, EntityPlayer, World, Integer> posResponder;
    private Consumer<NBTTagCompound> nbtResponder;
    private Supplier<Integer> rangeSupplier;
    private NBTTagCompound nbt;
    private EntityPlayer player;
    private World world;
    private boolean isPressed;
    private String name;
    private int index;
    private int worldID;
    private int x;
    private int y;
    private int z;

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        this.nbt = player.getHeldItem(hand).getOrCreateSubCompound("Link.XYZ");
        NBTTagCompound modeNBT = player.getHeldItem(hand).getOrCreateSubCompound("LinkMode");
        LinkingMode linkingMode = modeNBT.hasKey("Mode") ? LinkingMode.values()[modeNBT.getInteger("Mode")] : BLOCK;
        double x = this.nbt.hasKey("X") ? this.nbt.getDouble("X") : 0;
        double y = this.nbt.hasKey("Y") ? this.nbt.getDouble("Y") : 0;
        double z = this.nbt.hasKey("Z") ? this.nbt.getDouble("Z") : 0;
        int linkI = this.nbt.hasKey("I") ? this.nbt.getInteger("I") : 0;
        String name = this.nbt.hasKey("Name") ? this.nbt.getString("Name") : "Null";
        MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(world, pos);
        TileEntity targetTE = world.getTileEntity(pos);
        if (!world.isRemote && hand == MAIN_HAND) {
            if (!player.isSneaking()) {
                if (!name.equals("Null")) {
                    WorldServer getWorld = this.nbt.hasKey("DimensionID") ? DimensionManager.getWorld(this.nbt.getInteger("DimensionID")) : (WorldServer) world;
                    BlockPos worldPos = new BlockPos(x, y, z);
                    getWorld.getChunk(worldPos);
                    MetaTileEntity linkedGTTE = BlockMachine.getMetaTileEntity(getWorld, worldPos);
                    if (linkedGTTE instanceof LinkPos && (linkingMode == BLOCK || linkingMode == BLOCK_PROMPT)) {
                        LinkPos linkPos = (LinkPos) linkedGTTE;
                        this.posResponder = linkPos::setPos;
                        this.nbtResponder = linkPos::setLinkData;
                        this.rangeSupplier = linkPos::getRange;
                        this.player = player;
                        this.world = world;
                        this.worldID = world.provider.getDimensionType().getId();
                        this.x = pos.getX();
                        this.y = pos.getY();
                        this.z = pos.getZ();
                        this.name = world.getBlockState(pos).getBlock().getLocalizedName();
                        this.item = new ItemStack(world.getBlockState(pos).getBlock());
                        this.nbt = linkPos.getLinkData();
                        player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", this.nbt);
                        if (targetTE != null) {
                            this.x = targetTE.getPos().getX();
                            this.y = targetTE.getPos().getY();
                            this.z = targetTE.getPos().getZ();
                            this.name = targetTE.getBlockType().getLocalizedName();
                            this.item = new ItemStack(targetTE.getBlockType());
                        }
                        if (targetGTTE != null) {
                            this.x = targetGTTE.getPos().getX();
                            this.y = targetGTTE.getPos().getY();
                            this.z = targetGTTE.getPos().getZ();
                            this.name = targetGTTE.getMetaFullName();
                            this.item = targetGTTE.getStackForm();
                        }
                        if (linkI > 0) {
                            for (int i = 0; i < 2; i++) {
                                for (int j = 0; j < linkPos.getPosSize(); j++) {
                                    BlockPos targetPos = linkPos.getPos(j);
                                    if (i == 0 && targetPos != null && targetPos.getX() == this.x && targetPos.getY() == this.y && targetPos.getZ() == this.z) {
                                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.occupied")
                                                .appendText(" ")
                                                .appendSibling(new TextComponentTranslation(linkedGTTE.getMetaFullName()).setStyle(new Style().setColor(TextFormatting.YELLOW))));
                                        return EnumActionResult.SUCCESS;
                                    }
                                    if (i == 1 && targetPos == null) {
                                        this.index = j;
                                        if (linkingMode == BLOCK_PROMPT) {
                                            this.isPressed = false;
                                            PlayerInventoryHolder.openHandItemUI(player, hand);
                                            return EnumActionResult.SUCCESS;
                                        }
                                        if (this.inRange())
                                            this.setPos();
                                        break;
                                    }
                                }
                            }
                            if (targetGTTE instanceof LinkSet) {
                                ((LinkSet) targetGTTE).setLink(() -> linkedGTTE);
                            }
                            MinecraftForge.EVENT_BUS.post(new MTELinkEvent(linkedGTTE, targetGTTE));
                        } else {
                            player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.no_remaining"));
                        }
                        return EnumActionResult.SUCCESS;
                    }
                } else {
                    player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.nolink"));
                    return EnumActionResult.SUCCESS;
                }
            } else {
                if (targetGTTE instanceof LinkPos) {
                    LinkPos linkPos = (LinkPos) targetGTTE;
                    boolean hasLink = false;
                    if (linkPos.getLinkData() == null) {
                        linkPos.setLinkData(this.nbt);
                        this.setLinkData(this.nbt, targetGTTE, linkPos);
                    } else {
                        this.nbt = linkPos.getLinkData();
                        player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", nbt);
                        hasLink = true;
                    }
                    ITextComponent textComponent = new TextComponentTranslation(hasLink ? "metaitem.linking.device.message.link.continue" : "metaitem.linking.device.message.link").appendText(" ");
                    player.sendMessage(textComponent.appendSibling(new TextComponentTranslation(targetGTTE.getMetaFullName()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                            .appendSibling(new TextComponentString("\nX: " + targetGTTE.getPos().getX()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                            .appendSibling(new TextComponentString("\nY: " + targetGTTE.getPos().getY()).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                            .appendSibling(new TextComponentString("\nZ: " + targetGTTE.getPos().getZ() + "\n").setStyle(new Style().setColor(TextFormatting.YELLOW)))
                            .appendSibling(new TextComponentTranslation("metaitem.linking.device.message.remaining").appendSibling(new TextComponentString(" " + nbt.getInteger("I")))
                                    .setStyle(new Style().setColor(TextFormatting.YELLOW))));
                } else {
                    player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.capable"));
                    return EnumActionResult.SUCCESS;
                }
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    private void setLinkData(NBTTagCompound nbt, MetaTileEntity targetGTTE, LinkPos linkPos) {
        nbt.setString("Name", targetGTTE.getMetaFullName());
        nbt.setDouble("X", targetGTTE.getPos().getX());
        nbt.setDouble("Y", targetGTTE.getPos().getY());
        nbt.setDouble("Z", targetGTTE.getPos().getZ());
        nbt.setInteger("I", linkPos.getPosSize());
        nbt.setInteger("Range", linkPos.getRange());
        nbt.removeTag("DimensionID");
        if (linkPos.isInterDimensional())
            nbt.setInteger("DimensionID", linkPos.dimensionID());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        this.nbt = player.getHeldItem(hand).getOrCreateSubCompound("Link.XYZ");
        NBTTagCompound modeNBT = player.getHeldItem(hand).getOrCreateSubCompound("LinkMode");
        LinkingMode linkingMode = modeNBT.hasKey("Mode") ? LinkingMode.values()[modeNBT.getInteger("Mode")] : BLOCK;
        double x = this.nbt.hasKey("X") ? this.nbt.getDouble("X") : 0;
        double y = this.nbt.hasKey("Y") ? this.nbt.getDouble("Y") : 0;
        double z = this.nbt.hasKey("Z") ? this.nbt.getDouble("Z") : 0;
        int linkI = this.nbt.hasKey("I") ? this.nbt.getInteger("I") : 0;
        String name = this.nbt.hasKey("Name") ? this.nbt.getString("Name") : "Null";
        if (!world.isRemote) {
            if (!name.equals("Null") && !player.isSneaking() && hand == MAIN_HAND) {
                WorldServer getWorld = this.nbt.hasKey("DimensionID") ? DimensionManager.getWorld(this.nbt.getInteger("DimensionID")) : (WorldServer) world;
                BlockPos worldPos = new BlockPos(x, y, z);
                getWorld.getChunk(worldPos);
                MetaTileEntity metaTileEntity = BlockMachine.getMetaTileEntity(getWorld, worldPos);
                if (metaTileEntity instanceof LinkEntity && (linkingMode == ENTITY || linkingMode == ENTITY_PROMPT)) {
                    LinkEntity linkEntity = (LinkEntity) metaTileEntity;
                    this.posResponder = linkEntity::setPos;
                    this.nbtResponder = linkEntity::setLinkData;
                    this.rangeSupplier = linkEntity::getRange;
                    this.player = player;
                    this.world = world;
                    this.worldID = world.provider.getDimensionType().getId();
                    this.x = player.getPosition().getX();
                    this.y = player.getPosition().getY();
                    this.z = player.getPosition().getZ();
                    this.nbt = linkEntity.getLinkData();
                    player.getHeldItem(hand).getTagCompound().setTag("Link.XYZ", this.nbt);
                    if (linkI > 0) {
                        for (int i = 0; i < 2; i++) {
                            for (int j = 0; j < linkEntity.getPosSize(); j++) {
                                Entity targetEntity = linkEntity.getEntity(j);
                                if (i == 0 && targetEntity != null && targetEntity.isEntityEqual(player)) {
                                    player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.occupied", targetEntity.getName()));
                                    return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
                                }
                                if (i == 1 && targetEntity == null) {
                                    if (linkingMode == ENTITY_PROMPT) {
                                        this.isPressed = false;
                                        PlayerInventoryHolder.openHandItemUI(player, hand);
                                        return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
                                    }
                                    if (this.inRange())
                                        this.setPos();
                                    break;
                                }
                            }
                        }
                    } else {
                        player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.no_remaining"));
                    }
                }
            } else if (hand == OFF_HAND) {
                int mode = linkingMode.ordinal();
                modeNBT.setInteger("Mode", ++mode > 3 ? 0 : mode);
                linkingMode = LinkingMode.values()[modeNBT.getInteger("Mode")];
                player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.mode", net.minecraft.util.text.translation.I18n.translateToLocal(linkingMode.getMode())));
            }
        }
        return ActionResult.newResult(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        NBTTagCompound nbt = itemStack.getOrCreateSubCompound("Link.XYZ");
        NBTTagCompound modeNBT = itemStack.getOrCreateSubCompound("LinkMode");
        LinkingMode linkingMode = modeNBT.hasKey("Mode") ? LinkingMode.values()[modeNBT.getInteger("Mode")] : BLOCK;
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
        lines.add(I18n.format("metaitem.linking.device.message.mode.description"));
        lines.add(I18n.format("metaitem.linking.device.message.mode", net.minecraft.util.text.translation.I18n.translateToLocal(linkingMode.getMode())));
    }

    public IItemHandlerModifiable getItemSlot() {
        return itemSlot;
    }

    @Override
    public ModularUI createUI(PlayerInventoryHolder holder, EntityPlayer player) {
        itemSlot.setStackInSlot(0, item);
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new TJSlotWidget(this::getItemSlot, 0, 4, 10, false, false).setSize(40, 40));
        widgetGroup.addWidget(new TJTextFieldWidget(90, 14, 80, 18, true, this::getName, this::setName)
                .setTooltipText("metaitem.linking.device.set.name")
                .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
        widgetGroup.addWidget(new TJTextFieldWidget(90, 34, 80, 18, true, this::getWorldID, this::setWorldID)
                .setTooltipText("metaitem.linking.device.set.world")
                .setValidator(str -> Pattern.compile("-*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new TJTextFieldWidget(90, 54, 80, 18, true, this::getX, this::setX)
                .setTooltipText("metaitem.linking.device.set.x")
                .setValidator(str -> Pattern.compile("-*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new TJTextFieldWidget(90, 74, 80, 18, true, this::getY, this::setY)
                .setTooltipText("metaitem.linking.device.set.y")
                .setValidator(str -> Pattern.compile("-*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new TJTextFieldWidget(90, 94, 80, 18, true, this::getZ, this::setZ)
                .setTooltipText("metaitem.linking.device.set.z")
                .setValidator(str -> Pattern.compile("-*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new ToggleButtonWidget(4, 114, 166, 18, TOGGLE_BUTTON_BACK, this::isPressed, this::setPressed));
        widgetGroup.addWidget(new LabelWidget(60, 120, "metaitem.linking.device.set.position", 0x000000));
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 138)
                .widget(widgetGroup)
                .build(holder, player);
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setWorldID(String worldID) {
        this.worldID = Integer.parseInt(worldID);
    }

    private void setX(String x) {
        this.x = Integer.parseInt(x);
    }

    private void setY(String y) {
        this.y = Integer.parseInt(y);
    }

    private void setZ(String z) {
        this.z = Integer.parseInt(z);
    }

    private void setPressed(boolean isPressed) {
        if (!this.isPressed) {
            this.isPressed = isPressed;
            if (this.inRange())
                this.setPos();
        }
    }

    private boolean inRange() {
        double x = this.nbt.hasKey("X") ? this.nbt.getDouble("X") : 0;
        double y = this.nbt.hasKey("Y") ? this.nbt.getDouble("Y") : 0;
        double z = this.nbt.hasKey("Z") ? this.nbt.getDouble("Z") : 0;
        int xDiff = (int) (this.x - x);
        int yDiff = (int) (this.y - y);
        int zDiff = (int) (this.z - z);
        int range = this.rangeSupplier.get();
        if (xDiff <= range && xDiff >= -range)
            if (yDiff <= range && yDiff >= -range)
                return zDiff <= range && zDiff >= -range;
        return false;
    }

    private void setPos() {
        BlockPos pos = new BlockPos(this.x, this.y, this.z);
        double x = this.nbt.getDouble("X");
        double y = this.nbt.getDouble("Y");
        double z = this.nbt.getDouble("Z");
        WorldServer getWorld = this.nbt.hasKey("DimensionID") ? DimensionManager.getWorld(this.nbt.getInteger("DimensionID")) : (WorldServer) world;
        MetaTileEntity linkedGTTE = BlockMachine.getMetaTileEntity(getWorld, new BlockPos(x, y, z));
        MetaTileEntity targetGTTE = BlockMachine.getMetaTileEntity(this.world, pos);
        int linkI = this.nbt.hasKey("I") ? this.nbt.getInteger("I") : 0;
        this.nbt.setInteger("I", linkI - 1);
        this.world.getChunk(pos);
        this.posResponder.accept(this.name, pos, this.player, this.world, this.index);
        this.nbtResponder.accept(this.nbt);
        this.player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.success"));
        this.player.sendMessage(new TextComponentTranslation("metaitem.linking.device.message.remaining")
                .appendSibling(new TextComponentString(" " + this.nbt.getInteger("I"))));
        MinecraftForge.EVENT_BUS.post(new MTELinkEvent(linkedGTTE, targetGTTE));
    }

    public String getName() {
        return this.name;
    }

    public String getWorldID() {
        return String.valueOf(this.worldID);
    }

    public String getX() {
        return String.valueOf(this.x);
    }

    public String getY() {
        return String.valueOf(this.y);
    }

    public String getZ() {
        return String.valueOf(this.z);
    }

    public boolean isPressed() {
        return this.isPressed;
    }
}
