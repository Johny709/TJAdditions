package tj.integration.jei;

import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public abstract class TJMultiblockInfoPage extends MultiblockInfoPage {

    protected static final ITextComponent COMPONENT_BLOCK_TOOLTIP = new TextComponentTranslation("gregtech.multiblock.universal.component_casing.tooltip").setStyle(new Style().setColor(TextFormatting.RED));
    protected static final ITextComponent COMPONENT_TIER_ANY_TOOLTIP = new TextComponentTranslation("tj.multiblock.component_casing.any.tooltip").setStyle(new Style().setColor(TextFormatting.GREEN));

    protected IBlockState getVoltageCasing(int tier) {
        switch (tier) {
            case 1: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_LV);
            case 2: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_MV);
            case 3: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_HV);
            case 4: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_EV);
            case 5: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_IV);
            case 6: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_LUV);
            case 7: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_ZPM);
            case 8: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_UV);
            case 9: return GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.TIERED_HULL_UHV);
            case 10: return GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.TIERED_HULL_UEV);
            case 11: return GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.TIERED_HULL_UIV);
            case 12: return GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.TIERED_HULL_UMV);
            case 13: return GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.TIERED_HULL_UXV);
            case 14: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_MAX);
            default: return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.TIERED_HULL_ULV);
        }
    }

    protected MetaTileEntity getEnergyHatch(int tier, boolean isOutput) {
        switch (tier) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: return isOutput ? MetaTileEntities.ENERGY_OUTPUT_HATCH[tier] : MetaTileEntities.ENERGY_INPUT_HATCH[tier];
            case 9:
            case 10:
            case 11:
            case 12:
            case 13: return isOutput ? GATileEntities.ENERGY_OUTPUT[tier - 9] : GATileEntities.ENERGY_INPUT[tier - 9];
            case 14: return isOutput ? MetaTileEntities.ENERGY_OUTPUT_HATCH[9] : MetaTileEntities.ENERGY_INPUT_HATCH[9];
            default: return isOutput ? MetaTileEntities.ENERGY_OUTPUT_HATCH[0] : MetaTileEntities.ENERGY_INPUT_HATCH[0];
        }
    }
}
