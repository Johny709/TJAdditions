package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.SensorCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import tj.machines.TJMetaTileEntities;

import java.util.Collections;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.BlackSteel;

public class LargeEnchanterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_ENCHANTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return Collections.singletonList(GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN)
                .aisle("~~~~~~~", "~~~~~~~", "~~~C~~~", "~~CCC~~", "~~~C~~~", "~~~~~~~", "~~~~~~~")
                .aisle("~~~~~~~", "~~~~~~~", "~~CCC~~", "~~CeC~~", "~~CCC~~", "~~~~~~~", "~~~~~~~")
                .aisle("~~~~~~~", "~C~~~C~", "~~CGC~~", "~~G#G~~", "~~CGC~~", "~C~~~C~", "~~~~~~~")
                .aisle("~~~~~~~", "~CFFFC~", "~FCCCF~", "~FCCCF~", "~FCCCF~", "~CFFFC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CCGCC~", "~C###C~", "~G###G~", "~C###C~", "~CCGCC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CGGGC~", "~GBBBG~", "~GB#BG~", "~GBBBG~", "~CGGGC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CGGGC~", "~GBBBG~", "~GB#BG~", "~GBBBG~", "~CGGGC~", "~~~~~~~")
                .aisle("~~~~~~~", "~CGGGC~", "~GBBBG~", "~GB#BG~", "~GBBBG~", "~CGGGC~", "~~~~~~~")
                .aisle("~~~~~~~", "~iISOC~", "~COOOC~", "~COOOC~", "~COOOC~", "~CCECC~", "~~~~~~~")
                .aisle("~CCCCC~", "CCCMCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "~CCCCC~")
                .where('S', this.getController(), EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.BLACK_STEEL))
                .where('O', new BlockInfo(Blocks.OBSIDIAN))
                .where('B', new BlockInfo(Block.getBlockFromName("apotheosis:hellshelf")))
                .where('e', GAMetaBlocks.EMITTER_CASING.getDefaultState())
                .where('F', MetaBlocks.FRAMES.get(BlackSteel).getDefaultState())
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], EnumFacing.WEST)
                .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], EnumFacing.WEST)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], EnumFacing.WEST)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EnumFacing.EAST)
                .build());
    }

    @Override
    public String[] getDescription() {
        return new String[]{I18n.format("tj.multiblock.large_enchanter.description")};
    }
}
