package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
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
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregicadditions.GAMaterials.EglinSteel;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelLargeSifterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_SIFTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo < 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN);
            for (int layer = 1; layer < shapeInfo; layer++) {
                builder.aisle("~CCC~", "C###C", "C###C", "C###C", "~CCC~");
                builder.aisle("CCCCC", "PGGGP", "CGGGC", "PGGGP", "CCCCC");
                builder.aisle("~CCC~", "C###C", "C###C", "C###C", "~CCC~");
                builder.aisle("~FCF~", "F###F", "C###C", "F###F", "~FCF~");
            }
            shapeInfos.add(builder
                    .aisle("~CCC~", "C###C", "C###C", "C###C", "~CCC~")
                    .aisle("CISOC", "PGGGP", "CGGGC", "PGGGP", "CCECC")
                    .aisle("~CMC~", "C###C", "C###C", "C###C", "~CCC~")
                    .aisle("~C~C~", "CCCCC", "~C~C~", "CCCCC", "~C~C~")
                    .aisle("~C~C~", "CCCCC", "~C~C~", "CCCCC", "~C~C~")
                    .where('S', getController(), WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.EGLIN_STEEL))
                    .where('G', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING))
                    .where('F', MetaBlocks.FRAMES.get(EglinSteel).getDefaultState())
                    .where('P', GAMetaBlocks.PISTON_CASING.getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EAST)
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_sifter.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}

