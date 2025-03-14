package com.johny.tj.integration.theoneprobe;

import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.TJCapabilities;
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

public class ParallelControllerInfoProvider extends CapabilityInfoProvider<IParallelController> {

    @Override
    protected Capability<IParallelController> getCapability() {
        return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER;
    }

    @Override
    protected void addProbeInfo(IParallelController capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        long maxEUt = capability.getMaxEUt();
        int energyBonus = capability.getEUBonus();
        long totalEnergy = capability.getTotalEnergy();
        long voltageTier = capability.getVoltageTier();
        RecipeMap<?> multiblockRecipe = capability.getMultiblockRecipe();

        IProbeInfo controllerInfo = probeInfo.vertical(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.maxeut*}§e " + maxEUt + " §r(" + GAValues.VN[GAUtility.getTierByVoltage(maxEUt)] + ")");
        if (energyBonus != -1)
            controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.energy_bonus*}§b " + (100 - energyBonus) + "%");
        controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.total_energy*}§e " + totalEnergy + " §7EU/t");
        controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.voltage_tier*}§a " + voltageTier + " §r(§a" + GAValues.VN[GAUtility.getTierByVoltage(voltageTier)] + "§r)");
        if (multiblockRecipe != null)
            controllerInfo.text(TextStyleClass.INFO + "{*tj.top.parallel_controller.multiblock_recipe*}§6 " + multiblockRecipe.getLocalizedName());
    }

    @Override
    public String getID() {
        return "tj:parallel_controller_provider";
    }

}
