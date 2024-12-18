package com.johny.tj.integration.jei.multi;

import com.johny.tj.machines.TJMetaTileEntities;
import gregicadditions.GAConfig;
import gregicadditions.item.GAHeatingCoil;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.multiblock.BlockPattern;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gregicadditions.item.GAMetaBlocks.METAL_CASING_1;
import static gregicadditions.item.GAMetaBlocks.METAL_CASING_2;

public class LargeAlloySmelterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_ALLOY_SMELTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        ArrayList<MultiblockShapeInfo> shapeInfo = new ArrayList<>();
        for (BlockWireCoil.CoilType coilType : BlockWireCoil.CoilType.values()) {
            if (!Arrays.asList(GAConfig.multis.heatingCoils.gtceHeatingCoilsBlacklist).contains(coilType.getName())) {

                shapeInfo.add(GAMultiblockShapeInfo.builder(BlockPattern.RelativeDirection.RIGHT, BlockPattern.RelativeDirection.UP, BlockPattern.RelativeDirection.BACK)
                        .aisle("VVVVV", "CCCCC", "CCECC", "CCCCC")
                        .aisle("VVVVV", "CcCcC", "c#C#c", "CcCcC")
                        .aisle("VVVVV", "CcCcC", "c#C#c", "CcCcC")
                        .aisle("VVVVV", "CcCcC", "c#C#c", "CcCcC")
                        .aisle("VVVVV", "CCCCC", "CISOC", "CCMCC")
                        .where('S', TJMetaTileEntities.LARGE_ALLOY_SMELTER, EnumFacing.NORTH)
                        .where('C', METAL_CASING_1.getState(MetalCasing1.CasingType.ZIRCONIUM_CARBIDE))
                        .where('V', METAL_CASING_2.getState(MetalCasing2.CasingType.STABALLOY))
                        .where('c', MetaBlocks.WIRE_COIL.getState(coilType))
                        .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.NORTH)
                        .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.NORTH)
                        .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.NORTH)
                        .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.SOUTH)
                        .build());
            }
        }
        for (GAHeatingCoil.CoilType coilType : GAHeatingCoil.CoilType.values()) {
            if (!Arrays.asList(GAConfig.multis.heatingCoils.gregicalityheatingCoilsBlacklist).contains(coilType.getName())) {

                shapeInfo.add(GAMultiblockShapeInfo.builder(BlockPattern.RelativeDirection.RIGHT, BlockPattern.RelativeDirection.UP, BlockPattern.RelativeDirection.BACK)
                        .aisle("VVVVV", "CCCCC", "CCECC", "CCCCC")
                        .aisle("VVVVV", "CcCcC", "c#C#c", "CcCcC")
                        .aisle("VVVVV", "CcCcC", "c#C#c", "CcCcC")
                        .aisle("VVVVV", "CcCcC", "c#C#c", "CcCcC")
                        .aisle("VVVVV", "CCCCC", "CISOC", "CCMCC")
                        .where('S', TJMetaTileEntities.LARGE_ALLOY_SMELTER, EnumFacing.NORTH)
                        .where('C', METAL_CASING_1.getState(MetalCasing1.CasingType.ZIRCONIUM_CARBIDE))
                        .where('V', METAL_CASING_2.getState(MetalCasing2.CasingType.STABALLOY))
                        .where('c', GAMetaBlocks.HEATING_COIL.getState(coilType))
                        .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.NORTH)
                        .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.NORTH)
                        .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.NORTH)
                        .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.SOUTH)
                        .build());
            }
        }
        return shapeInfo;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.default.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
