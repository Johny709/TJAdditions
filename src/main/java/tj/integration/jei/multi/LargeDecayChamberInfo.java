package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.nuclear.MetaTileEntityNuclearReactor;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import tj.machines.TJMetaTileEntities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LargeDecayChamberInfo extends MultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_DECAY_CHAMBER;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        return Arrays.stream(MetaTileEntityNuclearReactor.RodType.values()).map(rodType -> MultiblockShapeInfo.builder()
                .aisle("~~C~~", "~CCC~", "CCCCC", "~CCC~", "~~C~~")
                .aisle("~CCC~", "!###C", "C#F#C", "I###C", "~CCC~")
                .aisle("CCCCC", "C#F#C", "SFRFE", "M#F#C", "CCCCC")
                .aisle("~CCC~", "0###C", "C#F#C", "O###C", "~CCC~")
                .aisle("~~C~~", "~CCC~", "CCCCC", "~CCC~", "~~C~~")
                .where('S', TJMetaTileEntities.LARGE_DECAY_CHAMBER, EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.LEAD))
                .where('F', GAMetaBlocks.FIELD_GEN_CASING.getDefaultState())
                .where('R', rodType.casingState)
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('E', MetaTileEntities.ENERGY_INPUT_HATCH[GTValues.LuV], EnumFacing.EAST)
                .where('I', MetaTileEntities.ITEM_IMPORT_BUS[GTValues.LuV], EnumFacing.WEST)
                .where('O', MetaTileEntities.ITEM_EXPORT_BUS[GTValues.LuV], EnumFacing.WEST)
                .where('!', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.LuV], EnumFacing.WEST)
                .where('0', MetaTileEntities.FLUID_EXPORT_HATCH[GTValues.LuV], EnumFacing.WEST)
                .build()).collect(Collectors.toList());
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_decay_chamber.description")};
    }

    @Override
    public float getDefaultZoom() {
        return 0.5f;
    }
}
