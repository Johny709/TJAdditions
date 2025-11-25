package tj.integration.jei.multi;

import gregicadditions.item.CellCasing;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.builder.multicontrollers.ExtendableMultiblockController;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class EnderBatteryTowerInfo extends TJMultiblockInfoPage {

    @Override
    public ExtendableMultiblockController getController() {
        return TJMetaTileEntities.ENDER_BATTERY_TOWER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return IntStream.range(1, this.getController().getMaxParallel())
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN);
                    builder.aisle("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC");
                    for (int layer = 0; layer < shapeInfo; layer++) {
                        builder.aisle("GGGGG", "GcccG", "GcccG", "GcccG", "GGGGG");
                    }
                    return builder.aisle("CCSCC", "CCCCC", "CCCCC", "CCCCC", "CEMeC")
                            .where('S', this.getController(), EnumFacing.WEST)
                            .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.BOROSILICATE_GLASS))
                            .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.HASTELLOY_X78))
                            .where('c', GAMetaBlocks.CELL_CASING.getState(CellCasing.CellType.CELL_EV))
                            .where('e', MetaTileEntities.ENERGY_OUTPUT_HATCH[4], EnumFacing.EAST)
                            .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[4], EnumFacing.EAST)
                            .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.EAST)
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        Arrays.stream(GATransparentCasing.CasingType.values()).forEach(casingType -> this.addBlockTooltip(GAMetaBlocks.TRANSPARENT_CASING.getItemVariant(casingType), COMPONENT_TIER_ANY_TOOLTIP));
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.HASTELLOY_X78), new TextComponentTranslation("gregtech.multiblock.preview.limit", 10)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }
}
