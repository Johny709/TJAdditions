package com.johny.tj.integration.jei.multi;

import com.google.common.collect.Lists;
import com.johny.tj.blocks.BlockPipeCasings;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.components.SensorCasing;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.MetaBlocks;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;

import java.util.List;

import static com.johny.tj.machines.TJMetaTileEntities.MEGA_REACTION_CHAMBER;
import static gregicadditions.machines.GATileEntities.MAINTENANCE_HATCH;
import static gregtech.common.metatileentities.MetaTileEntities.*;

public class MegaReactionChamberInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return MEGA_REACTION_CHAMBER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("###############", "###############", "###############", "###############", "###############", "#####PPCPP#####", "#####CCCCC#####", "#####PPCPP#####", "###############", "###############", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "######PCP######", "###PP#####PP###", "###CC#P#P#CC###", "###PP#####PP###", "######PCP######", "###############", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "######PCP######", "###############", "##P###GGG###P##", "##C###PCP###C##", "##P###GGG###P##", "###############", "######PCP######", "###############", "###############", "###############")
                .aisle("###############", "###############", "######PCP######", "###############", "######GGG######", "#P##GG!!!GG##P#", "#C##CC!!!CC##C#", "#P##GG!!!GG##P#", "######GGG######", "###############", "######PCP######", "###############", "###############")
                .aisle("###############", "######PCP######", "###############", "######GGG######", "####GG!!!GG####", "#P#G!!!!!!!G#P#", "#C#C!!!!!!!C#C#", "#P#G!!!!!!!G#P#", "####GG!!!GG####", "######GGG######", "###############", "######PCP######", "###############")
                .aisle("######PCP######", "###############", "######GGG######", "#####G!!!G#####", "####G!!!!!G####", "P##G!!!!!!!G##P", "I##C!!!!!!!C##C", "P##G!!!!!!!G##P", "####G!!!!!G####", "#####G!!!G#####", "######GGG######", "###############", "######PCP######")
                .aisle("#####PCCCP#####", "####P#ccc#P####", "###P#G!!!G#P###", "##P#G!!!!!G#P##", "#P#G!!!!!!!G#P#", "P#G!!!!!!!!!G#P", "iPP!!!!s!!!!PPC", "P#G!!!!!!!!!G#P", "#P#G!!!!!!!G#P#", "##P#G!!!!!G#P##", "###P#G!!!G#P###", "####P#ccc#P####", "#####PCCCP#####")
                .aisle("#####CCCCC#####", "####C#cFc#C####", "###C#G!!!G#C###", "##C#G!!!!!G#C##", "#C#G!!!!!!!G#C#", "C#G!!!!p!!!!G#C", "S#C!!!efe!!!C#E", "M#G!!!!p!!!!G#C", "#C#G!!!!!!!G#C#", "##C#G!!!!!G#C##", "###C#G!!!G#C###", "####C#cFc#C####", "#####CCCCC#####")
                .aisle("#####PCCCP#####", "####P#ccc#P####", "###P#G!!!G#P###", "##P#G!!!!!G#P##", "#P#G!!!!!!!G#P#", "P#G!!!!!!!!!G#P", "OPP!!!!s!!!!PPC", "P#G!!!!!!!!!G#P", "#P#G!!!!!!!G#P#", "##P#G!!!!!G#P##", "###P#G!!!G#P###", "####P#ccc#P####", "#####PCCCP#####")
                .aisle("######PCP######", "###############", "######GGG######", "#####G!!!G#####", "####G!!!!!G####", "P##G!!!!!!!G##P", "o##C!!!!!!!C##C", "P##G!!!!!!!G##P", "####G!!!!!G####", "#####G!!!G#####", "######GGG######", "###############", "######PCP######")
                .aisle("###############", "######PCP######", "###############", "######GGG######", "####GG!!!GG####", "#P#G!!!!!!!G#P#", "#C#C!!!!!!!C#C#", "#P#G!!!!!!!G#P#", "####GG!!!GG####", "######GGG######", "###############", "######PCP######", "###############")
                .aisle("###############", "###############", "######PCP######", "###############", "######GGG######", "#P##GG!!!GG##P#", "#C##CC!!!CC##C#", "#P##GG!!!GG##P#", "######GGG######", "###############", "######PCP######", "###############", "###############")
                .aisle("###############", "###############", "###############", "######PCP######", "###############", "##P###GGG###P##", "##C###PCP###C##", "##P###GGG###P##", "###############", "######PCP######", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "######PCP######", "###PP#####PP###", "###CC#P#P#CC###", "###PP#####PP###", "######PCP######", "###############", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "###############", "#####PPCPP#####", "#####CCCCC#####", "#####PPCPP#####", "###############", "###############", "###############", "###############", "###############")
                .where('S', MEGA_REACTION_CHAMBER, EnumFacing.WEST)
                .where('C', TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.CHEMICALLY_INERT_FPM_CASING))
                .where('P', TJMetaBlocks.PIPE_CASING.getState(BlockPipeCasings.PipeCasingType.FPM_PIPE_CASING))
                .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS))
                .where('p', GAMetaBlocks.PUMP_CASING.getState(PumpCasing.CasingType.PUMP_UMV))
                .where('s', GAMetaBlocks.SENSOR_CASING.getState(SensorCasing.CasingType.SENSOR_UMV))
                .where('e', GAMetaBlocks.EMITTER_CASING.getState(EmitterCasing.CasingType.EMITTER_UMV))
                .where('f', GAMetaBlocks.FIELD_GEN_CASING.getState(FieldGenCasing.CasingType.FIELD_GENERATOR_UMV))
                .where('c', MetaBlocks.WIRE_COIL.getDefaultState())
                .where('F', GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.TIERED_HULL_UMV))
                .where('I', ITEM_IMPORT_BUS[GTValues.MAX], EnumFacing.WEST)
                .where('i', ITEM_EXPORT_BUS[GTValues.MAX], EnumFacing.WEST)
                .where('O', FLUID_IMPORT_HATCH[GTValues.MAX], EnumFacing.WEST)
                .where('o', FLUID_EXPORT_HATCH[GTValues.MAX], EnumFacing.WEST)
                .where('M', MAINTENANCE_HATCH[2], EnumFacing.WEST)
                .where('E', ENERGY_INPUT_HATCH[GTValues.MAX], EnumFacing.EAST)
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_vial_processor.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
