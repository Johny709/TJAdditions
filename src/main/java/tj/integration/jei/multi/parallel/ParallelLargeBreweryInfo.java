package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregicadditions.GAMaterials.Grisium;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class ParallelLargeBreweryInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_BREWERY;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int index = 1; index < 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT);
            builder.aisle("CCCCC", "F~C~F", "CCECC", "CCMCC", "CCCCC", "~CCC~");
            for (int count = 0; count < index; count++) {
                builder.aisle("~CCC~", "~~C~~", "~G~G~", "C~P~C", "~G~G~", "~~C~~");
                builder.aisle("~CCC~", "~~C~~", "~G~G~", "p~P~p", "~G~G~", "~~C~~");
                builder.aisle("~CCC~", "~~C~~", "~G~G~", "C~P~C", "~G~G~", "~~C~~");
            }
            shapeInfos.add(builder.aisle("CCCCC", "F~C~F", "CiSoC", "CIMOC", "CCmCC", "~CCC~")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.GRISIUM))
                    .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS))
                    .where('P', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE))
                    .where('M', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('p', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('F', MetaBlocks.FRAMES.get(Grisium).getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('m', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .where('~', Blocks.AIR.getDefaultState())
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.GRISIUM), new TextComponentTranslation("gregtech.multiblock.preview.limit", 0)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.MOTOR_CASING.getItemVariant(MotorCasing.CasingType.MOTOR_LV), COMPONENT_BLOCK_TOOLTIP);
        this.addBlockTooltip(GAMetaBlocks.PUMP_CASING.getItemVariant(PumpCasing.CasingType.PUMP_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_brewery.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
