package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.machines.TJMetaTileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockFireboxCasing;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.List;

public class MegaBoilerInfo extends MultiblockInfoPage {
    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.MEGA_TUNGSTENSTEEL_BOILER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("XXXXXXXXXXXXXXX", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("HXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "OPPPPPPPPPPPPPC", "SPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("IXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC")
                .where('S', TJMetaTileEntities.MEGA_TUNGSTENSTEEL_BOILER, EnumFacing.WEST)
                .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.TUNGSTENSTEEL_ROBUST))
                .where('X', MetaBlocks.BOILER_FIREBOX_CASING.getState(BlockFireboxCasing.FireboxCasingType.TUNGSTENSTEEL_FIREBOX))
                .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.MAX], EnumFacing.WEST)
                .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.MAX], EnumFacing.WEST)
                .where('H', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.MAX], EnumFacing.EAST)
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.default.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.3f;
    }
}
