package tj.mixin;

import gregtech.api.capability.IWorkable;
import gregtech.integration.theoneprobe.provider.WorkableInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = WorkableInfoProvider.class, remap = false)
public abstract class WorkableInfoProviderMixin {

    @Inject(method = "addProbeInfo(Lgregtech/api/capability/IWorkable;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V",
            at = @At("HEAD"), cancellable = true)
    private void injectProgressInfo(IWorkable capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing sideHit, CallbackInfo ci) {
        float currentProgress = capability.getProgress();
        float maxProgress = capability.getMaxProgress();
        if (maxProgress > 0) {
            int progressScaled = (int) Math.floor(currentProgress / (maxProgress * 1.0) * 100);
            IProbeInfo progressInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            progressInfo.text(TextStyleClass.INFO + "{*gregtech.top.progress*} ");
            progressInfo.progress(progressScaled, 100, probeInfo.defaultProgressStyle()
                    .width(105)
                    .prefix((currentProgress / 20) + "s / " + (maxProgress / 20) + "s | ")
                    .suffix("%")
                    .borderColor(-1)
                    .backgroundColor(16777216)
                    .filledColor(0xFF000099)
                    .alternateFilledColor(0xFF000077));
        }
        ci.cancel();
    }
}


