package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.BlackSteel;

public class LargeEnchanterInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_ENCHANTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        GAMultiblockShapeInfo.Builder shapeInfo = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN)
                .aisle("~~~~~~~", "~~~~~~~", "~~~C~~~", "~~CCC~~", "~~~C~~~", "~~~~~~~", "~~~~~~~")
                .aisle("~~~~~~~", "~~~~~~~", "~~CCC~~", "~~CeC~~", "~~CCC~~", "~~~~~~~", "~~~~~~~")
                .aisle("~~~~~~~", "~C~~~C~", "~~CGC~~", "~~G#G~~", "~~CGC~~", "~C~~~C~", "~~~~~~~")
                .aisle("~~~~~~~", "~CFFFC~", "~FCCCF~", "~FCCCF~", "~FCCCF~", "~CFFFC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CCGCC~", "~C###C~", "~G###G~", "~C###C~", "~CCGCC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CGGGC~", "~GBBBG~", "~GB#BG~", "~GBBBG~", "~CGGGC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CGGGC~", "~GBBBG~", "~GB#BG~", "~GBBBG~", "~CGGGC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CGGGC~", "~GBBBG~", "~GB#BG~", "~GBBBG~", "~CGGGC~", "~~~~~~~")
                .aisle("~~~~~~~", "~iISOC~", "~CoooC~", "~CoooC~", "~CoooC~", "~CCECC~", "~~~~~~~")
                .aisle("~CCMCC~", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "~CCCCC~")
                .where('S', this.getController(), EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.BLACK_STEEL))
                .where('o', Blocks.OBSIDIAN.getDefaultState())
                .where('B', Block.getBlockFromName("apotheosis:hellshelf").getDefaultState())
                .where('e', GAMetaBlocks.EMITTER_CASING.getDefaultState())
                .where('F', MetaBlocks.FRAMES.get(BlackSteel).getDefaultState())
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], EnumFacing.WEST)
                .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], EnumFacing.WEST)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], EnumFacing.WEST)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EnumFacing.EAST);
        return Arrays.stream(GATransparentCasing.CasingType.values())
                .map(glass -> shapeInfo.where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(glass)).build())
                .collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_2.getItemVariant(MetalCasing2.CasingType.BLACK_STEEL), new TextComponentTranslation("gregtech.multiblock.preview.limit", 64)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.EMITTER_CASING.getItemVariant(EmitterCasing.CasingType.EMITTER_LV), COMPONENT_BLOCK_TOOLTIP);
    }


    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("tj.multiblock.large_enchanter.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
