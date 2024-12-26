package com.johny.tj.machines.multi.electric;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import com.johny.tj.TJConfig;
import com.johny.tj.builder.ParallelLargeChemicalReactorRecipeMapBuilder;
import com.johny.tj.builder.multicontrollers.TJGARecipeMapMultiblockController;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.capabilities.impl.GAMultiblockRecipeLogic;
import gregicadditions.capabilities.impl.GARecipeMapMultiblockController;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.recipes.GARecipeMaps;
import gregicadditions.recipes.impl.LargeRecipeBuilder;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static com.johny.tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.unification.material.Materials.Steel;

public class MetaTileEntityParallelLargeChemicalReactor extends TJGARecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_ITEMS, MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};

    private final ArrayList<ParallelChemicalReactorWorkableHandler> chemicalReactorWorkableHandlers;
    private final HashSet<Recipe> occupiedRecipes;
    private int energyBonus;
    private int parallelLayer = 1;
    private long maxVoltage = 0;
    private int pageIndex = 0;
    private final int pageSize = 6;
    private int countId = 0;
    public final ParallelLargeChemicalReactorRecipeMapBuilder LargeChemicalRecipeMap;

    public MetaTileEntityParallelLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, GARecipeMaps.LARGE_CHEMICAL_RECIPES, false, true, true);
        this.chemicalReactorWorkableHandlers = new ArrayList<>();
        this.occupiedRecipes = new HashSet<>();
        this.LargeChemicalRecipeMap = new ParallelLargeChemicalReactorRecipeMapBuilder("parallel_large_chemical_reactor",
                0, 3, 0, 3, 0, 5, 0, 4, (new LargeRecipeBuilder(RecipeMaps.CHEMICAL_RECIPES))
                .EUt(30));
        LargeChemicalRecipeMap.addRecipes(GARecipeMaps.LARGE_CHEMICAL_RECIPES.getRecipeList());
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("gregtech.multiblock.industrial_fusion_reactor.message", this.parallelLayer));
        if (isStructureFormed()) {

            ITextComponent page = new TextComponentString(":");
            page.appendText(" ");
            page.appendSibling(withButton(new TextComponentString("[<]"), "leftPage"));
            page.appendText(" ");
            page.appendSibling(withButton(new TextComponentString("[>]"), "rightPage"));
            textList.add(page);

            int recipeHandlersSize = chemicalReactorWorkableHandlers.size();
            for (int i = pageIndex, recipeHandlerIndex = i + 1; i < pageIndex + pageSize; i++, recipeHandlerIndex++) {
                if (i < recipeHandlersSize) {
                    ParallelChemicalReactorWorkableHandler recipeHandler = chemicalReactorWorkableHandlers.get(i);

                    StringBuilder recipeHandlerIntance = getStringBuilder(recipeHandler);

                    ITextComponent recipeInstance = new TextComponentString("-");
                    recipeInstance.appendText(" ");
                    recipeInstance.appendSibling(new TextComponentString("[" + recipeHandlerIndex + "] " + (recipeHandler.isActive() ? I18n.format("gregtech.multiblock.running") : I18n.format("gregtech.multiblock.idling")))
                            .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(recipeHandlerIntance.toString())))));
                    textList.add(recipeInstance);
                }
            }
        }
        else {
            ITextComponent tooltip = new TextComponentTranslation("gregtech.multiblock.invalid_structure.tooltip");
            tooltip.setStyle(new Style().setColor(TextFormatting.GRAY));
            textList.add(new TextComponentTranslation("gregtech.multiblock.invalid_structure")
                    .setStyle(new Style().setColor(TextFormatting.RED)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
    }

    @NotNull
    private static StringBuilder getStringBuilder(ParallelChemicalReactorWorkableHandler recipeHandler) {
        double progressPercent = recipeHandler.getProgressPercent() * 100;
        StringBuilder recipeHandlerIntance = new StringBuilder();

        recipeHandlerIntance.append(I18n.format("gregtech.multiblock.parallel_large_chemical_reactor.eu"))
            .append(" ")
            .append(recipeHandler.getRecipeEUt())
            .append("\n");

        recipeHandlerIntance.append(I18n.format("gregtech.multiblock.parallel_large_chemical_reactor.progress"))
            .append(" ")
            .append(((int) progressPercent))
            .append(" %");
        return recipeHandlerIntance;
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "leftPage":
                if (pageIndex > 0)
                    pageIndex -= pageSize;
                break;
            default:
                if (pageIndex < chemicalReactorWorkableHandlers.size() - pageSize)
                    pageIndex += pageSize;
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        Predicate<BlockWorldState> machineControllerPredicate = this.countMatch("RedstoneControllerAmount", tilePredicate((state, tile) -> ((IMultiblockAbilityPart<?>) tile).getAbility() == REDSTONE_CONTROLLER));

        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(BlockPattern.RelativeDirection.LEFT, BlockPattern.RelativeDirection.FRONT, BlockPattern.RelativeDirection.DOWN);

        factoryPattern.aisle("HHHHH", "HHHHH", "HHHHH", "HHHHH", "HHHHH");
        for (int count = 0; count < this.parallelLayer; count++) {
            this.chemicalReactorWorkableHandlers.add(count, new ParallelChemicalReactorWorkableHandler(this, count));
            factoryPattern.aisle("F###F", "#PPP#", "#PBP#", "#PPP#", "F###F");
            factoryPattern.aisle("F###F", "#CCC#", "#CCC#", "#CCC#", "F###F");
            factoryPattern.validateLayer(2 + count * 2, (context) -> context.getInt("RedstoneControllerAmount") <= 1);
        }

        this.recipeMapWorkable = this.chemicalReactorWorkableHandlers.get(0);
        factoryPattern.aisle("F###F", "#PPP#", "#PBP#", "#PPP#", "F###F");
        factoryPattern.aisle("HHSHH", "HHHHH", "HHHHH", "HHHHH", "HHHHH")
                .where('S', selfPredicate())
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', statePredicate(getCasingState()).or(machineControllerPredicate))
                .where('P', statePredicate(GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.PTFE_PIPE)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Steel).getDefaultState()))
                .where('B', pumpPredicate())
                .where('#', (tile) -> true);
        return factoryPattern.build();
    }

    public static Predicate<BlockWorldState> pumpPredicate() {
        return (blockWorldState) -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (!(blockState.getBlock() instanceof PumpCasing)) {
                return false;
            } else {
                PumpCasing motorCasing = (PumpCasing) blockState.getBlock();
                PumpCasing.CasingType tieredCasingType = motorCasing.getState(blockState);
                PumpCasing.CasingType currentCasing = blockWorldState.getMatchContext().getOrPut("Pump", tieredCasingType);
                return currentCasing.getName().equals(tieredCasingType.getName());
            }
        };
    }

    protected IBlockState getCasingState() {
        return GAMetaBlocks.MUTLIBLOCK_CASING.getState(GAMultiblockCasing.CasingType.CHEMICALLY_INERT);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return ClientHandler.CHEMICALLY_INERT;
    }

    @Override
    public MetaTileEntityParallelLargeChemicalReactor createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeChemicalReactor(metaTileEntityId);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        PumpCasing.CasingType pump = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV);
        int min = pump.getTier();
        maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    protected void updateFormedValid() {
        int index = 0;
        for (ParallelChemicalReactorWorkableHandler reactorWorkableHandler : this.chemicalReactorWorkableHandlers) {
            reactorWorkableHandler.updateWorkable();

            if (countId == reactorWorkableHandler.WORKABLE_ID)
                reactorWorkableHandler.canRun = true;

            if (index < getAbilities(REDSTONE_CONTROLLER).size())
                reactorWorkableHandler.setWorkingEnabled(!getAbilities(REDSTONE_CONTROLLER).get(index).getRedstonePowered());
            index++;
        }
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.maxVoltage = 0;
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
    protected void reinitializeStructurePattern() {
        this.structurePattern = null;
    }

    public void resetStructure() {
        this.invalidateStructure();
        this.chemicalReactorWorkableHandlers.clear();
        this.LargeChemicalRecipeMap.addRecipes(occupiedRecipes);
        this.occupiedRecipes.clear();
        this.countId = 0;
        this.structurePattern = createStructurePattern();
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
                if (this.parallelLayer < TJConfig.parallelLCR.maximumLayers) {
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
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Parallel", this.parallelLayer);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.parallelLayer = data.getInteger("Parallel");
    }












    private static class ParallelChemicalReactorWorkableHandler extends GAMultiblockRecipeLogic {

        MetaTileEntityParallelLargeChemicalReactor chemicalReactor;
        private final int WORKABLE_ID;
        private boolean canRun = false;
        ParallelLargeChemicalReactorRecipeMapBuilder recipeMapFilter;

        public ParallelChemicalReactorWorkableHandler(MetaTileEntityParallelLargeChemicalReactor tileEntity, int workableId) {
            super(tileEntity);
            this.chemicalReactor = tileEntity;
            this.WORKABLE_ID = workableId;
        }

        @Override
        public void updateWorkable() {
            if (canRun)
                super.updateWorkable();
        }

        @Override
        protected boolean trySearchNewRecipe() {
            long maxVoltage = chemicalReactor.maxVoltage;
            Recipe currentRecipe = null;
            IItemHandlerModifiable importInventory = getInputInventory();
            IMultipleTankHandler importFluids = getInputTank();
            if (this.recipeMapFilter == null) {
                this.recipeMapFilter = chemicalReactor.LargeChemicalRecipeMap;
                for (Recipe recipe : chemicalReactor.occupiedRecipes)
                    this.recipeMapFilter.removeRecipe(recipe);
            }
            Recipe foundRecipe = this.previousRecipe.get(importInventory, importFluids);
            if (foundRecipe != null) {
                //if previous recipe still matches inputs, try to use it
                currentRecipe = foundRecipe;
            } else {
                    boolean dirty = checkRecipeInputsDirty(importInventory, importFluids);
                    if (dirty || forceRecipeRecheck) {
                        this.forceRecipeRecheck = false;
                        //else, try searching new recipe for given inputs
                        currentRecipe = findRecipe(maxVoltage, importInventory, importFluids, this.useOptimizedRecipeLookUp);
                        if (currentRecipe != null) {
                            this.previousRecipe.put(currentRecipe);
                            this.previousRecipe.cacheUnutilized();
                        }
                }
            }
            if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
                if (foundRecipe != null) {
                    this.previousRecipe.cacheUtilized();
                }
                chemicalReactor.occupiedRecipes.add(currentRecipe);
                setupRecipe(currentRecipe);
                chemicalReactor.countId++;
                return true;
            }
            return false;
        }

        @Override
        protected boolean trySearchNewRecipeDistinct() {
            long maxVoltage = chemicalReactor.maxVoltage;
            Recipe currentRecipe = null;
            List<IItemHandlerModifiable> importInventory = getInputBuses();
            IMultipleTankHandler importFluids = getInputTank();
            if (this.recipeMapFilter == null) {
                this.recipeMapFilter = chemicalReactor.LargeChemicalRecipeMap;
                for (Recipe recipe : chemicalReactor.occupiedRecipes)
                    this.recipeMapFilter.removeRecipe(recipe);
            }

            // Our caching implementation
            // This guarantees that if we get a recipe cache hit, our efficiency is no different from other machines
            Recipe foundRecipe = this.previousRecipe.get(importInventory.get(lastRecipeIndex), importFluids);
            HashSet<Integer> foundRecipeIndex = new HashSet<>();
            if (foundRecipe != null) {
                currentRecipe = foundRecipe;
                if (setupAndConsumeRecipeInputs(currentRecipe, lastRecipeIndex)) {
                    this.previousRecipe.cacheUtilized();
                    setupRecipe(currentRecipe);
                    return true;
                }
                foundRecipeIndex.add(lastRecipeIndex);
            }

            for (int i = 0; i < importInventory.size(); i++) {
                if (i == lastRecipeIndex) {
                    continue;
                }
                foundRecipe = this.previousRecipe.get(importInventory.get(i), importFluids);
                if (foundRecipe != null) {
                    currentRecipe = foundRecipe;
                    if (setupAndConsumeRecipeInputs(currentRecipe, i)) {
                        this.previousRecipe.cacheUtilized();
                        setupRecipe(currentRecipe);
                        return true;
                    }
                    foundRecipeIndex.add(i);
                }
            }

            // On a cache miss, our efficiency is much worse, as it will check
            // each bus individually instead of the combined inventory all at once.
            for (int i = 0; i < importInventory.size(); i++) {
                if (foundRecipeIndex.contains(i)) {
                    continue;
                }
                IItemHandlerModifiable bus = importInventory.get(i);
                boolean dirty = checkRecipeInputsDirty(bus, importFluids, i);
                if (!dirty && !forceRecipeRecheck) {
                    continue;
                }
                this.forceRecipeRecheck = false;
                currentRecipe = findRecipe(maxVoltage, bus, importFluids, this.useOptimizedRecipeLookUp);
                if (currentRecipe == null) {
                    continue;
                }
                this.previousRecipe.put(currentRecipe);
                this.previousRecipe.cacheUnutilized();
                if (!setupAndConsumeRecipeInputs(currentRecipe, i)) {
                    continue;
                }
                lastRecipeIndex = i;
                chemicalReactor.occupiedRecipes.add(currentRecipe);
                setupRecipe(currentRecipe);
                chemicalReactor.countId++;
                return true;
            }
            return false;
        }

        @Override
        protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
            return recipeMapFilter.findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp);
        }

        @Override
        protected void setupRecipe(Recipe recipe) {
            int energyBonus = 0;
            long maxVoltage = chemicalReactor.maxVoltage;

            int[] resultOverclock = calculateOverclock(recipe.getEUt(), maxVoltage, recipe.getDuration());
            this.progressTime = 1;

//            // perfect overclocking
//            if (resultOverclock[1] < recipe.getDuration())
//                resultOverclock[1] *= 0.5;

            // apply energy bonus
            resultOverclock[0] -= (int) (resultOverclock[0] * energyBonus * 0.01f);
            setMaxProgress(resultOverclock[1]);

            this.recipeEUt = resultOverclock[0];
            this.fluidOutputs = GTUtility.copyFluidList(recipe.getFluidOutputs());
            int tier = getMachineTierForRecipe(recipe);
            this.itemOutputs = GTUtility.copyStackList(recipe.getResultItemOutputs(getOutputInventory().getSlots(), random, tier));
            if (this.wasActiveAndNeedsUpdate) {
                this.wasActiveAndNeedsUpdate = false;
            } else {
                this.setActive(true);
            }
        }

        @Override
        protected int[] calculateOverclock(int EUt, long voltage, int duration) {
            int numMaintenanceProblems = (this.metaTileEntity instanceof GARecipeMapMultiblockController) ?
                    ((GARecipeMapMultiblockController) metaTileEntity).getNumProblems() : 0;

            double maintenanceDurationMultiplier = 1.0 + (0.2 * numMaintenanceProblems);
            int durationModified = (int) (duration * maintenanceDurationMultiplier);

            boolean negativeEU = EUt < 0;
            int tier = getOverclockingTier(voltage);
            if (GAValues.V[tier] <= EUt || tier == 0)
                return new int[]{EUt, durationModified};
            if (negativeEU)
                EUt = -EUt;
            int resultEUt = EUt;
            double resultDuration = durationModified;
            //do not overclock further if duration is already too small
            while (resultDuration >= 1 && resultEUt <= GAValues.V[tier - 1]) {
                resultEUt *= 4;
                resultDuration /= 4;
            }
            previousRecipeDuration = (int) resultDuration;
            return new int[]{negativeEU ? -resultEUt : resultEUt, (int) Math.ceil(resultDuration)};
        }
    }
}
