package tj.mixin.gregic_probe;

import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vfyjxf.gregicprobe.config.GregicProbeConfig;
import vfyjxf.gregicprobe.integration.gregtech.EnergyInfoProvider;


@Mixin(value = EnergyInfoProvider.class, remap = false)
public abstract class ElectricContainerInfoProviderMixin {

    @Inject(method = "addProbeInfo(Lgregtech/api/capability/IEnergyContainer;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V",
            at = @At("HEAD"), cancellable = true)
    private void injectElectricContainer(IEnergyContainer capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing sideHit, CallbackInfo ci) {
        long energyStored = capability.getEnergyStored();
        long maxStorage = capability.getEnergyCapacity();
        if (maxStorage > 0) {
            IProbeInfo horizontalPane = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
            String additionalSpacing = tileEntity.hasCapability(GregtechTileCapabilities.CAPABILITY_WORKABLE, sideHit) ? "   " : "";
            horizontalPane.text(TextStyleClass.INFO + "{*gregtech.top.energy_stored*} " + additionalSpacing);
            horizontalPane.progress(energyStored, maxStorage, probeInfo.defaultProgressStyle()
                    .width(110)
                    .suffix("/" + maxStorage + " EU")
                    .borderColor(GregicProbeConfig.borderColorEnergy)
                    .backgroundColor(GregicProbeConfig.backgroundColorEnergy)
                    .alternateFilledColor(GregicProbeConfig.alternateFilledColorEnergy)
                    .filledColor(GregicProbeConfig.filledColorEnergy));
        }
        ci.cancel();
    }
}
