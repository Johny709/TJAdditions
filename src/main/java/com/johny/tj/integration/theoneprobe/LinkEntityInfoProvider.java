package com.johny.tj.integration.theoneprobe;

import com.johny.tj.capability.LinkEntity;
import com.johny.tj.capability.LinkEntityInterDim;
import com.johny.tj.capability.TJCapabilities;
import gregtech.integration.theoneprobe.provider.CapabilityInfoProvider;
import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;

import static com.johny.tj.capability.TJCapabilities.CAPABILITY_LINK_ENTITY_INTERDIM;

public class LinkEntityInfoProvider extends CapabilityInfoProvider<LinkEntity<Entity>> {

    @Override
    protected Capability<LinkEntity<Entity>> getCapability() {
        return TJCapabilities.CAPABILITY_LINK_ENTITY;
    }

    @Override
    protected void addProbeInfo(LinkEntity<Entity> capability, IProbeInfo probeInfo, TileEntity tileEntity, EnumFacing enumFacing) {
        LinkEntityInterDim<Entity> interDimPos = tileEntity.getCapability(CAPABILITY_LINK_ENTITY_INTERDIM, null);

        int pageIndex = capability.getPageIndex();
        int pageSize = capability.getPageSize();
        int size = capability.getPosSize();

        IProbeInfo pageInfo = probeInfo.vertical(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
        pageInfo.text(TextStyleClass.INFO + "§b(" +(pageIndex + 1) + "/" + size + ")");

        for (int i = pageIndex; i < pageIndex + pageSize; i++) {
            WorldServer world = interDimPos != null ? DimensionManager.getWorld(interDimPos.getDimension(i)) : (WorldServer) capability.world();
            DimensionType worldType = world.provider.getDimensionType();
            Entity entity = capability.getPos(i);
            if (i < size && entity != null) {

                IProbeInfo nameInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                nameInfo.text(TextStyleClass.INFO + "§b[" + (i + 1) + "]§r ");

                IProbeInfo entityInfo = probeInfo.vertical(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                entityInfo.text(TextStyleClass.INFO + (entity.hasCustomName() ? entity.getCustomNameTag() : entity.getName()));

                IProbeInfo posInfo = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT));
                posInfo.text(TextStyleClass.INFO + "X:§e " + entity.posX + ", §rY:§e " + entity.posY + ", §rZ:§e " + entity.posZ);
                posInfo.text(TextStyleClass.INFO + " " + worldType.getName() + " (" + worldType.getId() + ")");
            }
        }
    }

    @Override
    public String getID() {
        return "tj:linked_entity_provider";
    }
}
