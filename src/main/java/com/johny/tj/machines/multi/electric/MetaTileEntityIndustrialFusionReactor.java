package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockAbilityCasings;
import com.johny.tj.blocks.BlockFusionCasings;
import com.johny.tj.blocks.BlockFusionGlass;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJRecipeMapMultiblockController;
import com.johny.tj.capability.IHeatInfo;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.widgets.TJCycleButtonWidget;
import com.johny.tj.textures.TJTextures;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.fusion.GAFusionCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.utils.GALog;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.WidgetGroup;
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
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipeproperties.FusionEUToStartProperty;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.johny.tj.capability.TJMultiblockDataCodes.PARALLEL_LAYER;
import static com.johny.tj.gui.TJGuiTextures.*;
import static gregtech.api.gui.GuiTextures.TOGGLE_BUTTON_BACK;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.INPUT_ENERGY;

public class MetaTileEntityIndustrialFusionReactor extends TJRecipeMapMultiblockController implements IHeatInfo {

    private int parallelLayer;
    private long energyToStart;
    private long energyToStartProperty;
    private final int tier;
    private EnergyContainerList inputEnergyContainers;
    private long heat;
    private long maxHeat;
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");
    private BatchMode batchMode = BatchMode.ONE;
    private Recipe recipe;

