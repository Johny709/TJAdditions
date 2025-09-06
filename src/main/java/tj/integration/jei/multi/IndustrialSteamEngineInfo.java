package tj.integration.jei.multi;

import com.google.common.collect.Lists;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.machines.TJMetaTileEntities;

import java.util.List;

import static gregicadditions.item.GAMetaBlocks.METAL_CASING_1;

public class IndustrialSteamEngineInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.INDUSTRIAL_STEAM_ENGINE;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo shapeInfo = MultiblockShapeInfo.builder()
                .aisle("~~C~", "~CCC", "~~C~")
                .aisle("mCCC", "SFRE", "ICCO")
                .aisle("CCCC", "CCCC", "CCCC")
                .where('S', TJMetaTileEntities.INDUSTRIAL_STEAM_ENGINE, EnumFacing.WEST)
                .where('m', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TUMBAGA))
                .where('E', MetaTileEntities.ENERGY_OUTPUT_HATCH[GTValues.MV], EnumFacing.EAST)
                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.MV], EnumFacing.WEST)
                .where('O', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.MV], EnumFacing.EAST)
                .where('R', GAMetaBlocks.MOTOR_CASING.getDefaultState())
                .where('F', GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_MV))
                .build();
        return Lists.newArrayList(shapeInfo);
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.TUMBAGA), new TextComponentTranslation("gregtech.multiblock.preview.limit", 8)
                .setStyle(new Style().setColor(TextFormatting.RED)));
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.industrial_steam_engine.description")};
    }
}
