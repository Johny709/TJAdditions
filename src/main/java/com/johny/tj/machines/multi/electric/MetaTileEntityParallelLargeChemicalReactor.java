package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.MultipleRecipeMapMultiblockController;
import com.johny.tj.capability.impl.MultiblockMultiRecipeLogic;
import com.johny.tj.recipes.RecipeLoader;
import gregicadditions.GAConfig;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAHeatingCoil;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregtech.api.unification.material.Materials.Steel;

public class MetaTileEntityParallelLargeChemicalReactor extends MultipleRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private int energyBonus;

    public MetaTileEntityParallelLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, RecipeLoader.LARGE_CHEMICAL_REACTOR_RECIPES);
        this.recipeMapWorkable = new ParallelChemicalReactorWorkableHandler(this);
        this.isWorkingEnabled = false;
    }

    @Override
    public MetaTileEntityParallelLargeChemicalReactor createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeChemicalReactor(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.3"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));

        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(BlockPattern.RelativeDirection.LEFT, BlockPattern.RelativeDirection.FRONT, BlockPattern.RelativeDirection.DOWN);

        factoryPattern.aisle("HHHHH", "HHHHH", "HHHHH", "HHHHH", "HHHHH");
        for (int count = 0; count < this.parallelLayer; count++) {
            factoryPattern.aisle("F###F", "#PPP#", "#PBP#", "#PPP#", "F###F");
            factoryPattern.aisle("F###F", "#CCC#", "#CcC#", "#CCC#", "F###F");
            factoryPattern.validateLayer(2 + count * 2, (context) -> context.getInt("RedstoneControllerAmount") <= 1);
        }

        factoryPattern.aisle("F###F", "#PPP#", "#PBP#", "#PPP#", "F###F");
        factoryPattern.aisle("HHSHH", "HHHHH", "HHHHH", "HHHHH", "HHHHH")
                .where('S', selfPredicate())
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', statePredicate(getCasingState()).or(machineControllerPredicate))
                .where('c', heatingCoilPredicate().or(heatingCoilPredicate2()))
                .where('P', statePredicate(GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Steel).getDefaultState()))
                .where('B', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('#', (tile) -> true);
        return factoryPattern.build();
    }

    protected IBlockState getCasingState() {
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
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.universal.energy_usage", 100 - energyBonus).setStyle(new Style().setColor(TextFormatting.AQUA)));
            textList.add(new TextComponentString(I18n.format("gregtech.universal.tooltip.voltage_in", maxVoltage, GAValues.VN[GTUtility.getGATierByVoltage(maxVoltage)])));
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        PumpCasing.CasingType pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV);
        int min = pump.getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
        energyBonus = context.getOrDefault("coilLevel", 0) * 5;
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.maxVoltage = 0;
    }

    @Override
    protected void checkStructurePattern() {
        try {
            if (!getWorld().isRemote && getWorld() != null) {
                if (this.structurePattern == null)
                    this.structurePattern = createStructurePattern();
                super.checkStructurePattern();
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void reinitializeStructurePattern() {
        this.structurePattern = null;
    }

    public int getEnergyBonus() {
        return energyBonus;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelChemicalReactor.maximumParallel;
    }

    private static class ParallelChemicalReactorWorkableHandler extends MultiblockMultiRecipeLogic {

        public ParallelChemicalReactorWorkableHandler(MetaTileEntityParallelLargeChemicalReactor tileEntity) {
            super(tileEntity, 64);
        }

        @Override
        public long getMaxVoltage() {
            return this.controller.getMaxVoltage();
        }

        @Override
        protected void setupRecipe(Recipe recipe, int i) {
            int energyBonus = ((MetaTileEntityParallelLargeChemicalReactor) this.controller).getEnergyBonus();
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
