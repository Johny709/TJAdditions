package com.johny.tj.integration.theoneprobe;

import com.johny.tj.capability.IItemHandlerInfo;
import com.johny.tj.capability.TJCapabilities;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import java.util.List;

public class IItemHandlerInfoProvider extends CapabilityInfoProvider<IItemHandlerInfo> {

    @Override
    protected Capability<IItemHandlerInfo> getCapability() {
        return TJCapabilities.CAPABILITY_ITEM_HANDLING;
    }

    @Override
    protected void addProbeInfo(IItemHandlerInfo capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        List<ItemStack> inputs = capability.getItemInputs();
        List<ItemStack> outputs = capability.getItemOutputs();

        if (inputs != null) {
            IProbeInfo inputInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            inputInfo.text(TextStyleClass.INFO + "{*tj.top.items.inputs*} ");
            for (ItemStack item : inputs) {
                IProbeInfo itemInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                itemInfo.item(item);
                itemInfo.text(TextStyleClass.INFO + " {*" + item.getTranslationKey() + "*} " + item.getCount());
            }
        }

        if (outputs != null) {
            IProbeInfo outputInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
            outputInfo.text(TextStyleClass.INFO + "{*tj.top.items.outputs*} ");
            for (ItemStack item : outputs) {
                IProbeInfo itemInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                itemInfo.item(item);
                itemInfo.text(TextStyleClass.INFO + " {*" + item.getTranslationKey() + "*} " + item.getCount());
            }
        }
    }

    @Override
    public String getID() {
        return "";
    }
}
