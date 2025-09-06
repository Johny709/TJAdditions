package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class ParallelLargeCentrifugeInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_CENTRIFUGE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo < 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, RIGHT, DOWN);
            for (int layer = 0; layer < shapeInfo; layer++) {

                String entityS = layer == shapeInfo - 1 ? "~CSC~" : "~CGC~";

                builder.aisle("~CCC~", "CcccC", "CcmcC", "CcccC", "~CCC~");
                builder.aisle("CCCCC", "C###C", "C#P#C", "C###C", "CCCCC");
                builder.aisle(entityS, "C###C", "G#P#G", "C###C", "~CGC~");
                builder.aisle("CCCCC", "C###C", "C#P#C", "C###C", "CCCCC");
            }
            shapes.add(builder.aisle("~IMO~", "CcccC", "CcmcC", "CcccC", "~iEo~")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.RED_STEEL))
                    .where('G', MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING))
                    .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE))
                    .where('c', MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.CUPRONICKEL))
                    .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .build());
        }
        return shapes;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_2.getItemVariant(MetalCasing2.CasingType.RED_STEEL), new TextComponentTranslation("gregtech.multiblock.preview.limit", 26)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.MOTOR_CASING.getItemVariant(MotorCasing.CasingType.MOTOR_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_centrifuge.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
