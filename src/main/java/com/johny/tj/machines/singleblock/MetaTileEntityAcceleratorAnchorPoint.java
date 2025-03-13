package com.johny.tj.machines.singleblock;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.TJValues;
import com.johny.tj.machines.LinkSet;
import com.johny.tj.machines.multi.electric.MetaTileEntityLargeWorldAccelerator;
import com.johny.tj.textures.TJTextures;
import gregicadditions.client.ClientHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.CycleButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.render.SimpleSidedCubeRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Supplier;

public class MetaTileEntityAcceleratorAnchorPoint extends MetaTileEntity implements LinkSet {

    private Supplier<MetaTileEntity> entitySupplier;
    private boolean isActive;
    private int tier;
    private boolean redStonePowered;
    private boolean inverted;

    public MetaTileEntityAcceleratorAnchorPoint(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityAcceleratorAnchorPoint(metaTileEntityId);
    }

    @Override
    public void update() {
        super.update();
        if (getWorld().isRemote || entitySupplier == null)
            return;
        if (entitySupplier.get() instanceof MetaTileEntityLargeWorldAccelerator) {
            MetaTileEntityLargeWorldAccelerator accelerator = (MetaTileEntityLargeWorldAccelerator)entitySupplier.get();
            setActive(accelerator.isActive());
            if (isActive)
                for (EnumFacing facing : EnumFacing.VALUES)
                    this.redStonePowered = getInputRedstoneSignal(facing, false) > 0;
        }
    }

    @Override
    protected boolean canMachineConnectRedstone(EnumFacing side) {
        return true;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getBaseRenderer().render(renderState, translation, pipeline);

        int oldBaseColor = renderState.baseColour;
        int oldAlphaOverride = renderState.alphaOverride;
        renderState.baseColour = TJValues.VC[tier] << 8;
        renderState.alphaOverride = 0xFF;
        TJTextures.FIELD_GENERATOR_CORE.render(renderState, translation, pipeline);

        renderState.baseColour = oldBaseColor;
        renderState.alphaOverride = oldAlphaOverride;

        for (EnumFacing facing : EnumFacing.VALUES) {
            TJTextures.FIELD_GENERATOR_SPIN.renderSided(facing, renderState, translation, pipeline);
        }
    }

    @SideOnly(Side.CLIENT)
    protected SimpleSidedCubeRenderer getBaseRenderer() {
        return ClientHandler.VOLTAGE_CASINGS[tier];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(getBaseRenderer().getParticleSprite(), getPaintingColor());
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new ImageWidget(10, 30, 150, 30, GuiTextures.DISPLAY));
        widgetGroup.addWidget(new AdvancedTextWidget(15, 35, this::addDisplayText, 0xFFFFFF));
        widgetGroup.addWidget(new CycleButtonWidget(10, 60, 150, 20, this::isInverted, this::setInverted,
                "cover.machine_controller.normal", "cover.machine_controller.inverted"));
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 176, 187)
                .widget(widgetGroup)
                .label(10, 5, "tj.machine.accelerator_anchor_point.name")
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 105)
                .build(getHolder(), player);
    }

    private void addDisplayText(List<ITextComponent> textList) {
        textList.add(entitySupplier != null ? new TextComponentTranslation(entitySupplier.get().getMetaFullName())
                .appendText("\n")
                .appendSibling(new TextComponentString(" X: ").appendSibling(new TextComponentString("" + entitySupplier.get().getPos().getX()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                .appendSibling(new TextComponentString(" Y: ").appendSibling(new TextComponentString("" + entitySupplier.get().getPos().getY()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                .appendSibling(new TextComponentString(" Z: ").appendSibling(new TextComponentString("" + entitySupplier.get().getPos().getZ()).setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                : new TextComponentTranslation("machine.universal.linked.entity.null"));
    }

    @Override
    public void setLink(Supplier<MetaTileEntity> entitySupplier) {
        if (entitySupplier.get() instanceof MetaTileEntityLargeWorldAccelerator) {
            MetaTileEntityLargeWorldAccelerator accelerator = (MetaTileEntityLargeWorldAccelerator) entitySupplier.get();
            this.tier = accelerator.getTier();
            this.entitySupplier = entitySupplier;
            if (!getWorld().isRemote) {
                writeCustomData(2, buf -> buf.writeInt(tier));
                markDirty();
            }
        } else {
            this.tier = 0;
            writeCustomData(2, buf -> buf.writeInt(tier));
            markDirty();
        }
    }

    @Override
    public MetaTileEntity getLink() {
        return this;
    }

    public boolean isRedStonePowered() {
        return !isInverted() == redStonePowered;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
        }
        if (dataId == 2) {
            this.tier = buf.readInt();
        }
        scheduleRenderUpdate();
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
        buf.writeInt(tier);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
        this.tier = buf.readInt();
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Tier", tier);
        data.setBoolean("IsActive", isActive);
        data.setBoolean("Inverted", inverted);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.tier = data.getInteger("Tier");
        this.isActive = data.getBoolean("IsActive");
        this.inverted = data.getBoolean("Inverted");
    }
}
