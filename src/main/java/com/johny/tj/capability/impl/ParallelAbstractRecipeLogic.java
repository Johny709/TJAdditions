package com.johny.tj.capability.impl;

import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import com.johny.tj.capability.IMultipleWorkable;
import com.johny.tj.capability.TJCapabilities;
import gregtech.api.GTValues;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.util.GTUtility;
import gregtech.common.ConfigHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.*;
import java.util.function.LongSupplier;

public abstract class ParallelAbstractRecipeLogic extends MTETrait implements IMultipleWorkable {

    private static final String ALLOW_OVERCLOCKING = "AllowOverclocking";
    private static final String OVERCLOCK_VOLTAGE = "OverclockVoltage";

    protected final ParallelRecipeMapMultiblockController controller;
    protected final OverclockManager overclockManager = new OverclockManager();
    private int size;

    protected boolean[] forceRecipeRecheck;
    protected ItemStack[] lastItemInputs;
    protected FluidStack[] lastFluidInputs;
    public ParallelRecipeLRUCache previousRecipe;
    public int recipeCacheSize;
    protected boolean useOptimizedRecipeLookUp = true;
    protected boolean allowOverclocking = true;
    private long overclockVoltage = 0;
    protected LongSupplier overclockPolicy = this::getMaxVoltage;

    protected int[] progressTime;
    protected int[] maxProgressTime;
    protected int[] recipeEUt;
    protected int[] parallel;
    protected Recipe[] occupiedRecipes;
    protected Map<Integer, List<FluidStack>> fluidOutputs;
    protected Map<Integer, NonNullList<ItemStack>> itemOutputs;
    protected final Random random = new Random();

    protected boolean isActive;
    protected boolean distinct;
    protected boolean[] isInstanceActive;
    protected boolean[] workingEnabled;
    protected boolean[] hasNotEnoughEnergy;
    protected boolean[] wasActiveAndNeedsUpdate;
    protected boolean[] lockRecipe;
    private final long[] V;
    private final String[] VN;

    private int[] sleepTimer;
    private int[] sleepTime;
    private int[] failCount;
    protected int[] evictRecipeTimer;

    public ParallelAbstractRecipeLogic(MetaTileEntity metaTileEntity, int recipeCacheSize) {
        super(metaTileEntity);
        this.controller = (ParallelRecipeMapMultiblockController) metaTileEntity;
        this.size = 1;
        this.recipeCacheSize = recipeCacheSize;
        this.forceRecipeRecheck = new boolean[this.size];
        this.previousRecipe = new ParallelRecipeLRUCache(this.recipeCacheSize);
        this.progressTime = new int[this.size];
        this.maxProgressTime = new int[this.size];
        this.recipeEUt = new int[this.size];
        this.parallel = new int[this.size];
        this.fluidOutputs = new HashMap<>();
        this.itemOutputs = new HashMap<>();
        this.isInstanceActive = new boolean[this.size];
        this.workingEnabled = new boolean[this.size];
        this.wasActiveAndNeedsUpdate = new boolean[this.size];
        this.hasNotEnoughEnergy = new boolean[this.size];
        this.lockRecipe = new boolean[this.size];
        this.sleepTimer = new int[this.size];
        this.sleepTime = new int[this.size];
        this.failCount = new int[this.size];
        this.evictRecipeTimer = new int[this.size];
        this.occupiedRecipes = new Recipe[this.size];

        Arrays.fill(this.sleepTime, 1);
        Arrays.fill(this.workingEnabled, true);
        Arrays.fill(this.parallel, 1);
        if (ConfigHolder.gregicalityOverclocking) {
            V = GTValues.V2;
            VN = GTValues.VN2;
        } else {
            V = GTValues.V;
            VN = GTValues.VN;
        }
    }

