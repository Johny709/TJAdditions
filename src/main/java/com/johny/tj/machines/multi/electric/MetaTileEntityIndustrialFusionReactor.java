package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.johny.tj.TJConfig;
import com.johny.tj.builder.TJGARecipeMapMultiblockController;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.fusion.GAFusionCasing;
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
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.*;
import gregtech.api.recipes.recipeproperties.FusionEUToStartProperty;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.util.GTFluidUtils;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaTileEntityIndustrialFusionReactor extends TJGARecipeMapMultiblockController {

    private int parallelLayer = 1;
    private long energyToStart;
    private final int tier;
    private EnergyContainerList inputEnergyContainers;
    private long heat = 0;
    IndustrialFusionRecipeLogic fusionRecipeLogic;

    public void resetStructure() {
        this.invalidateStructure();
        this.structurePattern = createStructurePattern();
    }

    public MetaTileEntityIndustrialFusionReactor(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, RecipeMaps.FUSION_RECIPES, false, false, false);
        IndustrialFusionRecipeLogic fusionRecipeLogic = new IndustrialFusionRecipeLogic(this, 100, 100, 100, 1);
        this.recipeMapWorkable = fusionRecipeLogic;
        this.fusionRecipeLogic = fusionRecipeLogic;
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
        }
        this.energyContainer = new EnergyContainerHandler(this, Integer.MAX_VALUE, 0, 0 ,0, 0) {
            @Override
            public String getName() {
                return "EnergyContainerInternal";
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityIndustrialFusionReactor(metaTileEntityId, tier);
    }

    @Override
    protected void reinitializeStructurePattern() {
        this.structurePattern = null;
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
                    })))
            .where('I', statePredicate(getCasingState()).or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS)))
            .where('#', (tile) -> true);
        return factoryPattern.build();
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.FUSION_TEXTURE;
    }

    private IBlockState getCasingState() {
        switch (tier) {
            case 6:
                return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING);
            case 7:
                return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2);
            case 8:
            default:
                return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_3);
        }
    }

    private IBlockState getCoilState() {
        switch (tier) {
            case 6:
                return MetaBlocks.WIRE_COIL.getState(BlockWireCoil.CoilType.FUSION_COIL);
            case 7:
                return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_COIL_2);
            case 8:
            default:
                return GAMetaBlocks.FUSION_CASING.getState(GAFusionCasing.CasingType.FUSION_COIL_3);
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        long energyStored = this.energyContainer.getEnergyStored();
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
        long euCapacity = energyInputs.size() * 10000000L * (long) Math.pow(2, tier - 6);
        this.energyContainer = new EnergyContainerHandler(this, euCapacity, GAValues.V[tier], 0, 0, 0) {
            @Override
            public String getName() {
                return "EnergyContainerInternal";
            }
        };
    }

    @Override
    protected void updateFormedValid() {
        if (!getWorld().isRemote) {
            if (this.inputEnergyContainers.getEnergyStored() > 0) {
                long energyAdded = this.energyContainer.addEnergy(this.inputEnergyContainers.getEnergyStored());
                if (energyAdded > 0) this.inputEnergyContainers.removeEnergy(energyAdded);
            }
            super.updateFormedValid();
        }
    }

    @Override
    protected void checkStructurePattern() {
        try {
            if (!getWorld().isRemote && getWorld() != null) {
                if (this.structurePattern == null)
                    this.structurePattern = createStructurePattern();
                super.checkStructurePattern();
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("gregtech.multiblock.industrial_fusion_reactor.message", this.parallelLayer));
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
        if (!getWorld().isRemote) {
            if (!playerIn.isSneaking()) {
                if (!(this.parallelLayer <= TJConfig.industrialFusionReactor.maximumSlices)) {
                    this.parallelLayer++;
                    playerIn.sendMessage(new TextComponentTranslation("gregtech.multiblock.industrial_fusion_reactor.message.1").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                } else {
                    playerIn.sendMessage(new TextComponentTranslation("gregtech.multiblock.industrial_fusion_reactor.message.4").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                }
            } else {
                if (this.parallelLayer > 1) {
                    this.parallelLayer--;
                    playerIn.sendMessage(new TextComponentTranslation("gregtech.multiblock.industrial_fusion_reactor.message.2").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                } else
                    playerIn.sendMessage(new TextComponentTranslation("gregtech.multiblock.industrial_fusion_reactor.message.3").appendSibling(new TextComponentString(" " + this.parallelLayer)));
            }
            this.resetStructure();
        }
        return true;
    }

    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return ClientHandler.FUSION_REACTOR_OVERLAY;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.industrial_fusion_reactor.description"));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", recipeMap.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", 1 + " + Number of Slices"));
        tooltip.add(I18n.format("tj.multiblock.industrial_fusion_reactor.energy", this.energyToStart));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.3", "Duration / 2, " + "Energy * 2"));
    }











    private static class IndustrialFusionRecipeLogic extends LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic {

        private final MetaTileEntityIndustrialFusionReactor fusionReactor;
        private final int EUtPercentage;
        private final int durationPercentage;
        private final int chancePercentage;
        private final int stack;
        public RecipeMap<?> recipeMap;

        public IndustrialFusionRecipeLogic(MetaTileEntityIndustrialFusionReactor tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
            super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack);
            this.allowOverclocking = false;
            this.fusionReactor = tileEntity;
            this.EUtPercentage = EUtPercentage;
            this.durationPercentage = durationPercentage;
            this.chancePercentage = chancePercentage;
            this.stack = stack;
            this.recipeMap = tileEntity.recipeMap;
        }

        @Override
        public void updateWorkable() {
            super.updateWorkable();
            if (!isActive && fusionReactor.heat > 0) {
                fusionReactor.heat = fusionReactor.heat <= 10000 ? 0 : (fusionReactor.heat - 10000);
            }
        }

        @Override
        protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
            Recipe recipe = super.findRecipe(maxVoltage, inputs, fluidInputs, useOptimizedRecipeLookUp);
            return (recipe != null && recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) <= fusionReactor.energyContainer.getEnergyCapacity()) ? recipe : null;
        }

        @Override
        protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
            long heatDiff = recipe.getRecipePropertyStorage().getRecipePropertyValue(FusionEUToStartProperty.getInstance(), 0L) - fusionReactor.heat;
            if (heatDiff <= 0) {
                return super.setupAndConsumeRecipeInputs(recipe);
            }
            if (fusionReactor.energyContainer.getEnergyStored() < heatDiff || !super.setupAndConsumeRecipeInputs(recipe)) {
                return false;
            }
            fusionReactor.energyContainer.removeEnergy(heatDiff);
            fusionReactor.heat += heatDiff;
            return true;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = super.serializeNBT();
            tag.setLong("Heat", fusionReactor.heat);
            tag.setInteger("Parallel", fusionReactor.parallelLayer);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound compound) {
            super.deserializeNBT(compound);
            fusionReactor.heat = compound.getLong("Heat");
            fusionReactor.parallelLayer = compound.getInteger("Parallel");
        }

        @Override
        protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
            int EUt;
            int duration;
            //int currentTier = getOverclockingTier(maxVoltage);
            //int tierNeeded;
            int minMultiplier = Integer.MAX_VALUE;
            int overclockMultiplier = 2;

            //tierNeeded = Math.max(1, GAUtility.getTierByVoltage(matchingRecipe.getEUt()));
            //maxItemsLimit *= currentTier - tierNeeded;

            Map<String, Integer> countFluid = new HashMap<>();
            if (matchingRecipe.getFluidInputs().size() != 0) {

                this.findFluid(countFluid, fluidInputs);
                minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, fusionReactor.parallelLayer));
            }

            if (minMultiplier == Integer.MAX_VALUE) {
                GALog.logger.error("Cannot calculate ratio of items for large multiblocks");
                return null;
            }
            EUt = matchingRecipe.getEUt();
            duration = matchingRecipe.getDuration();

            int tierDiff = fusionOverclockMultiplier(matchingRecipe, fusionReactor.energyToStart);

                List<CountableIngredient> newRecipeInputs = new ArrayList<>();
                List<FluidStack> newFluidInputs = new ArrayList<>();
                List<ItemStack> outputI = new ArrayList<>();
                List<FluidStack> outputF = new ArrayList<>();
                this.multiplyInputsAndOutputs(newRecipeInputs, newFluidInputs, outputI, outputF, matchingRecipe, fusionReactor.parallelLayer);


                RecipeBuilder<?> newRecipe = recipeMap.recipeBuilder();
                copyChancedItemOutputs(newRecipe, matchingRecipe, fusionReactor.parallelLayer);

                // determine if there is enough room in the output to fit all of this
                // if there isn't, we can't process this recipe.
                boolean canFitOutputs = GTFluidUtils.simulateFluidStackMerge(outputF, this.getOutputTank());
                if (!canFitOutputs) {
                    return matchingRecipe;
                }

                newRecipe.inputsIngredients(newRecipeInputs)
                        .fluidInputs(newFluidInputs)
                        .outputs(outputI)
                        .fluidOutputs(outputF)
                        .EUt(Math.max(1, (EUt * this.EUtPercentage * fusionReactor.parallelLayer / 100) * tierDiff))
                        .duration((int) Math.max(3, (duration * (this.durationPercentage / 100.0)) / tierDiff));

                return newRecipe.build().getResult();
        }

        @Override
        protected void multiplyInputsAndOutputs(List<CountableIngredient> newRecipeInputs, List<FluidStack> newFluidInputs, List<ItemStack> outputI, List<FluidStack> outputF, Recipe r, int multiplier) {
            for (FluidStack fs : r.getFluidInputs()) {
                FluidStack newFluid = new FluidStack(fs.getFluid(), fs.amount * multiplier);
                newFluidInputs.add(newFluid);
            }
            for (FluidStack f : r.getFluidOutputs()) {
                int fluidNum = f.amount * multiplier;
                FluidStack fluidCopy = f.copy();
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
