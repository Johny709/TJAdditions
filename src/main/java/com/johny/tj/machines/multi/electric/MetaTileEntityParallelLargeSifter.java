package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.PistonCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Predicate;

import static com.johny.tj.TJRecipeMaps.PARALLEL_SIFTER_RECIPES;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.GAMaterials.EglinSteel;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.SIFTER_RECIPES;

public class MetaTileEntityParallelLargeSifter extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeSifter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_SIFTER_RECIPES);
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, SIFTER_RECIPES, TJConfig.parallelLargeSifter.eutPercentage,
                TJConfig.parallelLargeSifter.durationPercentage, TJConfig.parallelLargeSifter.chancePercentage, TJConfig.parallelLargeSifter.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeSifter(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                SIFTER_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeSifter.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeSifter.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeSifter.stack));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeSifter.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, FRONT, DOWN);
        for (int count = 1; count < parallelLayer; count++) {
            factoryPattern.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
            factoryPattern.aisle("MMMMM", "PGGGP", "MGGGM", "PGGGP", "MMMMM");
            factoryPattern.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
            factoryPattern.aisle("~FCF~", "F###F", "C###C", "F###F", "~FCF~");
            factoryPattern.validateLayer(3 + count * 4, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
        factoryPattern.aisle("MMSMM", "PGGGP", "MGGGM", "PGGGP", "MMMMM");
        factoryPattern.aisle("~HHH~", "H###H", "H###H", "H###H", "~HHH~");
        factoryPattern.aisle("~C~C~", "CCCCC", "~C~C~", "CCCCC", "~C~C~");
        factoryPattern.aisle("~C~C~", "CCCCC", "~C~C~", "CCCCC", "~C~C~");
        factoryPattern.validateLayer(3, context -> context.getInt("RedstoneControllerAmount") <= 1)
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)).or(machineControllerPredicate))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(EglinSteel).getDefaultState()))
                .where('G', statePredicate(MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING)))
                .where('P', LargeSimpleRecipeMapMultiblockController.pistonPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true);
        return factoryPattern.build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.EGLIN_STEEL);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.EGLIN_STEEL_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.SIFTER_OVERLAY;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            Style style = new Style().setColor(TextFormatting.GREEN);
            textList.add(new TextComponentTranslation("machine.universal.tooltip.voltage_tier")
                    .appendText(" ")
                    .appendSibling(new TextComponentString(String.valueOf(maxVoltage)).setStyle(style))
                    .appendText(" (")
                    .appendSibling(new TextComponentString(String.valueOf(GAValues.VN[GTUtility.getGATierByVoltage(maxVoltage)])).setStyle(style))
                    .appendText(")"));
        }
        textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(new TextComponentTranslation("recipemap." + SIFTER_RECIPES.getUnlocalizedName() + ".name")
                        .setStyle(new Style().setColor(TextFormatting.YELLOW))));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int min = context.getOrDefault("Piston", PistonCasing.CasingType.PISTON_LV).getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        maxVoltage = 0;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{SIFTER_RECIPES};
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeSifter.maximumParallel;
    }

    @Override
    public RecipeMap<?> getMultiblockRecipe() {
        return SIFTER_RECIPES;
    }
}
