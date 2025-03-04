package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.MultiRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;

import static com.johny.tj.TJRecipeMaps.PARALLEL_CHEMICAL_PLANT_RECIPES;
import static com.johny.tj.TJRecipeMaps.PARALLEL_CHEMICAL_REACTOR_RECIPES;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;

public class MetaTileEntityAdvancedParallelLargeChemicalReactor extends ParallelRecipeMapMultiblockController {

    private final MultiRecipeMap chemicalPlantMap;
    private int recipeMapIndex;
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS,
            MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private int energyBonus;
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityAdvancedParallelLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_CHEMICAL_REACTOR_RECIPES);
        this.chemicalPlantMap = PARALLEL_CHEMICAL_PLANT_RECIPES;
        this.recipeMapWorkable = new AdvancedParallelMultiblockChemicalReactorWorkableHandler(this);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityAdvancedParallelLargeChemicalReactor(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                GARecipeMaps.LARGE_CHEMICAL_RECIPES.getLocalizedName() + ", " + GARecipeMaps.CHEMICAL_PLANT_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.advancedParallelChemicalReactor.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.advancedParallelChemicalReactor.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.advancedParallelChemicalReactor.stack));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.advancedParallelChemicalReactor.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("tj.multiblock.parallel.chemical_plant.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.3"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, FRONT, DOWN);
        if (!(parallelLayer % 2 == 0)) {
            factoryPattern.aisle("~~~~~~HHHHH~C~~~C", "~~~~~~HHHHH~CCCCC", "~~~~~~HHHHH~C~~~C", "~~~~~~HHHHH~CCCCC", "~~~~~~HHHHH~C~~~C");
            factoryPattern.aisle("~~~~~~F~~~F~CCCCC", "~~~~~~~~P~~~CcccC", "~~~~~~~PpPPPPPPPC", "~~~~~~~~P~~~CcccC", "~~~~~~F~~~F~CCCCC");
            factoryPattern.aisle("~~~~~~F~~~F~C~~~C", "~~~~~~~~PPPPPPPPC", "~~~~~~~PpP~~CmmmM", "~~~~~~~~PPPPPPPPC", "~~~~~~F~~~F~C~~~C");
            factoryPattern.aisle("~~~~~~F~~~F~CCCCC", "~~~~~~~~P~~~CcccC", "~~~~~~~PpPPPPPPPC", "~~~~~~~~P~~~CcccC", "~~~~~~F~~~F~CCCCC");
        } else {
            factoryPattern.aisle("C~~~C~HHHHH~C~~~C", "CCCCC~HHHHH~CCCCC", "C~~~C~HHHHH~C~~~C", "CCCCC~HHHHH~CCCCC", "C~~~C~HHHHH~C~~~C");
            factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
            factoryPattern.aisle("C~~~C~F~~~F~C~~~C", "CPPPPPPPPPPPPPPPC", "MmmmC~~PpP~~CmmmM", "CPPPPPPPPPPPPPPPC", "C~~~C~F~~~F~C~~~C");
            factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
        }
        for (int count = 1; count < parallelLayer; count++) {
            if (count % 2 == 0) {
                factoryPattern.aisle("C~~~C~F~~~F~C~~~C", "CCCCC~~~P~~~CCCCC", "C~~~C~~PpP~~C~~~C", "CCCCC~~~P~~~CCCCC", "C~~~C~F~~~F~C~~~C");
                factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
                factoryPattern.aisle("C~~~C~F~~~F~C~~~C", "CPPPPPPPPPPPPPPPC", "MmmmC~~PpP~~CmmmM", "CPPPPPPPPPPPPPPPC", "C~~~C~F~~~F~C~~~C");
                factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
            }
        }
        if (parallelLayer > 1)
            factoryPattern.aisle("C~~~C~HHSHH~C~~~C", "CCCCC~HHHHH~CCCCC", "C~~~C~HHHHH~C~~~C", "CCCCC~HHHHH~CCCCC", "C~~~C~HHHHH~C~~~C");
        else
            factoryPattern.aisle("~~~~~~HHSHH~C~~~C", "~~~~~~HHHHH~CCCCC", "~~~~~~HHHHH~C~~~C", "~~~~~~HHHHH~CCCCC", "~~~~~~HHHHH~C~~~C");
        factoryPattern.where('S', selfPredicate())
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', statePredicate(getCasingState()))
                .where('P', statePredicate(GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Steel).getDefaultState()))
                .where('c', MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate().or(MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate2()))
                .where('p', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('M', statePredicate(getCasingState()).or(abilityPartPredicate(REDSTONE_CONTROLLER)))
                .where('~', tile -> true);
        return factoryPattern.build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.CHEMICALLY_INERT;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.CHEMICAL_REACTOR_OVERLAY;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.universal.energy_usage", 100 - energyBonus).setStyle(new Style().setColor(TextFormatting.AQUA)));
            textList.add(new TextComponentString(I18n.format("gregtech.universal.tooltip.voltage_in", maxVoltage, GAValues.VN[GTUtility.getGATierByVoltage(maxVoltage)])));
        }
        textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(getRecipeMapIndex() == 0 ? withButton(new TextComponentString(GARecipeMaps.LARGE_CHEMICAL_RECIPES.getLocalizedName()), "chemicalReactor")
                        : withButton(new TextComponentString(GARecipeMaps.CHEMICAL_PLANT_RECIPES.getLocalizedName()), "chemicalPlant")));
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        this.recipeMapWorkable.previousRecipe.clear();
        this.recipeMapIndex = componentData.equals("chemicalReactor") ? 1 : 0;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        MotorCasing.CasingType motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV);
        PumpCasing.CasingType pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV);
        int min = Math.min(motor.getTier(), pump.getTier());
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
        if (getWorld() == null)
            return;
        if (this.structurePattern == null)
            this.structurePattern = createStructurePattern();
        super.checkStructurePattern();
    }

    @Override
    protected void reinitializeStructurePattern() {
        this.structurePattern = null;
    }

    public int getEnergyBonus() {
        return energyBonus;
    }

    public int getRecipeMapIndex() {
        return recipeMapIndex;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.advancedParallelChemicalReactor.maximumParallel;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagCompound tagCompound = super.writeToNBT(data);
        tagCompound.setInteger("RecipeMapIndex", this.recipeMapIndex);
        return tagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.recipeMapIndex = data.getInteger("RecipeMapIndex");
    }

    private static class AdvancedParallelMultiblockChemicalReactorWorkableHandler extends ParallelGAMultiblockRecipeLogic {

        public AdvancedParallelMultiblockChemicalReactorWorkableHandler(ParallelRecipeMapMultiblockController tileEntity) {
            super(tileEntity, GARecipeMaps.LARGE_CHEMICAL_RECIPES, TJConfig.advancedParallelChemicalReactor.eutPercentage, TJConfig.advancedParallelChemicalReactor.durationPercentage,
                    TJConfig.advancedParallelChemicalReactor.chancePercentage, TJConfig.advancedParallelChemicalReactor.stack);
        }

        @Override
        public long getMaxVoltage() {
            return this.controller.getMaxVoltage();
        }

        @Override
        public boolean isBatching() {
            return ((MetaTileEntityAdvancedParallelLargeChemicalReactor) this.controller).recipeMapIndex == 0;
        }

        @Override
        protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
            return ((MetaTileEntityAdvancedParallelLargeChemicalReactor) this.controller).getRecipeMapIndex() == 0
                    ? super.findRecipe(maxVoltage, inputs, fluidInputs, useOptimizedRecipeLookUp)
                    : ((MetaTileEntityAdvancedParallelLargeChemicalReactor) this.controller).chemicalPlantMap.findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp, occupiedRecipes, distinct);
        }

        @Override
        public RecipeMap<?> getRecipeMap() {
            return ((MetaTileEntityAdvancedParallelLargeChemicalReactor) this.controller).recipeMapIndex == 0
                    ? super.getRecipeMap()
                    : GARecipeMaps.CHEMICAL_PLANT_RECIPES;
        }

        @Override
        protected void setupRecipe(Recipe recipe, int i) {
            int energyBonus = ((MetaTileEntityAdvancedParallelLargeChemicalReactor) this.controller).getEnergyBonus();
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
