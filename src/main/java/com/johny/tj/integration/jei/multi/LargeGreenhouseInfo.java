package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

public class LargeGreenhouseInfo extends MultiblockInfoPage {
    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_GREENHOUSE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfo = new ArrayList<>();
        for (GATransparentCasing.CasingType glassType : GATransparentCasing.CasingType.values()) {

            shapeInfo.add(GAMultiblockShapeInfo.builder(BlockPattern.RelativeDirection.RIGHT, BlockPattern.RelativeDirection.UP, BlockPattern.RelativeDirection.BACK)
                    .aisle("~CCCCC~", "~CCCCC~", "~CCCCC~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~~~~~~~")
                    .aisle("CCCCCCC", "CDDDDDC", "C#####C", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                    .aisle("iCCCCCC", "IDDDDDC", "C#####C", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                    .aisle("MCCPCCC", "SDDDDDE", "C#####C", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                    .aisle("CCCCCCC", "ODDDDDC", "C#####C", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                    .aisle("CCCCCCC", "CDDDDDC", "C#####C", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                    .aisle("~CCCCC~", "~CCCCC~", "~CCCCC~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~~~~~~~")
                    .where('S', TJMetaTileEntities.LARGE_GREENHOUSE, EnumFacing.WEST)
                    .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STAINLESS_CLEAN))
                    .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(glassType))
                    .where('P', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('D', new BlockInfo(Block.getBlockFromName("randomthings:fertilizeddirt")))
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .build());
        }
        return shapeInfo;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_greenhouse.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
