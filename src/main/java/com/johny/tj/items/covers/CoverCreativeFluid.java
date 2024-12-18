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
import gregtech.common.covers.filter.SimpleFluidFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Arrays;
import java.util.Optional;

import static gregtech.api.unification.material.Materials.Lava;

public class CoverCreativeFluid extends CoverBehavior implements CoverWithUI, ITickable {

    private int speed = 1;
    private long timer = 0L;
    private final SimpleFluidFilter fluidFilter;
    private final IFluidHandler fluidHandler;

    public CoverCreativeFluid(ICoverable coverHolder, EnumFacing attachedSide, int speed) {
        super(coverHolder, attachedSide);
        this.speed = speed;
        this.fluidFilter = new SimpleFluidFilter();
        this.fluidHandler = this.coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.attachedSide) != null;
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
        fluidFilterGroup.addWidget(new LabelWidget(-15, -15, "cover.creative_fluid.title"));
        this.fluidFilter.initUI(fluidFilterGroup::addWidget);
        return ModularUI.builder(GuiTextures.BORDERED_BACKGROUND, 176, 105 + 82)
                .widget(fluidFilterGroup)
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 105)
                .build(this, player);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagCompound compound = new NBTTagCompound();
        this.fluidFilter.writeToNBT(compound);
        data.setTag("CreativeFilterFluid", compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagCompound tagCompound = data.getCompoundTag("CreativeFilterFluid");
        this.fluidFilter.readFromNBT(tagCompound);
    }

    @Override
    public void update() {
        if (timer++ % ((20 * speed) / (Math.pow(2, speed - 1))) == 0) {
            for (int index = 0; index < 9; index++) {
                FluidStack fluid = this.fluidFilter.getFluidInSlot(index);
                if (fluid != null) {
                    fluid.amount = Integer.MAX_VALUE;
                    Arrays.stream(fluidHandler.getTankProperties())
                            .filter(iFluidTankProperties -> Optional.ofNullable(iFluidTankProperties.getContents()).orElse(Lava.getFluid(1)).isFluidEqual(fluid) && iFluidTankProperties.getCapacity() > Integer.MAX_VALUE - 1)
                            .findFirst()
                            .ifPresent($ -> fluidHandler.fill(fluid, true));
                    //int amount = fluid != null ? Math.max(64000 - fluid.amount, 0) : 64000;
                    fluidHandler.fill(fluid, true);
                }
            }
        }
    }
}
