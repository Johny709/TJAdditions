package tj.integration.jei;

import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public abstract class TJMultiblockInfoPage extends MultiblockInfoPage {

    protected static final ITextComponent COMPONENT_BLOCK_TOOLTIP = new TextComponentTranslation("gregtech.multiblock.universal.component_casing.tooltip").setStyle(new Style().setColor(TextFormatting.RED));
    protected static final ITextComponent COMPONENT_TIER_ANY_TOOLTIP = new TextComponentTranslation("tj.multiblock.component_casing.any.tooltip").setStyle(new Style().setColor(TextFormatting.GREEN));
}
