package com.johny.tj.machines.multi.parallel;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.ParallelRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMultiblockCasing;
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
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Predicate;

import static com.johny.tj.TJRecipeMaps.*;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.machines.GATileEntities.*;
import static gregicadditions.recipes.GARecipeMaps.LARGE_CENTRIFUGE_RECIPES;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.THERMAL_CENTRIFUGE_RECIPES;

public class MetaTileEntityParallelLargeCentrifuge extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeCentrifuge(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_CENTRIFUGE_RECIPES, PARALLEL_THERMAL_CENTRIFUGE_RECIPES, PARALLEL_GAS_CENTRIFUGE_RECIPES});
        this.recipeMapWorkable = new ParallelLargeCentrifugeWorkableHandler(this, TJConfig.parallelLargeCentrifuge.eutPercentage,
                TJConfig.parallelLargeCentrifuge.durationPercentage, TJConfig.parallelLargeCentrifuge.chancePercentage, TJConfig.parallelLargeCentrifuge.stack);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeCentrifuge(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                LARGE_CENTRIFUGE_RECIPES.getLocalizedName() + ", " + THERMAL_CENTRIFUGE_RECIPES.getLocalizedName()
                        + ", " + GARecipeMaps.GAS_CENTRIFUGE_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeCentrifuge.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeCentrifuge.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeCentrifuge.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeCentrifuge.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.2"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, FRONT, DOWN);
        factoryPattern.aisle("~HHH~", "HCCCH", "HCmCH", "HCCCH", "~HHH~");
        for (int count = 1; count < this.parallelLayer; count++) {
            factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
            factoryPattern.aisle("~MGM~", "M###M", "G#P#G", "M###M", "~MGM~");
            factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
            factoryPattern.aisle("~HHH~", "HCCCH", "HCmCH", "HCCCH", "~HHH~");
            factoryPattern.validateLayer(2 + count * 4, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
        factoryPattern.aisle("~MSM~", "M###M", "G#P#G", "M###M", "~MGM~");
        factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
        factoryPattern.aisle("~HHH~", "HCCCH", "HCmCH", "HCCCH", "~HHH~");
        return factoryPattern.validateLayer(2, context -> context.getInt("RedstoneControllerAmount") <= 1)
                .where('S', this.selfPredicate())
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)).or(machineControllerPredicate))
                .where('C', MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate().or(MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate2()))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE)))
                .where('G', statePredicate(MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING)))
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private static IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.RED_STEEL);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.RED_STEEL_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        switch (getRecipeMapIndex()) {
            case 1: return Textures.THERMAL_CENTRIFUGE_OVERLAY;
            case 2: return Textures.MULTIBLOCK_WORKABLE_OVERLAY;
            default: return Textures.CENTRIFUGE_OVERLAY;
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int min = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
        this.energyBonus = context.getOrDefault("coilLevel", 0) * 5;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeCentrifuge.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{LARGE_CENTRIFUGE.recipeMap, LARGE_THERMAL_CENTRIFUGE.recipeMap, GAS_CENTRIFUGE.recipeMap};
    }

    private static class ParallelLargeCentrifugeWorkableHandler extends ParallelGAMultiblockRecipeLogic {

        public ParallelLargeCentrifugeWorkableHandler(ParallelRecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
            super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack);
        }

        @Override
        protected long getMaxVoltage() {
            return this.controller.getMaxVoltage();
        }

        @Override
        protected void setupRecipe(Recipe recipe, int i) {
            int energyBonus = this.controller.getEUBonus();
            long maxVoltage = getMaxVoltage();

            int[] resultOverclock = calculateOverclock(recipe.getEUt(), maxVoltage, recipe.getDuration());
            this.progressTime[i] = 1;

//            // perfect overclocking
//            if (resultOverclock[1] < recipe.getDuration())
//                resultOverclock[1] *= 0.5;

            // apply energy bonus
            resultOverclock[0] -= (int) (resultOverclock[0] * energyBonus * 0.01f);
            setMaxProgress(resultOverclock[1], i);

            this.timeToStop[i] = 20;
            this.recipeEUt[i] = resultOverclock[0];
            this.fluidOutputs.put(i, GTUtility.copyFluidList(recipe.getFluidOutputs()));
            int tier = getMachineTierForRecipe(recipe);
            this.itemOutputs.put(i, GTUtility.copyStackList(recipe.getResultItemOutputs(getOutputInventory().getSlots(), random, tier)));
            if (this.wasActiveAndNeedsUpdate) {
                this.wasActiveAndNeedsUpdate = false;
            } else {
                this.setActive(true, i);
            }
        }
    }
}
