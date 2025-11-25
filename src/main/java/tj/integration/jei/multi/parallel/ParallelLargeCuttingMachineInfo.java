package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
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

public class ParallelLargeCuttingMachineInfo extends TJMultiblockInfoPage {

    @Override
    public ParallelRecipeMapMultiblockController getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_CUTTING_MACHINE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return IntStream.range(1, this.getController().getMaxParallel() + 1)
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, UP, LEFT);
                    if (shapeInfo % 2 == 0) {
                        builder.aisle("CCCCCCC", "C#CEC#C", "C#C~C#C");
                        builder.aisle("CcCCCcC", "CmCCCmC", "C#C~C#C");
                    } else {
                        builder.aisle("~~CCCCC", "~~CEC#C", "~~~~C#C");
                        builder.aisle("~~CCCcC", "~~CCCmC", "~~~~C#C");
                    }
                    for (int layer = 1; layer < shapeInfo; layer++) {
                        if (layer % 2 == 0) {
                            builder.aisle("CCCCCCC", "C#CCC#C", "C#C~C#C");
                            builder.aisle("CcCCCcC", "CmCCCmC", "C#C~C#C");
                        }
                    }
                    String[] controller = shapeInfo > 1 ?
                            new String[]{"CCiMCCC", "C#ISO#C", "C#C~C#C"} :
                            new String[]{"~~CiMCC", "~~ISO#C", "~~~~C#C"};
                    return builder.aisle(controller)
                            .where('S', this.getController(), WEST)
                            .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.MARAGING_STEEL_250))
                            .where('c', GAMetaBlocks.CONVEYOR_CASING.getDefaultState())
                            .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                            .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                            .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EAST)
                            .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], WEST)
                            .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], WEST)
                            .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.MARAGING_STEEL_250), new TextComponentTranslation("gregtech.multiblock.preview.limit", 10)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.MOTOR_CASING.getItemVariant(MotorCasing.CasingType.MOTOR_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return ArrayUtils.addAll(new String[] {
                I18n.format("tj.multiblock.parallel_large_cutting_machine.description"),
                I18n.format("tj.multiblock.parallel.description"),
                I18n.format("tj.multiblock.parallel.extend.tooltip")},
                super.getDescription());
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
