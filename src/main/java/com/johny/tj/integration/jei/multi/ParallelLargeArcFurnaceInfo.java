package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.*;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelLargeArcFurnaceInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_ARC_FURNACE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(LEFT, FRONT, DOWN);
            for (int layer = 1; layer < shapeInfo; layer++) {
                builder.aisle("~CCC~", "CCCCC", "CcccC", "CCCCC", "~CCC~");
                builder.aisle("~GGG~", "GTPTG", "G###G", "GTPTG", "~GGG~");
            }
            builder.aisle("~CCC~", "CCCCI", "CcccC", "CCCCO", "~CCC~");
            builder.aisle("~GGG~", "GTPTG", "G###S", "GTPTG", "~GGG~");
            shapeInfos.add(builder.aisle("~CCC~", "CCCCi", "EcccM", "CCCCo", "~CCC~")
                    .where('S', this.getController(), WEST)
                    .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.INVAR_HEATPROOF))
                    .where('G', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING))
                    .where('P', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('T', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE))
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], WEST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], WEST)
                    .where('i', MetaTileEntities.ITEM_EXPORT_BUS[0], WEST)
                    .where('O', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[0], WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_arc_furnace.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
