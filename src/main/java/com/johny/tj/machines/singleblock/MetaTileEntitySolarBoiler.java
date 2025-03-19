package com.johny.tj.machines.singleblock;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.render.Textures;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;

public class MetaTileEntitySolarBoiler extends MetaTileEntityCoalBoiler {

    public MetaTileEntitySolarBoiler(ResourceLocation metaTileEntityId, BoilerType boilerType) {
        super(metaTileEntityId, boilerType);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntitySolarBoiler(metaTileEntityId, boilerType);
    }

    @Override
    protected boolean canBurn() {
        return getWorld().isDaytime() && getWorld().canBlockSeeSky(getPos().up()) && !getWorld().isRaining();
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        widget.accept(new ImageWidget(120, 15, 40, 40, isActive() ? boilerType.getSun() : boilerType.getMoon()));
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.SOLAR_BOILER_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive());
    }
}
