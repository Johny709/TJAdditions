package tj.machines.multi.parallel;

import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PistonCasing;
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
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
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

import static tj.TJRecipeMaps.*;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.motorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.pistonPredicate;
import static gregicadditions.recipes.GARecipeMaps.CLUSTER_MILL_RECIPES;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.BENDER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.FORMING_PRESS_RECIPES;

public class MetaTileEntityParallelLargeBendingAndForming extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, MAINTENANCE_HATCH, INPUT_ENERGY, REDSTONE_CONTROLLER};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeBendingAndForming(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_BENDER_RECIPES, PARALLEL_FORMING_PRESS_RECIPES, PARALLEL_CLUSTER_MILL_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, TJConfig.parallelLargeBendingAndForming.eutPercentage, TJConfig.parallelLargeBendingAndForming.durationPercentage,
                TJConfig.parallelLargeBendingAndForming.chancePercentage, TJConfig.parallelLargeBendingAndForming.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeBendingAndForming(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                BENDER_RECIPES.getLocalizedName() + ", " +
                        FORMING_PRESS_RECIPES.getLocalizedName() + ", " +
                        CLUSTER_MILL_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeBendingAndForming.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeBendingAndForming.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeBendingAndForming.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeBendingAndForming.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        factoryPattern.aisle("XXXXX", "XXXXX", "XPXPX");
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("XXXXX", "XMpMX", "XPXPX");
        }
        return factoryPattern.aisle("XXXXX", "XXSXX", "XPXPX")
                .where('S', this.selfPredicate())
                .where('X', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE)))
                .where('M', motorPredicate())
                .where('p', pistonPredicate())
                .build();
    }

    private IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.TITANIUM_STABLE);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int piston = context.getOrDefault("Piston", PistonCasing.CasingType.PISTON_LV).getTier();
        int min = Math.min(motor, piston);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.STABLE_TITANIUM_CASING;
    }

    @Override
    protected @NotNull OrientedOverlayRenderer getFrontOverlay() {
        switch (this.getRecipeMapIndex()) {
            case 1: return Textures.FORMING_PRESS_OVERLAY;
            case 2: return Textures.WIREMILL_OVERLAY;
            default: return Textures.BENDER_OVERLAY;
        }
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeBendingAndForming.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return GATileEntities.LARGE_BENDER_AND_FORMING.getRecipeMaps();
    }
}