    public void setLayer(int i, boolean remove) {
        this.size = i;
        this.forceRecipeRecheck = Arrays.copyOf(this.forceRecipeRecheck, this.size);
        this.progressTime = Arrays.copyOf(this.progressTime, this.size);
        this.maxProgressTime = Arrays.copyOf(this.maxProgressTime, this.size);
        this.recipeEUt = Arrays.copyOf(this.recipeEUt, this.size);
        this.parallel = Arrays.copyOf(this.parallel, this.size);
        this.isInstanceActive = Arrays.copyOf(this.isInstanceActive, this.size);
        this.workingEnabled = Arrays.copyOf(this.workingEnabled, this.size);
        this.wasActiveAndNeedsUpdate = Arrays.copyOf(this.wasActiveAndNeedsUpdate, this.size);
        this.hasNotEnoughEnergy = Arrays.copyOf(this.hasNotEnoughEnergy, this.size);
        this.lockRecipe = Arrays.copyOf(this.lockRecipe, this.size);
        this.sleepTimer = Arrays.copyOf(this.sleepTimer, this.size);
        this.sleepTime = Arrays.copyOf(this.sleepTime, this.size);
        this.failCount = Arrays.copyOf(this.failCount, this.size);
        this.evictRecipeTimer = Arrays.copyOf(this.evictRecipeTimer, this.size);
        this.occupiedRecipes = Arrays.copyOf(this.occupiedRecipes, this.size);
        if (remove) {
            this.fluidOutputs.remove(i);
            this.itemOutputs.remove(i);
        } else {
            this.sleepTime[this.size -1] = 1;
            this.workingEnabled[this.size -1] = true;
            this.parallel[i] = 1;
        }
    }

    protected abstract long getEnergyStored();

    protected abstract long getEnergyCapacity();

    protected abstract boolean drawEnergy(int recipeEUt);

    protected abstract long getMaxVoltage();

    protected IItemHandlerModifiable getInputInventory() {
        return this.metaTileEntity.getImportItems();
    }

    protected IItemHandlerModifiable getOutputInventory() {
        return this.metaTileEntity.getExportItems();
    }

    protected IMultipleTankHandler getInputTank() {
        return this.metaTileEntity.getImportFluids();
    }

    protected IMultipleTankHandler getOutputTank() {
        return this.metaTileEntity.getExportFluids();
    }

    @Override
    public int getSize() {
        return this.size;
    }

    public Recipe getRecipe(int i) {
        return this.occupiedRecipes[i];
    }

    @Override
    public int getPageIndex() {
        return this.controller.getPageIndex();
    }

    @Override
    public int getPageSize() {
        return this.controller.getPageSize();
    }

    public void setLockingMode(boolean setLockingMode, int i) {
        this.lockRecipe[i] = setLockingMode;
    }

    public boolean getLockingMode(int i) {
        return this.lockRecipe[i];
    }

    public void update(int i) {
        if (!this.getMetaTileEntity().getWorld().isRemote) {
            if (this.workingEnabled[i]) {
                if (this.progressTime[i] > 0) {
                    this.updateRecipeProgress(i);
                }
                if (this.progressTime[i] == 0 && this.sleepTimer[i] == 0) {
                    boolean result = this.trySearchNewRecipe(i);
                    if (!result) {
                        this.failCount[i]++;
                        if (this.failCount[i] > 4) {

                            this.sleepTime[i] = Math.min(this.sleepTime[i] * 2, (ConfigHolder.maxSleepTime >= 0 && ConfigHolder.maxSleepTime <= 400) ? ConfigHolder.maxSleepTime : 20);
                            this.failCount[i] = 0;

                        }
                        this.sleepTimer[i] = this.sleepTime[i];
                    } else {
                        this.sleepTime[i] = 1;
                        this.failCount[i] = 0;
                    }
                }
                if (this.sleepTimer[i] > 0) {
                    this.sleepTimer[i]--;
                }
            }
            if (this.wasActiveAndNeedsUpdate[i]) {
                this.wasActiveAndNeedsUpdate[i] = false;
                this.setActive(false, i);
            }
        }
    }

    protected void updateRecipeProgress(int i) {
        boolean drawEnergy = this.drawEnergy(this.recipeEUt[i]);
        if (drawEnergy || (this.recipeEUt[i] < 0)) {
            //as recipe starts with progress on 1 this has to be > only not => to compensate for it
            if (++this.progressTime[i] > this.maxProgressTime[i]) {
                this.completeRecipe(i);
            }
        } else if (this.recipeEUt[i] > 0) {
            //only set hasNotEnoughEnergy if this recipe is consuming recipe
            //generators always have enough energy
            this.hasNotEnoughEnergy[i] = true;
            //if current progress value is greater than 2, decrement it by 2
            if (this.progressTime[i] >= 2) {
                if (ConfigHolder.insufficientEnergySupplyWipesRecipeProgress) {
                    this.progressTime[i] = 1;
                } else {
                    this.progressTime[i] = Math.max(1, this.progressTime[i] - 2);
                }
            }
        }
        if (this.evictRecipeTimer[i] > 0 && !this.lockRecipe[i] && this.progressTime[i] <= 0) {
            if (--this.evictRecipeTimer[i] % 20 == 0) {
                this.occupiedRecipes[i] = null;
            }
        }
    }

