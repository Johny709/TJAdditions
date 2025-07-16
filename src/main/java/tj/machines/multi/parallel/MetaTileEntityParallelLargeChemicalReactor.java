package tj.machines.multi.parallel;

import gregtech.api.capability.IEnergyContainer;
import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelMultiblockRecipeLogic;
import gregicadditions.GAConfig;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAHeatingCoil;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static gregtech.api.metatileentity.multiblock.MultiblockAbility.INPUT_ENERGY;
import static tj.TJRecipeMaps.PARALLEL_CHEMICAL_REACTOR_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.recipes.GARecipeMaps.LARGE_CHEMICAL_RECIPES;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.CHEMICAL_RECIPES;
import static gregtech.api.unification.material.Materials.Steel;

public class MetaTileEntityParallelLargeChemicalReactor extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};

    public MetaTileEntityParallelLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_CHEMICAL_REACTOR_RECIPES});
        this.recipeMapWorkable = new ParallelMultiblockChemicalReactorWorkableHandler(this);
    }

    @Override
    public MetaTileEntityParallelLargeChemicalReactor createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeChemicalReactor(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.3"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, FRONT, DOWN);

        factoryPattern.aisle("XXXXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX");
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("F###F", "#MMM#", "#MCM#", "#MMM#", "F###F");
            factoryPattern.aisle("F###F", "#PPP#", "#PcP#", "#PPP#", "F###F");
        }
        return factoryPattern
                .aisle("F###F", "#MMM#", "#MCM#", "#MMM#", "F###F")
                .aisle("XXSXX", "XXXXX", "XXXXX", "XXXXX", "XXXXX")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(this.getCasingState()).or(abilityPartPredicate(REDSTONE_CONTROLLER)))
                .where('c', heatingCoilPredicate().or(heatingCoilPredicate2()))
                .where('P', statePredicate(GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Steel).getDefaultState()))
                .where('#', (tile) -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.CHEMICALLY_INERT;
    }

    public static Predicate<BlockWorldState> heatingCoilPredicate() {
        return blockWorldState -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof BlockWireCoil))
                return false;
            BlockWireCoil blockWireCoil = (BlockWireCoil) blockState.getBlock();
            BlockWireCoil.CoilType coilType = blockWireCoil.getState(blockState);
            if (Arrays.asList(GAConfig.multis.heatingCoils.gtceHeatingCoilsBlacklist).contains(coilType.getName()))
                return false;

            int coilLevel = coilType.ordinal();
            int currentLevel = blockWorldState.getMatchContext().getOrPut("coilLevel", coilLevel);

            BlockWireCoil.CoilType currentCoilType = blockWorldState.getMatchContext().getOrPut("coilType", coilType);

            return currentLevel == coilLevel && coilType.equals(currentCoilType);
        };
    }

    public static Predicate<BlockWorldState> heatingCoilPredicate2() {
        return blockWorldState -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof GAHeatingCoil))
                return false;
            GAHeatingCoil blockWireCoil = (GAHeatingCoil) blockState.getBlock();
            GAHeatingCoil.CoilType coilType = blockWireCoil.getState(blockState);
            if (Arrays.asList(GAConfig.multis.heatingCoils.gregicalityheatingCoilsBlacklist).contains(coilType.getName()))
                return false;

            int coilLevel = coilType.ordinal() + 8;
            int currentLevel = blockWorldState.getMatchContext().getOrPut("coilLevel", coilLevel);

            GAHeatingCoil.CoilType currentCoilType = blockWorldState.getMatchContext().getOrPut("gaCoilType", coilType);

            return currentLevel == coilLevel && coilType.equals(currentCoilType);
        };
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.maxVoltage = this.getAbilities(INPUT_ENERGY).stream()
                .mapToLong(IEnergyContainer::getInputVoltage)
                .max()
                .orElse(0);
        long amps = this.getAbilities(INPUT_ENERGY).stream()
                .filter(energy -> energy.getInputVoltage() == this.maxVoltage)
                .mapToLong(IEnergyContainer::getInputAmperage)
                .sum() / this.parallelLayer;
        amps = Math.min(1024, amps);
        while (amps >= 4) {
            amps /= 4;
            this.maxVoltage *= 4;
        }
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelChemicalReactor.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{CHEMICAL_RECIPES, LARGE_CHEMICAL_RECIPES};
    }

    private static class ParallelMultiblockChemicalReactorWorkableHandler extends ParallelMultiblockRecipeLogic {

        public ParallelMultiblockChemicalReactorWorkableHandler(MetaTileEntityParallelLargeChemicalReactor tileEntity) {
            super(tileEntity, TJConfig.machines.recipeCacheCapacity);
        }

        @Override
        public long getMaxVoltage() {
            return this.controller.getMaxVoltage();
        }

        @Override
        public double getDurationOverclock() {
            return 4;
        }

        @Override
        protected void setupRecipe(Recipe recipe, int i) {
            int energyBonus = this.controller.getEUBonus();
            int resultOverclock = this.overclockManager.getEUt();
            resultOverclock -= (int) (resultOverclock * energyBonus * 0.01f);
            this.overclockManager.setEUt(resultOverclock);
            super.setupRecipe(recipe, i);
        }
    }
}
