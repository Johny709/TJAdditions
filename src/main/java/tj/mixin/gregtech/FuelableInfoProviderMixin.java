package tj.mixin.gregtech;

import gregtech.integration.theoneprobe.provider.FuelableInfoProvider;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProgressStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tj.TJConfig;
import tj.TJValues;


@Mixin(value = FuelableInfoProvider.class, remap = false)
public abstract class FuelableInfoProviderMixin {

    @Redirect(method = "addProbeInfo(Lgregtech/api/capability/IFuelable;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V",
            at = @At(value = "INVOKE", target = "Lmcjty/theoneprobe/api/IProbeInfo;text(Ljava/lang/String;)Lmcjty/theoneprobe/api/IProbeInfo;", ordinal = 3))
    private IProbeInfo redirectAddProbeInfo(IProbeInfo probeInfo, String s) {
        if (TJConfig.machines.theOneProbeInfoProviderOverrides) {
            String[] info = s.split(" ");
            probeInfo.text(String.format("\n%s §b%s", info[0], TJValues.thousandFormat.format(Integer.parseInt(info[1]))));
        } else probeInfo.text(s);
        return probeInfo;
    }

    @Redirect(method = "addProbeInfo(Lgregtech/api/capability/IFuelable;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V",
            at = @At(value = "INVOKE", target = "Lmcjty/theoneprobe/api/IProbeInfo;text(Ljava/lang/String;)Lmcjty/theoneprobe/api/IProbeInfo;", ordinal = 4))
    private IProbeInfo redirectAddProbeInfo2(IProbeInfo probeInfo, String s) {
        if (TJConfig.machines.theOneProbeInfoProviderOverrides) {
            String[] info = s.split(" ");
            probeInfo.text(String.format("\n%s §b%s §r%s", info[0], TJValues.thousandFormat.format(Long.parseLong(info[1])), info[2]));
        } else probeInfo.text(s);
        return probeInfo;
    }

    @Redirect(method = "addProbeInfo(Lgregtech/api/capability/IFuelable;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V",
            at = @At(value = "INVOKE", target = "Lmcjty/theoneprobe/api/IProbeInfo;progress(IILmcjty/theoneprobe/api/IProgressStyle;)Lmcjty/theoneprobe/api/IProbeInfo;"))
    private IProbeInfo redirectAddProbeInfo3(IProbeInfo probeInfo, int fuelRemaining, int fuelCapacity, IProgressStyle style) {
        if (TJConfig.machines.theOneProbeInfoProviderOverrides) {
            int fuelPercent = (fuelRemaining / (fuelCapacity) * 100);
            String displayFuel = String.format("%s/%s | ", TJValues.thousandFormat.format(fuelRemaining), TJValues.thousandFormat.format(fuelCapacity));
            probeInfo.progress(fuelPercent, 100, probeInfo.defaultProgressStyle()
                    .width((int) (displayFuel.length() * 6.2))
                    .prefix(displayFuel)
                    .suffix("%")
                    .borderColor(0x00000000)
                    .backgroundColor(0x00000000)
                    .filledColor(0xFFFFE000)
                    .alternateFilledColor(0xFFEED000));
        } else probeInfo.progress(fuelRemaining, fuelCapacity, style.suffix("/" + fuelCapacity + " ")
                .borderColor(0x00000000)
                .backgroundColor(0x00000000)
                .filledColor(0xFFFFE000)
                .alternateFilledColor(0xFFEED000));
        return probeInfo;
    }
}
