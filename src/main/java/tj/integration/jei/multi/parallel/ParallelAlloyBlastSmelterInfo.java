package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.EAST;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelAlloyBlastSmelterInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_ALLOY_BLAST_FURNACE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);
            for (int layer = 0; layer < shapeInfo; layer++) {
                String mufflerMM = layer == 0 ? "CCCmCCC" : "CCCPCCC";
                builder.aisle("~CCCCC~", "CCCCCCC", "CCCCCCC", mufflerMM, "CCCCCCC", "CCCCCCC", "~CCCCC~");
                builder.aisle("~AAAAA~", "AcccccA", "Ac#c#cA", "AccPccA", "Ac#c#cA", "AcccccA", "~AAAAA~");
                builder.aisle("~AAAAA~", "AcccccA", "Ac#c#cA", "AccPccA", "Ac#c#cA", "AcccccA", "~AAAAA~");
            }
            shapeInfos.add(builder.aisle("~IiSoC~", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "~CCEMC~")
                    .where('S', this.getController(), WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.ZIRCONIUM_CARBIDE))
                    .where('A', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.STABALLOY))
                    .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EAST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], EAST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[0], WEST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[0], WEST)
                    .where('m', GATileEntities.MUFFLER_HATCH[0], EnumFacing.UP)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.ZIRCONIUM_CARBIDE), new TextComponentTranslation("gregtech.multiblock.preview.limit", 22)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_alloy_blast_smelter.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
