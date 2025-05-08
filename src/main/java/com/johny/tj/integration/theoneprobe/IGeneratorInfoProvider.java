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
        String[] info = capability.productionInfo();

        IProbeInfo pageInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        StringBuilder prefixBuilder = new StringBuilder(), suffixBuilder = new StringBuilder();

        boolean suffix = false;
        for (String text : info) {
            if (text.equals("suffix")) {
                suffix = true;
                continue;
            }

            String textInfo = text.startsWith("§") ? text
                    : text.startsWith(" ") ? " "
                    : "{*" + text + "*}";

            if (!suffix)
                prefixBuilder.append(textInfo);
            else
                suffixBuilder.append(textInfo);
        }
        pageInfo.text(TextStyleClass.INFO + prefixBuilder.toString() + String.format("%,d", generation) + suffixBuilder);
    }

    @Override
    public String getID() {
        return "tj:generator_provider";
    }
}
