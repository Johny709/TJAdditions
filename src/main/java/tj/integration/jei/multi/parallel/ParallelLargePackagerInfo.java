package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.ArrayUtils;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.WEST;


public class ParallelLargePackagerInfo extends TJMultiblockInfoPage implements IParallelMultiblockInfoPage {

    @Override
    public ParallelRecipeMapMultiblockController getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_PACKAGER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes(int tier) {
        return IntStream.range(1, this.getController().getMaxParallel() + 1)
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, UP, LEFT);
                    builder.aisle("CEC", "CCC", "CCC");
                    for (int layer = 0; layer < shapeInfo; layer++) {
                        builder.aisle("CCC", "CcC", "CRC");
                    }
                    return builder.aisle("IMO", "CSC", "CCC")
                            .where('S', this.getController(), WEST)
                            .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.HG_1223))
                            .where('c', GAMetaBlocks.CONVEYOR_CASING.getState(ConveyorCasing.CasingType.values()[Math.max(0, tier - 1)]))
                            .where('R', GAMetaBlocks.ROBOT_ARM_CASING.getState(RobotArmCasing.CasingType.values()[Math.max(0, tier - 1)]))
                            .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                            .where('E', this.getEnergyHatch(tier, false), EAST)
                            .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], WEST)
                            .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], WEST)
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return this.getMatchingShapes(0);
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.HG_1223), new TextComponentTranslation("gregtech.multiblock.preview.limit", 4)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.CONVEYOR_CASING.getItemVariant(ConveyorCasing.CasingType.CONVEYOR_LV), COMPONENT_BLOCK_TOOLTIP);
        this.addBlockTooltip(GAMetaBlocks.ROBOT_ARM_CASING.getItemVariant(RobotArmCasing.CasingType.ROBOT_ARM_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return ArrayUtils.addAll(new String[] {
                I18n.format("tj.multiblock.parallel_large_packager.description"),
                I18n.format("tj.multiblock.parallel.description"),
                I18n.format("tj.multiblock.parallel.extend.tooltip")},
                super.getDescription());
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
