package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
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
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Predicate;

import static com.johny.tj.TJRecipeMaps.PARALLEL_ELECTROLYZER_RECIPES;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class MetaTileEntityParallelLargeElectrolyzer extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeElectrolyzer(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_ELECTROLYZER_RECIPES);
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, RecipeMaps.ELECTROLYZER_RECIPES, TJConfig.parallelLargeElectrolyzer.eutPercentage,
                TJConfig.parallelLargeElectrolyzer.durationPercentage, TJConfig.parallelLargeElectrolyzer.chancePercentage, TJConfig.parallelLargeElectrolyzer.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeElectrolyzer(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                RecipeMaps.ELECTROLYZER_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeElectrolyzer.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeElectrolyzer.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeElectrolyzer.stack));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeElectrolyzer.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, DOWN, BACK);
        for (int count = 0; count < parallelLayer; count++) {
            factoryPattern.aisle("CC#CC", "HHGHH", "HHGHH", "HHGHH");
            factoryPattern.aisle("C###C", "MMGMM", "Mm#PM", "MMGMM");
            factoryPattern.validateLayer(1 + count * 2, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("CC#CC", "HHGHH", "HHSHH", "HHHHH")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)).or(machineControllerPredicate))
                .where('G', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.STEEL_PIPE)))
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('P', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('#', isAirPredicate());
        return factoryPattern.build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.POTIN);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.POTIN_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.ELECTROLYZER_OVERLAY;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentString(I18n.format("gregtech.universal.tooltip.voltage_in", maxVoltage, GAValues.VN[GTUtility.getGATierByVoltage(maxVoltage)])));
        }
        textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(new TextComponentString(RecipeMaps.ELECTROLYZER_RECIPES.getLocalizedName())
                        .setStyle(new Style().setColor(TextFormatting.YELLOW))));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        int min = Math.min(motor, pump);
        maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        maxVoltage = 0;
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

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeElectrolyzer.maximumParallel;
    }
}
