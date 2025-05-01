package com.johny.tj.integration.theoneprobe;

import com.johny.tj.capability.IFluidHandlerInfo;
import com.johny.tj.capability.TJCapabilities;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.List;

public class IFluidHandlerInfoProvider extends CapabilityInfoProvider<IFluidHandlerInfo> {

    @Override
    protected Capability<IFluidHandlerInfo> getCapability() {
        return TJCapabilities.CAPABILITY_FLUID_HANDLING;
    }

    @Override
    protected void addProbeInfo(IFluidHandlerInfo capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        List<FluidStack> inputs = capability.getFluidInputs();
        List<FluidStack> outputs = capability.getFluidOutputs();

        if (inputs != null) {
            IProbeInfo inputInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            inputInfo.text(TextStyleClass.INFO + "{*tj.top.fluid.inputs*} ");
            for (FluidStack fluid : inputs) {
                inputInfo.item(FluidUtil.getFilledBucket(fluid));
                inputInfo.text(String.format("%,d" + "L", fluid.amount));
            }
        }

        if (outputs != null) {
            IProbeInfo outputInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            outputInfo.text(TextStyleClass.INFO + "{*tj.top.fluid.outputs*} ");
            for (FluidStack fluid : outputs) {
                outputInfo.item(FluidUtil.getFilledBucket(fluid));
                outputInfo.text(String.format("%,d" + "L", fluid.amount));
            }
        }
    }

    @Override
    public String getID() {
        return "tj:fluid_handler_provider";
    }
}
