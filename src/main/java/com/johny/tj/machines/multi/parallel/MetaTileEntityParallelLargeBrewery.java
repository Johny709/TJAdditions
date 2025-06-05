package com.johny.tj.machines.multi.parallel;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.ParallelRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
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
import java.util.function.Predicate;

import static com.johny.tj.TJRecipeMaps.*;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.GAMaterials.Grisium;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.recipes.GARecipeMaps.CHEMICAL_DEHYDRATOR_RECIPES;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.BREWING_RECIPES;

public class MetaTileEntityParallelLargeBrewery extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, IMPORT_FLUIDS, EXPORT_FLUIDS, MAINTENANCE_HATCH};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeBrewery(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_BREWING_MACHINE_RECIPES, PARALLEL_FERMENTING_RECIPES, PARALLEL_CHEMICAL_DEHYDRATOR_RECIPES, PARALLEL_CRACKING_UNIT_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, TJConfig.parallelLargeBrewery.eutPercentage,
                TJConfig.parallelLargeBrewery.durationPercentage, TJConfig.parallelLargeBrewery.chancePercentage, TJConfig.parallelLargeBrewery.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeBrewery(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                BREWING_RECIPES.getLocalizedName() + ", " + RecipeMaps.FERMENTING_RECIPES.getLocalizedName()
                        + ", " + CHEMICAL_DEHYDRATOR_RECIPES.getLocalizedName() + ", " + RecipeMaps.CRACKING_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeBrewery.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeBrewery.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeBrewery.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeBrewery.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, DOWN, BACK);
        factoryPattern.aisle("~CCC~", "CHHHC", "CHmHC", "CHHHC", "F~C~F", "CCCCC");
        for (int count = 0; count < this.parallelLayer; count++) {
            factoryPattern.aisle("~~C~~", "~G#G~", "C#P#C", "~G#G~", "~~C~~", "~CCC~");
            factoryPattern.aisle("~~C~~", "~G#G~", "p#P#p", "~G#G~", "~~M~~", "~MMM~");
            factoryPattern.aisle("~~C~~", "~G#G~", "C#P#C", "~G#G~", "~~C~~", "~CCC~");
            factoryPattern.validateLayer(2 + count * 3, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("~CCC~", "CHHHC", "CHmHC", "CHSHC", "F~C~F", "CCCCC")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(getCasingState()).or(machineControllerPredicate))
                .where('G', statePredicate(GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS)))
                .where('P', statePredicate(GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Grisium).getDefaultState()))
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('p', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true);

        return factoryPattern.build();
    }

    private static IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.GRISIUM);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.GRISIUM_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        switch (this.getRecipeMapIndex()) {
            case 1: return Textures.FERMENTER_OVERLAY;
            case 2: return Textures.SIFTER_OVERLAY;
            case 3: return Textures.CRACKING_UNIT_OVERLAY;
            default: return Textures.BREWERY_OVERLAY;
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        int min = Math.min(motor, pump);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeBrewery.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return GATileEntities.LARGE_BREWERY.getRecipeMaps();
    }
}
