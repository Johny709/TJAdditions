package tj.integration.theoneprobe;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.recipes.RecipeMap;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import tj.capability.IParallelController;
import tj.capability.TJCapabilities;

public class ParallelControllerInfoProvider extends CapabilityInfoProvider<IParallelController> {

    @Override
    protected Capability<IParallelController> getCapability() {
        return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER;
    }

    @Override
    protected void addProbeInfo(IParallelController capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        long energyStored = capability.getEnergyStored();
        long energyCapacity = capability.getEnergyCapacity();
        long maxEUt = capability.getMaxEUt();
        int energyBonus = capability.getEUBonus();
        long totalEnergy = capability.getTotalEnergyConsumption();
        long voltageTier = capability.getVoltageTier();
        RecipeMap<?> multiblockRecipe = capability.getMultiblockRecipe();

        IProbeInfo controllerInfo = probeInfo.vertical(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.maxeut*}§e " + maxEUt + " §r(" + GAValues.VN[GAUtility.getTierByVoltage(maxEUt)] + ")");
        if (energyStored > 0 && energyCapacity > 0) {
            int progressScaled = (int) Math.floor(energyStored / (energyCapacity * 1.0) * 100);
            String displayEnergy = "% | " + energyStored +  " / " + energyCapacity + " EU";
            IProbeInfo energyStoredInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            energyStoredInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.energy_stored*} ");
            energyStoredInfo.progress(progressScaled, 100, probeInfo.defaultProgressStyle()
                    .suffix(displayEnergy)
                    .alternateFilledColor(0xFFF8EB34)
                    .filledColor(0xFFF8EB34)
                    .width((displayEnergy.length() * 12) / 2));
        }
        if (energyBonus > 0)
            controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.energy_bonus*}§b " + (100 - energyBonus) + "%");
        controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.energy_consumption*}§e " + totalEnergy + " §7EU/t");
        controllerInfo.text(TextStyleClass.INFO + "{*machine.universal.tooltip.voltage_tier*}§a " + voltageTier + " §r(§a" + GAValues.VN[GAUtility.getTierByVoltage(voltageTier)] + "§r)");
        if (multiblockRecipe != null)
            controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.multiblock_recipe*}§6 " + "{*recipemap." + multiblockRecipe.getUnlocalizedName() + ".name*}");
    }

    @Override
    public String getID() {
        return "tj:parallel_controller_provider";
    }

}
