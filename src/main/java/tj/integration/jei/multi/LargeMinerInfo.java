package tj.integration.jei.multi;

import com.google.common.collect.Lists;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import tj.machines.TJMiner;
import tj.machines.multi.electric.MetaTileEntityEliteLargeMiner;

import java.util.List;

public class LargeMinerInfo extends MultiblockInfoPage {

    private final MetaTileEntityEliteLargeMiner largeMiner;

    public LargeMinerInfo(MetaTileEntityEliteLargeMiner largeMiner) {
        this.largeMiner = largeMiner;
    }

    @Override
    public MultiblockControllerBase getController() {
        return largeMiner;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        if (largeMiner.getType() == TJMiner.Type.DESTROYER) {
            MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                    .aisle("F###F", "F###F", "PPPPP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                    .aisle("#####", "#####", "PPPPP", "#MPO#", "##F##", "##F##", "##F##", "#####", "#####", "#####")
                    .aisle("#####", "#####", "PPmPP", "#SPE#", "##F##", "##F##", "##F##", "##F##", "##F##", "##F##")
                    .aisle("#####", "#####", "PPPPP", "#IPP#", "##F##", "##F##", "##F##", "#####", "#####", "#####")
                    .aisle("F###F", "F###F", "PPPPP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .where('P', largeMiner.getCasingState())
                    .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[4], EnumFacing.EAST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], EnumFacing.EAST)
                    .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[0], EnumFacing.WEST)
                    .where('F', largeMiner.getFrameState())
                    .where('#', Blocks.AIR.getDefaultState())
                    .build();
            return Lists.newArrayList(shapeInfo);
        }
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("F###F", "F###F", "PPPPP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                .aisle("#####", "#####", "PPPPP", "#MPO#", "##F##", "##F##", "##F##", "#####", "#####", "#####")
                .aisle("#####", "#####", "PPPPP", "#SPE#", "##F##", "##F##", "##F##", "##F##", "##F##", "##F##")
                .aisle("#####", "#####", "PPPPP", "#IPP#", "##F##", "##F##", "##F##", "#####", "#####", "#####")
                .aisle("F###F", "F###F", "PPPPP", "#####", "#####", "#####", "#####", "#####", "#####", "#####")
                .where('S', getController(), EnumFacing.WEST)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('P', largeMiner.getCasingState())
                .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[4], EnumFacing.EAST)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], EnumFacing.EAST)
                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[0], EnumFacing.WEST)
                .where('F', largeMiner.getFrameState())
                .where('#', Blocks.AIR.getDefaultState())
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        if (largeMiner.getType() == TJMiner.Type.DESTROYER) {
            return new String[]{I18n.format("tj.multiblock.elite_large_miner.description", largeMiner.type.chunk, largeMiner.type.chunk, largeMiner.type.fortuneString),
            I18n.format("tj.multiblock.elite_large_miner.filter.warning")};
        }
        return new String[]{I18n.format("gtadditions.machine.miner.multi.description", largeMiner.type.chunk, largeMiner.type.chunk, largeMiner.type.fortuneString)};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
