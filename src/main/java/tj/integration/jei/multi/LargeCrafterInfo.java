package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;

public class LargeCrafterInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_CRAFTER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        GATransparentCasing.CasingType[] glasses = GATransparentCasing.CasingType.values();
        GAMultiblockShapeInfo.Builder shapeInfo = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT)
                .aisle("CCCCC", "FCCCF", "FCECF", "FCCCF", "~CCC~")
                .aisle("CCCCC", "G#c#G", "GR#RG", "F#c#F", "~CCC~")
                .aisle("CCCCC", "G#c#G", "GR#RG", "F#c#F", "~CCC~")
                .aisle("CCCCC", "G#c#G", "GR#RG", "F#c#F", "~CCC~")
                .aisle("CCCCC", "FIMOF", "FCSCF", "FCHCF", "~CCC~")
                .where('S', this.getController(), EnumFacing.WEST)
                .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID))
                .where('F', MetaBlocks.FRAMES.get(Steel).getDefaultState())
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[1], EnumFacing.WEST)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[1], EnumFacing.WEST)
                .where('H', TJMetaTileEntities.CRAFTER_HATCHES[0], EnumFacing.WEST);
        return Arrays.stream(ConveyorCasing.CasingType.values())
                .map(casingType -> shapeInfo.where('c', GAMetaBlocks.CONVEYOR_CASING.getState(casingType))
                        .where('R', GAMetaBlocks.ROBOT_ARM_CASING.getState(RobotArmCasing.CasingType.values()[casingType.ordinal()]))
                        .where('E', this.getEnergyHatch(casingType.getTier(), false), EnumFacing.EAST)
                        .where('G', GAMetaBlocks.TRANSPARENT_CASING.getState(glasses[Math.min(glasses.length - 1, casingType.ordinal())]))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(MetaBlocks.METAL_CASING.getItemVariant(BlockMetalCasing.MetalCasingType.STEEL_SOLID), new TextComponentTranslation("gregtech.multiblock.preview.limit", 25)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        Arrays.stream(GATransparentCasing.CasingType.values()).forEach(casingType -> this.addBlockTooltip(GAMetaBlocks.TRANSPARENT_CASING.getItemVariant(casingType), COMPONENT_TIER_ANY_TOOLTIP));
        Arrays.stream(ConveyorCasing.CasingType.values()).forEach(casingType -> this.addBlockTooltip(GAMetaBlocks.CONVEYOR_CASING.getItemVariant(casingType), COMPONENT_BLOCK_TOOLTIP));
        Arrays.stream(RobotArmCasing.CasingType.values()).forEach(casingType -> this.addBlockTooltip(GAMetaBlocks.ROBOT_ARM_CASING.getItemVariant(casingType), COMPONENT_BLOCK_TOOLTIP));
    }

    @Override
    public String[] getDescription() {
        return new String[]{""};
    }
}
