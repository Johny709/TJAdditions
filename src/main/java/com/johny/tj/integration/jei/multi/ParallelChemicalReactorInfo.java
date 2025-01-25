package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import com.johny.tj.machines.multi.electric.MetaTileEntityParallelLargeChemicalReactor;
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

public class ParallelChemicalReactorInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_CHEMICAL_REACTOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int index = 1; index < 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(LEFT, FRONT, DOWN);
            MetaTileEntityParallelLargeChemicalReactor chemicalReactor = TJMetaTileEntities.PARALLEL_CHEMICAL_REACTOR;
            builder.aisle("CCCCC", "CCCCi", "CCCCM", "CCCCo", "CCCCC");
            for (int num = 0; num < index; num++) {
                builder.aisle("F###F", "#PPP#", "#PBP#", "#PPP#", "F###F");
                builder.aisle("F###F", "#CCC#", "#CcC#", "#CCC#", "F###F");
            }
            builder.aisle("F###F", "#PPP#", "#PBP#", "#PPP#", "F###F");
            builder.aisle("CCCCC", "CCCCI", "ECCCS", "CCCCO", "CCCCC");
            builder.where('S', chemicalReactor, EnumFacing.WEST)
                    .where('C', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT))
                    .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('P', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE))
                    .where('F', MetaBlocks.FRAMES.get(Steel).getDefaultState())
                    .where('B', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.WEST)
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
