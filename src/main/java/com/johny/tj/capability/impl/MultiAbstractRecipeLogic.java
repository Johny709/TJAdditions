package com.johny.tj.capability.impl;

import com.johny.tj.builder.multicontrollers.MultipleRecipeMapMultiblockController;
import com.johny.tj.capability.IMultipleWorkable;
import com.johny.tj.capability.TJCapabilities;
import gregtech.api.GTValues;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.GTUtility;
import gregtech.common.ConfigHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
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

public abstract class MultiAbstractRecipeLogic extends MTETrait implements IMultipleWorkable {

    private static final String ALLOW_OVERCLOCKING = "AllowOverclocking";
    private static final String OVERCLOCK_VOLTAGE = "OverclockVoltage";

    protected MultipleRecipeMapMultiblockController controller;
    protected RecipeMap<?> recipeMap;
    private final int size;

    protected boolean[] forceRecipeRecheck;
    protected ItemStack[][] lastItemInputs;
    protected FluidStack[][] lastFluidInputs;
    public MultiRecipeLRUCache previousRecipe;
    public int recipeCacheSize;
    protected boolean useOptimizedRecipeLookUp = true;
    protected boolean allowOverclocking = true;
    private long overclockVoltage = 0;
    private LongSupplier overclockPolicy = this::getMaxVoltage;

    protected int[] progressTime;
    protected int[] maxProgressTime;
    protected int[] recipeEUt;
    protected Recipe[] occupiedRecipes;
    protected Map<Integer, List<FluidStack>> fluidOutputs;
    protected Map<Integer, NonNullList<ItemStack>> itemOutputs;
    protected final Random random = new Random();

    protected boolean[] isActive;
    protected boolean[] workingEnabled;
    protected boolean[] hasNotEnoughEnergy;
    protected boolean[] wasActiveAndNeedsUpdate;
    protected boolean[] lockRecipe;
    private final long[] V;
    private final String[] VN;

    private final int[] sleepTimer;
    private final int[] sleepTime;
    private final int[] failCount;
    protected final int[] timeToStop;

    public MultiAbstractRecipeLogic(MetaTileEntity metaTileEntity, int recipeCacheSize, int size) {
        super(metaTileEntity);
        this.controller = (MultipleRecipeMapMultiblockController) metaTileEntity;
        this.size = size;
        this.recipeCacheSize = recipeCacheSize;
        this.forceRecipeRecheck = new boolean[this.size];
        this.lastItemInputs = new ItemStack[this.size][];
        this.lastFluidInputs = new FluidStack[this.size][];
        this.previousRecipe = new MultiRecipeLRUCache(this.recipeCacheSize);
        this.progressTime = new int[this.size];
        this.maxProgressTime = new int[this.size];
        this.recipeEUt = new int[this.size];
        this.fluidOutputs = new HashMap<>();
        this.itemOutputs = new HashMap<>();
        this.isActive = new boolean[this.size];
        this.workingEnabled = new boolean[this.size];
        this.hasNotEnoughEnergy = new boolean[this.size];
        this.wasActiveAndNeedsUpdate = new boolean[this.size];
        this.lockRecipe = new boolean[this.size];
        this.sleepTimer = new int[this.size];
        this.sleepTime = new int[this.size];
        this.failCount = new int[this.size];
        this.timeToStop = new int[this.size];
        this.occupiedRecipes = new Recipe[this.size];

        Arrays.fill(sleepTime, 1);
        Arrays.fill(workingEnabled, true);
        if (ConfigHolder.gregicalityOverclocking) {
            V = GTValues.V2;
            VN = GTValues.VN2;
        } else {
            V = GTValues.V;
            VN = GTValues.VN;
        }
    }

    public void removeFrom(int i) {
        forceRecipeRecheck[i] = false;
        progressTime[i] = 0;
        maxProgressTime[i] = 0;
        recipeEUt[i] = 0;
        fluidOutputs.remove(i);
        itemOutputs.remove(i);
        isActive[i] = false;
        workingEnabled[i] = false;
        hasNotEnoughEnergy[i] = false;
        wasActiveAndNeedsUpdate[i] = false;
        sleepTimer[i] = 0;
        sleepTime[i] = 0;
        failCount[i] = 0;
        occupiedRecipes[i] = null;
    }

    protected abstract long getEnergyStored();

    protected abstract long getEnergyCapacity();

    protected abstract boolean drawEnergy(int recipeEUt);

    protected abstract long getMaxVoltage();

    protected IItemHandlerModifiable getInputInventory() {
        return metaTileEntity.getImportItems();
    }

    protected IItemHandlerModifiable getOutputInventory() {
        return metaTileEntity.getExportItems();
    }

    protected IMultipleTankHandler getInputTank() {
        return metaTileEntity.getImportFluids();
    }

