package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
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

public class ParallelLargeAssemblerInfo extends TJMultiblockInfoPage {

    @Override
    public ParallelRecipeMapMultiblockController getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_ASSEMBLER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return IntStream.range(1, this.getController().getMaxParallel() + 1)
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);

                    StringBuilder aisleC = new StringBuilder(), aisleG = new StringBuilder(), aisleP = new StringBuilder(),
                            aisleA = new StringBuilder(), aislec = new StringBuilder(), aisleR = new StringBuilder();
                    for (int layer = 1; layer < shapeInfo; layer++) {
                        aisleC.append("CCC");
                        aisleG.append("GGG");
                        aisleP.append("PPP");
                        aisleA.append("###");
                        aislec.append("ccc");
                        aisleR.append("RRR");
                    }
                    aisleC.append("C");
                    aisleG.append("C");
                    aisleP.append("C");
                    aisleA.append("C");
                    aislec.append("C");
                    aisleR.append("C");

                    return builder.aisle("I~CGGG" + aisleG, "CCCGGG" + aisleG, "CCCGGG" + aisleG, "CCCCCC" + aisleC)
                            .aisle("iMCGGG" + aisleG, "CPC###" + aisleA, "CPPPPP" + aisleP, "CCCCCC" + aisleC)
                            .aisle("OSCRRR" + aisleR, "CACccc" + aislec, "CACPPP" + aisleP, "CECCCC" + aisleC)
                            .aisle("CCCCCC" + aisleC, "CCCCCC" + aisleC, "CCCCCC" + aisleC, "CCCCCC" + aisleC)
                            .where('S', this.getController(), WEST)
                            .where('C', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.LARGE_ASSEMBLER))
                            .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                            .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS))
                            .where('A', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.ASSEMBLY_LINE_CASING))
                            .where('c', GAMetaBlocks.CONVEYOR_CASING.getDefaultState())
                            .where('R', GAMetaBlocks.ROBOT_ARM_CASING.getDefaultState())
                            .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                            .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EAST)
                            .where('I', MetaTileEntities.ITEM_IMPORT_BUS[0], WEST)
                            .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                            .where('O', MetaTileEntities.ITEM_EXPORT_BUS[0], WEST)
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.MUTLIBLOCK_CASING.getItemVariant(GAMultiblockCasing.CasingType.LARGE_ASSEMBLER), new TextComponentTranslation("gregtech.multiblock.preview.limit", 11)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.CONVEYOR_CASING.getItemVariant(ConveyorCasing.CasingType.CONVEYOR_LV), COMPONENT_BLOCK_TOOLTIP);
        this.addBlockTooltip(GAMetaBlocks.ROBOT_ARM_CASING.getItemVariant(RobotArmCasing.CasingType.ROBOT_ARM_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return ArrayUtils.addAll(new String[] {
                I18n.format("tj.multiblock.parallel_large_assembler.description"),
                I18n.format("tj.multiblock.parallel.description"),
                I18n.format("tj.multiblock.parallel.extend.tooltip")},
                super.getDescription());
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
