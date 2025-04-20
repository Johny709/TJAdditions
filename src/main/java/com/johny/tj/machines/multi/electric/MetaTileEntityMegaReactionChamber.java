package com.johny.tj.machines.multi.electric;

import com.johny.tj.blocks.BlockPipeCasings;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJMultiRecipeMapMultiblockController;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.EmitterCasing;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.components.SensorCasing;
import gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController;
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
import gregtech.api.util.GTUtility;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

import static com.johny.tj.machines.multi.electric.MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate;
import static com.johny.tj.machines.multi.electric.MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate2;
import static com.johny.tj.textures.TJTextures.CHEMICALLY_INERT_FPM;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.client.ClientHandler.ORGANIC_REPLICATOR_OVERLAY;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregicadditions.recipes.GARecipeMaps.*;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.render.Textures.CHEMICAL_REACTOR_OVERLAY;

public class MetaTileEntityMegaReactionChamber extends TJMultiRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, IMPORT_FLUIDS, EXPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH};
    private int energyBonus;

    public MetaTileEntityMegaReactionChamber(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, LARGE_CHEMICAL_RECIPES, 100, 100, 100, 512, new RecipeMap[]{LARGE_CHEMICAL_RECIPES, CHEMICAL_PLANT_RECIPES, BIO_REACTOR_RECIPES});
        this.recipeMapWorkable = new MegaMultiblockRecipeMapController.MegaMultiblockRecipeLogic(this, EUtPercentage, durationPercentage, chancePercentage) {
            @Override
            public long getMaxVoltage() {
                return maxVoltage;
            }

            @Override
            protected void setupRecipe(Recipe recipe) {

                int[] resultOverclock = calculateOverclock(recipe.getEUt(), recipe.getDuration());
                this.progressTime = 1;

                // apply energy bonus
                resultOverclock[0] -= (int) (resultOverclock[0] * getEnergyBonus() * 0.01f);

                setMaxProgress(resultOverclock[1]);

                this.recipeEUt = resultOverclock[0];
                this.fluidOutputs = GTUtility.copyFluidList(recipe.getFluidOutputs());
                int tier = getMachineTierForRecipe(recipe);
                this.itemOutputs = GTUtility.copyStackList(recipe.getResultItemOutputs(getOutputInventory().getSlots(), random, tier));
                if (this.wasActiveAndNeedsUpdate) {
                    this.wasActiveAndNeedsUpdate = false;
                } else {
                    this.setActive(true);
                }
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMegaReactionChamber(metaTileEntityId);
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return ORGANIC_REPLICATOR_OVERLAY;
    }

    @Override
    public OrientedOverlayRenderer getRecipeMapOverlay(int recipeMapIndex) {
        return recipeMapIndex == 2 ? ORGANIC_REPLICATOR_OVERLAY : CHEMICAL_REACTOR_OVERLAY;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("###############", "###############", "###############", "###############", "###############", "#####PPHPP#####", "#####HHHHH#####", "#####PPHPP#####", "###############", "###############", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "######PCP######", "###PP#####PP###", "###CC#P#P#CC###", "###PP#####PP###", "######PCP######", "###############", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "######PCP######", "###############", "##P###GGG###P##", "##C###PCP###C##", "##P###GGG###P##", "###############", "######PCP######", "###############", "###############", "###############")
                .aisle("###############", "###############", "######PCP######", "###############", "######GGG######", "#P##GG!!!GG##P#", "#C##CC!!!CC##C#", "#P##GG!!!GG##P#", "######GGG######", "###############", "######PCP######", "###############", "###############")
                .aisle("###############", "######PCP######", "###############", "######GGG######", "####GG!!!GG####", "#P#G!!!!!!!G#P#", "#C#C!!!!!!!C#C#", "#P#G!!!!!!!G#P#", "####GG!!!GG####", "######GGG######", "###############", "######PCP######", "###############")
                .aisle("######PCP######", "###############", "######GGG######", "#####G!!!G#####", "####G!!!!!G####", "P##G!!!!!!!G##P", "H##C!!!!!!!C##H", "P##G!!!!!!!G##P", "####G!!!!!G####", "#####G!!!G#####", "######GGG######", "###############", "######PCP######")
                .aisle("#####PHHHP#####", "####P#ccc#P####", "###P#G!!!G#P###", "##P#G!!!!!G#P##", "#P#G!!!!!!!G#P#", "P#G!!!!!!!!!G#P", "HPP!!!!s!!!!PPH", "P#G!!!!!!!!!G#P", "#P#G!!!!!!!G#P#", "##P#G!!!!!G#P##", "###P#G!!!G#P###", "####P#ccc#P####", "#####PHHHP#####")
                .aisle("#####CHHHC#####", "####C#cFc#C####", "###C#G!!!G#C###", "##C#G!!!!!G#C##", "#C#G!!!!!!!G#C#", "H#G!!!!p!!!!G#H", "H#C!!!efe!!!C#H", "H#G!!!!p!!!!G#H", "#C#G!!!!!!!G#C#", "##C#G!!!!!G#C##", "###C#G!!!G#C###", "####C#cFc#C####", "#####CHHHC#####")
                .aisle("#####PHHHP#####", "####P#ccc#P####", "###P#G!!!G#P###", "##P#G!!!!!G#P##", "#P#G!!!!!!!G#P#", "P#G!!!!!!!!!G#P", "HPP!!!!s!!!!PPH", "P#G!!!!!!!!!G#P", "#P#G!!!!!!!G#P#", "##P#G!!!!!G#P##", "###P#G!!!G#P###", "####P#ccc#P####", "#####PHHHP#####")
                .aisle("######PCP######", "###############", "######GGG######", "#####G!!!G#####", "####G!!!!!G####", "P##G!!!!!!!G##P", "H##C!!!!!!!C##H", "P##G!!!!!!!G##P", "####G!!!!!G####", "#####G!!!G#####", "######GGG######", "###############", "######PCP######")
                .aisle("###############", "######PCP######", "###############", "######GGG######", "####GG!!!GG####", "#P#G!!!!!!!G#P#", "#C#C!!!!!!!C#C#", "#P#G!!!!!!!G#P#", "####GG!!!GG####", "######GGG######", "###############", "######PCP######", "###############")
                .aisle("###############", "###############", "######PCP######", "###############", "######GGG######", "#P##GG!!!GG##P#", "#C##CC!!!CC##C#", "#P##GG!!!GG##P#", "######GGG######", "###############", "######PCP######", "###############", "###############")
                .aisle("###############", "###############", "###############", "######PCP######", "###############", "##P###GGG###P##", "##C###PCP###C##", "##P###GGG###P##", "###############", "######PCP######", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "######PCP######", "###PP#####PP###", "###CC#P#P#CC###", "###PP#####PP###", "######PCP######", "###############", "###############", "###############", "###############")
                .aisle("###############", "###############", "###############", "###############", "###############", "#####PPHPP#####", "#####HHSHH#####", "#####PPHPP#####", "###############", "###############", "###############", "###############", "###############")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('G', statePredicate(GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS)))
                .where('P', statePredicate(TJMetaBlocks.PIPE_CASING.getState(BlockPipeCasings.PipeCasingType.FPM_PIPE_CASING)))
                .where('p', pumpPredicate())
                .where('s', sensorPredicate())
                .where('e', emitterPredicate())
                .where('f', fieldGenPredicate())
                .where('c', heatingCoilPredicate().or(heatingCoilPredicate2()))
                .where('F', frameworkPredicate().or(frameworkPredicate2()))
                .where('!', isAirPredicate())
                .where('#', tile -> true)
                .build();
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        int sensor = context.getOrDefault("Sensor", SensorCasing.CasingType.SENSOR_LV).getTier();
        int emitter = context.getOrDefault("Emitter", EmitterCasing.CasingType.EMITTER_LV).getTier();
        int fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV).getTier();
        int framework = context.getOrDefault("framework", GAMultiblockCasing.CasingType.TIERED_HULL_LV).getTier();
        int framework2 = context.getOrDefault("framework2", GAMultiblockCasing2.CasingType.TIERED_HULL_UHV).getTier();
        int min = Math.min(pump, Math.min(sensor, Math.min(emitter, Math.min(fieldGen, Math.min(framework, framework2)))));
        maxVoltage = (long) (Math.pow(4, min) * 8);
        energyBonus = context.getOrDefault("coilLevel", 0) * 5;
    }

    private IBlockState getCasingState() {
        return TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.CHEMICALLY_INERT_FPM_CASING);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return CHEMICALLY_INERT_FPM;
    }

    public int getEnergyBonus() {
        return energyBonus;
    }
}
