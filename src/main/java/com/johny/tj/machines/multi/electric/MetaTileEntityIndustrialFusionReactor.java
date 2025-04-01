package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.johny.tj.TJConfig;
import com.johny.tj.blocks.BlockAbilityCasings;
import com.johny.tj.blocks.BlockFusionCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJRecipeMapMultiblockController;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.fusion.GAFusionCasing;
import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregicadditions.utils.GALog;
import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
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
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;

import static com.johny.tj.capability.TJMultiblockDataCodes.PARALLEL_LAYER;

public class MetaTileEntityIndustrialFusionReactor extends TJRecipeMapMultiblockController {

    private int parallelLayer;
    private long energyToStart;
    private final int tier;
    private long euCapacity;
    private EnergyContainerList inputEnergyContainers;
    private long heat = 0;
    DecimalFormat formatter = new DecimalFormat("#0.00");

    public void resetStructure() {
        this.invalidateStructure();
        this.recipeMapWorkable.previousRecipe.clear();
        this.structurePattern = createStructurePattern();
    }

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
        }
        this.energyContainer = new EnergyContainerHandler(this, Integer.MAX_VALUE, 0, 0 ,0, 0) {
            @Override
            public String getName() {
                return "EnergyContainerInternal";
            }
        };
        reinitializeStructurePattern();
    }

    public int getTier() {
        return tier;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityIndustrialFusionReactor(metaTileEntityId, tier);
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
                    factoryPattern.aisle("######OCO######", "####CCcccCC####", "###EccOCOccE###", "##EcEC###CEcE##", "#CcE#######EcC#", "#CcC#######CcC#", "OcO#########OcO", "CcC#########CcC", "OcO#########OcO", "#CcC#######CcC#", "#CcE#######EcC#", "##EcEC###CEcE##", "###EccOCOccE###", "####CCcccCC####", "######OCO######");
                }
        factoryPattern.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############");
        factoryPattern.aisle("######OSO######", "####CCcccCC####", "###EccOCOccE###", "##EcEC###CEcE##", "#CcE#######EcC#", "#CcC#######CcC#", "OcO#########OcO", "CcC#########CcC", "OcO#########OcO", "#CcC#######CcC#", "#CcE#######EcC#", "##EcEC###CEcE##", "###EccOCOccE###", "####CCcccCC####", "######OCO######");
        factoryPattern.aisle("###############", "######ICI######", "####CC###CC####", "###C#######C###", "##C#########C##", "##C#########C##", "#I###########I#", "#C###########C#", "#I###########I#", "##C#########C##", "##C#########C##", "###C#######C###", "####CC###CC####", "######ICI######", "###############")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('c', statePredicate(getCoilState()))
                .where('O', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.EXPORT_FLUIDS)))
                .where('E', statePredicate(getCasingState()).or(tilePredicate((state, tile) -> {
                    for (int i = tier; i < GTValues.V.length; i++) {
                        if (tile.metaTileEntityId.equals(MetaTileEntities.ENERGY_INPUT_HATCH[i].metaTileEntityId))
                            return true;
                    }
                    return false;
                    })).or(tilePredicate((state, tile) -> {
                    for (int i = Math.max(tier, 9); i < GAValues.V.length; i++) {
                        if (tile.metaTileEntityId.equals(GATileEntities.ENERGY_INPUT[i - 9].metaTileEntityId))
                            return true;
                    }
                    return false;
                })).or(energyPortPredicate(tier)))
            .where('I', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS)))
            .where('#', (tile) -> true);
        return tier != 0 ? factoryPattern.build() : null;
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
                return currentCasing.get(0).getName().equals(tieredCasingType.getName()) && currentCasing.get(0).getTier() == tier;
            }
        };
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.FUSION_TEXTURE;
    }

    public IBlockState getCasingState() {
        switch (tier) {
            case 6:
                return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING);
            case 7:
                return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2);
            case 8:
                return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_3);
            default:
                return TJMetaBlocks.FUSION_CASING.getState(BlockFusionCasings.FusionType.FUSION_CASING_UHV);
        }
    }

    public IBlockState getCoilState() {
        switch (tier) {
            case 6:
                return MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.FUSION_COIL);
            case 7:
                return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_COIL_2);
            case 8:
                return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_COIL_3);
            default:
                return TJMetaBlocks.FUSION_CASING.getState(BlockFusionCasings.FusionType.FUSION_COIL_UHV);
        }
    }


    @Override
    protected void formStructure(PatternMatchContext context) {
        euCapacity = 0;
        long energyStored = this.energyContainer.getEnergyStored();
        int energyPortAmount = Collections.unmodifiableList(context.getOrDefault("EnergyPort", Collections.emptyList())).size();
        euCapacity += energyPortAmount * 10000000L * (long) Math.pow(2, tier - 6);
        super.formStructure(context);
        this.initializeAbilities();
        ((EnergyContainerHandler) this.energyContainer).setEnergyStored(energyStored);
    }

    private void initializeAbilities() {
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.inputFluidInventory = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.outputFluidInventory = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        List<IEnergyContainer> energyInputs = getAbilities(MultiblockAbility.INPUT_ENERGY);
        this.inputEnergyContainers = new EnergyContainerList(energyInputs);
        euCapacity += energyInputs.size() * 10000000L * (long) Math.pow(2, tier - 6);
        this.energyContainer = new EnergyContainerHandler(this, euCapacity, GAValues.V[tier], 0, 0, 0) {
            @Override
            public String getName() {
                return "EnergyContainerInternal";
            }
        };
    }

    @Override
    protected void updateFormedValid() {
        if (this.inputEnergyContainers.getEnergyStored() > 0) {
            long energyAdded = this.energyContainer.addEnergy(this.inputEnergyContainers.getEnergyStored());
            if (energyAdded > 0) this.inputEnergyContainers.removeEnergy(energyAdded);
        }
        super.updateFormedValid();
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
            Recipe recipe = ((IndustrialFusionRecipeLogic) this.recipeMapWorkable).getCurrentRecipe();
            if (recipe != null) {
                long recipeEUStart = recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) * parallelLayer;
                textList.add(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.required_energy", recipeEUStart)
                        .setStyle(new Style().setColor(this.energyContainer.getEnergyCapacity() >= recipeEUStart ? TextFormatting.GREEN : TextFormatting.RED)));
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
        tagCompound.setInteger("Parallel", this.parallelLayer);
        return tagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.parallelLayer = data.getInteger("Parallel");
        if (data.hasKey("Parallel"))
            this.structurePattern = createStructurePattern();
    }


    private class IndustrialFusionRecipeLogic extends LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic {

        private final int EUtPercentage;
        private final int durationPercentage;
        public RecipeMap<?> recipeMap;
        private Recipe currentRecipe;

        public IndustrialFusionRecipeLogic(MetaTileEntityIndustrialFusionReactor tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
            super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack);
            this.allowOverclocking = false;
            this.EUtPercentage = EUtPercentage;
            this.durationPercentage = durationPercentage;
            this.recipeMap = tileEntity.recipeMap;
        }

        public Recipe getCurrentRecipe() {
            return currentRecipe;
        }

        @Override
        public void updateWorkable() {
            super.updateWorkable();
            if (!isActive && heat > 0) {
                heat = heat <= 10000 ? 0 : heat - 10000;
            }
        }

        @Override
        protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
            Recipe recipe = super.findRecipe(maxVoltage, inputs, fluidInputs, useOptimizedRecipeLookUp);
            if (recipe == null)
                return null;
            currentRecipe = recipe;
            long recipeEUStart = recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) * parallelLayer;
            return energyContainer.getEnergyCapacity() >= recipeEUStart ? recipe : null;
        }

        @Override
        protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
            long heatDiff = recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) - heat;
            if (heatDiff <= 0) {
                return super.setupAndConsumeRecipeInputs(recipe);
            }
            if (energyContainer.getEnergyStored() < heatDiff  || !super.setupAndConsumeRecipeInputs(recipe)) {
                return false;
            }
            energyContainer.removeEnergy(heatDiff);
            heat += heatDiff;
            return true;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = super.serializeNBT();
            tag.setLong("Heat", heat);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound compound) {
            super.deserializeNBT(compound);
            heat = compound.getLong("Heat");
        }

        @Override
        protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
            int EUt;
            int duration;
            int minMultiplier = Integer.MAX_VALUE;

            Map<String, Integer> countFluid = new HashMap<>();
            if (!matchingRecipe.getFluidInputs().isEmpty()) {

                this.findFluid(countFluid, fluidInputs);
                minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, parallelLayer));
            }

            if (minMultiplier == Integer.MAX_VALUE) {
                GALog.logger.error("Cannot calculate ratio of items for large multiblocks");
                return null;
            }
            EUt = matchingRecipe.getEUt();
            duration = matchingRecipe.getDuration();

            int tierDiff = fusionOverclockMultiplier(matchingRecipe, energyToStart);

            List<FluidStack> newFluidInputs = new ArrayList<>();
            List<FluidStack> outputF = new ArrayList<>();
            this.multiplyInputsAndOutputs(newFluidInputs, outputF, matchingRecipe, minMultiplier);

            RecipeBuilder<?> newRecipe = recipeMap.recipeBuilder();

            newRecipe.fluidInputs(newFluidInputs)
                    .fluidOutputs(outputF)
                    .EUt(Math.max(1, (EUt * this.EUtPercentage * minMultiplier / 100) * tierDiff))
                    .duration((int) Math.max(3, (duration * (this.durationPercentage / 100.0)) / tierDiff));

            return newRecipe.build().getResult();
        }

        protected void multiplyInputsAndOutputs(List<FluidStack> newFluidInputs, List<FluidStack> outputF, Recipe recipe, int multiplier) {
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

        protected int fusionOverclockMultiplier(Recipe matchingRecipe, long energyToStart) {
            long recipeEnergy = matchingRecipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L);
            boolean rounded = false;
            int multiplier = 1;
            while (!rounded) {
                if (recipeEnergy < (160_000_000L * multiplier) + 1) {
                    recipeEnergy = 160_000_000L * multiplier;
                    rounded = true;
                }
                multiplier *= 2;
            }
            return (int) (energyToStart / recipeEnergy);
        }
    }
}
