package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregicadditions.GAMaterials.Staballoy;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static net.minecraft.util.EnumFacing.WEST;

public class ParallelLargeMixerInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_MIXER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo <= 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = new GAMultiblockShapeInfo.Builder(FRONT, RIGHT, DOWN);
            builder.aisle("~~F~~", "~~F~~", "FFFFF", "~~F~~", "~~F~~");
            for (int layer = 0; layer < shapeInfo; layer++) {

                String entityS = layer == shapeInfo - 1 ? "~ISO~" : "~CCC~";

                builder.aisle("~CCC~", "C###C", "C#G#C", "C###C", "~CCC~");
                builder.aisle("~CCC~", "C###C", "C#m#C", "C###C", "~CCC~");
                builder.aisle(entityS, "C###C", "C#m#C", "C###C", "~CCC~");
            }
            shapeInfos.add(builder.aisle("~iMo~", "CCCCC", "CCCCC", "CCCCC", "~CEC~")
                    .where('S', this.getController(), WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.STABALLOY))
                    .where('G', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TUNGSTENSTEEL_GEARBOX_CASING))
                    .where('F', MetaBlocks.FRAMES.get(Staballoy).getDefaultState())
                    .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[0], WEST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[2], WEST)
                    .where('i', GATileEntities.OUTPUT_HATCH_MULTI.get(1), WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[2], WEST)
                    .where('o', GATileEntities.INPUT_HATCH_MULTI.get(1), WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_2.getItemVariant(MetalCasing2.CasingType.STABALLOY), new TextComponentTranslation("gregtech.multiblock.preview.limit", 20)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_mixer.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
