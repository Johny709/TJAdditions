package tj.integration.theoneprobe;

import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import tj.capability.IHeatInfo;
import tj.capability.TJCapabilities;

public class IHeatInfoProvider extends CapabilityInfoProvider<IHeatInfo> {

    @Override
    protected Capability<IHeatInfo> getCapability() {
        return TJCapabilities.CAPABILITY_HEAT;
    }

    @Override
    protected void addProbeInfo(IHeatInfo capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {

        long heat = capability.heat();
        long maxHeat = capability.maxHeat();
        int progressScaled = maxHeat == 0 ? 0 : (int) Math.floor(heat / (maxHeat * 1.0) * 100);
        String displayHeat = "% | " + heat +  " / " + maxHeat + " °C";

        IProbeInfo pageInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        pageInfo.text(TextStyleClass.INFO + "{*tj.top.progress.heat*} ");
        pageInfo.progress(progressScaled, 100, probeInfo.defaultProgressStyle()
                .suffix(displayHeat)
                .alternateFilledColor(0xFFF10000)
                .filledColor(0xFFF10000)
                .width((displayHeat.length() * 12) / 2));

    }

    @Override
    public String getID() {
        return "tj:heat_provider";
    }
}
