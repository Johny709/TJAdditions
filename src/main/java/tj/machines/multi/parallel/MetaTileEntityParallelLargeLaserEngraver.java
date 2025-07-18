package tj.machines.multi.parallel;

import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.EmitterCasing;
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
import gregtech.common.blocks.BlockTurbineCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static tj.TJRecipeMaps.PARALLEL_LARGE_ENGRAVER_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.conveyorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.emitterPredicate;
import static gregicadditions.recipes.GARecipeMaps.LARGE_ENGRAVER_RECIPES;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.EXTRUDER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.LASER_ENGRAVER_RECIPES;

public class MetaTileEntityParallelLargeLaserEngraver extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, IMPORT_FLUIDS, EXPORT_FLUIDS, MAINTENANCE_HATCH, INPUT_ENERGY, REDSTONE_CONTROLLER};

    public MetaTileEntityParallelLargeLaserEngraver(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_LARGE_ENGRAVER_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, () -> TJConfig.parallelLargeLaserEngraver.eutPercentage, () -> TJConfig.parallelLargeLaserEngraver.durationPercentage,
                () -> TJConfig.parallelLargeLaserEngraver.chancePercentage, () -> TJConfig.parallelLargeLaserEngraver.stack);
        this.recipeMapWorkable.setMaxVoltage(this::getMaxVoltage);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeLaserEngraver(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                EXTRUDER_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeLaserEngraver.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeLaserEngraver.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeLaserEngraver.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeLaserEngraver.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("XXX", "XGX", "XXX", "~C~");
            factoryPattern.aisle("XXX", "GcG", "XEX", "CBC");
        }
        return factoryPattern.aisle("XXX", "XSX", "XXX", "~C~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('G', statePredicate(GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.IRIDIUM_GLASS)))
                .where('B', statePredicate(MetaBlocks.TURBINE_CASING.getState(BlockTurbineCasing.TurbineCasingType.TITANIUM_GEARBOX)))
                .where('c', conveyorPredicate())
                .where('E', emitterPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.MUTLIBLOCK_CASING2.getState(GAMultiblockCasing2.CasingType.LASER_ENGRAVER);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int conveyor = context.getOrDefault("Conveyor", ConveyorCasing.CasingType.CONVEYOR_LV).getTier();
        int emitter = context.getOrDefault("Emitter", EmitterCasing.CasingType.EMITTER_LV).getTier();
        int min = Math.min(conveyor, emitter);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.LASER_ENGRAVER;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.LASER_ENGRAVER_OVERLAY;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeLaserEngraver.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{LASER_ENGRAVER_RECIPES, LARGE_ENGRAVER_RECIPES};
    }
}