    protected boolean trySearchNewRecipe(int i) {
        long maxVoltage = getMaxVoltage();
        Recipe currentRecipe = null;
        IItemHandlerModifiable importInventory = this.getInputInventory();
        IMultipleTankHandler importFluids = this.getInputTank();
        Recipe foundRecipe;
        if (this.lockRecipe[i] && this.occupiedRecipes[i] != null) {
            if (!this.occupiedRecipes[i].matches(false, importInventory, importFluids)) {
                return false;
            }
            foundRecipe = this.occupiedRecipes[i];
        } else {
            if (!distinct) {
                foundRecipe = this.previousRecipe.get(importInventory, importFluids);
            } else {
                foundRecipe = this.previousRecipe.get(importInventory, importFluids, i, this.occupiedRecipes);
            }
        }
        if (foundRecipe != null) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = foundRecipe;
        } else {
            boolean dirty = this.checkRecipeInputsDirty(importInventory, importFluids);
            if (dirty || this.forceRecipeRecheck[i]) {
                this.forceRecipeRecheck[i] = false;
                //else, try searching new recipe for given inputs
                currentRecipe = findRecipe(maxVoltage, importInventory, importFluids, this.useOptimizedRecipeLookUp);
                if (currentRecipe != null) {
                    this.previousRecipe.put(currentRecipe);
                }
            }
        }
        if (currentRecipe != null && this.setupAndConsumeRecipeInputs(currentRecipe)) {
            this.occupiedRecipes[i] = currentRecipe;
            this.setupRecipe(currentRecipe, i);
            return true;
        }
        return false;
    }

    public void forceRecipeRecheck(int i) {
        this.forceRecipeRecheck[i] = true;
    }

    public boolean getUseOptimizedRecipeLookUp() {
        return this.useOptimizedRecipeLookUp;
    }

    public void setUseOptimizedRecipeLookUp(boolean use) {
        this.useOptimizedRecipeLookUp = use;
    }

    public boolean toggleUseOptimizedRecipeLookUp() {
        this.setUseOptimizedRecipeLookUp(!this.useOptimizedRecipeLookUp);
        return this.useOptimizedRecipeLookUp;
    }

    protected int getMinTankCapacity(IMultipleTankHandler tanks) {
        if (tanks.getTanks() == 0) {
            return 0;
        }
        int result = Integer.MAX_VALUE;
        for (IFluidTank fluidTank : tanks.getFluidTanks()) {
            result = Math.min(fluidTank.getCapacity(), result);
        }
        return result;
    }

    protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean useOptimizedRecipeLookUp) {
        return this.controller.parallelRecipeMap[this.controller.getRecipeMapIndex()].findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp, this.occupiedRecipes, distinct);
    }

    protected boolean checkRecipeInputsDirty(IItemHandler inputs, IMultipleTankHandler fluidInputs) {
        boolean shouldRecheckRecipe = false;
        if (this.lastItemInputs == null || this.lastItemInputs.length != inputs.getSlots()) {
            this.lastItemInputs = new ItemStack[inputs.getSlots()];
            Arrays.fill(this.lastItemInputs, ItemStack.EMPTY);
        }
        if (this.lastFluidInputs == null || this.lastFluidInputs.length != fluidInputs.getTanks()) {
            this.lastFluidInputs = new FluidStack[fluidInputs.getTanks()];
        }
        for (int j = 0; j < this.lastItemInputs.length; j++) {
            ItemStack currentStack = inputs.getStackInSlot(j);
            ItemStack lastStack = this.lastItemInputs[j];
            if (!areItemStacksEqual(currentStack, lastStack)) {
                this.lastItemInputs[j] = currentStack.isEmpty() ? ItemStack.EMPTY : currentStack.copy();
                shouldRecheckRecipe = true;
            } else if (currentStack.getCount() != lastStack.getCount()) {
                lastStack.setCount(currentStack.getCount());
                shouldRecheckRecipe = true;
            }
        }
        for (int j = 0; j < this.lastFluidInputs.length; j++) {
            FluidStack currentStack = fluidInputs.getTankAt(j).getFluid();
            FluidStack lastStack = this.lastFluidInputs[j];
            if ((currentStack == null && lastStack != null) ||
                    (currentStack != null && !currentStack.isFluidEqual(lastStack))) {
                this.lastFluidInputs[j] = currentStack == null ? null : currentStack.copy();
                shouldRecheckRecipe = true;
            } else if (currentStack != null && lastStack != null &&
                    currentStack.amount != lastStack.amount) {
                lastStack.amount = currentStack.amount;
                shouldRecheckRecipe = true;
            }
        }
        return shouldRecheckRecipe;
    }

    protected static boolean areItemStacksEqual(ItemStack stackA, ItemStack stackB) {
        return (stackA.isEmpty() && stackB.isEmpty()) ||
                (ItemStack.areItemsEqual(stackA, stackB) &&
                        ItemStack.areItemStackTagsEqual(stackA, stackB));
    }

    protected boolean setupAndConsumeRecipeInputs(Recipe recipe) {
        this.calculateOverclock(recipe.getEUt(), recipe.getDuration());
        int resultEU = this.overclockManager.getEUt();
        long totalEUt = (long) resultEU * this.overclockManager.getDuration();
        IItemHandlerModifiable importInventory = this.getInputInventory();
        IItemHandlerModifiable exportInventory = this.getOutputInventory();
        IMultipleTankHandler importFluids = this.getInputTank();
        IMultipleTankHandler exportFluids = this.getOutputTank();
        return (totalEUt >= 0 ? this.getEnergyStored() >= (totalEUt > this.getEnergyCapacity() / 2 ? resultEU : totalEUt) :
                (this.getEnergyStored() - resultEU <= this.getEnergyCapacity())) &&
                MetaTileEntity.addItemsToItemHandler(exportInventory, true, recipe.getAllItemOutputs(exportInventory.getSlots())) &&
                MetaTileEntity.addFluidsToFluidHandler(exportFluids, true, recipe.getFluidOutputs()) &&
                recipe.matches(true, importInventory, importFluids);
    }

    public double getDurationOverclock() {
        return 2.8;
    }

    protected void calculateOverclock(int EUt, int duration) {
        if (!this.allowOverclocking) {
            this.overclockManager.setEUt(EUt);
            this.overclockManager.setDuration(duration);
            return;
        }
        boolean negativeEU = EUt < 0;
        int tier = this.getOverclockingTier(this.overclockPolicy.getAsLong());
        if (V[tier] <= EUt || tier == 0) {
            this.overclockManager.setEUt(EUt);
            this.overclockManager.setDuration(duration);
            return;
        }
        if (negativeEU)
            EUt = -EUt;
        int resultEUt = EUt;
        double resultDuration = duration;
        double durationModifier = this.getDurationOverclock();
        //do not overclock further if duration is already too small
        while (resultDuration >= 1 && resultEUt <= V[tier - 1]) {
            resultEUt *= 4;
            resultDuration /= durationModifier;
        }
        this.overclockManager.setEUt(resultEUt);
        this.overclockManager.setDuration((int) Math.round(resultDuration));
    }

    protected int getOverclockingTier(long voltage) {
        if (ConfigHolder.gregicalityOverclocking) {
            return GTUtility.getGATierByVoltage(voltage);
        } else {
            return GTUtility.getTierByVoltage(voltage);
        }
    }

    protected long getVoltageByTier(final int tier) {
        return V[tier];
    }

    public String[] getAvailableOverclockingTiers() {
        final int maxTier = this.getOverclockingTier(getMaxVoltage());
        final String[] result = new String[maxTier + 2];
        result[0] = "gregtech.gui.overclock.off";
        for (int i = 0; i < maxTier + 1; ++i) {
            result[i + 1] = VN[i];
        }
        return result;
    }

    protected void setupRecipe(Recipe recipe, int i) {
        this.progressTime[i] = 1;
        this.evictRecipeTimer[i] = 20;
        this.setMaxProgress(this.overclockManager.getDuration(), i);
        this.recipeEUt[i] = this.overclockManager.getEUt();
        this.fluidOutputs.put(i, GTUtility.copyFluidList(recipe.getFluidOutputs()));
        int tier = this.getMachineTierForRecipe(recipe);
        this.itemOutputs.put(i, GTUtility.copyStackList(recipe.getResultItemOutputs(this.getOutputInventory().getSlots(), this.random, tier)));
        if (this.wasActiveAndNeedsUpdate[i]) {
            this.wasActiveAndNeedsUpdate[i] = false;
        } else {
            this.setActive(true, i);
        }
    }

    protected int getMachineTierForRecipe(Recipe recipe) {
        return GTUtility.getGATierByVoltage(getMaxVoltage());
    }

    protected void completeRecipe(int i) {
        MetaTileEntity.addItemsToItemHandler(this.getOutputInventory(), false, this.itemOutputs.get(i));
        MetaTileEntity.addFluidsToFluidHandler(this.getOutputTank(), false, this.fluidOutputs.get(i));
        this.progressTime[i] = 0;
        this.setMaxProgress(0, i);
        this.recipeEUt[i] = 0;
        this.hasNotEnoughEnergy[i] = false;
        this.wasActiveAndNeedsUpdate[i] = true;
        //force recipe recheck because inputs could have changed since last time
        //we checked them before starting our recipe, especially if recipe took long time
        this.forceRecipeRecheck[i] = true;
    }

    public double getProgressPercent(int i) {
        return this.getMaxProgress(i) == 0 ? 0.0 : this.getProgress(i) / (this.getMaxProgress(i) * 1.0);
    }

    public int getTicksTimeLeft(int i) {
        return this.maxProgressTime[i] == 0 ? 0 : (this.maxProgressTime[i] - this.progressTime[i]);
    }

    @Override
    public int getProgress(int i) {
        return this.progressTime[i];
    }

    @Override
    public int getMaxProgress(int i) {
       return this.maxProgressTime[i];
    }

    @Override
    public int getRecipeEUt(int i) {
        return this.recipeEUt[i];
    }

    @Override
    public int getParallel(int i) {
        return this.parallel[i];
    }

    public void setMaxProgress(int maxProgress, int i) {
        this.maxProgressTime[i] = maxProgress;
    }

    protected void setActive(boolean active, int i) {
        this.isInstanceActive[i] = active;
        this.isActive = active;
        if (!this.metaTileEntity.getWorld().isRemote) {
            this.writeCustomData(1, buf -> buf.writeBoolean(active));
            this.metaTileEntity.markDirty();
        }
    }

    public boolean isActive() {
        return this.isActive;
    }

    @Override
    public boolean isInstanceActive(int i) {
        return this.isInstanceActive[i];
    }

    @Override
    public boolean isWorkingEnabled(int i) {
        return this.workingEnabled[i];
    }

    public boolean isAllowOverclocking() {
        return this.allowOverclocking;
    }

    public long getOverclockVoltage() {
        return this.overclockVoltage;
    }

    public void setOverclockVoltage(final long overclockVoltage) {
        this.overclockPolicy = this::getOverclockVoltage;
        this.overclockVoltage = overclockVoltage;
        this.allowOverclocking = (overclockVoltage != 0);
        this.metaTileEntity.markDirty();
    }

    public boolean isDistinct() {
        return this.distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
        this.previousRecipe.clear();
        Arrays.fill(this.occupiedRecipes, null);
        this.metaTileEntity.markDirty();
    }

    /**
     * Sets the overclocking policy to use getOverclockVoltage() instead of getMaxVoltage()
     * and initialises the overclock voltage to max voltage.
     * The actual value will come from the saved tag when the tile is loaded for pre-existing machines.
     * <p>
     * NOTE: This should only be used directly after construction of the workable.
     * Use setOverclockVoltage() or setOverclockTier() for a more dynamic use case.
     */
    public void enableOverclockVoltage() {
        setOverclockVoltage(getMaxVoltage());
    }

    // The overclocking tier
    // it is 1 greater than the index into GTValues.V since here the "0 tier" represents 0 EU or no overclock
    public int getOverclockTier() {
        if (this.overclockVoltage == 0) {
            return 0;
        }
        return 1 + this.getOverclockingTier(this.overclockVoltage);
    }

    public void setOverclockTier(final int tier) {
        if (tier == 0) {
            this.setOverclockVoltage(0);
            return;
        }
        this.setOverclockVoltage(getVoltageByTier(tier - 1));
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
        }
        this.getMetaTileEntity().getHolder().scheduleChunkForRenderUpdate();
    }

    @Override
    public void writeInitialData(PacketBuffer buf) {
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialData(PacketBuffer buf) {
        this.isActive = buf.readBoolean();
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled, int i) {
        this.workingEnabled[i] = workingEnabled;
        this.metaTileEntity.markDirty();
    }

    @Override
    public String getName() {
        return "RecipeMapWorkable";
    }

    @Override
    public int getNetworkID() {
        return TraitNetworkIds.TRAIT_ID_WORKABLE;
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_MULTIPLE_WORKABLE) {
            return TJCapabilities.CAPABILITY_MULTIPLE_WORKABLE.cast(this);
        } else if (capability == TJCapabilities.CAPABILITY_MULTI_CONTROLLABLE) {
            return TJCapabilities.CAPABILITY_MULTI_CONTROLLABLE.cast(this);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound mainCompound = new NBTTagCompound();

        NBTTagList occupiedRecipeList = new NBTTagList();
        for (int i = 0; i < this.occupiedRecipes.length; i++) {
            Recipe recipe = this.occupiedRecipes[i];
            if (recipe != null && this.lockRecipe[i]) {
                NBTTagCompound workableInstanceCompound = new NBTTagCompound();

                NBTTagList itemInputsList = new NBTTagList();
                for (int j = 0; j < recipe.getInputs().size(); j++) {
                    NBTTagCompound itemInputsCompound = new NBTTagCompound();
                    NBTTagList itemInputsOreDictList = new NBTTagList();
                    int count = recipe.getInputs().get(j).getCount();

                    for (ItemStack itemStack : recipe.getInputs().get(j).getIngredient().getMatchingStacks()) {
                        itemInputsOreDictList.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
                    }
                    itemInputsCompound.setInteger("Count", count);
                    itemInputsCompound.setTag("ItemInputsOreDict", itemInputsOreDictList);
                    itemInputsList.appendTag(itemInputsCompound);
                }

                NBTTagList itemChancedOutputsList = new NBTTagList();
                for (int j = 0; j < recipe.getChancedOutputs().size(); j++) {
                    NBTTagCompound chanceEntryCompound = new NBTTagCompound();
                    Recipe.ChanceEntry chanceEntry = recipe.getChancedOutputs().get(j);
                    chanceEntryCompound.setTag("ItemStack", chanceEntry.getItemStack().writeToNBT(new NBTTagCompound()));
                    chanceEntryCompound.setInteger("Chance", chanceEntry.getChance());
                    chanceEntryCompound.setInteger("BoostPerTier", chanceEntry.getBoostPerTier());
                    itemChancedOutputsList.appendTag(chanceEntryCompound);
                }

                NBTTagList itemOutputsList = new NBTTagList();
                for (ItemStack itemStack : recipe.getOutputs()) {
                    itemOutputsList.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
                }
                NBTTagList fluidInputsList = new NBTTagList();
                for (FluidStack fluidStack : recipe.getFluidInputs()) {
                    fluidInputsList.appendTag(fluidStack.writeToNBT(new NBTTagCompound()));
                }
                NBTTagList fluidOutputsList = new NBTTagList();
                for (FluidStack fluidStack : recipe.getFluidOutputs()) {
                    fluidOutputsList.appendTag(fluidStack.writeToNBT(new NBTTagCompound()));
                }

                workableInstanceCompound.setTag("ItemInputs", itemInputsList);
                workableInstanceCompound.setTag("ItemChancedOutputs", itemChancedOutputsList);
                workableInstanceCompound.setTag("ItemOutputs", itemOutputsList);
                workableInstanceCompound.setTag("FluidInputs", fluidInputsList);
                workableInstanceCompound.setTag("FluidOutputs", fluidOutputsList);
                workableInstanceCompound.setInteger("Energy", recipe.getEUt());
                workableInstanceCompound.setInteger("Duration", recipe.getDuration());
                workableInstanceCompound.setInteger("Parallel", this.parallel[i]);
                workableInstanceCompound.setInteger("Index", i);
                occupiedRecipeList.appendTag(workableInstanceCompound);
            }
        }

        NBTTagList workableInstanceList = new NBTTagList();
        for (int i = 0; i < this.workingEnabled.length; i++) {
            NBTTagCompound workableInstanceCompound = new NBTTagCompound();
            workableInstanceCompound.setInteger("Index", i);
            workableInstanceCompound.setBoolean("Enabled", this.workingEnabled[i]);
            workableInstanceCompound.setBoolean("Lock", this.lockRecipe[i]);
            workableInstanceCompound.setBoolean("Active", this.isInstanceActive[i]);
            workableInstanceCompound.setInteger("MaxProgress", this.maxProgressTime[i]);
            workableInstanceCompound.setInteger("Progress", this.progressTime[i]);
            workableInstanceCompound.setInteger("EUt", this.recipeEUt[i]);
            workableInstanceCompound.setInteger("Timer", this.evictRecipeTimer[i]);

            if (this.progressTime[i] > 0) {
                NBTTagList itemOutputsList = new NBTTagList();
                for (ItemStack itemOutput : this.itemOutputs.get(i)) {
                    itemOutputsList.appendTag(itemOutput.writeToNBT(new NBTTagCompound()));
                }
                NBTTagList fluidOutputsList = new NBTTagList();
                for (FluidStack fluidOutput : this.fluidOutputs.get(i)) {
                    fluidOutputsList.appendTag(fluidOutput.writeToNBT(new NBTTagCompound()));
                }

                workableInstanceCompound.setTag("ItemOutputs", itemOutputsList);
                workableInstanceCompound.setTag("FluidOutputs", fluidOutputsList);
                workableInstanceList.appendTag(workableInstanceCompound);
            }
        }
        mainCompound.setBoolean(ALLOW_OVERCLOCKING, allowOverclocking);
        mainCompound.setLong(OVERCLOCK_VOLTAGE, overclockVoltage);
        mainCompound.setBoolean("IsActive", this.isActive);
        mainCompound.setBoolean("Distinct", this.distinct);
        mainCompound.setInteger("Size", this.size);
        mainCompound.setTag("OccupiedRecipes", occupiedRecipeList);
        mainCompound.setTag("WorkableInstances", workableInstanceList);
        return mainCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        NBTTagList occupiedRecipeList = compound.getTagList("OccupiedRecipes", Constants.NBT.TAG_COMPOUND);
        NBTTagList workableInstanceList = compound.getTagList("WorkableInstances", Constants.NBT.TAG_COMPOUND);

        if (compound.hasKey("Distinct")) {
            this.distinct = compound.getBoolean("Distinct");
        }
        if (compound.hasKey("IsActive")) {
            this.isActive = compound.getBoolean("IsActive");
        }
        if (compound.hasKey(ALLOW_OVERCLOCKING)) {
            this.allowOverclocking = compound.getBoolean(ALLOW_OVERCLOCKING);
        }
        if (compound.hasKey(OVERCLOCK_VOLTAGE)) {
            this.overclockVoltage = compound.getLong(OVERCLOCK_VOLTAGE);
        } else {
            // Calculate overclock voltage based on old allow flag
            this.overclockVoltage = this.allowOverclocking ? getMaxVoltage() : 0;
        }
        if (!compound.hasKey("Size")) {
            return;
        }

        this.size = compound.getInteger("Size");
        this.forceRecipeRecheck = new boolean[this.size];
        this.progressTime = new int[this.size];
        this.maxProgressTime = new int[this.size];
        this.recipeEUt = new int[this.size];
        this.parallel = new int[this.size];
        this.isInstanceActive = new boolean[this.size];
        this.workingEnabled = new boolean[this.size];
        this.wasActiveAndNeedsUpdate = new boolean[this.size];
        this.hasNotEnoughEnergy = new boolean[this.size];
        this.lockRecipe = new boolean[this.size];
        this.sleepTimer = new int[this.size];
        this.sleepTime = new int[this.size];
        this.failCount = new int[this.size];
        this.evictRecipeTimer = new int[this.size];
        this.occupiedRecipes = new Recipe[this.size];
        Arrays.fill(this.sleepTime, 1);
        Arrays.fill(this.workingEnabled, true);
        Arrays.fill(this.parallel, 1);

        for (NBTBase tag : workableInstanceList) {
            NBTTagCompound workableInstanceCompound = (NBTTagCompound) tag;
            int index = workableInstanceCompound.getInteger("Index");
            this.workingEnabled[index] = workableInstanceCompound.getBoolean("Enabled");
            this.lockRecipe[index] = workableInstanceCompound.getBoolean("Lock");
            this.isInstanceActive[index] = workableInstanceCompound.getBoolean("Active");
            this.maxProgressTime[index] = workableInstanceCompound.getInteger("MaxProgress");
            this.progressTime[index] = workableInstanceCompound.getInteger("Progress");
            this.recipeEUt[index] = workableInstanceCompound.getInteger("EUt");
            this.evictRecipeTimer[index] = workableInstanceCompound.getInteger("Timer");
            if (this.progressTime[index] > 0) {
                NBTTagList itemOutputsList = workableInstanceCompound.getTagList("ItemOutputs", Constants.NBT.TAG_COMPOUND);
                this.itemOutputs.put(index, NonNullList.create());
                for (NBTBase itemTag : itemOutputsList) {
                    NBTTagCompound itemOutputCompound = (NBTTagCompound) itemTag;
                    this.itemOutputs.get(index).add(new ItemStack(itemOutputCompound));
                }
                NBTTagList fluidOutputsList = workableInstanceCompound.getTagList("FluidOutputs", Constants.NBT.TAG_COMPOUND);
                this.fluidOutputs.put(index, new ArrayList<>());
                for (NBTBase fluidTag : fluidOutputsList) {
                    NBTTagCompound fluidOutputCompound = (NBTTagCompound) fluidTag;
                    this.fluidOutputs.get(index).add(FluidStack.loadFluidStackFromNBT(fluidOutputCompound));
                }
            }
        }

        for (NBTBase tag : occupiedRecipeList) {
            NBTTagCompound occupiedRecipeCompound = (NBTTagCompound) tag;
            int index = occupiedRecipeCompound.getInteger("Index");
            int parallel = occupiedRecipeCompound.getInteger("Parallel");
            int duration = occupiedRecipeCompound.getInteger("Duration");
            int energy = occupiedRecipeCompound.getInteger("Energy");
            NBTTagList itemInputsList = occupiedRecipeCompound.getTagList("ItemInputs", Constants.NBT.TAG_COMPOUND);
            NBTTagList itemChancedOutputsList = occupiedRecipeCompound.getTagList("ItemChancedOutputs", Constants.NBT.TAG_COMPOUND);
            NBTTagList itemOutputsList = occupiedRecipeCompound.getTagList("ItemOutputs", Constants.NBT.TAG_COMPOUND);
            NBTTagList fluidInputsList = occupiedRecipeCompound.getTagList("FluidInputs", Constants.NBT.TAG_COMPOUND);
            NBTTagList fluidOutputsList = occupiedRecipeCompound.getTagList("FluidOutputs", Constants.NBT.TAG_COMPOUND);

            List<CountableIngredient> inputIngredients = NonNullList.create();
            for (NBTBase ingredientTag : itemInputsList) {
                NBTTagCompound itemInputsCompound = (NBTTagCompound) ingredientTag;
                NBTTagList itemInputsOreDictList = itemInputsCompound.getTagList("ItemInputsOreDict", Constants.NBT.TAG_COMPOUND);
                int count = itemInputsCompound.getInteger("Count");

                int i = 0;
                ItemStack[] oreStacks = new ItemStack[itemInputsOreDictList.tagCount()];
                for (NBTBase itemTag : itemInputsOreDictList) {
                    NBTTagCompound itemStackCompound = (NBTTagCompound) itemTag;
                    oreStacks[i++] = new ItemStack(itemStackCompound);
                }
                inputIngredients.add(new CountableIngredient(Ingredient.fromStacks(oreStacks), count));
            }

            List<Recipe.ChanceEntry> chanceOutputs = new ArrayList<>();
            for (NBTBase chanceOutputTag : itemChancedOutputsList) {
                NBTTagCompound chanceEntryCompound = (NBTTagCompound) chanceOutputTag;
                ItemStack itemStack = new ItemStack(chanceEntryCompound.getCompoundTag("ItemStack"));
                int chance = chanceEntryCompound.getInteger("Chance");
                int boost = chanceEntryCompound.getInteger("BoostPerTier");
                chanceOutputs.add(new Recipe.ChanceEntry(itemStack, chance, boost));
            }

            List<ItemStack> itemOutputs = NonNullList.create();
            for (NBTBase outputTag : itemOutputsList) {
                NBTTagCompound outputCompound = (NBTTagCompound) outputTag;
                itemOutputs.add(new ItemStack(outputCompound));
            }

            List<FluidStack> fluidInputs = new ArrayList<>();
            for (NBTBase inputTag : fluidInputsList) {
                NBTTagCompound inputCompound = (NBTTagCompound) inputTag;
                fluidInputs.add(FluidStack.loadFluidStackFromNBT(inputCompound));
            }

            List<FluidStack> fluidOutputs = new ArrayList<>();
            for (NBTBase outputTag : fluidOutputsList) {
                NBTTagCompound outputCompound = (NBTTagCompound) outputTag;
                fluidOutputs.add(FluidStack.loadFluidStackFromNBT(outputCompound));
            }

            Recipe recipe = new Recipe(inputIngredients, itemOutputs, chanceOutputs, fluidInputs, fluidOutputs, duration, energy, false);
            this.occupiedRecipes[index] = this.controller.parallelRecipeMap[this.controller.getRecipeMapIndex()].findRecipe(recipe);
            this.parallel[index] = parallel;
        }
    }
}
