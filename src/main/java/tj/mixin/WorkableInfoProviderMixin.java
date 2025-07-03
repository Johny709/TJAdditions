package tj.mixin;

import gregtech.api.util.GTLog;
import gregtech.integration.theoneprobe.provider.WorkableInfoProvider;
import mcjty.theoneprobe.api.IProgressStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(value = WorkableInfoProvider.class, remap = false)
public abstract class WorkableInfoProviderMixin {

    @ModifyArg(at = @At(value = "INVOKE", target = "Lmcjty/theoneprobe/api/IProbeInfo;progress(IILmcjty/theoneprobe/api/IProgressStyle;)Lmcjty/theoneprobe/api/IProbeInfo;"), index = 2,
        method = "addProbeInfo(Lgregtech/api/capability/IWorkable;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V")
    private IProgressStyle setProgressStyle(IProgressStyle style) {
        GTLog.logger.info("Mixin progress bar");
        return style.suffix("%")
                .borderColor(-1)
                .backgroundColor(16777216)
                .filledColor(0xFF009BFF)
                .alternateFilledColor(0xFF009BFF);
    }
}


