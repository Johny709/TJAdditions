package com.johny.tj.machines.multi.steam;

import com.johny.tj.builder.handlers.TJFuelRecipeLogic;
import com.johny.tj.builder.multicontrollers.MultiblockDisplaysUtility;
import com.johny.tj.builder.multicontrollers.TJFueledMultiblockController;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
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
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        boolean hasOutputEnergy = abilities.containsKey(MultiblockAbility.OUTPUT_ENERGY);
        boolean hasInputFluid = abilities.containsKey(MultiblockAbility.IMPORT_FLUIDS);
        boolean hasSteamInput = abilities.containsKey(GregicAdditionsCapabilities.STEAM);

        return super.checkStructureComponents(parts, abilities) && hasOutputEnergy && (hasInputFluid || hasSteamInput);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        List<IFluidTank> fluidTanks = new ArrayList<>();
        fluidTanks.addAll(getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        fluidTanks.addAll(getAbilities(GregicAdditionsCapabilities.STEAM));

        int framework = context.getOrDefault("framework", GAMultiblockCasing.CasingType.TIERED_HULL_LV).getTier();
        int framework2 = context.getOrDefault("framework2", GAMultiblockCasing2.CasingType.TIERED_HULL_UHV).getTier();
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.importFluidHandler = new FluidTankList(true, fluidTanks);
        this.tier = Math.min(motor, Math.min(framework, framework2));
        int tier = this.tier - 1;
        this.efficiency = Math.max(0.1F, (1.0F - (tier / 10.0F)));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("~CC", "CEC", "~CC")
                .aisle("CCC", "CRC", "CCC")
                .aisle("~CC", "CFC", "~CC")
                .aisle("~CC", "~SC", "~CC")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('E', abilityPartPredicate(MultiblockAbility.OUTPUT_ENERGY))
                .where('F', frameworkPredicate().or(frameworkPredicate2()))
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
