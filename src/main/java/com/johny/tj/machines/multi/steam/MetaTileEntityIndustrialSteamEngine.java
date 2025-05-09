package com.johny.tj.machines.multi.steam;

import com.johny.tj.builder.handlers.TJFuelRecipeLogic;
import com.johny.tj.builder.multicontrollers.MultiblockDisplaysUtility;
import com.johny.tj.builder.multicontrollers.TJFueledMultiblockController;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.blocks.BlockTurbineCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

import static net.minecraft.util.text.TextFormatting.*;

public class MetaTileEntityIndustrialSteamEngine extends TJFueledMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS,
            GregicAdditionsCapabilities.STEAM, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private float efficiency;
    private int tier;

    public MetaTileEntityIndustrialSteamEngine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, RecipeMaps.STEAM_TURBINE_FUELS, 0);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder metaTileEntityHolder) {
        return new MetaTileEntityIndustrialSteamEngine(metaTileEntityId);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.industrial_steam_engine.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (!isStructureFormed()) {
            MultiblockDisplaysUtility.isInvalid(textList, isStructureFormed());
            return;
        }

        textList.add(new TextComponentTranslation("gregtech.universal.tooltip.efficiency", efficiency * 100).setStyle(new Style().setColor(AQUA)));

        TJFuelRecipeLogic recipeLogic = (TJFuelRecipeLogic) workableHandler;
        if (recipeLogic.isActive()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.generation_eu", recipeLogic.getMaxVoltage()));
            int currentProgress = (int) Math.floor(recipeLogic.getProgress() / (recipeLogic.getMaxProgress() * 1.0) * 100);
            textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
        }

        ITextComponent isWorkingText = !recipeLogic.isWorkingEnabled() ? new TextComponentTranslation("gregtech.multiblock.work_paused")
                : !recipeLogic.isActive() ? new TextComponentTranslation("gregtech.multiblock.idling")
                : new TextComponentTranslation("gregtech.multiblock.running");

        isWorkingText.getStyle().setColor(!recipeLogic.isWorkingEnabled() ? YELLOW : !recipeLogic.isActive() ? WHITE : GREEN);
        textList.add(isWorkingText);

        if (energyContainer.getEnergyCanBeInserted() < recipeLogic.getProduction())
            textList.add(new TextComponentTranslation("machine.universal.output.full").setStyle(new Style().setColor(RED)));
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        return new TJFuelRecipeLogic(this, recipeMap, () -> energyContainer, () -> importFluidHandler, 0) {

            @Override
            protected int calculateFuelAmount(FuelRecipe currentRecipe) {
                return (int) ((super.calculateFuelAmount(currentRecipe) * 2) / ((MetaTileEntityIndustrialSteamEngine) metaTileEntity).getEfficiency());
            }

            @Override
            public long getMaxVoltage() {
                return GTValues.V2[((MetaTileEntityIndustrialSteamEngine) metaTileEntity).getTier()];
            }

            @Override
            protected int calculateRecipeDuration(FuelRecipe currentRecipe) {
                return super.calculateRecipeDuration(currentRecipe) * 2;
            }

            @Override
            protected boolean shouldVoidExcessiveEnergy() {
                return false;
            }
        };
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.tier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int tier = this.tier - 1;
        efficiency = Math.max(0, (float) (1.0 - ((double) tier / 10)));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("~CC", "CEC", "~CC")
                .aisle("CCC", "CRC", "CCC")
                .aisle("~CC", "CGC", "~CC")
                .aisle("~CC", "~SC", "~CC")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('E', abilityPartPredicate(MultiblockAbility.OUTPUT_ENERGY))
                .where('G', statePredicate(MetaBlocks.TURBINE_CASING.getState(BlockTurbineCasing.TurbineCasingType.BRONZE_GEARBOX)))
                .where('R', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TUMBAGA);
    }

    public float getEfficiency() {
        return efficiency;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.TUMBAGA_CASING;
    }

}
