package com.johny.tj.capability;

import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public interface IFluidHandlerInfo {

    List<FluidStack> getFluidInputs();

    List<FluidStack> getFluidOutputs();
}
