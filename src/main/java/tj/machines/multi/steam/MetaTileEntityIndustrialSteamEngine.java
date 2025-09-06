package tj.machines.multi.steam;

import tj.builder.handlers.TJFuelRecipeLogic;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.MultiblockDisplaysUtility;
import tj.builder.multicontrollers.TJFueledMultiblockControllerBase;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static net.minecraft.util.text.TextFormatting.AQUA;
import static net.minecraft.util.text.TextFormatting.RED;

public class MetaTileEntityIndustrialSteamEngine extends TJFueledMultiblockControllerBase {

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
        TJFuelRecipeLogic recipeLogic = (TJFuelRecipeLogic) workableHandler;
        MultiblockDisplayBuilder.start(textList)
                .custom(text -> {
                    text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.consuming.seconds", recipeLogic.getConsumption(),
                            net.minecraft.util.text.translation.I18n.translateToLocal(recipeLogic.getFuelName()),
                            recipeLogic.getMaxProgress() / 20)));
                    FluidStack fuelStack = recipeLogic.getFuelStack();
                    int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

                    ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
                    text.add(new TextComponentTranslation("tj.multiblock.fuel_amount", fuelAmount, fuelName));

                    text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.energy", recipeLogic.getProduction())));

                    text.add(new TextComponentTranslation("gregtech.universal.tooltip.efficiency", efficiency * 100).setStyle(new Style().setColor(AQUA)));

                    if (energyContainer.getEnergyCanBeInserted() < recipeLogic.getProduction())
                        text.add(new TextComponentTranslation("machine.universal.output.full").setStyle(new Style().setColor(RED)));
                })
                .isWorking(recipeLogic.isWorkingEnabled(), recipeLogic.isActive(), recipeLogic.getProgress(), recipeLogic.getMaxProgress());
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

        int framework = 0, framework2 = 0;
        if (context.get("framework") instanceof GAMultiblockCasing.CasingType) {
            framework = ((GAMultiblockCasing.CasingType) context.get("framework")).getTier();
        }
        if (context.get("framework2") instanceof GAMultiblockCasing2.CasingType) {
            framework2 = ((GAMultiblockCasing2.CasingType) context.get("framework2")).getTier();
        }
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.importFluidHandler = new FluidTankList(true, fluidTanks);
        this.tier = Math.min(motor, Math.max(framework, framework2));
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
                .setAmountAtLeast('L', 8)
                .where('S', selfPredicate())
                .where('L', statePredicate(this.getCasingState()))
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
