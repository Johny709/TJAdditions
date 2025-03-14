package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.MultiRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing2;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Predicate;

import static com.johny.tj.TJRecipeMaps.*;
import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.capabilities.MultiblockDataCodes.RECIPE_MAP_INDEX;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class MetaTileEntityParallelLargeCentrifuge extends ParallelRecipeMapMultiblockController {

    private int recipeMapIndex;
    private int energyBonus;
    private final MultiRecipeMap[] centrifugeRecipeMaps = {PARALLEL_CENTRIFUGE_RECIPES, PARALLEL_THERMAL_CENTRIFUGE_RECIPES, PARALLEL_GAS_CENTRIFUGE_RECIPES};
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeCentrifuge(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_CENTRIFUGE_RECIPES);
        this.recipeMapWorkable = new ParallelLargeCentrifugeWorkableHandler(this, null, TJConfig.parallelLargeCentrifuge.eutPercentage,
                TJConfig.parallelLargeCentrifuge.durationPercentage, TJConfig.parallelLargeCentrifuge.chancePercentage, TJConfig.parallelLargeCentrifuge.stack);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeCentrifuge(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                RecipeMaps.CENTRIFUGE_RECIPES.getLocalizedName() + ", " + RecipeMaps.THERMAL_CENTRIFUGE_RECIPES.getLocalizedName()
                        + ", " + GARecipeMaps.GAS_CENTRIFUGE_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeCentrifuge.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeCentrifuge.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeCentrifuge.stack));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeCentrifuge.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.2"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, FRONT, DOWN);
        factoryPattern.aisle("~HHH~", "HCCCH", "HCmCH", "HCCCH", "~HHH~");
        for (int count = 1; count < parallelLayer; count++) {
            factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
            factoryPattern.aisle("~MGM~", "M###M", "G#P#G", "M###M", "~MGM~");
            factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
            factoryPattern.aisle("~HHH~", "HCCCH", "HCmCH", "HCCCH", "~HHH~");
            factoryPattern.validateLayer(2 + count * 4, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
        factoryPattern.aisle("~MSM~", "M###M", "G#P#G", "M###M", "~MGM~");
        factoryPattern.aisle("HHHHH", "H###H", "H#P#H", "H###H", "HHHHH");
        factoryPattern.aisle("~HHH~", "HCCCH", "HCmCH", "HCCCH", "~HHH~");
        factoryPattern.validateLayer(2, context -> context.getInt("RedstoneControllerAmount") <= 1)
                .where('S', selfPredicate())
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('M', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)).or(machineControllerPredicate))
                .where('C', MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate().or(MetaTileEntityParallelLargeChemicalReactor.heatingCoilPredicate2()))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TITANIUM_PIPE)))
                .where('G', statePredicate(MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.GRATE_CASING)))
                .where('m', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true);
        return factoryPattern.build();
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
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.universal.energy_usage", 100 - energyBonus).setStyle(new Style().setColor(TextFormatting.AQUA)));
            Style style = new Style().setColor(TextFormatting.GREEN);
            textList.add(new TextComponentTranslation("machine.universal.tooltip.voltage_tier")
                    .appendText(" ")
                    .appendSibling(new TextComponentString(String.valueOf(maxVoltage)).setStyle(style))
                    .appendText(" (")
                    .appendSibling(new TextComponentString(String.valueOf(GAValues.VN[GTUtility.getGATierByVoltage(maxVoltage)])).setStyle(style))
                    .appendText(")"));
        }
        RecipeMap<?> recipeMap;
        switch (getRecipeMapIndex()) {
            case 1:
                recipeMap = RecipeMaps.THERMAL_CENTRIFUGE_RECIPES;
                break;
            case 2:
                recipeMap = GARecipeMaps.GAS_CENTRIFUGE_RECIPES;
                break;
            default:
                recipeMap = RecipeMaps.CENTRIFUGE_RECIPES;
        }
        textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(withButton(new TextComponentTranslation("recipemap." + recipeMap.getUnlocalizedName() + ".name"), recipeMap.getUnlocalizedName())));
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        if (this.recipeMapWorkable.isActive())
            return;
        this.recipeMapWorkable.previousRecipe.clear();
        this.recipeMapIndex = recipeMapIndex >= 2 ? 0 : recipeMapIndex +1;
        if (!getWorld().isRemote) {
            writeCustomData(RECIPE_MAP_INDEX, buf -> buf.writeInt(recipeMapIndex));
            markDirty();
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int min = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
        energyBonus = context.getOrDefault("coilLevel", 0) * 5;
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.maxVoltage = 0;
    }

    public int getRecipeMapIndex() {
        return recipeMapIndex;
    }

    @Override
    public int getEUBonus() {
        return energyBonus;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeCentrifuge.maximumParallel;
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeByte(recipeMapIndex);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        recipeMapIndex = buf.readByte();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == RECIPE_MAP_INDEX) {
            recipeMapIndex = buf.readInt();
            scheduleRenderUpdate();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagCompound tagCompound = super.writeToNBT(data);
        tagCompound.setInteger("RecipeMapIndex", this.recipeMapIndex);
        return tagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.recipeMapIndex = data.getInteger("RecipeMapIndex");
    }

    @Override
    public RecipeMap<?> getMultiblockRecipe() {
        switch (getRecipeMapIndex()) {
            case 1: return RecipeMaps.THERMAL_CENTRIFUGE_RECIPES;
            case 2: return GARecipeMaps.GAS_CENTRIFUGE_RECIPES;
            default: return RecipeMaps.CENTRIFUGE_RECIPES;
        }
    }

    private static class ParallelLargeCentrifugeWorkableHandler extends ParallelGAMultiblockRecipeLogic {

        public ParallelLargeCentrifugeWorkableHandler(ParallelRecipeMapMultiblockController tileEntity, RecipeMap<?> recipeMap, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
            super(tileEntity, recipeMap, EUtPercentage, durationPercentage, chancePercentage, stack);
        }

        @Override
        protected long getMaxVoltage() {
            return this.controller.getMaxVoltage();
        }

        @Override
        public RecipeMap<?> getRecipeMap() {
            return  this.controller.getMultiblockRecipe();
        }

        @Override
        protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
            MetaTileEntityParallelLargeCentrifuge controller = (MetaTileEntityParallelLargeCentrifuge) this.controller;
            return controller.centrifugeRecipeMaps[controller.getRecipeMapIndex()].findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp, occupiedRecipes, distinct);
        }

        @Override
        protected void setupRecipe(Recipe recipe, int i) {
            int energyBonus = this.controller.getEUBonus();
            long maxVoltage = getMaxVoltage();

            int[] resultOverclock = calculateOverclock(recipe.getEUt(), maxVoltage, recipe.getDuration());
            this.progressTime[i] = 1;

//            // perfect overclocking
//            if (resultOverclock[1] < recipe.getDuration())
//                resultOverclock[1] *= 0.5;

            // apply energy bonus
            resultOverclock[0] -= (int) (resultOverclock[0] * energyBonus * 0.01f);
            setMaxProgress(resultOverclock[1], i);

            this.timeToStop[i] = 20;
            this.recipeEUt[i] = resultOverclock[0];
            this.fluidOutputs.put(i, GTUtility.copyFluidList(recipe.getFluidOutputs()));
            int tier = getMachineTierForRecipe(recipe);
            this.itemOutputs.put(i, GTUtility.copyStackList(recipe.getResultItemOutputs(getOutputInventory().getSlots(), random, tier)));
            if (this.wasActiveAndNeedsUpdate) {
                this.wasActiveAndNeedsUpdate = false;
            } else {
                this.setActive(true, i);
            }
        }
    }
}
