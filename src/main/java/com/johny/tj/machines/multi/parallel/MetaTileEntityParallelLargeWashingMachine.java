package com.johny.tj.machines.multi.parallel;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing1;
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
import gregtech.common.blocks.BlockBoilerCasing;
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

import static com.johny.tj.TJRecipeMaps.PARALLEL_LARGE_WASHING_MACHINE_RECIPEMAP;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.recipes.GARecipeMaps.SIMPLE_ORE_WASHER_RECIPES;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.*;

public class MetaTileEntityParallelLargeWashingMachine extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeWashingMachine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_LARGE_WASHING_MACHINE_RECIPEMAP);
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, null, TJConfig.parallelLargeWashingMachine.eutPercentage,
                TJConfig.parallelLargeWashingMachine.durationPercentage, TJConfig.parallelLargeWashingMachine.chancePercentage, TJConfig.parallelLargeWashingMachine.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeWashingMachine(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                ORE_WASHER_RECIPES.getLocalizedName() + ", " + CHEMICAL_BATH_RECIPES.getLocalizedName()
                        + ", " + SIMPLE_ORE_WASHER_RECIPES.getLocalizedName() + ", " + RecipeMaps.AUTOCLAVE_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeWashingMachine.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeWashingMachine.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeWashingMachine.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeWashingMachine.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, DOWN, BACK);
        factoryPattern.aisle("~CCC~", "HHHHH", "HmHmH", "HHHHH");
        for (int count = 0; count < this.parallelLayer; count++) {
            if (count != 0)
                factoryPattern.aisle("~HHH~", "H###H", "HP#PH", "HHHHH");
            factoryPattern.aisle("CGCGC", "H###H", "HP#PH", "HHHHH");
            factoryPattern.aisle("CGCGC", "M###M", "MP#PM", "MMMMM");
            factoryPattern.aisle("CGCGC", "H###H", "HP#PH", "HHHHH");
            factoryPattern.validateLayer(2 + count * 4, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("~CCC~", "HHHHH", "HmSmH", "HHHHH")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)).or(machineControllerPredicate))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('G', statePredicate(GAMetaBlocks.TRANSPARENT_CASING.getState(GATransparentCasing.CasingType.OSMIRIDIUM_GLASS)))
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
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
        switch (getRecipeMapIndex()) {
            case 1: return Textures.CHEMICAL_BATH_OVERLAY;
            case 3: return Textures.AUTOCLAVE_OVERLAY;
            default: return Textures.ORE_WASHER_OVERLAY;
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int min = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.maxVoltage = 0;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeWashingMachine.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{ORE_WASHER_RECIPES, CHEMICAL_BATH_RECIPES, SIMPLE_ORE_WASHER_RECIPES, AUTOCLAVE_RECIPES};
    }
}
