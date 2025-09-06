package tj.integration.jei.multi.parallel;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockBoilerCasing;
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

public class ParallelLargeWashingMachineInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.PARALLEL_LARGE_WASHING_MACHINE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo < 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT);
            builder.aisle("CCCCC", "CmEmC", "CCCCC", "~CCC~");
            for (int layer = 0; layer < shapeInfo; layer++) {
                if (layer != 0) {
                    builder.aisle("CCCCC", "CP#PC", "C###C", "~CCC~");
                }
                builder.aisle("CCCCC", "CP#PC", "C###C", "CGCGC");
                builder.aisle("CCCCC", "CP#PC", "C###C", "CGCGC");
                builder.aisle("CCCCC", "CP#PC", "C###C", "CGCGC");
            }
            shapeInfos.add(builder.aisle("CICOC", "CmSmC", "CiMoC", "~CCC~")
                    .where('S', getController(), EnumFacing.WEST)
                    .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.GRISIUM))
                    .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS))
                    .where('P', MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE))
                    .where('m', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('i', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('o', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.IV], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.GRISIUM), new TextComponentTranslation("gregtech.multiblock.preview.limit", 24)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.MOTOR_CASING.getItemVariant(MotorCasing.CasingType.MOTOR_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.parallel_large_washing_machine.description"),
                I18n.format("tj.multiblock.parallel.description.parallel")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
