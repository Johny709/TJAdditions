package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.TJRecipeMaps;
import com.johny.tj.builder.multicontrollers.TJMultiRecipeMapMultiblockController;
import gregicadditions.GAUtility;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.recipes.GARecipeMaps;
import gregicadditions.utils.GALog;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTFluidUtils;
import gregtech.api.util.InventoryUtils;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MetaTileEntityLargeGreenhouse extends TJMultiRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityLargeGreenhouse(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId, recipeMap, TJConfig.largeGreenhouse.eutPercentage, TJConfig.largeGreenhouse.durationPercentage, TJConfig.largeGreenhouse.chancePercentage, TJConfig.largeGreenhouse.stack, new RecipeMap[]{GARecipeMaps.GREEN_HOUSE_RECIPES, TJRecipeMaps.GREENHOUSE_TREE_RECIPES});
        this.recipeMapWorkable = new GreenhouseRecipeLogic(this, EUtPercentage, durationPercentage, chancePercentage, stack, recipeMaps);
    }

    @Override
    public OrientedOverlayRenderer getRecipeMapOverlay(int i) {
        return Textures.FERMENTER_OVERLAY;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return Textures.CLEAN_STAINLESS_STEEL_CASING;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder Holder) {
        return new MetaTileEntityLargeGreenhouse(metaTileEntityId, GARecipeMaps.GREEN_HOUSE_RECIPES);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.mulitblock.greenhouse.treemode"));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.largeGreenhouse.eutPercentageTree / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.largeGreenhouse.durationPercentageTree / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.4", TJConfig.largeGreenhouse.stackTree));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.largeGreenhouse.chancePercentageTree));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("~HHHHH~", "~HHHHH~", "~HHHHH~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~~~~~~~")
                .aisle("HCCCCCH", "HDDDDDH", "H#####H", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                .aisle("HCCCCCH", "HDDDDDH", "H#####H", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                .aisle("HCCPCCH", "HDDDDDH", "H#####H", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                .aisle("HCCCCCH", "HDDDDDH", "H#####H", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                .aisle("HCCCCCH", "HDDDDDH", "H#####H", "G#####G", "G#####G", "G#####G", "G#####G", "~GGGGG~")
                .aisle("~HHHHH~", "~HHSHH~", "~HHHHH~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~GGGGG~", "~~~~~~~")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('G', glassPredicate())
                .where('P', pumpPredicate())
                .where('D', blockPredicate(Block.getBlockFromName("randomthings:fertilizeddirt")))
                .where('#', isAirPredicate())
                .where('~', (tile) -> true)
                .build();
    }

    protected IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STAINLESS_CLEAN);
    }

    public static Predicate<BlockWorldState> glassPredicate() {
        return blockWorldState -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof GATransparentCasing glassCasing))
                return false;
            GATransparentCasing.CasingType tieredCasingType = glassCasing.getState(blockState);
            GATransparentCasing.CasingType currentCasing = blockWorldState.getMatchContext().getOrPut("Glass", tieredCasingType);
            return currentCasing.getName().equals(tieredCasingType.getName());
        };
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int min = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
    }









    private class GreenhouseRecipeLogic extends MultiRecipeMapMultiblockRecipeLogic {

        public GreenhouseRecipeLogic(RecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack, RecipeMap<?>[] recipeMaps) {
            super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack, recipeMaps);
        }

        @Override
        protected void copyChancedItemOutputs(RecipeBuilder<?> newRecipe, Recipe oldRecipe, int multiplier) {
            for (Recipe.ChanceEntry s : oldRecipe.getChancedOutputs()) {
                int chance = Math.min(10000, s.getChance() * (getRecipeMapIndex() == 0 ? chancePercentage : TJConfig.largeGreenhouse.chancePercentageTree) / 100);
                int boost = s.getBoostPerTier() * (getRecipeMapIndex() == 0 ? chancePercentage : TJConfig.largeGreenhouse.chancePercentageTree) / 100;
                IntStream.range(0, multiplier).forEach(value -> {
                    ItemStack itemStack = s.getItemStack().copy();
                    newRecipe.chancedOutput(itemStack, chance, boost);
                });
            }
        }

        @Override
        protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
            int maxItemsLimit = (getRecipeMapIndex() == 0 ? this.getStack() : TJConfig.largeGreenhouse.stackTree);
            int EUt;
            int duration;
            int currentTier = getOverclockingTier(maxVoltage);
            int tierNeeded;
            int minMultiplier = Integer.MAX_VALUE;

            tierNeeded = Math.max(1, GAUtility.getTierByVoltage(matchingRecipe.getEUt()));
            maxItemsLimit *= currentTier - tierNeeded;
            maxItemsLimit = Math.max(1, maxItemsLimit);
            if (maxItemsLimit == 1) {
                return matchingRecipe;
            }

            Set<ItemStack> countIngredients = new HashSet<>();
            if (!matchingRecipe.getInputs().isEmpty()) {
                this.findIngredients(countIngredients, inputs);
                minMultiplier = Math.min(maxItemsLimit, this.getMinRatioItem(countIngredients, matchingRecipe, maxItemsLimit));
            }

            Map<String, Integer> countFluid = new HashMap<>();
            if (!matchingRecipe.getFluidInputs().isEmpty()) {

                this.findFluid(countFluid, fluidInputs);
                minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, maxItemsLimit));
            }

            if (minMultiplier == Integer.MAX_VALUE) {
                GALog.logger.error("Cannot calculate ratio of items for multi-recipe multiblocks");
                return null;
            }

            EUt = matchingRecipe.getEUt();
            duration = matchingRecipe.getDuration();

            int tierDiff = currentTier - tierNeeded;
            for (int i = 0; i < tierDiff; i++) {
                int attemptItemsLimit = this.getStack();
                attemptItemsLimit *= tierDiff - i;
                attemptItemsLimit = Math.max(1, attemptItemsLimit);
                attemptItemsLimit = Math.min(minMultiplier, attemptItemsLimit);
                List<CountableIngredient> newRecipeInputs = new ArrayList<>();
                List<FluidStack> newFluidInputs = new ArrayList<>();
                List<ItemStack> outputI = new ArrayList<>();
                List<FluidStack> outputF = new ArrayList<>();
                this.multiplyInputsAndOutputs(newRecipeInputs, newFluidInputs, outputI, outputF, matchingRecipe, attemptItemsLimit);
                // Get the currently selected RecipeMap

                RecipeBuilder<?> newRecipe = recipeMaps[getRecipeMapIndex()].recipeBuilder();
                copyChancedItemOutputs(newRecipe, matchingRecipe, attemptItemsLimit);

                // determine if there is enough room in the output to fit all of this
                // if there isn't, we can't process this recipe.
                List<ItemStack> totalOutputs = newRecipe.getChancedOutputs().stream().map(Recipe.ChanceEntry::getItemStack).collect(Collectors.toList());
                totalOutputs.addAll(outputI);
                boolean canFitOutputs = InventoryUtils.simulateItemStackMerge(totalOutputs, this.getOutputInventory());
                canFitOutputs = canFitOutputs && GTFluidUtils.simulateFluidStackMerge(outputF, this.getOutputTank());
                if (!canFitOutputs)
                    continue;

                newRecipe.inputsIngredients(newRecipeInputs)
                        .fluidInputs(newFluidInputs)
                        .outputs(outputI)
                        .fluidOutputs(outputF)
                        .EUt(Math.max(1, EUt * (getRecipeMapIndex() == 0 ? this.getEUtPercentage() : TJConfig.largeGreenhouse.eutPercentageTree) / 100))
                        .duration((int) Math.max(3, duration * ((getRecipeMapIndex() == 0 ? this.getDurationPercentage() : TJConfig.largeGreenhouse.durationPercentageTree) / 100.0)));

                return newRecipe.build().getResult();
            }
            return matchingRecipe;
        }
    }
}
