package com.johny.tj.integration.jei.multi.parallel;

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
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);

            StringBuilder aisleC = new StringBuilder(), aisleG = new StringBuilder(), aisleP = new StringBuilder(),
                    aisleA = new StringBuilder(), aislec = new StringBuilder(), aisleR = new StringBuilder();
            for (int layer = 1; layer < shapeInfo; layer++) {
                aisleC.append("CCC");
                aisleG.append("GGG");
                aisleP.append("PPP");
                aisleA.append("###");
                aislec.append("ccc");
                aisleR.append("RRR");
            }
            aisleC.append("C");
            aisleG.append("C");
            aisleP.append("C");
            aisleA.append("C");
            aislec.append("C");
            aisleR.append("C");

            shapeInfos.add(builder
                    .aisle("I~CGGG" + aisleG, "CCCGGG" + aisleG, "CCCGGG" + aisleG, "CCCCCC" + aisleC)
                    .aisle("iMCGGG" + aisleG, "CPC###" + aisleA, "CPPPPP" + aisleP, "CCCCCC" + aisleC)
                    .aisle("OSCRRR" + aisleR, "CACccc" + aislec, "CACPPP" + aisleP, "CCCCCC" + aisleC)
                    .aisle("CCCCCC" + aisleC, "CCCCCC" + aisleC, "CCCCCC" + aisleC, "CCCCCC" + aisleC)
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
