package com.johny.tj.mixin.gregtech;

import gregtech.integration.theoneprobe.provider.ElectricContainerInfoProvider;
import mcjty.theoneprobe.api.IProgressStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;


@Mixin(value = ElectricContainerInfoProvider.class, remap = false)
public class ElectricContainerInfoProviderMixin {

    @ModifyArgs(at = @At(value = "INVOKE", target = "Lmcjty/theoneprobe/api/IProbeInfo;progress(JJLmcjty/theoneprobe/api/IProgressStyle;)Lmcjty/theoneprobe/api/IProbeInfo;"),
        method = "addProbeInfo(Lgregtech/api/capability/IEnergyContainer;Lmcjty/theoneprobe/api/IProbeInfo;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)V")
    private void setProgressStyle(Args args) {
        long energyStored = args.get(0);
        long maxStorage = args.get(1);
        IProgressStyle style = args.get(2);
        args.set(0, energyStored);
        args.set(1, maxStorage);
        args.set(2, style
                .suffix("/" + maxStorage + " EU")
                .borderColor(-1)
                .backgroundColor(16777216)
                .alternateFilledColor(0xFFEED000)
                .filledColor(0xFFFFE000));
    }
}
