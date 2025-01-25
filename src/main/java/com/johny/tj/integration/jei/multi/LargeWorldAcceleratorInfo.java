package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class LargeWorldAcceleratorInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_WORLD_ACCELERATOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("#C#", "HEC", "#C#")
                .aisle("IEC", "SFE", "MEC")
                .aisle("#C#", "CEC", "#C#")
                .where('S', TJMetaTileEntities.LARGE_WORLD_ACCELERATOR, EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.TRITANIUM))
                .where('F', GAMetaBlocks.FIELD_GEN_CASING.getDefaultState())
                .where('E', GAMetaBlocks.EMITTER_CASING.getDefaultState())
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.UV], EnumFacing.WEST)
                .where('H', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.UV], EnumFacing.WEST)
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_world_accelerator.description")};
    }
}
