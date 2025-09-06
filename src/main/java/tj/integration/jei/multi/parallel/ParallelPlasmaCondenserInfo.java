package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
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
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelPlasmaCondenserInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_PLASMA_CONDENSER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);
            builder.aisle("~~~~~", "~CCC~", "~CCC~", "~CCC~", "~~~~~");
            for (int layer = 0; layer < shapeInfo; layer++) {

                String entityS = layer == shapeInfo - 1 ? "~ISO~" : "~CCC~";

                builder.aisle("~CCC~", "CG#GC", "C#T#C", "CG#GC", "~CCC~");
                builder.aisle(entityS, "CPTPC", "CTTTC", "CPTPC", "~CCC~");
            }
            shapeInfos.add(builder
                    .aisle("~iMo~", "CG#GC", "C#T#C", "CG#GC", "~CEC~")
                    .aisle("~~~~~", "~CCC~", "~CCC~", "~CCC~", "~~~~~")
                    .where('S', this.getController(), WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.HASTELLOY_N))
                    .where('G', MetaBlocks.TURBINE_CASING.getState(BlockTurbineCasing.TurbineCasingType.STEEL_GEARBOX))
                    .where('T', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                    .where('P', GAMetaBlocks.PUMP_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], WEST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], WEST)
                    .where('i', GATileEntities.OUTPUT_HATCH_MULTI.get(0), WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[1], WEST)
                    .where('o', GATileEntities.INPUT_HATCH_MULTI.get(0), WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.HASTELLOY_N), new TextComponentTranslation("gregtech.multiblock.preview.limit", 12)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.PUMP_CASING.getItemVariant(PumpCasing.CasingType.PUMP_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_plasma_condenser.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
