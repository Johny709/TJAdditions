package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class ParallelLargeMaceratorInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_MACERATOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo < 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT);
            builder.aisle("CCCCC", "CMEMC", "CCCCC", "CCCCC");
            for (int layer = 0; layer < shapeInfo; layer++) {
                builder.aisle("CCCCC", "CGBGC", "CB#BC", "C###C");
                builder.aisle("CCCCC", "CGBGC", "CB#BC", "C###C");
            }
            shapeInfos.add(builder
                    .aisle("CCCCC", "CGBGC", "CB#BC", "C###C")
                    .aisle("CCCCC", "CMSMC", "CImOC", "CCCCC")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.STELLITE))
                    .where('G', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TUNGSTENSTEEL_GEARBOX_CASING))
                    .where('B', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING))
                    .where('M', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('m', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_macerator.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
