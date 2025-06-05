package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelLargeAssemblerInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_ASSEMBLER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(LEFT, FRONT, DOWN);
            String[] aisle1 = {"CCCI", "CCC~", "CCCC", "CGGG", "CGGG", "CGGG"};
            String[] aisle2 = {"CCCi", "CPPM", "CPCC", "CP#G", "CP#G", "CP#G"};
            String[] aisle3 = {"CCCO", "EAAS", "CCCC", "CPcR", "CPcR", "CPcR"};
            String[] aisle4 = {"CCCC", "CCCC", "CCCC", "CCCC", "CCCC", "CCCC"};
            for (int layer = 0; layer < shapeInfo; layer++) {
                if (layer == shapeInfo - 1) {
                    aisle1 = ArrayUtils.addAll(aisle1, "CCCC");
                    aisle2 = ArrayUtils.addAll(aisle2, "CCCC");
                    aisle3 = ArrayUtils.addAll(aisle3, "CCCC");
                    aisle4 = ArrayUtils.addAll(aisle4, "CCCC");
                } else {
                    aisle1 = ArrayUtils.addAll(aisle1, "CGGG", "CGGG", "CGGG");
                    aisle2 = ArrayUtils.addAll(aisle2, "CP#G", "CP#G", "CP#G");
                    aisle3 = ArrayUtils.addAll(aisle3, "CPcR", "CPcR", "CPcR");
                    aisle4 = ArrayUtils.addAll(aisle4, "CCCC", "CCCC", "CCCC");
                }
            }
            shapeInfos.add(builder.aisle(aisle1)
                    .aisle(aisle2)
                    .aisle(aisle3)
                    .aisle(aisle4)
                    .where('S', this.getController(), WEST)
                    .where('C', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.LARGE_ASSEMBLER))
                    .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                    .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS))
                    .where('A', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.ASSEMBLY_LINE_CASING))
                    .where('c', GAMetaBlocks.CONVEYOR_CASING.getDefaultState())
                    .where('R', GAMetaBlocks.ROBOT_ARM_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EAST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_assembler.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
