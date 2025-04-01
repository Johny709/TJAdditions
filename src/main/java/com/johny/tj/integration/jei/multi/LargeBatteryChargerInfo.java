package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.CellCasing;
import gregicadditions.item.GAMetaBlocks;
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

public class LargeBatteryChargerInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_BATTERY_CHARGER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("CCCCC", "~CCC~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("iCCCC", "ICCCC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("MCCCC", "SCCCH", "~BFB~", "~BFB~", "~BFB~", "~BFB~", "~BFB~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("CCCCC", "OCCCC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CCCCC", "~CCC~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', getController(), EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TALONITE))
                .where('F', MetaBlocks.FRAMES.get(Talonite).getDefaultState())
                .where('B', GAMetaBlocks.CELL_CASING.getState(CellCasing.CellType.CELL_EV))
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.UV], EnumFacing.WEST)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.UV], EnumFacing.WEST)
                .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.UV], EnumFacing.WEST)
                .where('H', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.UV], EnumFacing.EAST)
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_wireless_energy_emitter.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
