package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
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
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

import static gregicadditions.GAMaterials.EglinSteel;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class ParallelLargeSifterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_SIFTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int index = 1; index < 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(LEFT, FRONT, DOWN);
            for (int count = 1; count < index; count++) {
                builder.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
                builder.aisle("HPHPH", "HGGGH", "HGGGH", "HGGGH", "HPHPH");
                builder.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
                builder.aisle("~FHF~", "F###F", "H###H", "F###F", "~FHF~");
            }
            builder.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
            builder.aisle("HPHPH", "HGGGO", "EGGGS", "HGGGI", "HPHPH");
            builder.aisle("~HHH~", "H###H", "H###m", "H###H", "~HHH~");
            builder.aisle("~H~H~", "HHHHH", "~H~H~", "HHHHH", "~H~H~");
            builder.aisle("~H~H~", "HHHHH", "~H~H~", "HHHHH", "~H~H~")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('H', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.EGLIN_STEEL))
                    .where('G', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING))
                    .where('F', MetaBlocks.FRAMES.get(EglinSteel).getDefaultState())
                    .where('P', GAMetaBlocks.PISTON_CASING.getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('m', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST);
            shapes.add(builder.build());
        }
        return shapes;
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

