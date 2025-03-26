package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.List;

import static gregicadditions.GAMaterials.Talonite;

public class LargeWirelessEnergyEmitterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_WIRELESS_CHARGING_STATION;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("~CCC~", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CCCCC", "CCFCC", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("iCCCH", "SFIFM", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~FIF~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("CCCCC", "CCFCC", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("~CCC~", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', getController(), EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TALONITE))
                .where('F', MetaBlocks.FRAMES.get(Talonite).getDefaultState())
                .where('I', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_LUV))
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.EAST)
                .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.LuV], EnumFacing.WEST)
                .where('H', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.LuV], EnumFacing.EAST)
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_wireless_energy_emitter.description=")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
