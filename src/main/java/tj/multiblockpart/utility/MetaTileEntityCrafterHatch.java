package tj.multiblockpart.utility;

import gregicadditions.machines.multi.multiblockpart.GAMetaTileEntityMultiblockPart;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.DummyContainer;
import gregtech.api.util.Position;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import tj.builder.handlers.IRecipeMapProvider;
import tj.gui.widgets.impl.CraftingRecipeTransferWidget;
import tj.gui.widgets.impl.SlotDisplayWidget;
import tj.multiblockpart.TJMultiblockAbility;

import java.util.List;
import java.util.Map;

import static gregtech.api.gui.GuiTextures.*;
import static tj.gui.TJGuiTextures.DARKENED_SLOT;


public class MetaTileEntityCrafterHatch extends GAMetaTileEntityMultiblockPart implements IMultiblockAbilityPart<IRecipeMapProvider>, IRecipeMapProvider {

    private final InventoryCrafting inventoryCrafting = new InventoryCrafting(new DummyContainer(), 3, 3);
    private final ItemStackHandler craftingInventory = new ItemStackHandler(9);
    private final ItemStackHandler encodingInventory = new ItemStackHandler(9);
    private final ItemStackHandler resultInventory = new ItemStackHandler(1);
    private final Int2ObjectMap<IRecipe> recipeMap = new Int2ObjectArrayMap<>();
    private Runnable clearRecipeCache;
    private IRecipe currentRecipe;

    public MetaTileEntityCrafterHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityCrafterHatch(this.metaTileEntityId, this.getTier());
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        WidgetGroup craftingSlotGroup = new WidgetGroup(new Position(7, 14)), encodingSlotGroup = new WidgetGroup(new Position(115, 14));
        for (int i = 0; i < this.craftingInventory.getSlots(); i++) {
            int finalI = i;
            craftingSlotGroup.addWidget(new PhantomSlotWidget(this.craftingInventory, i, 18 * (i % 3), 18 * (i / 3))
                    .setBackgroundTexture(SLOT)
                    .setChangeListener(() -> this.setCraftingResult(finalI, this.craftingInventory.getStackInSlot(finalI))));
        }
        for (int i = 0; i < this.encodingInventory.getSlots(); i++) {
            encodingSlotGroup.addWidget(new SlotDisplayWidget(this.encodingInventory, i, 18 * (i % 3), 18 * (i / 3))
                    .onPressedConsumer((button, slot, stack) -> {
                        if (button == 0) {
                            this.clearCraftingResult();
                            IRecipe recipe = this.recipeMap.get(slot);
                            for (int j = 0; j < recipe.getIngredients().size(); j++) {
                                ItemStack stack1 = recipe.getIngredients().get(j).getMatchingStacks()[0];
                                this.setCraftingResult(j, stack1);
                            }
                        } else if (button == 1) {
                            this.encodingInventory.extractItem(slot, Integer.MAX_VALUE, false);
                            this.recipeMap.remove(slot);
                            if (this.clearRecipeCache != null)
                                this.clearRecipeCache.run();
                            this.markDirty();
                        }
                    }));
        }
        return ModularUI.builder(BACKGROUND, 176, 180)
                .widget(new LabelWidget(7, 5, this.getMetaFullName()))
                .widget(new ImageWidget(75, 28, 26, 26, SLOT))
                .widget(new ImageWidget(115, 14, 54, 54, DARKENED_SLOT))
                .widget(new SlotDisplayWidget(this.resultInventory, 0, 79, 32)
                        .onPressedConsumer((button, slot, stack) -> this.addRecipe(this.currentRecipe)))
                .widget(new ClickButtonWidget(62, 14, 8, 8, "", (clickData) -> {
                    this.clearCraftingResult();
                    this.setCraftingResult(0, ItemStack.EMPTY);
                }).setButtonTexture(BUTTON_CLEAR_GRID))
                .widget(new CraftingRecipeTransferWidget(this::setCraftingResult))
                .widget(craftingSlotGroup)
                .widget(encodingSlotGroup)
                .bindPlayerInventory(player.inventory, 98)
                .build(this.getHolder(), player);
    }

    private void addRecipe(IRecipe recipe) {
        if (recipe != null)
            for (int i = 0; i < 9; i++) {
                if (!this.recipeMap.containsKey(i)) {
                    this.encodingInventory.setStackInSlot(i, recipe.getRecipeOutput());
                    this.recipeMap.put(i, recipe);
                    this.markDirty();
                    return;
                }
            }
    }

    private void clearCraftingResult() {
        for (int i = 0; i < this.craftingInventory.getSlots(); i++) {
            this.craftingInventory.setStackInSlot(i, ItemStack.EMPTY);
            this.inventoryCrafting.setInventorySlotContents(i, ItemStack.EMPTY);
        }
    }

    private void setCraftingResult(int index, ItemStack stack) {
        this.craftingInventory.setStackInSlot(index, stack);
        this.inventoryCrafting.setInventorySlotContents(index, stack);
        this.currentRecipe = CraftingManager.findMatchingRecipe(this.inventoryCrafting, this.getWorld());
        this.resultInventory.setStackInSlot(0, this.currentRecipe != null ? this.currentRecipe.getRecipeOutput() : ItemStack.EMPTY);
        this.markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList recipeList = new NBTTagList();
        for (Map.Entry<Integer, IRecipe> recipeEntry : this.recipeMap.entrySet()) {
            NBTTagCompound recipeNBT = new NBTTagCompound();
            recipeNBT.setInteger("index", recipeEntry.getKey());
            recipeNBT.setString("id", recipeEntry.getValue().getRegistryName().toString());
            recipeList.appendTag(recipeNBT);
        }
        data.setTag("recipeList", recipeList);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagList recipeList = data.getTagList("recipeList", 10);
        for (int i = 0; i < recipeList.tagCount(); i++) {
            int index = recipeList.getCompoundTagAt(i).getInteger("index");
            IRecipe recipe = CraftingManager.getRecipe(new ResourceLocation(recipeList.getCompoundTagAt(i).getString("id")));
            this.recipeMap.put(index, recipe);
            this.encodingInventory.setStackInSlot(index, recipe.getRecipeOutput());
        }
    }

    @Override
    public Int2ObjectMap<IRecipe> getRecipeMap() {
        return this.recipeMap;
    }

    @Override
    public MultiblockAbility<IRecipeMapProvider> getAbility() {
        return TJMultiblockAbility.CRAFTER;
    }

    @Override
    public void registerAbilities(List<IRecipeMapProvider> list) {
        list.add(this);
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase controller) {
        super.addToMultiBlock(controller);
        if (controller instanceof IRecipeMapProvider)
            this.clearRecipeCache = ((IRecipeMapProvider) controller)::clearRecipeCache;
    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase controller) {
        super.removeFromMultiBlock(controller);
        this.clearRecipeCache = null;
    }
}
