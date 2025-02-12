package com.johny.tj.machines.multi.steam;

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
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class MetaTileEntityIndustrialSteamEngine extends TJFueledMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH};
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
        super.addDisplayText(textList);
        if (workableHandler.isActive()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.generation_eu", workableHandler.getMaxVoltage()));
        }
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        return new FuelRecipeLogic(this, recipeMap, () -> energyContainer, () -> importFluidHandler, 0) {

            @Override
            protected int calculateFuelAmount(FuelRecipe currentRecipe) {
                return (int) (super.calculateFuelAmount(currentRecipe) / ((MetaTileEntityIndustrialSteamEngine) metaTileEntity).getEfficiency());
            }

            @Override
            public long getMaxVoltage() {
                return GTValues.V2[((MetaTileEntityIndustrialSteamEngine) metaTileEntity).getTier()];
            }
        };
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        tier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
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
