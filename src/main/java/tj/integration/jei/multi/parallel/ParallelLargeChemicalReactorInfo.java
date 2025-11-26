package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
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
import tj.machines.multi.parallel.MetaTileEntityParallelLargeChemicalReactor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;


public class ParallelLargeChemicalReactorInfo extends TJMultiblockInfoPage implements IParallelMultiblockInfoPage {

    @Override
    public ParallelRecipeMapMultiblockController getController() {
        return TJMetaTileEntities.PARALLEL_CHEMICAL_REACTOR;
    }

    @Override
    public List<MultiblockShapeInfo[]> getMatchingShapes(MultiblockShapeInfo[] shapes) {
        return IntStream.range(1, 17)
                .mapToObj(shapeInfo -> {
                    GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN);
                    MetaTileEntityParallelLargeChemicalReactor chemicalReactor = TJMetaTileEntities.PARALLEL_CHEMICAL_REACTOR;
                    builder.aisle("CCMCC", "CCCCC", "CCCCC", "CCCCC", "CCCCC");
                    for (int layer = 0; layer < shapeInfo; layer++) {
                        builder.aisle("F###F", "#CCC#", "#CCC#", "#CCC#", "F###F");
                        builder.aisle("F###F", "#PPP#", "#PcP#", "#PPP#", "F###F");
                    }
                    MultiblockShapeInfo[] infos = new MultiblockShapeInfo[15];
                    for (int tier = 0; tier < infos.length; tier++) {
                        infos[tier] = builder.aisle("F###F", "#CCC#", "#CCC#", "#CCC#", "F###F")
                                .aisle("IiSOo", "CCCCC", "CCCCC", "CCCCC", "CCECC")
                                .where('S', chemicalReactor, EnumFacing.WEST)
                                .where('C', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT))
                                .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                                .where('P', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE))
                                .where('F', MetaBlocks.FRAMES.get(Steel).getDefaultState())
                                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                                .where('E', this.getEnergyHatch(tier, false), EnumFacing.WEST)
                                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                                .where('i', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                                .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                                .where('o', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                                .build();
                    }
                    return infos;
                }).collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.MUTLIBLOCK_CASING.getItemVariant(GAMultiblockCasing.CasingType.CHEMICALLY_INERT), new TextComponentTranslation("gregtech.multiblock.preview.limit", 0)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public String[] getDescription() {
        return ArrayUtils.addAll(new String[] {
                I18n.format("tj.multiblock.parallel_chemical_reactor.description"),
                I18n.format("tj.multiblock.parallel.description"),
                I18n.format("tj.multiblock.parallel.extend.tooltip")},
                super.getDescription());
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
