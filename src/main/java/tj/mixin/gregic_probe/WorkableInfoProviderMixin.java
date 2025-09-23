package tj.mixin.gregic_probe;

import gregtech.api.capability.IWorkable;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tj.TJConfig;
import vfyjxf.gregicprobe.config.GregicProbeConfig;
import vfyjxf.gregicprobe.integration.gregtech.WorkableInforProvider;

import java.text.DecimalFormat;


@Mixin(value = WorkableInforProvider.class, remap = false)
public abstract class WorkableInfoProviderMixin {

    @Unique
    private final DecimalFormat twoPlaceFormat = new DecimalFormat("#0.00");

    @Inject(method = "addProbeInfo(Lgregtech/api/capability/IWorkable;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V",
            at = @At("HEAD"), cancellable = true)
    private void injectProgressInfo(IWorkable capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing sideHit, CallbackInfo ci) {
        if (!TJConfig.machines.theOneProbeInfoProviderOverrides) return;
        float currentProgress = capability.getProgress();
        float maxProgress = capability.getMaxProgress();
        if (maxProgress > 0) {
            int progressScaled = (int) Math.floor(currentProgress / (maxProgress * 1.0) * 100);
            IProbeInfo progressInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            progressInfo.text(TextStyleClass.INFO + "{*gregtech.top.progress*} ");
            progressInfo.progress(progressScaled, 100, probeInfo.defaultProgressStyle()
                    .width(110)
                    .prefix(this.twoPlaceFormat.format(currentProgress / 20) + "s / " + this.twoPlaceFormat.format(maxProgress / 20) + "s | ")
                    .suffix("%")
                    .borderColor(GregicProbeConfig.borderColorProgress)
                    .backgroundColor(GregicProbeConfig.backgroundColorProgress)
                    .filledColor(GregicProbeConfig.filledColorProgress)
                    .alternateFilledColor(GregicProbeConfig.alternateFilledColorProgress));
        }
        ci.cancel();
    }
}


