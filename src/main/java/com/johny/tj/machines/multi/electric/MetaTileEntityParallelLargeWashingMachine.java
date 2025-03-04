package com.johny.tj.machines.multi.electric;

import com.johny.tj.TJConfig;
import com.johny.tj.builder.MultiRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.components.MotorCasing;
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
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
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

//TODO WIP
public class MetaTileEntityParallelLargeWashingMachine extends ParallelRecipeMapMultiblockController {

    private int recipeMapIndex;
    private final MultiRecipeMap[] washerRecipeMaps = {PARALLEL_ORE_WASHER_RECIPES, PARALLEL_CHEMICAL_BATH_RECIPES, PARALLEL_SIMPLE_ORE_WASHER_RECIPES, PARALLEL_AUTOCLAVE_RECIPES};
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.INPUT_ENERGY,
            GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeWashingMachine(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, PARALLEL_ORE_WASHER_RECIPES);
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, null, TJConfig.parallelLargeWashingMachine.eutPercentage,
                TJConfig.parallelLargeWashingMachine.durationPercentage, TJConfig.parallelLargeWashingMachine.chancePercentage, TJConfig.parallelLargeWashingMachine.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }

            @Override
            public RecipeMap<?> getRecipeMap() {
                switch (((MetaTileEntityParallelLargeWashingMachine)this.controller).getRecipeMapIndex()) {
                    case 1: return RecipeMaps.CHEMICAL_BATH_RECIPES;
                    case 2: return GARecipeMaps.SIMPLE_ORE_WASHER_RECIPES;
                    case 3: return RecipeMaps.AUTOCLAVE_RECIPES;
                    default: return RecipeMaps.ORE_WASHER_RECIPES;
                }
            }

            @Override
            protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
                MetaTileEntityParallelLargeWashingMachine controller = (MetaTileEntityParallelLargeWashingMachine)this.controller;
                return controller.washerRecipeMaps[controller.getRecipeMapIndex()].findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp, occupiedRecipes, distinct);
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeWashingMachine(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                RecipeMaps.ORE_WASHER_RECIPES.getLocalizedName() + ", " + RecipeMaps.CHEMICAL_BATH_RECIPES.getLocalizedName()
                        + ", " + GARecipeMaps.SIMPLE_ORE_WASHER_RECIPES.getLocalizedName() + ", " + RecipeMaps.AUTOCLAVE_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeWashingMachine.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeWashingMachine.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeWashingMachine.stack));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeWashingMachine.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(LEFT, DOWN, BACK);
        factoryPattern.aisle("~CCC~", "HHHHH", "HmHmH", "HHHHH");
        for (int count = 0; count < parallelLayer; count++) {
            if (count != 0)
                factoryPattern.aisle("~HHH~", "H###H", "HP#PH", "HHHHH");
            factoryPattern.aisle("CGCGC", "H###H", "HP#PH", "HHHHH");
            factoryPattern.aisle("CGCGC", "M###M", "MP#PM", "MMMMM");
            factoryPattern.aisle("CGCGC", "H###H", "HP#PH", "HHHHH");
            factoryPattern.validateLayer(2 + count * 4, context -> context.getInt("RedstoneControllerAmount") <= 1);
        }
        factoryPattern.aisle("~CCC~", "HHHHH", "HmSmH", "HHHHH")
                .where('S', selfPredicate())
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
            case 1: return Textures.CHEMICAL_BATH_OVERLAY;
            case 3: return Textures.AUTOCLAVE_OVERLAY;
            default: return Textures.ORE_WASHER_OVERLAY;
        }
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentString(I18n.format("gregtech.universal.tooltip.voltage_in", maxVoltage, GAValues.VN[GTUtility.getGATierByVoltage(maxVoltage)])));
        }
        RecipeMap<?> recipeMap;
        switch (getRecipeMapIndex()) {
            case 1:
                recipeMap = RecipeMaps.CHEMICAL_BATH_RECIPES;
                break;
            case 2:
                recipeMap = GARecipeMaps.SIMPLE_ORE_WASHER_RECIPES;
                break;
            case 3:
                recipeMap = RecipeMaps.AUTOCLAVE_RECIPES;
                break;
            default:
                recipeMap = RecipeMaps.ORE_WASHER_RECIPES;
        }
        textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(withButton(new TextComponentString(recipeMap.getLocalizedName()), recipeMap.getUnlocalizedName())));
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
        int min = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.maxVoltage = 0;
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

    public int getRecipeMapIndex() {
        return recipeMapIndex;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeWashingMachine.maximumParallel;
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
}
