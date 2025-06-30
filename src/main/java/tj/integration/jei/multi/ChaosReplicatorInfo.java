package tj.integration.jei.multi;

import com.google.common.collect.Lists;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.BlockInfo;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import tj.blocks.BlockSolidCasings;
import tj.blocks.TJMetaBlocks;
import tj.machines.TJMetaTileEntities;

import java.util.List;

public class ChaosReplicatorInfo extends MultiblockInfoPage {
    public ChaosReplicatorInfo() {
    }
    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.CHAOS_REPLICATOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("CCCCCCC", "CCCJCCC", "CQQCQQC", "CQQCQQC", "CQQCQQC", "CQQQQQC", "CQQQQQC", "CCCCCCC", "CCCCCCC")
                .aisle("CCCCCCC", "ODDDDDC", "QF~~~FQ", "QF~~~FQ", "QF~A~FQ", "QF~~~FQ", "QF~~~FQ", "CDDDDDC", "CCCCCCC")
                .aisle("CCCCCCC", "CDDDDDC", "Q~DDD~Q", "Q~~~~~Q", "Q~~~~~Q", "Q~~~~~Q", "Q~DDD~Q", "CDDDDDC", "CCCCCCC")
                .aisle("CCCCCCC", "SDDDDDC", "C~DDD~C", "C~~D~~C", "CA~R~AC", "Q~~D~~Q", "Q~DDD~Q", "CDDDDDC", "CCCCCCC")
                .aisle("CCCCCCC", "MDDDDDC", "Q~DDD~Q", "Q~~~~~Q", "Q~~~~~Q", "Q~~~~~Q", "Q~DDD~Q", "CDDDDDC", "CCCCCCC")
                .aisle("CCCCCCC", "IDDDDDC", "QF~~~FQ", "QF~~~FQ", "QF~A~FQ", "QF~~~FQ", "QF~~~FQ", "CDDDDDC", "CCCCCCC")
                .aisle("CCCCCCC", "CCCKCCC", "CQQCQQC", "CQQCQQC", "CQQCQQC", "CQQQQQC", "CQQQQQC", "CCCCCCC", "CCCCCCC")
                .where('S', TJMetaTileEntities.CHAOS_REPLICATOR, EnumFacing.WEST)
                .where('C', TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.CHOATIC_CASING))
                .where('F', new BlockInfo(Block.getBlockFromName("gregtech:frame_enriched_naquadah_alloy")))
                .where('D', new BlockInfo(Block.getBlockFromName("draconicevolution:infused_obsidian")))
                .where('Q', new BlockInfo(Block.getBlockFromName("enderio:block_fused_quartz")))
                .where('A', new BlockInfo(Block.getBlockFromName("draconicevolution:draconic_block")))
                .where('R', new BlockInfo(Block.getBlockFromName("gregtech:frame_chaos")))
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.MV], EnumFacing.WEST)
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.MV], EnumFacing.WEST)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('J', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.NORTH)
                .where('K', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.UV], EnumFacing.SOUTH)
                .where('~', Blocks.AIR.getDefaultState())
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.chaos_replicator.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
