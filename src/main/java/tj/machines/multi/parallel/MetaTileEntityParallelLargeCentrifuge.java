package tj.machines.multi.parallel;

import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.recipes.GARecipeMaps;
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
import java.util.List;

import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.GATileEntities.*;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static tj.TJRecipeMaps.*;
import static gregicadditions.recipes.GARecipeMaps.LARGE_CENTRIFUGE_RECIPES;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.THERMAL_CENTRIFUGE_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;

public class MetaTileEntityParallelLargeCentrifuge extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, MAINTENANCE_HATCH, IMPORT_FLUIDS, EXPORT_FLUIDS, REDSTONE_CONTROLLER};

    public MetaTileEntityParallelLargeCentrifuge(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_CENTRIFUGE_RECIPES, PARALLEL_THERMAL_CENTRIFUGE_RECIPES, PARALLEL_GAS_CENTRIFUGE_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, () -> TJConfig.parallelLargeCentrifuge.eutPercentage,
                () -> TJConfig.parallelLargeCentrifuge.durationPercentage, () -> TJConfig.parallelLargeCentrifuge.chancePercentage, () -> TJConfig.parallelLargeCentrifuge.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }

            @Override
            protected void setupRecipe(Recipe recipe, int i) {
                int energyBonus = this.controller.getEUBonus();
                int resultOverclock = this.overclockManager.getEUt();
                resultOverclock -= (int) (resultOverclock * energyBonus * 0.01f);
                this.overclockManager.setEUt(resultOverclock);
                super.setupRecipe(recipe, i);
            }
        };
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
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, FRONT, DOWN);
        for (int layer = 0; layer < this.parallelLayer; layer++) {

            String entityS = layer == this.parallelLayer - 1 ? "~XSX~" : "~XGX~";

            factoryPattern.aisle("~XXX~", "XCCCX", "XCmCX", "XCCCX", "~XXX~");
            factoryPattern.aisle("XXXXX", "X###X", "X#P#X", "X###X", "XXXXX");
            factoryPattern.aisle(entityS, "X###X", "G#P#G", "X###X", "~XGX~");
            factoryPattern.aisle("XXXXX", "X###X", "X#P#X", "X###X", "XXXXX");
        }
        return factoryPattern.aisle("~XXX~", "XCCCX", "XCmCX", "XCCCX", "~XXX~")
                .validateLayer(2, context -> context.getInt("RedstoneControllerAmount") <= 1)
                .where('S', this.selfPredicate())
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate().or(MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate2()))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE)))
                .where('G', statePredicate(MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING)))
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
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
}
