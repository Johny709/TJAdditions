package com.johny.tj.multiblockpart.utility;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.items.handlers.LargeItemStackHandler;
import gregicadditions.GAValues;
import gregicadditions.machines.multi.multiblockpart.GAMetaTileEntityMultiblockPart;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.render.Textures;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static gregtech.api.metatileentity.multiblock.MultiblockAbility.EXPORT_ITEMS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_ITEMS;

public class MetaTileEntitySuperItemBus extends GAMetaTileEntityMultiblockPart implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    private final boolean isExport;
    private int ticks = 5;

    public MetaTileEntitySuperItemBus(ResourceLocation metaTileEntityId, int tier, boolean isExport) {
        super(metaTileEntityId, tier);
        this.isExport = isExport;
        initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntitySuperItemBus(metaTileEntityId, getTier(), isExport);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        int slots = Math.min(100,(getTier() + 1) * (getTier() + 1));
        int size = getTier() < GAValues.UMV ? 1024
                : getTier() < GAValues.MAX ? 16384
                : Integer.MAX_VALUE;

        tooltip.add(I18n.format("machine.universal.stack", size));
        tooltip.add(I18n.format("machine.universal.slots", slots));
        tooltip.add(I18n.format("gregtech.universal.enabled"));
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        int slots = Math.min(100,(getTier() + 1) * (getTier() + 1));
        int size = getTier() < GAValues.UMV ? 1024
                : getTier() < GAValues.MAX ? 16384
                : Integer.MAX_VALUE;

        return isExport ? super.createImportItemHandler()
                : new LargeItemStackHandler(slots, size);
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        int slots = Math.min(100,(getTier() + 1) * (getTier() + 1));
        int size = getTier() < GAValues.UMV ? 1024
                : getTier() < GAValues.MAX ? 16384
                : Integer.MAX_VALUE;

        return isExport ? new LargeItemStackHandler(slots, size)
                : super.createExportItemHandler();
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote && getOffsetTimer() % ticks == 0) {
            if (isExport) {
                pushItemsIntoNearbyHandlers(getFrontFacing());
            } else {
                pullItemsFromNearbyHandlers(getFrontFacing());
            }
        }
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        IItemHandlerModifiable bus = isExport ? exportItems : importItems;
        int tier = Math.min(3, getTier() / 3);
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new ImageWidget(169, 72 * tier, 18, 18, GuiTextures.DISPLAY));
        widgetGroup.addWidget(new AdvancedTextWidget(170, 5 + 72 * tier, this::addDisplayText, 0xFFFFFF));
        widgetGroup.addWidget(new ToggleButtonWidget(169, -18 + 72 * tier, 18, 18, TJGuiTextures.UP_BUTTON, this::isIncrement, this::onIncrement)
                .setTooltipText("machine.universal.toggle.increment"));
        widgetGroup.addWidget(new ToggleButtonWidget(169, 18 + 72 * tier, 18, 18, TJGuiTextures.DOWN_BUTTON, this::isDecrement, this::onDecrement)
                .setTooltipText("machine.universal.toggle.decrement"));
        widgetGroup.addWidget(new ToggleButtonWidget(169, 40 + 72 * tier, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::onReset)
                .setTooltipText("machine.universal.toggle.reset"));
        for (int i = 0; i < bus.getSlots(); i++) {
            widgetGroup.addWidget(new SlotWidget(bus, i, 7 + 18 * (i % 10), 14 + 18 * (i / 10), true, !isExport)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 196, 63 + 72 * tier)
                .label(7, 4, getMetaFullName())
                .bindPlayerInventory(player.inventory, -18 + 72 * tier)
                .widget(widgetGroup)
                .build(getHolder(), player);
    }

    private void addDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentString(ticks + "t")
                .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.ticks.operation")))));
    }

    private void onIncrement(boolean b) {
        ticks = MathHelper.clamp(ticks + 1, 1, Integer.MAX_VALUE);
    }

    private boolean isIncrement() {
        return false;
    }

    private void onDecrement(boolean b) {
        ticks = MathHelper.clamp(ticks - 1, 1, Integer.MAX_VALUE);
    }

    private boolean isDecrement() {
        return false;
    }

    private void onReset(boolean b) {
        ticks = 5;
    }

    private boolean isReset() {
        return false;
    }


    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        if (isExport) {
            Textures.ITEM_OUTPUT_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
            Textures.ITEM_HATCH_OUTPUT_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
            Textures.PIPE_OUT_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
        }
        else {
            Textures.ITEM_HATCH_INPUT_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
            Textures.PIPE_IN_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
        }
    }

    @Override
    public MultiblockAbility<IItemHandlerModifiable> getAbility() {
        return isExport ? EXPORT_ITEMS : IMPORT_ITEMS;
    }

    @Override
    public void registerAbilities(List<IItemHandlerModifiable> list) {
        list.add(isExport ? exportItems : importItems);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Ticks", ticks);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        ticks = data.hasKey("Ticks") ? data.getInteger("Ticks") : 5;
    }
}
