package tj.builder.handlers;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.util.GTLog;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Triple;;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;
import tj.capability.impl.CraftingRecipeLRUCache;
import tj.multiblockpart.TJMultiblockAbility;
import tj.util.ItemStackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CrafterRecipeLogic extends AbstractWorkableHandler<IItemHandlerModifiable, IMultipleTankHandler> implements IItemFluidHandlerInfo {

    private final CraftingRecipeLRUCache previousRecipe = new CraftingRecipeLRUCache(10);
    private final List<Int2ObjectMap<Triple<IRecipe, NonNullList<CountableIngredient>, NonNullList<ItemStack>>>> recipeList = new ArrayList<>();
    private final List<ItemStack> itemInputs = new ArrayList<>();
    private final List<ItemStack> itemOutputs = new ArrayList<>();

    public CrafterRecipeLogic(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    @Override
    public void initialize(int busCount) {
        super.initialize(busCount);
        this.recipeList.clear();
        if (this.metaTileEntity instanceof MultiblockControllerBase) {
            List<IRecipeMapProvider> crafters = ((MultiblockControllerBase) this.metaTileEntity).getAbilities(TJMultiblockAbility.CRAFTER);
            for (IRecipeMapProvider provider : crafters)
                this.recipeList.add(provider.getRecipeMap());
        } else if (this.metaTileEntity instanceof IRecipeMapProvider) {
            this.recipeList.add(((IRecipeMapProvider) this.metaTileEntity).getRecipeMap());
        }
    }

    public void clearCache() {
        this.previousRecipe.clear();
    }

    private boolean trySearchForRecipe(IItemHandlerModifiable importItems) {
        int parallels = this.parallel.getAsInt();
        Triple<IRecipe, NonNullList<CountableIngredient>, NonNullList<ItemStack>> currentRecipe = this.previousRecipe.get(importItems);
        if (currentRecipe == null) {
            recipeList:
            for (int i = 0; i < this.recipeList.size(); i++) {
                Map<Integer, Triple<IRecipe, NonNullList<CountableIngredient>, NonNullList<ItemStack>>> recipeMap = this.recipeList.get(i);
                recipe:
                for (int j = 0; j < recipeMap.size(); j++) {
                    Triple<IRecipe, NonNullList<CountableIngredient>, NonNullList<ItemStack>> recipe = recipeMap.get(j);
                    if (recipe == null)
                        continue;
                    List<CountableIngredient> countableIngredients = recipe.getMiddle();
                    for (int k = 0; k < countableIngredients.size(); k++) {
                        CountableIngredient ingredient = countableIngredients.get(k);
                        int size = ingredient.getCount();
                        int extracted = ItemStackHelper.extractFromItemHandlerByIngredient(importItems, ingredient.getIngredient(), size, true);
                        GTLog.logger.info(size);
                        GTLog.logger.info(extracted < size);
                        if (extracted < size)
                            continue recipe;
                    }
                    currentRecipe = recipe;
                    break recipeList;
                }
            }
        }
        if (currentRecipe != null) {
            int amountToExtract = parallels;
            for (CountableIngredient ingredient : currentRecipe.getMiddle()) {
                int size = ingredient.getCount();
                int extracted = ItemStackHelper.extractFromItemHandlerByIngredient(importItems, ingredient.getIngredient(), parallels * size, true);
                amountToExtract = Math.min(amountToExtract, extracted / size);
            }
            for (CountableIngredient ingredient : currentRecipe.getMiddle()) {
                int size = ingredient.getCount();
                ItemStackHelper.extractFromItemHandlerByIngredient(importItems, ingredient.getIngredient(), amountToExtract * size, false);
            }
            ItemStack output = currentRecipe.getLeft().getRecipeOutput().copy();
            output.setCount(output.getCount() * amountToExtract);
            this.itemOutputs.add(output);
            this.previousRecipe.put(currentRecipe);
            return true;
        }
        return false;
    }

    @Override
    protected boolean startRecipe() {
        boolean canStart = false;
        IItemHandlerModifiable itemInputs = this.isDistinct ? this.inputBus.apply(this.lastInputIndex) : this.importItems.get();
        if (this.trySearchForRecipe(itemInputs)) {
            this.maxProgress = this.calculateOverclock(30, 20, 2.8F);
            canStart = true;
        }
        if (++this.lastInputIndex == this.busCount)
            this.lastInputIndex = 0;
        return canStart;
    }

    @Override
    protected boolean completeRecipe() {
        if (ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), this.itemOutputs.get(0), true).isEmpty()) {
            ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), this.itemOutputs.get(0), false);
            this.itemInputs.clear();
            this.itemOutputs.clear();
            return true;
        }
        return false;
    }

    @Override
    public List<ItemStack> getItemInputs() {
        return this.itemInputs;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return this.itemOutputs;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound data = super.serializeNBT();
        NBTTagList inputList = new NBTTagList(), outputList = new NBTTagList();
        for (ItemStack stack : this.itemInputs)
            inputList.appendTag(stack.serializeNBT());
        for (ItemStack stack : this.itemOutputs)
            outputList.appendTag(stack.serializeNBT());
        data.setTag("inputList", inputList);
        data.setTag("outputList", outputList);
        return data;
    }

    @Override
    public void deserializeNBT(NBTTagCompound data) {
        super.deserializeNBT(data);
        NBTTagList inputList = data.getTagList("inputList", 10), outputList = data.getTagList("outputList", 10);
        for (int i = 0; i < inputList.tagCount(); i++)
            this.itemInputs.add(new ItemStack(inputList.getCompoundTagAt(i)));
        for (int i = 0; i < outputList.tagCount(); i++)
            this.itemOutputs.add(new ItemStack(outputList.getCompoundTagAt(i)));
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability);
    }
}
