package tj.builder.handlers;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.GTUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Pair;
import tj.builder.RecipeUtility;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;
import tj.capability.impl.CraftingRecipeLRUCache;
import tj.util.ItemStackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


//TODO WIP
public class CrafterRecipeLogic extends AbstractWorkableHandler<IItemHandlerModifiable, IMultipleTankHandler> implements IItemFluidHandlerInfo {

    private final CraftingRecipeLRUCache previousRecipe = new CraftingRecipeLRUCache(10);
    private final List<Map<Integer, IRecipe>> recipeList = new ArrayList<>();
    private final List<ItemStack> itemInputs = new ArrayList<>();
    private final List<ItemStack> itemOutputs = new ArrayList<>();

    public CrafterRecipeLogic(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    @Override
    public void initialize(int busCount) {
        super.initialize(busCount);
        if (this.metaTileEntity instanceof MultiblockControllerBase) {
            System.out.println("multiblock");
        } else if (this.metaTileEntity instanceof IRecipeMapProvider) {
            this.recipeList.add(((IRecipeMapProvider) this.metaTileEntity).getRecipeMap());
        }
    }

    public void clearCache() {
        this.previousRecipe.clear();
    }

    private boolean trySearchForRecipe(IItemHandlerModifiable importItems) {
        IRecipe currentRecipe = this.previousRecipe.get(importItems);
        if (currentRecipe != null) {
            this.itemOutputs.add(currentRecipe.getRecipeOutput().copy());
            return true;
        } else {
            for (int i = 0; i < this.recipeList.size(); i++) {
                Map<Integer, IRecipe> recipeMap = this.recipeList.get(i);
                for (int j = 0; j < recipeMap.size(); j++) {
                    IRecipe recipe = recipeMap.get(j);
                    if (recipe == null)
                        continue;
                    List<ItemStack> inputs = GTUtility.itemHandlerToList(importItems);
                    Pair<Boolean, int[]> matchingRecipe = RecipeUtility.craftingRecipeMatches(inputs, recipe.getIngredients());
                    if (matchingRecipe.getLeft()) {
                        int[] itemAmountInSlot = matchingRecipe.getValue();
                        for (int k = 0; k < itemAmountInSlot.length; k++) {
                            ItemStack itemInSlot = inputs.get(k);
                            int itemAmount = itemAmountInSlot[k];
                            if (itemInSlot.isEmpty() || itemInSlot.getCount() == itemAmount)
                                continue;
                            itemInSlot.setCount(itemAmountInSlot[k]);
                        }
                        currentRecipe = recipe;
                        break;
                    }
                }
            }
            if (currentRecipe != null) {
                this.previousRecipe.put(currentRecipe);
                this.itemOutputs.add(currentRecipe.getRecipeOutput().copy());
                return true;
            }
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
