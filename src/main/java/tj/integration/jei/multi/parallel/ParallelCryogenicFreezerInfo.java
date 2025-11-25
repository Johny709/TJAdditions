package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
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


public class ParallelCryogenicFreezerInfo extends TJMultiblockInfoPage {

    @Override
    public ParallelRecipeMapMultiblockController getController() {
        return TJMetaTileEntities.PARALLEL_CRYOGENIC_FREEZER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return IntStream.range(1, this.getController().getMaxParallel() + 1)
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);
                    for (int layer = 0; layer < shapeInfo; layer++) {
                        String entityP = layer == 0 ? "CCCCC" : "CCPCC";
                        String entityS = layer == shapeInfo - 1 ? "~ISO~" : "~CCC~";
                        String energyH = layer == shapeInfo - 1 ? "~CEM~" : "~CCC~";
                        builder.aisle("~CCC~", "CCCCC", entityP, "CCCCC", "~CCC~");
                        builder.aisle(entityS, "C#P#C", "CPPPC", "C#P#C", energyH);
                    }
                    return builder.aisle("~iCo~", "CCCCC", "CCCCC", "CCCCC", "~CCC~")
                            .where('S', this.getController(), WEST)
                            .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.INCOLOY_MA956))
                            .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                            .where('M', GATileEntities.MAINTENANCE_HATCH[0], EAST)
                            .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EAST)
                            .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], WEST)
                            .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                            .where('O', MetaTileEntities.ITEM_EXPORT_BUS[1], WEST)
                            .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[0], WEST)
                            .build();
                }).collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.INCOLOY_MA956), new TextComponentTranslation("gregtech.multiblock.preview.limit", 16)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public String[] getDescription() {
        return ArrayUtils.addAll(new String[] {
                I18n.format("tj.multiblock.parallel_cryogenic_freezer.description"),
                I18n.format("tj.multiblock.parallel.description"),
                I18n.format("tj.multiblock.parallel.extend.tooltip"),
                I18n.format("gregtech.multiblock.vol_cryo.description")},
                super.getDescription());
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
