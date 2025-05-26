package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.FieldGenCasing;
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

import java.util.List;

import static com.johny.tj.machines.TJMetaTileEntities.TELEPORTER;

public class TeleporterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TELEPORTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("~CFC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CCCCC", "~CCC~", "~C#C~", "~C#C~", "~C#C~", "~CCC~", "~~C~~")
                .aisle("FCfCF", "~ISE~", "~###~", "~###~", "~###~", "~MFC~", "~CfC~")
                .aisle("CCCCC", "~CCC~", "~C#C~", "~C#C~", "~C#C~", "~CCC~", "~~C~~")
                .aisle("~CFC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.getController(), EnumFacing.WEST)
                .where('C', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2))
                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.ZPM], EnumFacing.WEST)
                .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.ZPM], EnumFacing.EAST)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('F', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_ZPM))
                .where('f', GAMetaBlocks.FIELD_GEN_CASING.getState(FieldGenCasing.CasingType.FIELD_GENERATOR_ZPM))
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.teleporter.description")};
    }
}
