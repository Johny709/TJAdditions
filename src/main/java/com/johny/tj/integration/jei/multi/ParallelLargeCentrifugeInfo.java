package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMultiblockCasing;
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

public class ParallelLargeCentrifugeInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_CENTRIFUGE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int index = 1; index < 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(LEFT, FRONT, DOWN);
            builder.aisle("~HHH~", "HCCCH", "HCMCH", "HCCCH", "~HHH~");
            for (int count = 1; count < index; count++) {
                builder.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
                builder.aisle("~HGH~", "H###H", "G#P#G", "H###H", "~HGH~");
                builder.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
                builder.aisle("~HHH~", "HCCCH", "HCMCH", "HCCCH", "~HHH~");
            }
            builder.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
            builder.aisle("~HGH~", "o###O", "G#P#S", "i###I", "~HGH~");
            builder.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
            builder.aisle("~HHH~", "HCCCH", "ECMCm", "HCCCH", "~HHH~")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('H', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.RED_STEEL))
                    .where('G', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING))
                    .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE))
                    .where('C', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('M', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('m', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST);
            shapes.add(builder.build());
        }
        return shapes;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_centrifuge.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
