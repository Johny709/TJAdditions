package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.MultipleRecipeMapMultiblockController;
import com.johny.tj.capability.impl.MultiGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.capabilities.impl.GARecipeMapMultiblockController;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.machines.multi.simple.TileEntityLargeChemicalReactor;
import gregicadditions.recipes.GARecipeMaps;
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
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.function.Predicate;

import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregtech.api.unification.material.Materials.Steel;

public class MetaTileEntityParallelLargeChemicalReactor extends MultipleRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private int energyBonus;

    public MetaTileEntityParallelLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, GARecipeMaps.LARGE_CHEMICAL_RECIPES);
        this.recipeMapWorkable = new ParallelChemicalReactorWorkableHandler(this, TJConfig.parallelLCR.maximumRecipeParallel);
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
                .where('c', TileEntityLargeChemicalReactor.heatingCoilPredicate().or(TileEntityLargeChemicalReactor.heatingCoilPredicate2()))
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

    @Override
    public MetaTileEntityParallelLargeChemicalReactor createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeChemicalReactor(metaTileEntityId);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        PumpCasing.CasingType pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV);
        int min = pump.getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
        int temperature = context.getOrDefault("blastFurnaceTemperature", 0);

        switch (temperature){

            case 2700:
                energyBonus = 5;
                break;
            case 3600:
                energyBonus = 10;
                break;
            case 4500:
                energyBonus = 15;
                break;
            case 5400:
                energyBonus = 20;
                break;
            case 7200:
                energyBonus = 25;
                break;
            case 8600:
                energyBonus = 30;
                break;
            case 9600:
                energyBonus = 35;
                break;
            case 10700:
                energyBonus = 40;
                break;
            case 11200:
                energyBonus = 45;
                break;
            case 12600:
                energyBonus = 50;
                break;
            case 14200:
                energyBonus = 55;
                break;
            case 28400:
                energyBonus = 60;
                break;
            case 56800:
                energyBonus = 65;
                break;
            default:
                energyBonus = 0;
        }
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

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Parallel", this.parallelLayer);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.parallelLayer = data.getInteger("Parallel");
    }












    private static class ParallelChemicalReactorWorkableHandler extends MultiGAMultiblockRecipeLogic {

        MetaTileEntityParallelLargeChemicalReactor chemicalReactor;

        public ParallelChemicalReactorWorkableHandler(MetaTileEntityParallelLargeChemicalReactor tileEntity, int size) {
            super(tileEntity, size);
            this.chemicalReactor = tileEntity;
        }

        @Override
        protected void setupRecipe(Recipe recipe, int i) {
            int energyBonus = chemicalReactor.energyBonus;
            long maxVoltage = chemicalReactor.maxVoltage;

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
            if (this.wasActiveAndNeedsUpdate[i]) {
                this.wasActiveAndNeedsUpdate[i] = false;
            } else {
                this.setActive(true, i);
            }
        }

        @Override
        protected int[] calculateOverclock(int EUt, long voltage, int duration) {
            int numMaintenanceProblems = (this.metaTileEntity instanceof GARecipeMapMultiblockController) ?
                    ((GARecipeMapMultiblockController) metaTileEntity).getNumProblems() : 0;

            double maintenanceDurationMultiplier = 1.0 + (0.2 * numMaintenanceProblems);
            int durationModified = (int) (duration * maintenanceDurationMultiplier);

            boolean negativeEU = EUt < 0;
            int tier = getOverclockingTier(voltage);
            if (GAValues.V[tier] <= EUt || tier == 0)
                return new int[]{EUt, durationModified};
            if (negativeEU)
                EUt = -EUt;
            int resultEUt = EUt;
            double resultDuration = durationModified;
            //do not overclock further if duration is already too small
            while (resultDuration >= 1 && resultEUt <= GAValues.V[tier - 1]) {
                resultEUt *= 4;
                resultDuration /= 4;
            }
            previousRecipeDuration = (int) resultDuration;
            return new int[]{negativeEU ? -resultEUt : resultEUt, (int) Math.ceil(resultDuration)};
        }
    }
}
