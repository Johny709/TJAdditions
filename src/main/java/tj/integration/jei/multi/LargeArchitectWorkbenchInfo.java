package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.jei.GAMultiblockShapeInfo;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import tj.machines.TJMetaTileEntities;

import java.util.ArrayList;
import java.util.List;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class LargeArchitectWorkbenchInfo extends MultiblockInfoPage {
    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_ARCHITECT_WORKBENCH;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        List<MultiblockShapeInfo> shapes = new ArrayList<>();
        for (int index = 1; index < 16; index++) {
            GAMultiblockShapeInfo.Builder builder = GAMultiblockShapeInfo.builder(FRONT, UP, LEFT);
            for (int num = 0; num < index; num++) {
                if (num == (index -1))
                    builder.aisle("CCC", "CEC", "~~~", "~~~");
                else
                    builder.aisle("CCC", "CCC", "~~~", "~~~");
                builder.aisle("CCC", "CcC", "C~C", "CrC");
            }
            builder.aisle("CMC", "ISO", "~~~", "~~~");
            builder.where('S', TJMetaTileEntities.LARGE_ARCHITECT_WORKBENCH, EnumFacing.WEST)
                    .where('C', MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID))
                    .where('c', GAMetaBlocks.CONVEYOR_CASING.getDefaultState())
                    .where('r', GAMetaBlocks.ROBOT_ARM_CASING.getDefaultState())
                    .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                    .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.IV], EnumFacing.EAST)
                    .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.IV], EnumFacing.WEST)
                    .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.IV], EnumFacing.WEST);
            shapes.add(builder.build());
        }
        return shapes;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_architect_workbench.description")};
    }
}
