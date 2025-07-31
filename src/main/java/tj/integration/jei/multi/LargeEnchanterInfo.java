package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.SensorCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.BlockInfo;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import tj.machines.TJMetaTileEntities;

import java.util.Collections;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class LargeEnchanterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_ENCHANTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return Collections.singletonList(GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN)
                .aisle("~DDD~", "DCCCD", "DCCCD", "DCCCD", "~DDD~")
                .aisle("DBBBD", "B###B", "B#s#B", "B###B", "DBBBD")
                .aisle("DBSBD", "B#s#B", "BsFsB", "B#s#B", "DBBBD")
                .aisle("DBBBD", "B###B", "B#s#B", "B###B", "DBBBD")
                .aisle("~DDD~", "DIMOD", "DiECD", "DCCCD", "~DDD~")
                .where('S', this.getController(), EnumFacing.WEST)
                .where('C', new BlockInfo(Blocks.OBSIDIAN))
                .where('D', new BlockInfo(Blocks.DIAMOND_BLOCK))
                .where('B', new BlockInfo(Block.getBlockFromName("apotheosis:hellshelf")))
                .where('s', GAMetaBlocks.SENSOR_CASING.getDefaultState())
                .where('F', GAMetaBlocks.FIELD_GEN_CASING.getDefaultState())
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], EnumFacing.DOWN)
                .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], EnumFacing.DOWN)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], EnumFacing.DOWN)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.DOWN)
                .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EnumFacing.DOWN)
                .build());
    }

    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("tj.multiblock.large_enchanter.description")};
    }
}
