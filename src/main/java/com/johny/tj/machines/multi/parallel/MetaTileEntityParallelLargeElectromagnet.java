package com.johny.tj.machines.multi.parallel;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.ParallelRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

import static com.johny.tj.TJRecipeMaps.PARALLEL_ELECTROMAGNETIC_SEPARATOR_RECIPES;
import static com.johny.tj.TJRecipeMaps.PARALLEL_POLARIZER_RECIPES;
import static com.johny.tj.machines.multi.electric.MetaTileEntityLargeGreenhouse.glassPredicate;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.fieldGenPredicate;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.ELECTROMAGNETIC_SEPARATOR_RECIPES;
import static gregtech.api.recipes.RecipeMaps.POLARIZER_RECIPES;
import static gregtech.api.render.Textures.ELECTROMAGNETIC_SEPARATOR_OVERLAY;
import static gregtech.api.render.Textures.POLARIZER_OVERLAY;

public class MetaTileEntityParallelLargeElectromagnet extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, MAINTENANCE_HATCH, INPUT_ENERGY};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeElectromagnet(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_POLARIZER_RECIPES, PARALLEL_ELECTROMAGNETIC_SEPARATOR_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, TJConfig.parallelLargeElectromagnet.eutPercentage, TJConfig.parallelLargeElectromagnet.durationPercentage,
                TJConfig.parallelLargeElectromagnet.chancePercentage, TJConfig.parallelLargeElectromagnet.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeElectromagnet(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                POLARIZER_RECIPES.getLocalizedName() + ", " +
                        ELECTROMAGNETIC_SEPARATOR_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeElectromagnet.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeElectromagnet.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeElectromagnet.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeElectromagnet.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        factoryPattern.aisle("~~~~~", "~HHH~", "~HHH~", "~HHH~", "~~~~~");
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("~C~C~", "H#H#H", "G###G", "H#H#H", "~C~C~");
            factoryPattern.aisle("~C~C~", "H#H#H", "GF#FG", "H#H#H", "~C~C~");
        }
        return factoryPattern.aisle("~C~C~", "H#H#H", "G###G", "H#H#H", "~C~C~")
                .aisle("~~~~~", "~HHH~", "~HSH~", "~HHH~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('G', glassPredicate())
                .where('F', fieldGenPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private static IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.BABBITT_ALLOY);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV).getTier();
        this.maxVoltage = (long) (Math.pow(4, fieldGen) * 8);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.BABBITT_ALLOY_CASING;
    }

    @Override
    protected @NotNull OrientedOverlayRenderer getFrontOverlay() {
        return this.getRecipeMapIndex() == 0 ? POLARIZER_OVERLAY : ELECTROMAGNETIC_SEPARATOR_OVERLAY;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeElectromagnet.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return GATileEntities.LARGE_ELECTROMAGNET.getRecipeMaps();
    }
}
