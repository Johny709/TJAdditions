package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.MultiRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.item.metal.MetalCasing1;
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
import static gregicadditions.GAMaterials.Grisium;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.capabilities.MultiblockDataCodes.RECIPE_MAP_INDEX;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class MetaTileEntityParallelLargeBrewery extends ParallelRecipeMapMultiblockController {

    private int recipeMapIndex;
    private final MultiRecipeMap[] breweryRecipeMaps = {PARALLEL_BREWING_MACHINE_RECIPES, PARALLEL_FERMENTING_RECIPES, PARALLEL_CHEMICAL_DEHYDRATOR_RECIPES, PARALLEL_CRACKING_UNIT_RECIPES};
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, IMPORT_FLUIDS, EXPORT_FLUIDS, MAINTENANCE_HATCH};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeBrewery(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_BREWING_MACHINE_RECIPES);
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, null, TJConfig.parallelLargeBrewery.eutPercentage,
                TJConfig.parallelLargeBrewery.durationPercentage, TJConfig.parallelLargeBrewery.chancePercentage, TJConfig.parallelLargeBrewery.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }

            @Override
            public RecipeMap<?> getRecipeMap() {
                return this.controller.getMultiblockRecipe();
            }

            @Override
            protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
                MetaTileEntityParallelLargeBrewery controller = (MetaTileEntityParallelLargeBrewery)this.controller;
                return controller.breweryRecipeMaps[controller.getRecipeMapIndex()].findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp, occupiedRecipes, distinct);
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeBrewery(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                RecipeMaps.BREWING_RECIPES.getLocalizedName() + ", " + RecipeMaps.FERMENTING_RECIPES.getLocalizedName()
                        + ", " + GARecipeMaps.CHEMICAL_DEHYDRATOR_RECIPES.getLocalizedName() + ", " + RecipeMaps.CRACKING_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeBrewery.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeBrewery.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeBrewery.stack));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeBrewery.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, DOWN, BACK);
        factoryPattern.aisle("~CCC~", "CHHHC", "CHmHC", "CHHHC", "F~C~F", "CCCCC");
        for (int count = 0; count < parallelLayer; count++) {
            factoryPattern.aisle("~~C~~", "~G#G~", "C#P#C", "~G#G~", "~~C~~", "~CCC~");
            factoryPattern.aisle("~~C~~", "~G#G~", "p#P#p", "~G#G~", "~~M~~", "~MMM~");
            factoryPattern.aisle("~~C~~", "~G#G~", "C#P#C", "~G#G~", "~~C~~", "~CCC~");
            factoryPattern.validateLayer(2 + count * 3, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("~CCC~", "CHHHC", "CHmHC", "CHSHC", "F~C~F", "CCCCC")
                .where('S', selfPredicate())
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

    private IBlockState getCasingState() {
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
            case 1: return Textures.FERMENTER_OVERLAY;
            case 2: return Textures.SIFTER_OVERLAY;
            case 3: return Textures.CRACKING_UNIT_OVERLAY;
            default: return Textures.BREWERY_OVERLAY;
        }
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
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
                recipeMap = RecipeMaps.FERMENTING_RECIPES;
                break;
            case 2:
                recipeMap = GARecipeMaps.CHEMICAL_DEHYDRATOR_RECIPES;
                break;
            case 3:
                recipeMap = RecipeMaps.CRACKING_RECIPES;
                break;
            default:
                recipeMap = RecipeMaps.BREWING_RECIPES;
        }
        textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(withButton(new TextComponentTranslation("recipemap." + recipeMap.getUnlocalizedName() + ".name"), recipeMap.getUnlocalizedName())));
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        if (this.recipeMapWorkable.isActive())
            return;
        this.recipeMapWorkable.previousRecipe.clear();
        this.recipeMapIndex = recipeMapIndex >= 3 ? 0 : recipeMapIndex +1;
        if (!getWorld().isRemote) {
            writeCustomData(RECIPE_MAP_INDEX, buf -> buf.writeInt(recipeMapIndex));
            markDirty();
        }
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
        this.maxVoltage = 0;
    }

    public int getRecipeMapIndex() {
        return recipeMapIndex;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeBrewery.maximumParallel;
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
            case 1: return RecipeMaps.FERMENTING_RECIPES;
            case 2: return GARecipeMaps.CHEMICAL_DEHYDRATOR_RECIPES;
            case 3: return RecipeMaps.CRACKING_RECIPES;
            default: return RecipeMaps.BREWING_RECIPES;
        }
    }
}
