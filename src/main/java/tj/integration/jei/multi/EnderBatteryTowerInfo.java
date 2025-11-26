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
import tj.integration.jei.multi.parallel.IParallelMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class EnderBatteryTowerInfo extends TJMultiblockInfoPage implements IParallelMultiblockInfoPage {

    @Override
    public ExtendableMultiblockController getController() {
        return TJMetaTileEntities.ENDER_BATTERY_TOWER;
    }

    @Override
    public List<MultiblockShapeInfo[]> getMatchingShapes(MultiblockShapeInfo[] shapes) {
        return IntStream.range(1, this.getController().getMaxParallel())
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN);
                    builder.aisle("CCCCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC");
                    for (int layer = 0; layer < shapeInfo; layer++) {
                        builder.aisle("GGGGG", "GcccG", "GcccG", "GcccG", "GGGGG");
                    }
                    return builder.aisle("CCSCC", "CCCCC", "CCCCC", "CCCCC", "CEMeC");
                }).map(builder -> {
                    MultiblockShapeInfo[] infos = new MultiblockShapeInfo[15];
                    for (int tier = 0; tier < infos.length; tier++) {
                        infos[tier] = builder.where('S', this.getController(), EnumFacing.WEST)
                                .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.BOROSILICATE_GLASS))
                                .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.HASTELLOY_X78))
                                .where('c', GAMetaBlocks.CELL_CASING.getState(CellCasing.CellType.values()[Math.max(0, tier - 3)]))
                                .where('e', this.getEnergyHatch(tier, true), EnumFacing.EAST)
                                .where('E', this.getEnergyHatch(tier, false), EnumFacing.EAST)
                                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.EAST)
                                .build();
                    }
                    return infos;
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
