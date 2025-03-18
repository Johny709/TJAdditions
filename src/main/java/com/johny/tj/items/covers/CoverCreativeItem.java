package com.johny.tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.textures.TJTextures;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.util.Position;
import gregtech.common.covers.filter.SimpleItemFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.List;


public class CoverCreativeItem extends CoverBehavior implements CoverWithUI, ITickable {

    private int speed = 1;
    private long timer = 1L;
    private final SimpleItemFilter itemFilter;
    private final IItemHandler itemHandler;


    public CoverCreativeItem(ICoverable coverHolder, EnumFacing attachedSide) {
        super(coverHolder, attachedSide);
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
        WidgetGroup itemFilterGroup = new WidgetGroup(new Position(51, 25));
        itemFilterGroup.addWidget(new LabelWidget(-15, -15, "cover.creative_item.title"));
        itemFilterGroup.addWidget(new ImageWidget(10, 55, 55, 18, GuiTextures.DISPLAY));
        itemFilterGroup.addWidget(new AdvancedTextWidget(12, 60, this::displayText, 0xFFFFFF));
        itemFilterGroup.addWidget(new ClickButtonWidget(-8, 55, 18, 18, "+", this::onIncrement));
        itemFilterGroup.addWidget(new ClickButtonWidget(65, 55, 18, 18, "-", this::onDecrement));
        itemFilterGroup.addWidget(new ToggleButtonWidget(83, 55, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::onReset)
                .setTooltipText("machine.universal.toggle.reset"));
        this.itemFilter.initUI(itemFilterGroup::addWidget);
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 176, 105 + 82)
                .widget(itemFilterGroup)
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 105)
                .build(this, player);
    }

    private void onReset(boolean reset) {
        speed = 1;
    }

    private boolean isReset() {
        return false;
    }

    private void displayText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("metaitem.creative.cover.display.ticks", speed));
    }


    private void onIncrement(Widget.ClickData clickData) {
        int value = clickData.isCtrlClick ? 100
                : clickData.isShiftClick ? 10
                : 1;
        speed = MathHelper.clamp(speed +value, 1, Integer.MAX_VALUE);
    }

    private void onDecrement(Widget.ClickData clickData) {
        int value = clickData.isCtrlClick ? 100
                : clickData.isShiftClick ? 10
                : 1;
        speed = MathHelper.clamp(speed -value, 1, Integer.MAX_VALUE);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagCompound compound = new NBTTagCompound();
        this.itemFilter.writeToNBT(compound);
        data.setInteger("Speed", speed);
        data.setTag("CreativeFilterItem", compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagCompound tagCompound = data.getCompoundTag("CreativeFilterItem");
        this.itemFilter.readFromNBT(tagCompound);
        if (data.hasKey("Speed"))
            this.speed = data.getInteger("Speed");
    }

    @Override
    public void update() {
        if (++timer % speed == 0) {
            for (int i = 0; i < 9; i++) {
                ItemStack itemFromFilter = this.itemFilter.getItemFilterSlots().getStackInSlot(i).copy();
                int remainingCount = itemFromFilter.getCount();
                for (int j = 0; j < itemHandler.getSlots(); j++) {
                    itemFromFilter.setCount(remainingCount);
                    ItemStack inStack = itemHandler.getStackInSlot(j);
                    if (inStack.isItemEqual(itemFromFilter) || inStack.isEmpty())
                        if (this.itemHandler.isItemValid(j, itemFromFilter)) {
                            this.itemHandler.insertItem(j, itemFromFilter, false);
                            remainingCount -= inStack.getMaxStackSize() - inStack.getCount();
                            if (remainingCount <= 0)
                                break;
                    }
                }
            }
        }
    }
}
