package com.johny.tj.multiblockpart.utility;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.render.Textures;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

import static gregtech.api.metatileentity.multiblock.MultiblockAbility.EXPORT_ITEMS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_ITEMS;

public class MetaTileEntitySuperItemBus extends MetaTileEntityMultiblockPart implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    private final boolean isExport;

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
        tooltip.add(I18n.format("machine.universal.slots", (getTier() + 1) * (getTier() + 1)));
        tooltip.add(I18n.format("machine.universal.stack", 1024));
        tooltip.add(I18n.format("gregtech.universal.enabled"));
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return isExport ? super.createImportItemHandler()
                : new ItemStackHandler((getTier() + 1) * (getTier() + 1)) {

            @Override
            protected int getStackLimit(int slot, @NotNull ItemStack stack) {
                return 1024;
            }
        };
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return isExport ? new ItemStackHandler((getTier() + 1) * (getTier() + 1)) {

            @Override
            protected int getStackLimit(int slot, @NotNull ItemStack stack) {
                return 1024;
            }

            @Override
            @Nonnull
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (amount == 0)
                    return ItemStack.EMPTY;

                validateSlotIndex(slot);

                ItemStack existing = this.stacks.get(slot);

                if (existing.isEmpty())
                    return ItemStack.EMPTY;

                int toExtract = Math.min(amount, 1024);

                if (existing.getCount() <= toExtract) {
                    if (!simulate) {
                        this.stacks.set(slot, ItemStack.EMPTY);
                        onContentsChanged(slot);
                    }
                    return existing;
                }
                else {
                    if (!simulate) {
                        this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                        onContentsChanged(slot);
                    }
                    return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
                }
            }
        } : super.createExportItemHandler();
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote && getOffsetTimer() % 5 == 0) {
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
        WidgetGroup widgetGroup = new WidgetGroup();
        for (int i = 0; i < bus.getSlots(); i++) {
            widgetGroup.addWidget(new SlotWidget(bus, i, 7 + 18 * (i % 10), 14 + 18 * (i / 10), true, !isExport)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 196, 63 + 72 * (getTier() / 3))
                .label(7, 4, getMetaFullName())
                .bindPlayerInventory(player.inventory, -18 + 72 * (getTier() / 3))
                .widget(widgetGroup)
                .build(getHolder(), player);
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
}
