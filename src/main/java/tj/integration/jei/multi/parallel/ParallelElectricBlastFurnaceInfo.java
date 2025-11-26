package tj.integration.jei.multi.parallel;

import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
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


public class ParallelElectricBlastFurnaceInfo extends TJMultiblockInfoPage implements IParallelMultiblockInfoPage {

    @Override
    public ParallelRecipeMapMultiblockController getController() {
        return TJMetaTileEntities.PARALLEL_ELECTRIC_BLAST_FURNACE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes(int tier) {
        return IntStream.range(1, this.getController().getMaxParallel() + 1)
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);
                    for (int layer = 0; layer < shapeInfo; layer++) {
                        String muffler = layer == 0 ? "CCmCC" : "CCPCC";
                        builder.aisle("CCCCC", "CCCCC", muffler, "CCCCC", "CCCCC");
                        builder.aisle("ccccc", "c#c#c", "ccPcc", "c#c#c", "ccccc");
                        builder.aisle("ccccc", "c#c#c", "ccPcc", "c#c#c", "ccccc");
                    }
                    return builder.aisle("IiSOo", "CCCCC", "CCCCC", "CCCCC", "CCEMC")
                            .where('S', this.getController(), WEST)
                            .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.INVAR_HEATPROOF))
                            .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                            .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                            .where('M', GATileEntities.MAINTENANCE_HATCH[0], EAST)
                            .where('E', this.getEnergyHatch(tier, false), EAST)
                            .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], WEST)
                            .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                            .where('O', MetaTileEntities.ITEM_EXPORT_BUS[1], WEST)
                            .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[0], WEST)
                            .where('m', GATileEntities.MUFFLER_HATCH[0], EnumFacing.UP)
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
        this.addBlockTooltip(MetaBlocks.METAL_CASING.getItemVariant(BlockMetalCasing.MetalCasingType.INVAR_HEATPROOF), new TextComponentTranslation("gregtech.multiblock.preview.limit", 12)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public String[] getDescription() {
        return ArrayUtils.addAll(new String[] {
                I18n.format("tj.multiblock.parallel_electric_blast_furnace.description"),
                I18n.format("tj.multiblock.parallel.description"),
                I18n.format("tj.multiblock.parallel.extend.tooltip"),
                I18n.format("gtadditions.multiblock.electric_blast_furnace.tooltip.1"),
                I18n.format("gtadditions.multiblock.electric_blast_furnace.tooltip.2"),
                I18n.format("gtadditions.multiblock.electric_blast_furnace.tooltip.3")},
                super.getDescription());
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
