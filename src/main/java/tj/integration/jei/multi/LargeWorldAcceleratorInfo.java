package tj.integration.jei.multi;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.GATileEntities;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockShapeInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import tj.integration.jei.TJMultiblockInfoPage;
import tj.machines.TJMetaTileEntities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LargeWorldAcceleratorInfo extends TJMultiblockInfoPage {

    @Override
    public MultiblockControllerBase getController() {
        return TJMetaTileEntities.LARGE_WORLD_ACCELERATOR;
    }

    @Override
    public List<MultiblockShapeInfo> getMatchingShapes() {
        MultiblockShapeInfo.Builder shapeInfo = MultiblockShapeInfo.builder()
                .aisle("#C#", "EeC", "#C#")
                .aisle("IeC", "SFe", "MeC")
                .aisle("#C#", "CeC", "#C#")
                .where('S', TJMetaTileEntities.LARGE_WORLD_ACCELERATOR, EnumFacing.WEST)
                .where('C', GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.TRITANIUM))
                .where('M', GATileEntities.MAINTENANCE_HATCH[0], EnumFacing.WEST)
                .where('I', MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.UV], EnumFacing.WEST);
        return Arrays.stream(FieldGenCasing.CasingType.values())
                .map(casingType -> shapeInfo.where('F', GAMetaBlocks.FIELD_GEN_CASING.getState(casingType))
                        .where('e', GAMetaBlocks.EMITTER_CASING.getState(EmitterCasing.CasingType.values()[casingType.ordinal()]))
                        .where('E', this.getEnergyHatch(casingType.getTier(), false), EnumFacing.WEST)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected void generateBlockTooltips() {
        super.generateBlockTooltips();
        this.addBlockTooltip(GAMetaBlocks.METAL_CASING_2.getItemVariant(MetalCasing2.CasingType.TRITANIUM), new TextComponentTranslation("gregtech.multiblock.preview.limit", 2)
                .setStyle(new Style().setColor(TextFormatting.RED)));
        Arrays.stream(FieldGenCasing.CasingType.values()).forEach(casingType -> this.addBlockTooltip(GAMetaBlocks.FIELD_GEN_CASING.getItemVariant(casingType), COMPONENT_BLOCK_TOOLTIP));
        Arrays.stream(EmitterCasing.CasingType.values()).forEach(casingType -> this.addBlockTooltip(GAMetaBlocks.EMITTER_CASING.getItemVariant(casingType), COMPONENT_BLOCK_TOOLTIP));
    }

    @Override
    public String[] getDescription() {
        return new String[] {
                I18n.format("tj.multiblock.large_world_accelerator.description"),
                I18n.format("metaitem.item.linking.device.link.from")};
    }
}
