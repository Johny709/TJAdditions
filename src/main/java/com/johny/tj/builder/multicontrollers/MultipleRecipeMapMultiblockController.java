package com.johny.tj.builder.multicontrollers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.johny.tj.TJConfig;
import com.johny.tj.builder.MultiRecipeMapBuilder;
import com.johny.tj.capability.impl.MultiblockMultiRecipeLogic;
import com.johny.tj.multiblockpart.TJMultiblockAbility;
import gregicadditions.recipes.impl.LargeRecipeBuilder;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.common.items.MetaItems;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public abstract class MultipleRecipeMapMultiblockController extends TJMultiblockDisplayBase {

    public final RecipeMap<?> recipeMap;

    public final MultiRecipeMapBuilder multiRecipeMap;
    public MultiblockMultiRecipeLogic recipeMapWorkable;
    protected int parallelLayer = 1;
    protected long maxVoltage = 0;
    protected int pageIndex = 0;
    protected final int pageSize = 6;

    protected IItemHandlerModifiable inputInventory;
    protected IItemHandlerModifiable outputInventory;
    protected IMultipleTankHandler inputFluidInventory;
    protected IMultipleTankHandler outputFluidInventory;
    protected IEnergyContainer energyContainer;

    public MultipleRecipeMapMultiblockController(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId);
        this.recipeMap = recipeMap;
        this.multiRecipeMap = new MultiRecipeMapBuilder(
                0, 3, 0, 3, 0, 5, 0, 4, (new LargeRecipeBuilder(RecipeMaps.CHEMICAL_RECIPES))
                .EUt(30));
        multiRecipeMap.addRecipes(recipeMap.getRecipeList());
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

            for (int i = pageIndex, recipeHandlerPos = i + 1; i < pageIndex + pageSize; i++, recipeHandlerPos++) {
                if (i < parallelLayer) {

                    double progressPercent = recipeMapWorkable.getProgressPercent(i) * 100;
                    ITextComponent recipeInstance = new TextComponentString("-");
                    recipeInstance.appendText(" ");
                    recipeInstance.appendSibling(new TextComponentString("[" + recipeHandlerPos + "] " + (recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isActive(i) ? I18n.format("gregtech.multiblock.running") + " " : I18n.format("gregtech.multiblock.idling") + " ") : I18n.format("gregtech.multiblock.work_paused") + " "))
                                    .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                            new TextComponentTranslation("tj.multiblock.parallel.status")
                                                                    .appendSibling(new TextComponentString(recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isActive(i) ? " " + I18n.format("gregtech.multiblock.running") + "\n" : " " + I18n.format("gregtech.multiblock.idling") + "\n") : " " + I18n.format("gregtech.multiblock.work_paused") + "\n")
                                                                            .setStyle(new Style().setColor(recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isActive(i) ? TextFormatting.GREEN : TextFormatting.WHITE) : TextFormatting.YELLOW)))
                                                                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.eu").appendSibling(new TextComponentString(" " + recipeMapWorkable.getRecipeEUt(i) + "\n")))
                                                                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.progress").appendSibling(new TextComponentString(" " + (int) progressPercent + "%")))
                                                    ))
                                                    .setColor(recipeMapWorkable.isWorkingEnabled(i) ? (recipeMapWorkable.isActive(i) ? TextFormatting.GREEN : TextFormatting.WHITE) : TextFormatting.YELLOW)
                                    ))
                            .appendSibling(recipeMapWorkable.getLockingMode(i) ? withButton(new TextComponentTranslation("tj.multiblock.parallel.lock"), "lock" + i) : withButton(new TextComponentTranslation("tj.multiblock.parallel.unlock"), "unlock" + i));
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

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        for (int i = pageIndex; i < pageIndex + pageSize; i++) {
            if (componentData.equals("lock" + i)) {
                recipeMapWorkable.setLockingMode(false, i);
            } else if (componentData.equals("unlock" + i)) {
                recipeMapWorkable.setLockingMode(true, i);
            }
        }
        if (componentData.equals("leftPage")) {
            if (pageIndex > 0)
                pageIndex -= pageSize;
        } else if (componentData.equals("rightPage")){
            if (pageIndex < parallelLayer - pageSize)
                pageIndex += pageSize;
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
        super.invalidateStructure();
        resetTileAbilities();
    }

    @Override
    protected void updateFormedValid() {
        if (!getWorld().isRemote) {
            if (getOffsetTimer() > 100) {
                for (int i = 0; i < parallelLayer; i++) {
                    recipeMapWorkable.update(i);
                }
            }
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
        int fluidInputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();
        //noinspection SuspiciousMethodCalls
        return itemInputsCount >= recipeMap.getMinInputs() &&
                fluidInputsCount >= recipeMap.getMinFluidInputs() &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        this.getFrontOverlay().render(renderState, translation, pipeline, getFrontFacing(), recipeMapWorkable.isActive(0));
    }

    public void resetStructure() {
        this.invalidateStructure();
        if (parallelLayer < 1) {
            this.recipeMapWorkable.removeFrom(parallelLayer);
        }
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
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.1").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                } else {
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.4").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                }
            } else {
                if (this.parallelLayer > 1) {
                    this.parallelLayer--;
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.2").appendSibling(new TextComponentString(" " + this.parallelLayer)));
                } else
                    playerIn.sendMessage(new TextComponentTranslation("tj.multiblock.industrial_fusion_reactor.message.3").appendSibling(new TextComponentString(" " + this.parallelLayer)));
            }
            this.resetStructure();
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
