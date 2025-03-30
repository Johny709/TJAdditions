package com.johny.tj.integration.theoneprobe;

import com.johny.tj.capability.LinkPos;
import com.johny.tj.capability.LinkPosInterDim;
import com.johny.tj.capability.TJCapabilities;
import gregtech.api.block.machines.BlockMachine;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;

import static com.johny.tj.capability.TJCapabilities.CAPABILITY_LINK_POS_INTERDIM;

public class LinkedPosInfoProvider extends CapabilityInfoProvider<LinkPos> {

    @Override
    protected Capability<LinkPos> getCapability() {
        return TJCapabilities.CAPABILITY_LINK_POS;
    }

    @Override
    protected void addProbeInfo(LinkPos capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        LinkPosInterDim interDimPos = tileEntity.getCapability(CAPABILITY_LINK_POS_INTERDIM, null);

        int pageIndex = capability.getPageIndex();
        int pageSize = capability.getPageSize();
        int size = capability.getBlockPosSize();

        IProbeInfo pageInfo = probeInfo.vertical(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        pageInfo.text(TextStyleClass.INFO + "§b(" +(pageIndex + 1) + "/" + size + ")");

        for (int i = pageIndex; i < pageIndex + pageSize; i++) {
            WorldServer world = interDimPos != null ? DimensionManager.getWorld(interDimPos.getDimension(i)) : (WorldServer) capability.world();
            BlockPos pos = capability.getBlockPos(i);
            if (i < size && pos != null) {
                TileEntity entity = world.getTileEntity(pos);
                MetaTileEntity gregEntity = BlockMachine.getMetaTileEntity(world, pos);

                IProbeInfo nameInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                nameInfo.text(TextStyleClass.INFO + "§b[" + (i + 1) + "]§r ");

                if (entity != null || gregEntity != null) {
                    nameInfo.item(gregEntity != null ? gregEntity.getStackForm() : new ItemStack(entity.getBlockType()));
                    nameInfo.text(TextStyleClass.INFO + (gregEntity != null ? " {*" + gregEntity.getMetaFullName() + "*}" : "{*" + entity.getBlockType().getTranslationKey() + ".name*}"));

                    IProbeInfo posInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                    posInfo.text(TextStyleClass.INFO + (gregEntity != null ? "X:§e " + gregEntity.getPos().getX() + ", §rY:§e " + gregEntity.getPos().getY() + ", §rZ:§e " + gregEntity.getPos().getZ()
                            : "X:§e " + entity.getPos().getX() + ", §rY:§e " + entity.getPos().getY() + ", §rZ:§e " + entity.getPos().getZ()));
                }
            }
        }
    }

    @Override
    public String getID() {
        return "tj:linked_pos_provider";
    }
}
