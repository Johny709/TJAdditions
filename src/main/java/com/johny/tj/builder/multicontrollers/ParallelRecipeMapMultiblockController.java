package com.johny.tj.builder.multicontrollers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.johny.tj.builder.MultiRecipeMap;
import com.johny.tj.capability.impl.ParallelMultiblockRecipeLogic;
import com.johny.tj.multiblockpart.TJMultiblockAbility;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.common.blocks.BlockTurbineCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public abstract class ParallelRecipeMapMultiblockController extends TJMultiblockDisplayBase {

    public final MultiRecipeMap multiRecipeMap;
    public ParallelMultiblockRecipeLogic recipeMapWorkable;
    protected int parallelLayer = 1;
    protected long maxVoltage = 0;
    protected int pageIndex = 0;
    protected final int pageSize = 6;
    protected boolean advancedText;

    protected IItemHandlerModifiable inputInventory;
    protected IItemHandlerModifiable outputInventory;
    protected IMultipleTankHandler inputFluidInventory;
    protected IMultipleTankHandler outputFluidInventory;
    protected IEnergyContainer energyContainer;

    public ParallelRecipeMapMultiblockController(ResourceLocation metaTileEntityId, MultiRecipeMap recipeMap) {
        super(metaTileEntityId);
        this.multiRecipeMap = recipeMap;
    }

    public IEnergyContainer getEnergyContainer() {
        return energyContainer;
    }

    public IItemHandlerModifiable getInputInventory() {
        return inputInventory;
    }

    public IItemHandlerModifiable getOutputInventory() {
        return outputInventory;
    }

    public IMultipleTankHandler getInputFluidInventory() {
        return inputFluidInventory;
    }

    public IMultipleTankHandler getOutputFluidInventory() {
        return outputFluidInventory;
    }

    /**
     * Performs extra checks for validity of given recipe before multiblock
     * will start it's processing.
     */
    public boolean checkRecipe(Recipe recipe, boolean consumeIfSuccess) {
        return true;
    }

    public long getMaxVoltage() {
        return maxVoltage;
    }

    public int getMaxParallel() {
        return 1;
    }

    @Override
    protected List<Triple<String, ItemStack, AbstractWidgetGroup>> addNewTabs(List<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        tabs.add(new ImmutableTriple<>("tj.multiblock.tab.workable", MetaBlocks.TURBINE_CASING.getItemVariant(BlockTurbineCasing.TurbineCasingType.STEEL_GEARBOX), workableTab()));
        return tabs;
    }

    private AbstractWidgetGroup workableTab() {
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new AdvancedTextWidget(10, 18, this::addWorkableDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleWorkableDisplayClick));
        return widgetGroup;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
            long maxVoltage = energyContainer.getInputVoltage();
            String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
            textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
        }
        int powerConsumptionSum = 0;
        for (int i = 0; i < recipeMapWorkable.getSize(); i++) {
            powerConsumptionSum += recipeMapWorkable.getRecipeEUt(i);
        }
        textList.add(new TextComponentTranslation("tj.multiblock.parallel.sum", powerConsumptionSum));
    }

    private static ITextComponent displayItemInputs(Recipe recipe) {
        ITextComponent itemInputs = new TextComponentTranslation("tj.multiblock.parallel.advanced.itemInputs");
        Style gold = new Style().setColor(TextFormatting.GOLD);
        List<CountableIngredient> itemStackIn = recipe.getInputs();
        itemStackIn.forEach(item -> {
            itemInputs.appendText("\n-");
            itemInputs.appendSibling(new TextComponentString(item.getIngredient().getMatchingStacks()[0].getDisplayName()).setStyle(gold));
            itemInputs.appendText(" ");
            itemInputs.appendText(String.valueOf(item.getIngredient().getMatchingStacks()[0].getCount()));
        });
        return itemInputs;
    }

    private static ITextComponent displayItemOutputs(Recipe recipe) {
        ITextComponent itemOutputs = new TextComponentTranslation("tj.multiblock.parallel.advanced.itemOutputs");
        Style gold = new Style().setColor(TextFormatting.GOLD);
        List<ItemStack> itemStackOut = recipe.getOutputs();
        itemStackOut.forEach(item -> {
            itemOutputs.appendText("\n-");
            itemOutputs.appendSibling(new TextComponentString(item.getDisplayName()).setStyle(gold));
            itemOutputs.appendText("\n");
            itemOutputs.appendText(String.valueOf(item.getCount()));
        });
        return itemOutputs;
    }

    private static ITextComponent displayFluidInputs(Recipe recipe) {
        ITextComponent fluidInputs = new TextComponentTranslation("tj.multiblock.parallel.advanced.fluidInput");
        Style aqua = new Style().setColor(TextFormatting.AQUA);
        List<FluidStack> fluidStackIn = recipe.getFluidInputs();
        fluidStackIn.forEach(fluid -> {
            fluidInputs.appendText("\n-");
            fluidInputs.appendSibling(new TextComponentString(fluid.getLocalizedName()).setStyle(aqua));
            fluidInputs.appendText(" ");
            fluidInputs.appendText(String.valueOf(fluid.amount));
        });
        return fluidInputs;
    }

    private static ITextComponent displayFluidOutputs(Recipe recipe) {
        ITextComponent fluidOutputs = new TextComponentTranslation("tj.multiblock.parallel.advanced.fluidOutput");
        Style aqua = new Style().setColor(TextFormatting.AQUA);
        List<FluidStack> fluidStackOut = recipe.getFluidOutputs();
        fluidStackOut.forEach(fluid -> {
            fluidOutputs.appendText("\n-");
            fluidOutputs.appendSibling(new TextComponentString(fluid.getLocalizedName()).setStyle(aqua));
            fluidOutputs.appendText(" ");
            fluidOutputs.appendText(String.valueOf(fluid.amount));
        });
        return fluidOutputs;
    }

    private void addWorkableDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message", this.parallelLayer));
        textList.add(new TextComponentTranslation("tj.multiblock.parallel.advanced")
                .appendText(" ")
                .appendSibling(advancedText ? withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.enabled"), "advanced")
                        : withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.disabled"), "basic")));
        textList.add(new TextComponentString(":")
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[<]"), "leftPage"))
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[>]"), "rightPage")));
        if (isStructureFormed()) {
            for (int i = pageIndex, recipeHandlerPos = i + 1; i < pageIndex + pageSize; i++, recipeHandlerPos++) {
                if (i < parallelLayer) {

                    double progressPercent = recipeMapWorkable.getProgressPercent(i) * 100;
                    ITextComponent recipeInstance = new TextComponentString(": ");
                    ITextComponent advancedTooltip = advancedText ? new TextComponentTranslation("tj.multiblock.parallel.advanced.on")
                            : new TextComponentTranslation("tj.multiblock.parallel.advanced.off").setStyle(new Style().setColor(TextFormatting.GRAY));
                    if (advancedText) {
                        Recipe recipe = recipeMapWorkable.getRecipe(i);
                        if (recipe != null) {
                            advancedTooltip.setStyle(new Style().setColor(TextFormatting.YELLOW))
                                    .appendText("\n")
                                    .appendSibling(displayItemInputs(recipe).setStyle(new Style().setBold(true)))
                                    .appendText("\n")
                                    .appendSibling(displayItemOutputs(recipe).setStyle(new Style().setBold(true)))
                                    .appendText("\n")
                                    .appendSibling(displayFluidInputs(recipe).setStyle(new Style().setBold(true)))
                                    .appendText("\n")
                                    .appendSibling(displayFluidOutputs(recipe).setStyle(new Style().setBold(true)));
                        }
                    }
                    recipeInstance.appendSibling(new TextComponentString("[" + recipeHandlerPos + "] " + (recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isInstanceActive(i) ? I18n.format("gregtech.multiblock.running") + " " : I18n.format("gregtech.multiblock.idling") + " ") : I18n.format("gregtech.multiblock.work_paused") + " "))
                                    .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("tj.multiblock.parallel.status")
                                                    .appendSibling(new TextComponentString(recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isInstanceActive(i) ? " " + I18n.format("gregtech.multiblock.running") + "\n" : " " + I18n.format("gregtech.multiblock.idling") + "\n") : " " + I18n.format("gregtech.multiblock.work_paused") + "\n")
                                                            .setStyle(new Style().setColor(recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isInstanceActive(i) ? TextFormatting.GREEN : TextFormatting.WHITE) : TextFormatting.YELLOW)))
                                                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.eu").appendSibling(new TextComponentString(" " + recipeMapWorkable.getRecipeEUt(i) + "\n")))
                                                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.progress").appendSibling(new TextComponentString(" " + (int) progressPercent + "%\n")))
                                                    .appendText("\n")
                                                    .appendSibling(advancedTooltip)))
                                            .setColor(recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isInstanceActive(i) ? TextFormatting.GREEN : TextFormatting.WHITE) : TextFormatting.YELLOW)))
                            .appendSibling(recipeMapWorkable.getLockingMode(i) ? withButton(new TextComponentTranslation("tj.multiblock.parallel.lock"), "lock" + i) : withButton(new TextComponentTranslation("tj.multiblock.parallel.unlock"), "unlock" + i));
                    textList.add(recipeInstance);
                }
            }
        }
    }

    private void handleWorkableDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "leftPage":
                if (pageIndex > 0)
                    pageIndex -= pageSize;
                return;
            case "rightPage":
                if (pageIndex < parallelLayer - pageSize)
                    pageIndex += pageSize;
                return;
            case "basic":
                advancedText = true;
                return;
            case "advanced":
                advancedText = false;
                return;
        }
        for (int i = pageIndex; i < pageIndex + pageSize; i++) {
            if (componentData.equals("lock" + i)) {
                recipeMapWorkable.setLockingMode(false, i);
            }
            if (componentData.equals("unlock" + i)) {
                recipeMapWorkable.setLockingMode(true, i);
            }
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        initializeAbilities();
        for (int i = 0; i < getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER).size(); i++) {
            getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER).get(i).setID(this, i);
        }
    }

    @Override
    public void invalidateStructure() {
        for (int i = 0; i < getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER).size(); i++) {
            getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER).get(i).setID(null, 0);
        }
        super.invalidateStructure();
        resetTileAbilities();
    }

    @Override
    protected void updateFormedValid() {
        if (!isWorkingEnabled)
            return;
        for (int i = 0; i < recipeMapWorkable.getSize(); i++) {
            recipeMapWorkable.update(i);
        }
    }

    private void initializeAbilities() {
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.inputFluidInventory = new FluidTankList(allowSameFluidFillForOutputs(), getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.outputFluidInventory = new FluidTankList(allowSameFluidFillForOutputs(), getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    private void resetTileAbilities() {
        this.inputInventory = new ItemStackHandler(0);
        this.inputFluidInventory = new FluidTankList(true);
        this.outputInventory = new ItemStackHandler(0);
        this.outputFluidInventory = new FluidTankList(true);
        this.energyContainer = new EnergyContainerList(Lists.newArrayList());
    }

    protected boolean allowSameFluidFillForOutputs() {
        return true;
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        //basically check minimal requirements for inputs count
        //noinspection SuspiciousMethodCalls
        int itemInputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_ITEMS, Collections.emptyList())
                .stream().map(it -> (IItemHandler) it).mapToInt(IItemHandler::getSlots).sum();
        //noinspection SuspiciousMethodCalls
        int itemOutputsCount = abilities.getOrDefault(MultiblockAbility.EXPORT_ITEMS, Collections.emptyList())
                .stream().map(it -> (IItemHandler) it).mapToInt(IItemHandler::getSlots).sum();
        //noinspection SuspiciousMethodCalls
        int fluidInputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();

        int fluidOutputsCount = abilities.getOrDefault(MultiblockAbility.EXPORT_FLUIDS, Collections.emptyList()).size();
        return itemInputsCount >= multiRecipeMap.getMinInputs() &&
                itemOutputsCount >= multiRecipeMap.getMinOutputs() &&
                fluidInputsCount >= multiRecipeMap.getMinFluidInputs() &&
                fluidOutputsCount >= multiRecipeMap.getMinFluidOutputs() &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        this.getFrontOverlay().render(renderState, translation, pipeline, getFrontFacing(), recipeMapWorkable.isActive());
    }

    public void resetStructure() {
        this.invalidateStructure();
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
                if (this.parallelLayer < getMaxParallel()) {
                    this.recipeMapWorkable.setLayer(++parallelLayer, false);
                    this.resetStructure();
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.1").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                } else {
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.4").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                }
            } else {
                if (this.parallelLayer > 1) {
                    this.recipeMapWorkable.setLayer(--parallelLayer, true);
                    this.resetStructure();
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.2").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                } else
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.3").appendSibling(new TextComponentString(" " + this.parallelLayer)));
            }
        }
        return true;
    }

    @Override
    public boolean onSawToolClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (playerIn.isSneaking()) {
            this.recipeMapWorkable.previousRecipe.clear();
            markDirty();
            playerIn.sendMessage(new TextComponentString("The recipe cache has been cleared."));
            return true;
        }
        boolean useOptimizedRecipeLookUp = this.recipeMapWorkable.toggleUseOptimizedRecipeLookUp();
        markDirty();
        if (useOptimizedRecipeLookUp) {
            playerIn.sendMessage(new TextComponentString("Using optimized recipe lookup, might fail to detects some of the recipes"));
        }
        else {
            playerIn.sendMessage(new TextComponentString("Using unoptimized recipe lookup, can detects all of the recipes but with poor performance"));
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagCompound tagCompound = super.writeToNBT(data);
        tagCompound.setInteger("Parallel", this.parallelLayer);
        tagCompound.setBoolean("UseOptimizedRecipeLookUp", this.recipeMapWorkable.getUseOptimizedRecipeLookUp());
        return tagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.parallelLayer = data.getInteger("Parallel");
        if (data.hasKey("UseOptimizedRecipeLookUp")) {
            this.recipeMapWorkable.setUseOptimizedRecipeLookUp(data.getBoolean("UseOptimizedRecipeLookUp"));
        }
    }
}
