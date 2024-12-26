package com.johny.tj.multiblockpart.utility;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.multiblockpart.TJMultiblockAbility;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.render.Textures;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class MetaTileEntityMachineController extends MetaTileEntityMultiblockPart implements IMultiblockAbilityPart<MetaTileEntityMachineController> {

    private boolean redstonePowered = false;

    public MetaTileEntityMachineController(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, 1);
    }

    public boolean getRedstonePowered() {
        return this.redstonePowered;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMachineController(metaTileEntityId);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MACHINE_CONTROLLER_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 176, 105 + 82)
                .label(10, 5, "cover.machine_controller.name")
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 105)
                .build(getHolder(), player);
    }

    @Override
    public MultiblockAbility<MetaTileEntityMachineController> getAbility() {
        return TJMultiblockAbility.REDSTONE_CONTROLLER;
    }

    @Override
    public void registerAbilities(List<MetaTileEntityMachineController> abilityList) {
        abilityList.add(this);
    }

    @Override
    protected boolean canMachineConnectRedstone(EnumFacing side) {
        return side == getFrontFacing();
    }

    @Override
    public void update() {
        super.update();

        if (!getWorld().isRemote && getOffsetTimer() % 5 == 0) {
            this.redstonePowered = getInputRedstoneSignal(getFrontFacing(), false) > 0;
        }
    }

    @Override
    public NBTTagCompound writeToNBT (NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("RedstonePowered", this.redstonePowered);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.redstonePowered = data.getBoolean("RedstonePowered");
    }
}
