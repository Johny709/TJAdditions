package tj.integration.theoneprobe;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.capabilities.Capability;
import tj.TJValues;
import tj.capability.IRecipeInfo;
import tj.capability.TJCapabilities;

public class IRecipeInfoProvider extends CapabilityInfoProvider<IRecipeInfo> {

    @Override
    protected Capability<IRecipeInfo> getCapability() {
        return TJCapabilities.CAPABILITY_RECIPE;
    }

    @Override
    protected void addProbeInfo(IRecipeInfo capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        long recipeEUt = capability.getEUt();
        int tier = GAUtility.getTierByVoltage(recipeEUt) + 1;
        String voltage = TJValues.VCC[tier] + GAValues.VN[tier];
        if (recipeEUt > 0)
            probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT))
                    .text(TextStyleClass.INFO + I18n.translateToLocalFormatted("tj.multiblock.eu", recipeEUt) + String.format(" §r(%s§r)", voltage));
    }

    @Override
    public String getID() {
        return "tj:recipe_provider";
    }
}
