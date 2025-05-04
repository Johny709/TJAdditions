package com.johny.tj.integration.theoneprobe;

import com.johny.tj.capability.IGeneratorInfo;
import com.johny.tj.capability.TJCapabilities;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

public class IGeneratorInfoProvider extends CapabilityInfoProvider<IGeneratorInfo> {

    @Override
    protected Capability<IGeneratorInfo> getCapability() {
        return TJCapabilities.CAPABILITY_GENERATOR;
    }

    @Override
    protected void addProbeInfo(IGeneratorInfo capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {

        long generation = capability.getProduction();
        String prefix = capability.prefix() != null ? capability.prefix() : "";
        String suffix = capability.suffix() != null ? capability.suffix() : "";

        IProbeInfo pageInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        pageInfo.text(TextStyleClass.INFO + "{*" + prefix + "*} " + String.format("%,d", generation) + " {*" + suffix + "*}");

    }

    @Override
    public String getID() {
        return "tj:generator_provider";
    }
}
