package com.johny.tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.textures.TJTextures;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.Position;
import gregtech.common.covers.filter.SimpleItemFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;


public class CoverCreativeItem extends CoverBehavior implements CoverWithUI, ITickable {

    private int speed = 1;
    private long timer = 0L;
    private final SimpleItemFilter itemFilter;
    private final IItemHandler itemHandler;


    public CoverCreativeItem(ICoverable coverHolder, EnumFacing attachedSide, int speed) {
        super(coverHolder, attachedSide);
        this.speed = speed;
        this.itemFilter = new SimpleItemFilter();
        this.itemHandler = this.coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.attachedSide) != null;
    }

    @Override
    public void renderCover(CCRenderState ccRenderState, Matrix4 matrix4, IVertexOperation[] iVertexOperations, Cuboid6 cuboid6, BlockRenderLayer blockRenderLayer) {
        TJTextures.COVER_CREATIVE_FLUID.renderSided(attachedSide, cuboid6, ccRenderState, iVertexOperations, matrix4);
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (!playerIn.world.isRemote) {
            this.openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup fluidFilterGroup = new WidgetGroup(new Position(51, 25));
        fluidFilterGroup.addWidget(new LabelWidget(-15, -15, "cover.creative_item.title"));
        this.itemFilter.initUI(fluidFilterGroup::addWidget);
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 176, 105 + 82)
                .widget(fluidFilterGroup)
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 105)
                .build(this, player);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagCompound compound = new NBTTagCompound();
        this.itemFilter.writeToNBT(compound);
        data.setTag("CreativeFilterItem", compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagCompound tagCompound = data.getCompoundTag("CreativeFilterItem");
        this.itemFilter.readFromNBT(tagCompound);
    }

    @Override
    public void update() {
        if (timer++ % ((20 * speed) / (Math.pow(2, speed - 1))) == 0) {
            for (int index = 0; index < 9; index++) {
                ItemStack item = this.itemFilter.getItemFilterSlots().getStackInSlot(index);
                ItemStack insertItem = item.copy();
                insertItem.setCount(64);
                for (int jndex = 0; jndex < itemHandler.getSlots(); jndex++) {
                    if (this.itemHandler.isItemValid(jndex, insertItem)) {
                        this.itemHandler.insertItem(jndex, insertItem, false);
                    }
                }
            }
        }
    }
}
