package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;

public class AdvancedParallelChemicalReactorInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.ADVANCED_PARALLEL_CHEMICAL_REACTOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int index = 1; index <= 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(LEFT, FRONT, DOWN);
            if (!(index % 2 == 0)) {
                builder.aisle("CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC", "~~~~~", "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~");
                builder.aisle("CCCCC", "CcPcC", "CcPcC", "CcPcC", "CCPCC", "~~P~~", "F~P~F", "~~P~~", "~PpP~", "~~P~~", "F~~~F", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~");
                builder.aisle("CCCCC", "~PmP~", "~PmP~", "~PmP~", "CPCPC", "~P~P~", "FP~PF", "~PPP~", "~PpP~", "~~P~~", "F~~~F", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~");
                builder.aisle("CCCCC", "CcPcC", "CcPcC", "CcPcC", "CCPCC", "~~P~~", "F~P~F", "~~P~~", "~PpP~", "~~P~~", "F~~~F", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~");
            } else {
                builder.aisle("CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC", "~~~~~", "CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC", "~~~~~", "CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC");
                builder.aisle("CCCCC", "CcPcC", "CcPcC", "CcPcC", "CCPCC", "~~P~~", "F~P~F", "~~P~~", "~PpP~", "~~P~~", "F~P~F", "~~P~~", "CCPCC", "CcPcC", "CcPcC", "CcPcC", "CCCCC");
                builder.aisle("CCCCC", "~PmP~", "~PmP~", "~PmP~", "CPCPC", "~P~P~", "FP~PF", "~PPP~", "~PpP~", "~PPP~", "FP~PF", "~P~P~", "CPCPC", "~PmP~", "~PmP~", "~PmP~", "CCCCC");
                builder.aisle("CCCCC", "CcPcC", "CcPcC", "CcPcC", "CCPCC", "~~P~~", "F~P~F", "~~P~~", "~PpP~", "~~P~~", "F~P~F", "~~P~~", "CCPCC", "CcPcC", "CcPcC", "CcPcC", "CCCCC");
            }
            for (int num = 1; num < index; num++) {
                if (num % 2 == 0) {
                    builder.aisle("CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC", "~~~~~", "F~~~F", "~~P~~", "~PpP~", "~~P~~", "F~~~F", "~~~~~", "CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC");
                    builder.aisle("CCCCC", "CcPcC", "CcPcC", "CcPcC", "CCPCC", "~~P~~", "F~P~F", "~~P~~", "~PpP~", "~~P~~", "F~P~F", "~~P~~", "CCPCC", "CcPcC", "CcPcC", "CcPcC", "CCCCC");
                    builder.aisle("CCCCC", "~PmP~", "~PmP~", "~PmP~", "CPCPC", "~P~P~", "FP~PF", "~PPP~", "~PpP~", "~PPP~", "FP~PF", "~P~P~", "CPCPC", "~PmP~", "~PmP~", "~PmP~", "CCCCC");
                    builder.aisle("CCCCC", "CcPcC", "CcPcC", "CcPcC", "CCPCC", "~~P~~", "F~P~F", "~~P~~", "~PpP~", "~~P~~", "F~P~F", "~~P~~", "CCPCC", "CcPcC", "CcPcC", "CcPcC", "CCCCC");
                }
            }
            if (index > 1)
                builder.aisle("CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC", "~~~~~", "CCCCi", "CCCCI", "ECCCS", "MCCCO", "CCCCo", "~~~~~", "CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC");
            else
                builder.aisle("CCCCC", "~C~C~", "~C~C~", "~C~C~", "CCCCC", "~~~~~", "CCCCi", "CCCCI", "ECCCS", "MCCCO", "CCCCo", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~");
            builder.where('S', getController(), EnumFacing.WEST)
                    .where('C', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT))
                    .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('P', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE))
                    .where('F', MetaBlocks.FRAMES.get(Steel).getDefaultState())
                    .where('p', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.EAST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('i', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('o', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST);
            shapes.add(builder.build());
        }
        return shapes;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.industrial_fusion_reactor.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
