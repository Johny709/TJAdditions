package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.components.PistonCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockTurbineCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelLargeLaserEngraverInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_LASER_ENGRAVER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, UP, LEFT);
            for (int layer = 0; layer < shapeInfo; layer++) {
                String energ = layer == 0 ? "CEC" : "CCC";
                builder.aisle(energ, "CGC", "CCC", "~C~");
                builder.aisle("CCC", "GcG", "CeC", "CBC");
            }
            shapeInfos.add(builder.aisle("iMo", "ISO", "CCC", "~C~")
                    .where('S', this.getController(), WEST)
                    .where('C', GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.LASER_ENGRAVER))
                    .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.IRIDIUM_GLASS))
                    .where('B', MetaBlocks.TURBINE_CASING.getState(BlockTurbineCasing.TurbineCasingType.TITANIUM_GEARBOX))
                    .where('c', GAMetaBlocks.CONVEYOR_CASING.getDefaultState())
                    .where('e', GAMetaBlocks.EMITTER_CASING.getDefaultState())
                    .where('P', GAMetaBlocks.PISTON_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EAST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], WEST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[0], WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.MUTLIBLOCK_CASING2.getItemVariant(GAMultiblockCasing2.CasingType.LASER_ENGRAVER), new TextComponentTranslation("gregtech.multiblock.preview.limit", 8)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.CONVEYOR_CASING.getItemVariant(ConveyorCasing.CasingType.CONVEYOR_LV), COMPONENT_BLOCK_TOOLTIP);
        this.addBlockTooltip(GAMetaBlocks.EMITTER_CASING.getItemVariant(EmitterCasing.CasingType.EMITTER_LV), COMPONENT_BLOCK_TOOLTIP);
        this.addBlockTooltip(GAMetaBlocks.PISTON_CASING.getItemVariant(PistonCasing.CasingType.PISTON_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_laser_engraver.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