    public MetaTileEntityIndustrialFusionReactor(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, RecipeMaps.FUSION_RECIPES);
        this.recipeMapWorkable = new IndustrialFusionRecipeLogic(this, TJConfig.industrialFusionReactor.eutPercentage, TJConfig.industrialFusionReactor.durationPercentage, 100, 1);
        this.tier = tier;
        switch (tier) {
            case 6:
                this.energyToStart = 160_000_000;
                break;
            case 7:
                this.energyToStart = 320_000_000;
                break;
            case 8:
                this.energyToStart = 640_000_000;
                break;
            case 9:
                this.energyToStart = 1_280_000_000;
                break;
            case 10:
                this.energyToStart = 2_560_000_000L;
        }
        this.energyContainer = new EnergyContainerHandler(this, Integer.MAX_VALUE, 0, 0 ,0, 0) {
            @Override
            public String getName() {
                return "EnergyContainerInternal";
            }
        };
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityIndustrialFusionReactor(metaTileEntityId, tier);
    }

    public void resetStructure() {
        this.invalidateStructure();
        this.recipeMapWorkable.previousRecipe.clear();
        this.structurePattern = createStructurePattern();
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected void reinitializeStructurePattern() {
        this.parallelLayer = 1;
        super.reinitializeStructurePattern();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(BlockPattern.RelativeDirection.LEFT, BlockPattern.RelativeDirection.FRONT, BlockPattern.RelativeDirection.DOWN);
                for (int count = 1; count < this.parallelLayer; count++) {
                    factoryPattern.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############");
                    factoryPattern.aisle("######OGO######", "####GGcccGG####", "###EccOGOccE###", "##EcEG###GEcE##", "#GcE#######EcG#", "#GcG#######GcG#", "OcO#########OcO", "GcG#########GcG", "OcO#########OcO", "#GcG#######GcG#", "#GcE#######EcG#", "##EcEG###GEcE##", "###EccOGOccE###", "####GGcccGG####", "######OGO######");
                }
        factoryPattern.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############");
        factoryPattern.aisle("######OSO######", "####GGcccGG####", "###EccOGOccE###", "##EcEG###GEcE##", "#GcE#######EcG#", "#GcG#######GcG#", "OcO#########OcO", "GcG#########GcG", "OcO#########OcO", "#GcG#######GcG#", "#GcE#######EcG#", "##EcEG###GEcE##", "###EccOGOccE###", "####GGcccGG####", "######OGO######");
        factoryPattern.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('G', statePredicate(getCasingState()).or(statePredicate(getGlassState())))
                .where('c', statePredicate(getCoilState()))
                .where('O', statePredicate(getCasingState()).or(statePredicate(getGlassState())).or(abilityPartPredicate(MultiblockAbility.EXPORT_FLUIDS)))
                .where('E', statePredicate(getCasingState()).or(statePredicate(getGlassState())).or(tilePredicate(energyHatchPredicate(tier))).or(energyPortPredicate(tier)))
            .where('I', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS)))
            .where('#', (tile) -> true);
        return tier != 0 ? factoryPattern.build() : null;
    }

    public static BiFunction<BlockWorldState, MetaTileEntity, Boolean> energyHatchPredicate(int tier) {
        return (state, tile) -> {
            if (tile instanceof MetaTileEntityMultiblockPart) {
                MetaTileEntityMultiblockPart multiblockPart = (MetaTileEntityMultiblockPart) tile;
                if (multiblockPart instanceof IMultiblockAbilityPart<?>) {
                    IMultiblockAbilityPart<?> abilityPart = (IMultiblockAbilityPart<?>) multiblockPart;
                    return abilityPart.getAbility() == INPUT_ENERGY && multiblockPart.getTier() >= tier;
                }
            }

            return false;
        };
    }

    public static Predicate<BlockWorldState> energyPortPredicate(int tier) {
        return (blockWorldState) -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof BlockAbilityCasings)) {
                return false;
            } else {
                BlockAbilityCasings abilityCasings = (BlockAbilityCasings)blockState.getBlock();
                BlockAbilityCasings.AbilityType tieredCasingType = abilityCasings.getState(blockState);
                List<BlockAbilityCasings.AbilityType> currentCasing = blockWorldState.getMatchContext().getOrCreate("EnergyPort", ArrayList::new);
                currentCasing.add(tieredCasingType);
                return currentCasing.get(0).getName().equals(tieredCasingType.getName()) && currentCasing.get(0).getTier() >= tier;
            }
        };
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        switch (tier) {
            case 6: return TJTextures.FUSION_PORT_LUV;
            case 7: return TJTextures.FUSION_PORT_ZPM;
            case 8: return TJTextures.FUSION_PORT_UV;
            case 9: return TJTextures.FUSION_PORT_UHV;
            default: return TJTextures.FUSION_PORT_UEV;
        }
    }

    public IBlockState getCasingState() {
        switch (tier) {
            case 6: return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING);
            case 7: return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2);
            case 8: return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_3);
            case 9: return TJMetaBlocks.FUSION_CASING.getState(BlockFusionCasings.FusionType.FUSION_CASING_UHV);
            default: return TJMetaBlocks.FUSION_CASING.getState(BlockFusionCasings.FusionType.FUSION_CASING_UEV);
        }
    }

    public IBlockState getGlassState() {
        switch (tier) {
            case 6: return TJMetaBlocks.FUSION_GLASS.getState(BlockFusionGlass.GlassType.FUSION_GLASS_LUV);
            case 7: return TJMetaBlocks.FUSION_GLASS.getState(BlockFusionGlass.GlassType.FUSION_GLASS_ZPM);
            case 8: return TJMetaBlocks.FUSION_GLASS.getState(BlockFusionGlass.GlassType.FUSION_GLASS_UV);
            case 9: return TJMetaBlocks.FUSION_GLASS.getState(BlockFusionGlass.GlassType.FUSION_GLASS_UHV);
            default: return TJMetaBlocks.FUSION_GLASS.getState(BlockFusionGlass.GlassType.FUSION_GLASS_UEV);
        }
    }

    public IBlockState getCoilState() {
        switch (tier) {
            case 6: return MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.FUSION_COIL);
            case 7: return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_COIL_2);
            case 8: return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_COIL_3);
            case 9: return TJMetaBlocks.FUSION_CASING.getState(BlockFusionCasings.FusionType.FUSION_COIL_UHV);
            default: return TJMetaBlocks.FUSION_CASING.getState(BlockFusionCasings.FusionType.FUSION_COIL_UEV);
        }
    }


    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        long euCapacity = 0;
        long energyStored = this.energyContainer.getEnergyStored();
        int energyPortAmount = Collections.unmodifiableList(context.getOrDefault("EnergyPort", Collections.emptyList())).size();
        euCapacity += energyPortAmount * 10000000L * (long) Math.pow(2, tier - 6);

        List<IEnergyContainer> energyInputs = getAbilities(INPUT_ENERGY);
        this.inputEnergyContainers = new EnergyContainerList(energyInputs);
        euCapacity += energyInputs.size() * 10000000L * (long) Math.pow(2, tier - 6);
        this.energyContainer = new EnergyContainerHandler(this, euCapacity, GAValues.V[tier], 0, 0, 0) {
            @Override
            public String getName() {
                return "EnergyContainerInternal";
            }
        };
        ((EnergyContainerHandler) this.energyContainer).setEnergyStored(energyStored);
    }

    @Override
    public boolean checkRecipe(Recipe recipe, boolean consumeIfSuccess) {
        long energyToStart = this.recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) * parallelLayer;
        return heat >= energyToStart;
    }

    @Override
    protected void updateFormedValid() {
        super.updateFormedValid();
        long inputEnergyStored = inputEnergyContainers.getEnergyStored();
        if (inputEnergyStored > 0) {
            long energyAdded = energyContainer.addEnergy(inputEnergyStored);
            if (energyAdded > 0)
                inputEnergyContainers.removeEnergy(energyAdded);
        }

        if (heat > maxHeat)
            heat = maxHeat;

        if (!recipeMapWorkable.isActive() || !recipeMapWorkable.isWorkingEnabled()) {
            heat -= Math.min(heat, 10000L * parallelLayer);
        }

        if (recipe != null && recipeMapWorkable.isWorkingEnabled()) {
            long remainingHeat = maxHeat - heat;
            long energyToRemove = Math.min(remainingHeat, inputEnergyContainers.getInputAmperage() * inputEnergyContainers.getInputVoltage());
            heat += Math.abs(energyContainer.removeEnergy(energyToRemove));
        }
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        return widgetGroup.apply(new TJCycleButtonWidget(172, 151, 18, 18, BatchMode.class, this::getBatchMode, this::setBatchMode, BUTTON_BATCH_ONE, BUTTON_BATCH_FOUR, BUTTON_BATCH_SIXTEEN, BUTTON_BATCH_SIXTY_FOUR)
                .setTooltipFormat(this::getTooltipFormat)
                .setToggle(true)
                .setButtonTexture(TOGGLE_BUTTON_BACK)
                .setTooltipHoverString("machine.universal.batch.amount"));
    }

    private void setBatchMode(BatchMode batchMode) {
        this.batchMode = batchMode;
        markDirty();
    }

    private BatchMode getBatchMode() {
        return batchMode;
    }

    private String[] getTooltipFormat() {
        return ArrayUtils.toArray(String.valueOf(batchMode.getAmount()));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message", this.parallelLayer));
        if (!this.isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.invalid_structure").setStyle(new Style().setColor(TextFormatting.RED)));
        }
        if (this.isStructureFormed()) {
            if (!this.recipeMapWorkable.isWorkingEnabled()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.work_paused"));
            } else if (this.recipeMapWorkable.isActive()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.running"));
                int currentProgress;
                if (energyContainer.getEnergyCapacity() > 0) {
                    currentProgress = (int) (this.recipeMapWorkable.getProgressPercent() * 100.0D);
                    textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
                } else {
                    currentProgress = -this.recipeMapWorkable.getRecipeEUt();
                    textList.add(new TextComponentTranslation("gregtech.multiblock.generation_eu", currentProgress));
                }
            } else {
                textList.add(new TextComponentTranslation("gregtech.multiblock.idling"));
            }
            if (recipe != null) {
                long energyToStart = recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) * parallelLayer;
                textList.add(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.required_heat", energyToStart)
                        .setStyle(new Style().setColor(heat >= energyToStart ? TextFormatting.GREEN : TextFormatting.RED)));
            }
            if (this.recipeMapWorkable.isHasNotEnoughEnergy()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.not_enough_energy").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }

        textList.add(new TextComponentString("EU: " + this.energyContainer.getEnergyStored() + " / " + this.energyContainer.getEnergyCapacity()));
        textList.add(new TextComponentTranslation("gtadditions.multiblock.fusion_reactor.heat", heat));
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (playerIn.getHeldItemMainhand().isItemEqual(MetaItems.SCREWDRIVER.getStackForm()))
            return false;
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        ITextComponent textComponent;
        if (!playerIn.isSneaking()) {
            if (parallelLayer < TJConfig.industrialFusionReactor.maximumSlices) {
                this.parallelLayer++;
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.increment.success").appendSibling(new TextComponentString(" " + this.parallelLayer));
            } else
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.increment.fail").appendSibling(new TextComponentString(" " + this.parallelLayer));
        } else {
            if (parallelLayer > 1) {
                this.parallelLayer--;
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.decrement.success").appendSibling(new TextComponentString(" " + this.parallelLayer));
            } else
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.decrement.fail").appendSibling(new TextComponentString(" " + this.parallelLayer));
        }
        if (getWorld().isRemote)
            playerIn.sendMessage(textComponent);
        else {
            writeCustomData(PARALLEL_LAYER, buf -> buf.writeInt(parallelLayer));
        }
        this.resetStructure();
        return true;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return ClientHandler.FUSION_REACTOR_OVERLAY;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.industrial_fusion_reactor.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.industrialFusionReactor.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.industrialFusionReactor.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", TJConfig.industrialFusionReactor.maximumSlices));
        tooltip.add(I18n.format("tj.multiblock.industrial_fusion_reactor.energy", this.energyToStart));
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == PARALLEL_LAYER) {
            this.parallelLayer = buf.readInt();
            this.structurePattern = createStructurePattern();
            scheduleRenderUpdate();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeInt(parallelLayer);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.parallelLayer = buf.readInt();
        this.structurePattern = createStructurePattern();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagCompound tagCompound = super.writeToNBT(data);
        tagCompound.setLong("Heat", heat);
        tagCompound.setLong("MaxHeat", maxHeat);
        tagCompound.setInteger("Parallel", this.parallelLayer);
        tagCompound.setInteger("BatchMode", this.batchMode.ordinal());
        return tagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.maxHeat = data.getLong("MaxHeat");
        this.heat = data.getLong("Heat");
        this.parallelLayer = data.getInteger("Parallel");
        this.batchMode = BatchMode.values()[data.getInteger("BatchMode")];
        if (data.hasKey("Parallel"))
            this.structurePattern = createStructurePattern();
    }

    @Override
    public long heat() {
        return heat;
    }

    @Override
    public long maxHeat() {
        return maxHeat;
    }

    private void setRecipe(Recipe recipe, long heat) {
        long energyCapacity = energyContainer.getEnergyCapacity();
        long heatCapacity = heat * parallelLayer;
        this.recipe = recipe;
        this.maxHeat = Math.min(energyCapacity, heatCapacity);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_HEAT)
            return TJCapabilities.CAPABILITY_HEAT.cast(this);
        return super.getCapability(capability, side);
    }

    private class IndustrialFusionRecipeLogic extends LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic {

        private final int EUtPercentage;
        private final int durationPercentage;
        public RecipeMap<?> recipeMap;
        private final BiConsumer<Recipe, Long> currentRecipe;

        public IndustrialFusionRecipeLogic(MetaTileEntityIndustrialFusionReactor tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
            super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack);
            this.allowOverclocking = false;
            this.EUtPercentage = EUtPercentage;
            this.durationPercentage = durationPercentage;
            this.recipeMap = tileEntity.recipeMap;
            this.currentRecipe = tileEntity::setRecipe;
        }

        @Override
        protected void completeRecipe() {
            super.completeRecipe();
            this.currentRecipe.accept(null, 0L);
        }

        @Override
        protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
            int EUt;
            int duration;
            int minMultiplier = Integer.MAX_VALUE;
            long recipeEnergy = Math.max(160_000_000, matchingRecipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L));

            this.currentRecipe.accept(matchingRecipe, recipeEnergy);
            Map<String, Integer> countFluid = new HashMap<>();
            if (!matchingRecipe.getFluidInputs().isEmpty()) {

                this.findFluid(countFluid, fluidInputs);
                minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, parallelLayer * batchMode.getAmount()));
            }

            if (minMultiplier == Integer.MAX_VALUE) {
                GALog.logger.error("Cannot calculate ratio of items for large multiblocks");
                return null;
            }
            EUt = matchingRecipe.getEUt();
            duration = matchingRecipe.getDuration();

            float tierDiff = fusionOverclockMultiplier(energyToStart, recipeEnergy);

            List<FluidStack> newFluidInputs = new ArrayList<>();
            List<FluidStack> outputF = new ArrayList<>();
            multiplyInputsAndOutputs(newFluidInputs, outputF, matchingRecipe, minMultiplier);

            RecipeBuilder<?> newRecipe = recipeMap.recipeBuilder();

            newRecipe.fluidInputs(newFluidInputs)
                    .fluidOutputs(outputF)
                    .EUt((int) Math.max(1, ((EUt * this.EUtPercentage * minMultiplier / 100.0) * tierDiff) / batchMode.getAmount()))
                    .duration((int) Math.max(1, ((duration * (this.durationPercentage / 100.0)) / tierDiff) * batchMode.getAmount()));

            return newRecipe.build().getResult();
        }

        private static void multiplyInputsAndOutputs(List<FluidStack> newFluidInputs, List<FluidStack> outputF, Recipe recipe, int multiplier) {
            for (FluidStack fluidS : recipe.getFluidInputs()) {
                FluidStack newFluid = new FluidStack(fluidS.getFluid(), fluidS.amount * multiplier);
                newFluidInputs.add(newFluid);
            }
            for (FluidStack fluid : recipe.getFluidOutputs()) {
                int fluidNum = fluid.amount * multiplier;
                FluidStack fluidCopy = fluid.copy();
                fluidCopy.amount = fluidNum;
                outputF.add(fluidCopy);
            }
        }

        private static float fusionOverclockMultiplier(long energyToStart, long recipeEnergy) {
            recipeEnergy = Math.max(160_000_000, recipeEnergy);
            long recipeEnergyOld = recipeEnergy;
            float OCMultiplier = 1;
            while (recipeEnergy <= energyToStart) {
                if (recipeEnergy != recipeEnergyOld)
                    OCMultiplier *= recipeEnergy > 640_000_000 ? 4 : 2.8F;
                recipeEnergy *= 2;
            }
            return OCMultiplier;
        }
    }

    public enum BatchMode implements IStringSerializable {
        ONE("batch_one", 1),
        FOUR("batch_four", 4),
        SIXTEEN("batch_sixteen", 16),
        SIXTY_FOUR("batch_sixty_four", 64);

        BatchMode(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        private final String name;
        private final int amount;

        @Override
        public String getName() {
            return name;
        }

        public int getAmount() {
            return amount;
        }
    }
}
