package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.machines.multi.electric.MetaTileEntityLargeAtmosphereCollector;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class LargeAtmosphereCollectorInfo extends MultiblockInfoPage {

    private final MetaTileEntityLargeTurbine.TurbineType turbineType;
    private final MetaTileEntityLargeAtmosphereCollector tileEntity;

    public LargeAtmosphereCollectorInfo(MetaTileEntityLargeTurbine.TurbineType turbineType, MetaTileEntityLargeAtmosphereCollector tileEntity) {
        this.turbineType = turbineType;
        this.tileEntity = tileEntity;
    }

    @Override
    public MultiblockControllerBase getController() {
        return tileEntity;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        GAMultiblockShapeInfo builder = GAMultiblockShapeInfo.builder()
                .aisle("CCC", "CfC", "PPP", "PPP", "PPP", "CCC")
                .aisle("CFC", "SCC", "PPP", "PPP", "PPP", "CRC")
                .aisle("CCC", "CoC", "PPP", "PPP", "PPP", "CCC")
                .where('S', tileEntity, EnumFacing.WEST)
                .where('C', turbineType.casingState)
                .where('P', tileEntity.getPipeState())
                .where('R', MetaTileEntities.ROTOR_HOLDER[0], EnumFacing.UP)
                .where('F', MetaTileEntities.FLUID_EXPORT_HATCH[3 + turbineType.ordinal()], EnumFacing.DOWN)
                .where('f', MetaTileEntities.FLUID_IMPORT_HATCH[3 + turbineType.ordinal()], EnumFacing.NORTH)
                .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[3 + turbineType.ordinal()], EnumFacing.SOUTH)
                .build();
        return Lists.newArrayList(builder);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_atmosphere_collector.description")};
    }
}
