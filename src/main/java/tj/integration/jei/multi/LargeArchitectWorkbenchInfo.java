package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockMetalCasing;
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

public class LargeArchitectWorkbenchInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_ARCHITECT_WORKBENCH;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapeInfos = new ArrayList<>();
        for (int shapeInfo = 1; shapeInfo < 16; shapeInfo++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT);
            for (int layer = 0; layer < shapeInfo; layer++) {
                String power = layer == 0 ? "CEC" : "CCC";
                builder.aisle(power, "CCC", "~~~", "~~~");
                builder.aisle("CCC", "CcC", "C#C", "CrC");
            }
            shapeInfos.add(builder.aisle("CMC", "ISO", "~~~", "~~~")
                    .where('S', this.getController(), EnumFacing.WEST)
                    .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID))
                    .where('c', GAMetaBlocks.CONVEYOR_CASING.getDefaultState())
                    .where('r', GAMetaBlocks.ROBOT_ARM_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .build());
        }
        return shapeInfos;
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(MetaBlocks.METAL_CASING.getItemVariant(BlockMetalCasing.MetalCasingType.STEEL_SOLID), new TextComponentTranslation("gregtech.multiblock.preview.limit", 6)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        this.addBlockTooltip(GAMetaBlocks.CONVEYOR_CASING.getItemVariant(ConveyorCasing.CasingType.CONVEYOR_LV), COMPONENT_BLOCK_TOOLTIP);
        this.addBlockTooltip(GAMetaBlocks.ROBOT_ARM_CASING.getItemVariant(RobotArmCasing.CasingType.ROBOT_ARM_LV), COMPONENT_BLOCK_TOOLTIP);
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_architect_workbench.description")};
    }
}
