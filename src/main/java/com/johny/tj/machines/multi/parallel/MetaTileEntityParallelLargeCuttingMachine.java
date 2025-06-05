package com.johny.tj.machines.multi.parallel;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.ParallelRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

import static com.johny.tj.TJRecipeMaps.PARALLEL_CUTTER_RECIPES;
import static com.johny.tj.TJRecipeMaps.PARALLEL_LATHE_RECIPES;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.conveyorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.motorPredicate;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.CUTTER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.LATHE_RECIPES;
import static gregtech.api.render.Textures.CUTTER_OVERLAY;
import static gregtech.api.render.Textures.LATHE_OVERLAY;

public class MetaTileEntityParallelLargeCuttingMachine extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, IMPORT_FLUIDS, MAINTENANCE_HATCH, INPUT_ENERGY};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeCuttingMachine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_CUTTER_RECIPES, PARALLEL_LATHE_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, TJConfig.parallelLargeCuttingMachine.eutPercentage, TJConfig.parallelLargeCuttingMachine.durationPercentage,
                TJConfig.parallelLargeCuttingMachine.chancePercentage, TJConfig.parallelLargeCuttingMachine.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeCuttingMachine(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                CUTTER_RECIPES.getLocalizedName() + ", " +
                        LATHE_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeCanningMachine.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeCanningMachine.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeCanningMachine.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeCanningMachine.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        if (this.parallelLayer % 2 == 0) {
            factoryPattern.aisle("CCCCCCC", "C#CCC#C", "C#C~C#C");
            factoryPattern.aisle("CcCCCcC", "CMCCCMC", "C#C~C#C");
        } else {
            factoryPattern.aisle("~~CCCCC", "~~CCC#C", "~~~~C#C");
            factoryPattern.aisle("~~CCCcC", "~~CCCMC", "~~~~C#C");
        }
        for (int layer = 1; layer < this.parallelLayer; layer++) {
            if (layer % 2 == 0) {
                factoryPattern.aisle("CCCCCCC", "C#CCC#C", "C#C~C#C");
                factoryPattern.aisle("CcCCCcC", "CMCCCMC", "C#C~C#C");
            }
        }
        if (this.parallelLayer > 1) {
            factoryPattern.aisle("CCCCCCC", "C#CSC#C", "C#C~C#C");
        } else {
            factoryPattern.aisle("~~CCCCC", "~~CSC#C", "~~~~C#C");
        }
        return factoryPattern.where('S', this.selfPredicate())
                .where('C', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('c', conveyorPredicate())
                .where('M', motorPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private static IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.MARAGING_STEEL_250);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int conveyor = context.getOrDefault("Conveyor", ConveyorCasing.CasingType.CONVEYOR_LV).getTier();
        int min = Math.min(motor, conveyor);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.MARAGING_STEEL_250_CASING;
    }

    @Override
    protected @NotNull OrientedOverlayRenderer getFrontOverlay() {
        return this.getRecipeMapIndex() == 0 ? CUTTER_OVERLAY : LATHE_OVERLAY;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeCuttingMachine.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return GATileEntities.LARGE_CUTTING.getRecipeMaps();
    }
}