    protected IMultipleTankHandler getOutputTank() {
        return metaTileEntity.getExportFluids();
    }

    public int getSize() {
        return size;
    }

    public void setLockingMode(boolean setLockingMode, int i) {
        lockRecipe[i] = setLockingMode;
    }

    public boolean getLockingMode(int i) {
        return lockRecipe[i];
    }

    public void update(int i) {
        if (!getMetaTileEntity().getWorld().isRemote) {
            if (workingEnabled[i]) {
                if (progressTime[i] > 0) {
                    updateRecipeProgress(i);
                }
                if (progressTime[i] == 0 && sleepTimer[i] == 0) {
                    boolean result = trySearchNewRecipe(i);
                    if (!result) {
                        failCount[i]++;
                        if (failCount[i] == 5) {

                            sleepTime[i] = Math.min(sleepTime[i] * 2, (ConfigHolder.maxSleepTime >= 0 && ConfigHolder.maxSleepTime <= 400) ? ConfigHolder.maxSleepTime : 20);
                            failCount[i] = 0;

                        }
                        sleepTimer[i] = sleepTime[i];
                    } else {
                        sleepTime[i] = 1;
                        failCount[i] = 0;
                    }
                }
                if (sleepTimer[i] > 0) {
                    sleepTimer[i]--;
                }
                if (timeToStop[i] > 0) {
                    if (!lockRecipe[i]) {
                        if (progressTime[i] <= 0) {
                            if (--timeToStop[i] % 20 == 0) {
                                if (occupiedRecipes[i] != null) {
                                    controller.multiRecipeMap.addRecipe(occupiedRecipes[i]);
                                }
                                occupiedRecipes[i] = null;
                                setActive(false, i);
                            }
                        }
                    }
                }
            }
            if (wasActiveAndNeedsUpdate[i]) {
                wasActiveAndNeedsUpdate[i] = false;
                setActive(false, i);
            }
        }
    }

    protected void updateRecipeProgress(int i) {
        boolean drawEnergy = drawEnergy(recipeEUt[i]);
        if (drawEnergy || (recipeEUt[i] < 0)) {
            //as recipe starts with progress on 1 this has to be > only not => to compensate for it
            if (++progressTime[i] > maxProgressTime[i]) {
                completeRecipe(i);
            }
        } else if (recipeEUt[i] > 0) {
            //only set hasNotEnoughEnergy if this recipe is consuming recipe
            //generators always have enough energy
            this.hasNotEnoughEnergy[i] = true;
            //if current progress value is greater than 2, decrement it by 2
            if (progressTime[i] >= 2) {
                if (ConfigHolder.insufficientEnergySupplyWipesRecipeProgress) {
                    this.progressTime[i] = 1;
                } else {
                    this.progressTime[i] = Math.max(1, progressTime[i] - 2);
                }
            }
        }
    }

    protected boolean trySearchNewRecipe(int i) {
        long maxVoltage = controller.getMaxVoltage();
        Recipe currentRecipe = null;
        IItemHandlerModifiable importInventory = getInputInventory();
        IMultipleTankHandler importFluids = getInputTank();
        Recipe foundRecipe;
        if (lockRecipe[i]) {
            foundRecipe = occupiedRecipes[i];
        } else {
            foundRecipe = this.previousRecipe.get(importInventory, importFluids, i);
        }
        if (foundRecipe != null) {
            //if previous recipe still matches inputs, try to use it
            currentRecipe = foundRecipe;
        } else {
            boolean dirty = checkRecipeInputsDirty(importInventory, importFluids, i);
            if (dirty || forceRecipeRecheck[i]) {
                this.forceRecipeRecheck[i] = false;
                //else, try searching new recipe for given inputs
                currentRecipe = findRecipe(maxVoltage, importInventory, importFluids, this.useOptimizedRecipeLookUp);
                if (currentRecipe != null) {
                    this.occupiedRecipes[i] = currentRecipe;
                    this.controller.multiRecipeMap.removeRecipe(currentRecipe);
                    this.previousRecipe.put(currentRecipe, i);
                    this.previousRecipe.cacheUnutilized();
                }
            }
        }
        if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
            if (foundRecipe != null) {
                this.previousRecipe.cacheUtilized(i);
            }
            setupRecipe(currentRecipe, i);
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
        setUseOptimizedRecipeLookUp(!this.useOptimizedRecipeLookUp);
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
        return controller.multiRecipeMap.findRecipe(maxVoltage, inputs, fluidInputs, getMinTankCapacity(getOutputTank()), useOptimizedRecipeLookUp);
    }

