package tj.machines.multi.parallel;

import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.GATileEntities;
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

import static tj.TJRecipeMaps.PARALLEL_CHEMICAL_PLANT_RECIPES;
import static tj.TJRecipeMaps.PARALLEL_CHEMICAL_REACTOR_RECIPES;
import static tj.machines.multi.parallel.MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate;
import static tj.machines.multi.parallel.MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate2;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.motorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.pumpPredicate;
import static gregicadditions.recipes.GARecipeMaps.CHEMICAL_PLANT_RECIPES;
import static gregicadditions.recipes.GARecipeMaps.LARGE_CHEMICAL_RECIPES;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;

public class MetaTileEntityParallelAdvancedLargeChemicalReactor extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS,
            MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelAdvancedLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_CHEMICAL_REACTOR_RECIPES, PARALLEL_CHEMICAL_PLANT_RECIPES});
        this.recipeMapWorkable = new AdvancedParallelMultiblockChemicalReactorWorkableHandler(this);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelAdvancedLargeChemicalReactor(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                LARGE_CHEMICAL_RECIPES.getLocalizedName() + ", " + CHEMICAL_PLANT_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.advancedParallelChemicalReactor.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.advancedParallelChemicalReactor.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.advancedParallelChemicalReactor.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.advancedParallelChemicalReactor.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("tj.multiblock.parallel.chemical_plant.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.3"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, FRONT, DOWN);
        if (!(this.parallelLayer % 2 == 0)) {
            factoryPattern.aisle("C~~~C~XXXXX~~~~~~", "CCCCC~XXXXX~~~~~~", "C~~~C~XXXXX~~~~~~", "CCCCC~XXXXX~~~~~~", "C~~~C~XXXXX~~~~~~");
            factoryPattern.aisle("CCCCC~F~~~F~~~~~~", "CcccC~~~P~~~~~~~~", "CPPPPPPPpP~~~~~~~", "CcccC~~~P~~~~~~~~", "CCCCC~F~~~F~~~~~~");
            factoryPattern.aisle("C~~~C~F~~~F~~~~~~", "CPPPPPPPP~~~~~~~~", "MmmmC~~PpP~~~~~~~", "CPPPPPPPP~~~~~~~~", "C~~~C~F~~~F~~~~~~");
            factoryPattern.aisle("CCCCC~F~~~F~~~~~~", "CcccC~~~P~~~~~~~~", "CPPPPPPPpP~~~~~~~", "CcccC~~~P~~~~~~~~", "CCCCC~F~~~F~~~~~~");
        } else {
            factoryPattern.aisle("C~~~C~XXXXX~C~~~C", "CCCCC~XXXXX~CCCCC", "C~~~C~XXXXX~C~~~C", "CCCCC~XXXXX~CCCCC", "C~~~C~XXXXX~C~~~C");
            factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
            factoryPattern.aisle("C~~~C~F~~~F~C~~~C", "CPPPPPPPPPPPPPPPC", "MmmmC~~PpP~~CmmmM", "CPPPPPPPPPPPPPPPC", "C~~~C~F~~~F~C~~~C");
            factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
        }
        for (int count = 1; count < this.parallelLayer; count++) {
            if (count % 2 == 0) {
                factoryPattern.aisle("C~~~C~F~~~F~C~~~C", "CCCCC~~~P~~~CCCCC", "C~~~C~~PpP~~C~~~C", "CCCCC~~~P~~~CCCCC", "C~~~C~F~~~F~C~~~C");
                factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
                factoryPattern.aisle("C~~~C~F~~~F~C~~~C", "CPPPPPPPPPPPPPPPC", "MmmmC~~PpP~~CmmmM", "CPPPPPPPPPPPPPPPC", "C~~~C~F~~~F~C~~~C");
                factoryPattern.aisle("CCCCC~F~~~F~CCCCC", "CcccC~~~P~~~CcccC", "CPPPPPPPpPPPPPPPC", "CcccC~~~P~~~CcccC", "CCCCC~F~~~F~CCCCC");
            }
        }
        String[] controller = this.parallelLayer > 1 ?
                new String[]{"C~~~C~XXSXX~C~~~C", "CCCCC~XXXXX~CCCCC", "C~~~C~XXXXX~C~~~C", "CCCCC~XXXXX~CCCCC", "C~~~C~XXXXX~C~~~C"} :
                new String[]{"C~~~C~XXSXX~~~~~~", "CCCCC~XXXXX~~~~~~", "C~~~C~XXXXX~~~~~~", "CCCCC~XXXXX~~~~~~", "C~~~C~XXXXX~~~~~~"};

        return factoryPattern.aisle(controller)
                .where('S', this.selfPredicate())
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', statePredicate(this.getCasingState()))
                .where('P', statePredicate(GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Steel).getDefaultState()))
                .where('c', heatingCoilPredicate().or(heatingCoilPredicate2()))
                .where('p', pumpPredicate())
                .where('m', motorPredicate())
                .where('M', statePredicate(this.getCasingState()).or(abilityPartPredicate(REDSTONE_CONTROLLER)))
                .where('~', tile -> true)
                .build();
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
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        int min = Math.min(motor, pump);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
        this.energyBonus = context.getOrDefault("coilLevel", 0) * 5;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.advancedParallelChemicalReactor.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return GATileEntities.CHEMICAL_PLANT.getRecipeMaps();
    }

    private static class AdvancedParallelMultiblockChemicalReactorWorkableHandler extends ParallelGAMultiblockRecipeLogic {

        public AdvancedParallelMultiblockChemicalReactorWorkableHandler(ParallelRecipeMapMultiblockController tileEntity) {
            super(tileEntity, TJConfig.advancedParallelChemicalReactor.eutPercentage, TJConfig.advancedParallelChemicalReactor.durationPercentage,
                    TJConfig.advancedParallelChemicalReactor.chancePercentage, TJConfig.advancedParallelChemicalReactor.stack);
        }

        @Override
        public long getMaxVoltage() {
            return this.controller.getMaxVoltage();
        }

        @Override
        public boolean isBatching() {
            return ((MetaTileEntityParallelAdvancedLargeChemicalReactor) this.controller).recipeMapIndex == 0;
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