    protected boolean checkRecipeInputsDirty(IItemHandler inputs, IMultipleTankHandler fluidInputs, int i) {
        boolean shouldRecheckRecipe = false;
        if (lastItemInputs[i] == null || lastItemInputs[i].length != inputs.getSlots()) {
            this.lastItemInputs[i] = new ItemStack[inputs.getSlots()];
            Arrays.fill(lastItemInputs[i], ItemStack.EMPTY);
        }
        if (lastFluidInputs[i] == null || lastFluidInputs[i].length != fluidInputs.getTanks()) {
            this.lastFluidInputs[i] = new FluidStack[fluidInputs.getTanks()];
        }
        for (int j = 0; j < lastItemInputs[i].length; j++) {
            ItemStack currentStack = inputs.getStackInSlot(j);
            ItemStack lastStack = lastItemInputs[i][j];
            if (!areItemStacksEqual(currentStack, lastStack)) {
                this.lastItemInputs[i][j] = currentStack.isEmpty() ? ItemStack.EMPTY : currentStack.copy();
                shouldRecheckRecipe = true;
            } else if (currentStack.getCount() != lastStack.getCount()) {
                lastStack.setCount(currentStack.getCount());
                shouldRecheckRecipe = true;
            }
        }
        for (int j = 0; j < lastFluidInputs[i].length; j++) {
            FluidStack currentStack = fluidInputs.getTankAt(j).getFluid();
            FluidStack lastStack = lastFluidInputs[i][j];
            if ((currentStack == null && lastStack != null) ||
                    (currentStack != null && !currentStack.isFluidEqual(lastStack))) {
                this.lastFluidInputs[i][j] = currentStack == null ? null : currentStack.copy();
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
        int[] resultOverclock = calculateOverclock(recipe.getEUt(), recipe.getDuration());
        int totalEUt = resultOverclock[0] * resultOverclock[1];
        IItemHandlerModifiable importInventory = getInputInventory();
        IItemHandlerModifiable exportInventory = getOutputInventory();
        IMultipleTankHandler importFluids = getInputTank();
        IMultipleTankHandler exportFluids = getOutputTank();
        return (totalEUt >= 0 ? getEnergyStored() >= (totalEUt > getEnergyCapacity() / 2 ? resultOverclock[0] : totalEUt) :
                (getEnergyStored() - resultOverclock[0] <= getEnergyCapacity())) &&
                MetaTileEntity.addItemsToItemHandler(exportInventory, true, recipe.getAllItemOutputs(exportInventory.getSlots())) &&
                MetaTileEntity.addFluidsToFluidHandler(exportFluids, true, recipe.getFluidOutputs()) &&
                recipe.matches(true, importInventory, importFluids);
    }

    protected int[] calculateOverclock(int EUt, int duration) {
        return calculateOverclock(EUt, this.overclockPolicy.getAsLong(), duration);
    }

    protected int[] calculateOverclock(int EUt, long voltage, int duration) {

        if (!allowOverclocking) {
            return new int[]{EUt, duration};
        }
        boolean negativeEU = EUt < 0;
        int tier = getOverclockingTier(voltage);
        if (V[tier] <= EUt || tier == 0)
            return new int[]{EUt, duration};
        if (negativeEU)
            EUt = -EUt;
        int resultEUt = EUt;
        double resultDuration = duration;
        //do not overclock further if duration is already too small
        while (resultDuration >= 1 && resultEUt <= V[tier - 1]) {
            resultEUt *= 4;
            resultDuration /= 2.8;
        }
        return new int[]{negativeEU ? -resultEUt : resultEUt, (int) Math.ceil(resultDuration)};
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
        final int maxTier = getOverclockingTier(getMaxVoltage());
        final String[] result = new String[maxTier + 2];
        result[0] = "gregtech.gui.overclock.off";
        for (int i = 0; i < maxTier + 1; ++i) {
            result[i + 1] = VN[i];
        }
        return result;
    }

    protected void setupRecipe(Recipe recipe, int i) {
        int[] resultOverclock = calculateOverclock(recipe.getEUt(), recipe.getDuration());
        this.progressTime[i] = 1;
        this.timeToStop[i] = 20;
        setMaxProgress(resultOverclock[1], i);
        this.recipeEUt[i] = resultOverclock[0];
        this.fluidOutputs.put(i, GTUtility.copyFluidList(recipe.getFluidOutputs()));
        int tier = getMachineTierForRecipe(recipe);
        this.itemOutputs.put(i, GTUtility.copyStackList(recipe.getResultItemOutputs(getOutputInventory().getSlots(), random, tier)));
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
        MetaTileEntity.addItemsToItemHandler(getOutputInventory(), false, itemOutputs.get(i));
        MetaTileEntity.addFluidsToFluidHandler(getOutputTank(), false, fluidOutputs.get(i));
        this.progressTime[i] = 0;
        setMaxProgress(0, i);
        this.recipeEUt[i] = 0;
        this.fluidOutputs.remove(i);
        this.itemOutputs.remove(i);
        this.hasNotEnoughEnergy[i] = false;
        this.wasActiveAndNeedsUpdate[i] = true;
        //force recipe recheck because inputs could have changed since last time
        //we checked them before starting our recipe, especially if recipe took long time
        this.forceRecipeRecheck[i] = true;
    }

    public double getProgressPercent(int i) {
        return getMaxProgress(i) == 0 ? 0.0 : getProgress(i) / (getMaxProgress(i) * 1.0);
    }

    public int getTicksTimeLeft(int i) {
        return maxProgressTime[i] == 0 ? 0 : (maxProgressTime[i] - progressTime[i]);
    }

    @Override
    public int getProgress(int i) {
        return progressTime[i];
    }

    @Override
    public int getMaxProgress(int i) {
       return maxProgressTime[i];
    }

    public int getRecipeEUt(int i) {
        return recipeEUt[i];
    }

    public void setMaxProgress(int maxProgress, int i) {
        this.maxProgressTime[i] = maxProgress;
    }

    protected void setActive(boolean active, int i) {
        this.isActive[i] = active;
        metaTileEntity.markDirty();
        if (!metaTileEntity.getWorld().isRemote) {
            writeCustomData(i + 1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public boolean isActive(int i) {
        return isActive[i];
    }

    @Override
    public boolean isWorkingEnabled(int i) {
        return workingEnabled[i];
    }

    public boolean isAllowOverclocking() {
        return allowOverclocking;
    }

    public long getOverclockVoltage() {
        return overclockVoltage;
    }

    public void setOverclockVoltage(final long overclockVoltage) {
        this.overclockPolicy = this::getOverclockVoltage;
        this.overclockVoltage = overclockVoltage;
        this.allowOverclocking = (overclockVoltage != 0);
        metaTileEntity.markDirty();
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
        return 1 + getOverclockingTier(this.overclockVoltage);
    }

    public void setOverclockTier(final int tier) {
        if (tier == 0) {
            setOverclockVoltage(0);
            return;
        }
        setOverclockVoltage(getVoltageByTier(tier - 1));
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        for (int i = 0; i < size; i++) {
            if (dataId == i + 1) {
                this.isActive[i] = buf.readBoolean();
            }
        }
        getMetaTileEntity().getHolder().scheduleChunkForRenderUpdate();
    }

    @Override
    public void writeInitialData(PacketBuffer buf) {
        for (int i = 0; i < size; i++) {
            buf.writeBoolean(this.isActive[i]);
        }
    }

    @Override
    public void receiveInitialData(PacketBuffer buf) {
        for (int i = 0; i < size; i++) {
            this.isActive[i] = buf.readBoolean();
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled, int i) {
        this.workingEnabled[i] = workingEnabled;
        metaTileEntity.markDirty();
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
        if (capability == TJCapabilities.CAPABILITY_MULTIPLEWORKABLE) {
            return TJCapabilities.CAPABILITY_MULTIPLEWORKABLE.cast(this);
        } else if (capability == TJCapabilities.CAPABILITY_MULTICONTROLLABLE) {
            return TJCapabilities.CAPABILITY_MULTICONTROLLABLE.cast(this);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound mainCompound = new NBTTagCompound(),
                workingCompound = new NBTTagCompound(),
                isActiveCompound = new NBTTagCompound(),
                progressCompound = new NBTTagCompound(),
                maxProgressCompound = new NBTTagCompound(),
                recipeEUtCompound = new NBTTagCompound(),
                lockCompound = new NBTTagCompound(),
                timerCompound = new NBTTagCompound(),
                itemCompound = new NBTTagCompound(),
                fluidCompound = new NBTTagCompound(),
                recipeItemInputCompound = new NBTTagCompound(),
                recipeItemInputCountCompound = new NBTTagCompound(),
                recipeItemOutputCompound = new NBTTagCompound(),
                recipeFluidInputCompound = new NBTTagCompound(),
                recipeFluidOutputCompound = new NBTTagCompound(),
                recipeChancedOutputCompound = new NBTTagCompound(),
                recipeChancedOutputChanceCompound = new NBTTagCompound(),
                recipeChancedOutputBoostCompound = new NBTTagCompound(),
                recipeEnergyCompound = new NBTTagCompound(),
                recipeDurationCompound = new NBTTagCompound(),
                recipeIndexCompound = new NBTTagCompound();

        NBTTagList workingList = new NBTTagList(),
                isActiveList = new NBTTagList(),
                maxProgressList = new NBTTagList(),
                progressList = new NBTTagList(),
                EUtList = new NBTTagList(),
                lockList = new NBTTagList(),
                timerList = new NBTTagList(),
                totalOutputItemsList = new NBTTagList(),
                totalOutputFluidsList = new NBTTagList(),
                totalRecipeItemInputsList = new NBTTagList(),
                totalRecipeItemInputsCountList = new NBTTagList(),
                totalRecipeItemOutputList = new NBTTagList(),
                totalRecipeFluidInputsList = new NBTTagList(),
                totalRecipeFluidOutputsList = new NBTTagList(),
                totalRecipeChancedOutputsList = new NBTTagList(),
                totalRecipeChancedOutputsChanceList = new NBTTagList(),
                totalRecipeChancedOutputsBoostList = new NBTTagList(),
                recipeEnergyList = new NBTTagList(),
                recipeDurationList = new NBTTagList(),
                recipeIndexList = new NBTTagList();

        mainCompound.setBoolean(ALLOW_OVERCLOCKING, allowOverclocking);
        mainCompound.setLong(OVERCLOCK_VOLTAGE, overclockVoltage);

        for (int i = 0, recipeIndex = 0; i < occupiedRecipes.length; i++) {
            if (occupiedRecipes[i] != null) {
                if (lockRecipe[i]) {
                    NBTTagList recipeItemInputsList = new NBTTagList();
                    NBTTagList recipeItemInputsCountList = new NBTTagList();
                    for (int j = 0; j < occupiedRecipes[i].getInputs().size(); j++) {
                        NBTTagList recipeItemInputsSlotsList = new NBTTagList();
                        for (ItemStack itemStack : occupiedRecipes[i].getInputs().get(j).getIngredient().getMatchingStacks()) {
                            recipeItemInputsSlotsList.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
                        }
                        NBTTagCompound recipeItemInputsSlotsCompound = new NBTTagCompound();
                        recipeItemInputsSlotsCompound.setTag("RecipeItemInput" + i + ":" + j, recipeItemInputsSlotsList);
                        recipeItemInputsList.appendTag(recipeItemInputsSlotsCompound);
                        NBTTagCompound recipeItemInputsCountCompound = new NBTTagCompound();
                        recipeItemInputsCountCompound.setInteger("RecipeItemInputCount" + i + ":" + j, occupiedRecipes[i].getInputs().get(j).getCount());
                        recipeItemInputsCountList.appendTag(recipeItemInputsCountCompound);
                    }

                    NBTTagList recipeItemOutputsList = new NBTTagList();
                    for (ItemStack itemStack : occupiedRecipes[i].getOutputs()) {
                        recipeItemOutputsList.appendTag(itemStack.writeToNBT(new NBTTagCompound()));
                    }
                    NBTTagList recipeFluidInputsList = new NBTTagList();
                    for (FluidStack fluidStack : occupiedRecipes[i].getFluidInputs()) {
                        recipeFluidInputsList.appendTag(fluidStack.writeToNBT(new NBTTagCompound()));
                    }
                    NBTTagList recipeFluidOutputsList = new NBTTagList();
                    for (FluidStack fluidStack : occupiedRecipes[i].getFluidOutputs()) {
                        recipeFluidOutputsList.appendTag(fluidStack.writeToNBT(new NBTTagCompound()));
                    }
                    NBTTagList recipeChancedOutputsList = new NBTTagList();
                    NBTTagList recipeChancedOutputsChanceList = new NBTTagList();
                    NBTTagList recipeChancedOutputsBoostList = new NBTTagList();
                    for (int k = 0; k < occupiedRecipes[i].getChancedOutputs().size(); k++) {
                        Recipe.ChanceEntry chanceEntry = occupiedRecipes[i].getChancedOutputs().get(k);
                        NBTTagCompound recipeChancedOutputsChanceCompound = new NBTTagCompound();
                        NBTTagCompound recipeChancedOutputsBoostCompound = new NBTTagCompound();
                        recipeChancedOutputsList.appendTag(chanceEntry.getItemStack().writeToNBT(new NBTTagCompound()));
                        recipeChancedOutputsChanceCompound.setInteger("RecipeChancedOutputChance" + i + ":" + k, chanceEntry.getChance());
                        recipeChancedOutputsChanceList.appendTag(recipeChancedOutputsChanceCompound);
                        recipeChancedOutputsBoostCompound.setInteger("RecipeChancedOutputBoost" + i + ":" + k, chanceEntry.getBoostPerTier());
                        recipeChancedOutputsBoostList.appendTag(recipeChancedOutputsBoostCompound);
                    }

                    recipeItemInputCompound.setTag("RecipeItemInput" + i, recipeItemInputsList);
                    totalRecipeItemInputsList.appendTag(recipeItemInputCompound);
                    recipeItemInputCountCompound.setTag("RecipeItemInputCount" + i, recipeItemInputsCountList);
                    totalRecipeItemInputsCountList.appendTag(recipeItemInputCountCompound);
                    recipeItemOutputCompound.setTag("RecipeItemOutput" + i, recipeItemOutputsList);
                    totalRecipeItemOutputList.appendTag(recipeItemOutputCompound);
                    recipeFluidInputCompound.setTag("RecipeFluidInput" + i, recipeFluidInputsList);
                    totalRecipeFluidInputsList.appendTag(recipeFluidInputCompound);
                    recipeFluidOutputCompound.setTag("RecipeFluidOutput" + i, recipeFluidOutputsList);
                    totalRecipeFluidOutputsList.appendTag(recipeFluidOutputCompound);
                    recipeChancedOutputCompound.setTag("RecipeChancedOutput" + i, recipeChancedOutputsList);
                    totalRecipeChancedOutputsList.appendTag(recipeChancedOutputCompound);
                    recipeChancedOutputChanceCompound.setTag("RecipeChancedOutputChance" + i, recipeChancedOutputsChanceList);
                    totalRecipeChancedOutputsChanceList.appendTag(recipeChancedOutputChanceCompound);
                    recipeChancedOutputBoostCompound.setTag("RecipeChancedOutputBoost" + i, recipeChancedOutputsBoostList);
                    totalRecipeChancedOutputsBoostList.appendTag(recipeChancedOutputBoostCompound);
                    recipeEnergyCompound.setInteger("RecipeEnergy" + i, occupiedRecipes[i].getEUt());
                    recipeEnergyList.appendTag(recipeEnergyCompound);
                    recipeDurationCompound.setInteger("RecipeDuration" + i, occupiedRecipes[i].getDuration());
                    recipeDurationList.appendTag(recipeDurationCompound);
                    recipeIndexCompound.setInteger("RecipeIndex" + recipeIndex++, i);
                    recipeIndexList.appendTag(recipeIndexCompound);
                }
            }
        }

        for (int i = 0; i < workingEnabled.length; i++) {

            workingCompound.setBoolean("WorkingEnabled" + i, workingEnabled[i]);
            workingList.appendTag(workingCompound);

            lockCompound.setBoolean("RecipeLock" + i, lockRecipe[i]);
            lockList.appendTag(lockCompound);

            if (progressTime[i] > 0) {
                isActiveCompound.setBoolean("IsActive" + i, isActive[i]);
                isActiveList.appendTag(isActiveCompound);

                progressCompound.setInteger("Progress" + i, progressTime[i]);
                progressList.appendTag(progressCompound);

                maxProgressCompound.setInteger("MaxProgress" + i, maxProgressTime[i]);
                maxProgressList.appendTag(maxProgressCompound);

                recipeEUtCompound.setInteger("RecipeEUt" + i, recipeEUt[i]);
                EUtList.appendTag(recipeEUtCompound);

                timerCompound.setInteger("Timer" + i, timeToStop[i]);
                timerList.appendTag(timerCompound);

                NBTTagList itemOutputsList = new NBTTagList();
                for (ItemStack itemOutput : itemOutputs.get(i)) {
                    itemOutputsList.appendTag(itemOutput.writeToNBT(new NBTTagCompound()));
                }
                NBTTagList fluidOutputsList = new NBTTagList();
                for (FluidStack fluidOutput : fluidOutputs.get(i)) {
                    fluidOutputsList.appendTag(fluidOutput.writeToNBT(new NBTTagCompound()));
                }
                itemCompound.setTag("ItemOutputs" + i, itemOutputsList);
                totalOutputItemsList.appendTag(itemCompound);

                fluidCompound.setTag("FluidOutputs" + i, fluidOutputsList);
                totalOutputFluidsList.appendTag(fluidCompound);
            }
        }
        mainCompound.setTag("ItemOutputs", totalOutputItemsList);
        mainCompound.setTag("FluidOutputs", totalOutputFluidsList);
        mainCompound.setTag("WorkingEnabled", workingList);
        mainCompound.setTag("IsActive", isActiveList);
        mainCompound.setTag("Progress", progressList);
        mainCompound.setTag("MaxProgress", maxProgressList);
        mainCompound.setTag("RecipeEUt", EUtList);
        mainCompound.setTag("RecipeLock", lockList);
        mainCompound.setTag("Timer", timerList);
        mainCompound.setTag("RecipeItemInput", totalRecipeItemInputsList);
        mainCompound.setTag("RecipeItemInputCount", totalRecipeItemInputsCountList);
        mainCompound.setTag("RecipeItemOutput", totalRecipeItemOutputList);
        mainCompound.setTag("RecipeFluidInput", totalRecipeFluidInputsList);
        mainCompound.setTag("RecipeFluidOutput", totalRecipeFluidOutputsList);
        mainCompound.setTag("RecipeChancedOutput", totalRecipeChancedOutputsList);
        mainCompound.setTag("RecipeChancedOutputChance", totalRecipeChancedOutputsChanceList);
        mainCompound.setTag("RecipeChancedOutputBoost", totalRecipeChancedOutputsBoostList);
        mainCompound.setTag("RecipeEnergy", recipeEnergyList);
        mainCompound.setTag("RecipeDuration", recipeDurationList);
        mainCompound.setTag("RecipeIndex", recipeIndexList);
        return mainCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        NBTTagList totalOutputItemsList = compound.getTagList("ItemOutputs", Constants.NBT.TAG_COMPOUND),
                totalOutputFluidsList = compound.getTagList("FluidOutputs", Constants.NBT.TAG_COMPOUND),
                workingList = compound.getTagList("WorkingEnabled", Constants.NBT.TAG_COMPOUND),
                isActiveList = compound.getTagList("IsActive", Constants.NBT.TAG_COMPOUND),
                progressList = compound.getTagList("Progress", Constants.NBT.TAG_COMPOUND),
                maxProgressList = compound.getTagList("MaxProgress", Constants.NBT.TAG_COMPOUND),
                EUtList = compound.getTagList("RecipeEUt", Constants.NBT.TAG_COMPOUND),
                lockList = compound.getTagList("RecipeLock", Constants.NBT.TAG_COMPOUND),
                timerList = compound.getTagList("Timer", Constants.NBT.TAG_COMPOUND),
                totalRecipeItemInputsList = compound.getTagList("RecipeItemInput", Constants.NBT.TAG_COMPOUND),
                totalRecipeItemInputsCountList = compound.getTagList("RecipeItemInputCount", Constants.NBT.TAG_COMPOUND),
                totalRecipeItemOutputsList = compound.getTagList("RecipeItemOutput", Constants.NBT.TAG_COMPOUND),
                totalRecipeFluidInputsList = compound.getTagList("RecipeFluidInput", Constants.NBT.TAG_COMPOUND),
                totalRecipeFluidOutputsList = compound.getTagList("RecipeFluidOutput", Constants.NBT.TAG_COMPOUND),
                totalRecipeChancedOutputsList = compound.getTagList("RecipeChancedOutput", Constants.NBT.TAG_COMPOUND),
                totalRecipeChancedOutputsChanceList = compound.getTagList("RecipeChancedOutputChance", Constants.NBT.TAG_COMPOUND),
                totalRecipeChancedOutputsBoostList = compound.getTagList("RecipeChancedOutputBoost", Constants.NBT.TAG_COMPOUND),
                recipeEnergyList = compound.getTagList("RecipeEnergy", Constants.NBT.TAG_COMPOUND),
                recipeDurationList = compound.getTagList("RecipeDuration", Constants.NBT.TAG_COMPOUND),
                recipeIndexList = compound.getTagList("RecipeIndex", Constants.NBT.TAG_COMPOUND);

        if (compound.hasKey(ALLOW_OVERCLOCKING)) {
            allowOverclocking = compound.getBoolean(ALLOW_OVERCLOCKING);
        }
        if (compound.hasKey(OVERCLOCK_VOLTAGE)) {
            overclockVoltage = compound.getLong(OVERCLOCK_VOLTAGE);
        } else {
            // Calculate overclock voltage based on old allow flag
            overclockVoltage = allowOverclocking ? getMaxVoltage() : 0;
        }
        for (int i = 0; i < workingList.tagCount(); i++) {
            workingEnabled[i] = workingList.getCompoundTagAt(i).getBoolean("WorkingEnabled" + i);
            progressTime[i] = progressList.getCompoundTagAt(i).getInteger("Progress" + i);
            lockRecipe[i] = lockList.getCompoundTagAt(i).getBoolean("RecipeLock" + i);
            if (progressTime[i] > 0) {
                isActive[i] = isActiveList.getCompoundTagAt(i).getBoolean("IsActive" + i);
                maxProgressTime[i] = maxProgressList.getCompoundTagAt(i).getInteger("MaxProgress" + i);
                recipeEUt[i] = EUtList.getCompoundTagAt(i).getInteger("RecipeEUt" + i);
                timeToStop[i] = timerList.getCompoundTagAt(i).getInteger("Timer" + i);
                NBTTagList itemOutputsList = totalOutputItemsList.getCompoundTagAt(i).getTagList("ItemOutputs" + i, Constants.NBT.TAG_COMPOUND);
                itemOutputs.put(i, NonNullList.create());
                for (int j = 0; j < itemOutputsList.tagCount(); j++) {
                    itemOutputs.get(i).add(new ItemStack(itemOutputsList.getCompoundTagAt(j)));
                }
                NBTTagList fluidOutputsList = totalOutputFluidsList.getCompoundTagAt(i).getTagList("FluidOutputs" + i, Constants.NBT.TAG_COMPOUND);
                fluidOutputs.put(i, new ArrayList<>());
                for (int j = 0; j < fluidOutputsList.tagCount(); j++) {
                    fluidOutputs.get(i).add(FluidStack.loadFluidStackFromNBT(fluidOutputsList.getCompoundTagAt(j)));
                }
            }
        }
        for (int i = 0; i < recipeIndexList.tagCount(); i++) {
            int recipeIndex = recipeIndexList.getCompoundTagAt(i).getInteger("RecipeIndex" + i);
            NBTTagList recipeItemInputsList = totalRecipeItemInputsList.getCompoundTagAt(i).getTagList("RecipeItemInput" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeItemInputsCountList = totalRecipeItemInputsCountList.getCompoundTagAt(i).getTagList("RecipeItemInputCount" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeItemOutputsList = totalRecipeItemOutputsList.getCompoundTagAt(i).getTagList("RecipeItemOutput" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeFluidInputsList = totalRecipeFluidInputsList.getCompoundTagAt(i).getTagList("RecipeFluidInput" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeFluidOutputsList = totalRecipeFluidOutputsList.getCompoundTagAt(i).getTagList("RecipeFluidOutput" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeChancedOutputsList = totalRecipeChancedOutputsList.getCompoundTagAt(i).getTagList("RecipeChancedOutput" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeChancedOutputsChanceList = totalRecipeChancedOutputsChanceList.getCompoundTagAt(i).getTagList("RecipeChancedOutputChance" + recipeIndex, Constants.NBT.TAG_COMPOUND),
                    recipeChancedOutputsBoostList = totalRecipeChancedOutputsBoostList.getCompoundTagAt(i).getTagList("RecipeChancedOutputBoost" + recipeIndex, Constants.NBT.TAG_COMPOUND);

            List<CountableIngredient> inputIngredients = NonNullList.create();
            for (int j = 0; j < recipeItemInputsList.tagCount(); j++) {
                NBTTagList recipeItemInputsOreDictList = recipeItemInputsList.getCompoundTagAt(j).getTagList("RecipeItemInput" + recipeIndex + ":" + j, Constants.NBT.TAG_COMPOUND);
                ItemStack[] oreDictStack = new ItemStack[recipeItemInputsOreDictList.tagCount()];
                for (int k = 0; k < recipeItemInputsOreDictList.tagCount(); k++) {
                    oreDictStack[k] = new ItemStack(recipeItemInputsOreDictList.getCompoundTagAt(k));
                }
                inputIngredients.add(new CountableIngredient(Ingredient.fromStacks(oreDictStack),
                        recipeItemInputsCountList.getCompoundTagAt(j).getInteger("RecipeItemInputCount" + recipeIndex + ":" + j)));
            }
            List<ItemStack> outputItemStackCollection = NonNullList.create();
            for (int j = 0; j < recipeItemOutputsList.tagCount(); j++) {
                outputItemStackCollection.add(new ItemStack(recipeItemOutputsList.getCompoundTagAt(j)));
            }
            List<FluidStack> inputFluidStackCollection = new ArrayList<>(0);
            for (int j = 0; j < recipeFluidInputsList.tagCount(); j++) {
                inputFluidStackCollection.add(FluidStack.loadFluidStackFromNBT(recipeFluidInputsList.getCompoundTagAt(j)));
            }
            List<FluidStack> outputFluidStackCollection = new ArrayList<>(0);
            for (int j = 0; j < recipeFluidOutputsList.tagCount(); j++) {
                outputFluidStackCollection.add(FluidStack.loadFluidStackFromNBT(recipeFluidOutputsList.getCompoundTagAt(j)));
            }
            List<Recipe.ChanceEntry> chancedOutputCollection = new ArrayList<>();
            for (int j = 0; j < recipeChancedOutputsList.tagCount(); j++) {
                chancedOutputCollection.add(new Recipe.ChanceEntry(new ItemStack(recipeChancedOutputsList.getCompoundTagAt(j)),
                        recipeChancedOutputsChanceList.getCompoundTagAt(j).getInteger("RecipeChancedOutputChance" + recipeIndex + ":" + j),
                        recipeChancedOutputsBoostList.getCompoundTagAt(j).getInteger("RecipeChancedOutputBoost" + recipeIndex + ":" + j)
                        ));
            }

            occupiedRecipes[recipeIndex] = new Recipe(inputIngredients, outputItemStackCollection, chancedOutputCollection, inputFluidStackCollection, outputFluidStackCollection,
                    recipeDurationList.getCompoundTagAt(i).getInteger("RecipeDuration" + recipeIndex), recipeEnergyList.getCompoundTagAt(i).getInteger("RecipeEnergy" + recipeIndex), false);
            controller.multiRecipeMap.removeIfMatches(occupiedRecipes[recipeIndex]);
        }
    }
}
