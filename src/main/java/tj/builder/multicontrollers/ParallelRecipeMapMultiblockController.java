package tj.builder.multicontrollers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import gregicadditions.GAUtility;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.capabilities.IMultiRecipe;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.common.blocks.BlockTurbineCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import tj.builder.ParallelRecipeMap;
import tj.capability.IParallelController;
import tj.capability.TJCapabilities;
import tj.capability.impl.ParallelMultiblockRecipeLogic;
import tj.gui.TJGuiTextures;
import tj.gui.TJWidgetGroup;
import tj.gui.widgets.JEIRecipeTransferWidget;
import tj.multiblockpart.TJMultiblockAbility;
import tj.multiblockpart.utility.MetaTileEntityMachineController;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.LongStream;

import static gregicadditions.capabilities.MultiblockDataCodes.RECIPE_MAP_INDEX;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static tj.capability.TJMultiblockDataCodes.PARALLEL_LAYER;
import static tj.gui.TJGuiTextures.RESET_BUTTON;

public abstract class ParallelRecipeMapMultiblockController extends TJMultiblockDisplayBase implements IParallelController, IMultiRecipe {

    public final ParallelRecipeMap[] parallelRecipeMap;
    public ParallelMultiblockRecipeLogic recipeMapWorkable;
    protected int parallelLayer = 1;
    protected int energyBonus = -1;
    protected long maxVoltage = 0;
    protected int pageIndex = 0;
    protected final int pageSize = 6;
    protected boolean advancedText;
    protected boolean isDistinctBus;
    protected int recipeMapIndex;
    protected static final DecimalFormat formatter = new DecimalFormat("#0.00");

    protected IItemHandlerModifiable inputInventory;
    protected IItemHandlerModifiable outputInventory;
    protected IMultipleTankHandler inputFluidInventory;
    protected IMultipleTankHandler outputFluidInventory;
    protected IEnergyContainer energyContainer;

    public ParallelRecipeMapMultiblockController(ResourceLocation metaTileEntityId, ParallelRecipeMap[] recipeMap) {
        super(metaTileEntityId);
        this.parallelRecipeMap = recipeMap;
    }

    public IEnergyContainer getEnergyContainer() {
        return this.energyContainer;
    }

    public IItemHandlerModifiable getInputInventory() {
        return this.inputInventory;
    }

    public IItemHandlerModifiable getOutputInventory() {
        return this.outputInventory;
    }

    public IMultipleTankHandler getInputFluidInventory() {
        return this.inputFluidInventory;
    }

    public IMultipleTankHandler getOutputFluidInventory() {
        return this.outputFluidInventory;
    }

    @Override
    public long getMaxEUt() {
        return this.energyContainer.getInputVoltage();
    }

    @Override
    public long getTotalEnergyConsumption() {
        return LongStream.range(0, this.recipeMapWorkable.getSize())
                .map(i -> this.recipeMapWorkable.getRecipeEUt((int) i))
                .sum();
    }

    @Override
    public long getVoltageTier() {
        return this.maxVoltage;
    }

    @Override
    public int getEUBonus() {
        return this.energyBonus;
    }

    @Override
    public RecipeMap<?> getMultiblockRecipe() {
        return this.parallelRecipeMap[this.recipeMapIndex].getRecipeMap();
    }

    /**
     * Used to get the current index of the selected RecipeMap
     *
     * @return index of the current recipe
     */
    @Override
    public int getRecipeMapIndex() {
        return this.recipeMapIndex;
    }

    /**
     * Used to add new RecipeMaps to a given MultiBlock
     *
     * @param recipeMaps to add to the MultiBlock
     */
    @Override
    public void addRecipeMaps(RecipeMap<?>[] recipeMaps) {
        ArrayUtils.addAll(this.parallelRecipeMap, recipeMaps);
    }

    /**
     * Performs extra checks for validity of given recipe before multiblock
     * will start it's processing.
     */
    public boolean checkRecipe(Recipe recipe, boolean consumeIfSuccess) {
        return true;
    }

    public long getMaxVoltage() {
        return this.maxVoltage;
    }

    public int getMaxParallel() {
        return 1;
    }

    public int getPageIndex() {
        return this.pageIndex;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public boolean isDistinctBus() {
        return this.isDistinctBus;
    }

    public void setDistinctBus(Boolean isDistinctBus) {
        this.isDistinctBus = isDistinctBus;
        this.markDirty();
    }

    private void resetRecipeCache(boolean reset) {
        this.recipeMapWorkable.previousRecipe.clear();
    }

    @Override
    protected void reinitializeStructurePattern() {
        this.parallelLayer = 1;
        super.reinitializeStructurePattern();
    }

    @Override
    protected void additionalWidgets(Consumer<Widget> widgetGroup) {
        widgetGroup.accept(new JEIRecipeTransferWidget(0, 0, 100, 100)
                .setRecipeConsumer(this::setRecipe));
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup, int extended) {
        super.mainDisplayTab(widgetGroup, extended);
        return widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.DISTINCT_BUTTON, this::isDistinctBus, this::setDistinctBus)
                .setTooltipText("machine.universal.toggle.distinct.mode"));
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs, int extended) {
        super.addNewTabs(tabs, extended);
        TJWidgetGroup workableWidgetGroup = new TJWidgetGroup(), debugWidgetGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.workable", MetaBlocks.TURBINE_CASING.getItemVariant(BlockTurbineCasing.TurbineCasingType.STEEL_GEARBOX), this.workableTab(workableWidgetGroup::addWidgets)));
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.debug", MetaItems.WRENCH.getStackForm(), this.debugTab(debugWidgetGroup::addWidgets)));
    }

    private AbstractWidgetGroup workableTab(Function<Widget, WidgetGroup> widgetGroup) {
        widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, RESET_BUTTON, () -> false, this::resetRecipeCache)
                .setTooltipText("tj.multiblock.parallel.recipe.clear"));
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addWorkableDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180)
                .setClickHandler(this::handleWorkableDisplayClick));
    }

    private AbstractWidgetGroup debugTab(Function<Widget, WidgetGroup> widgetGroup) {
        widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, RESET_BUTTON, () -> false, this::resetRecipeCache)
                .setTooltipText("tj.multiblock.parallel.recipe.clear"));
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addDebugDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    private void setRecipe(List<ItemStack> itemInputs, List<ItemStack> itemOutputs, List<FluidStack> fluidInputs, List<FluidStack> fluidOutput, EntityPlayer player) {
        for (int i = 0; i < this.recipeMapWorkable.getSize(); i++) {
            if (this.recipeMapWorkable.getRecipe(i) == null) {
                Recipe newRecipe = this.parallelRecipeMap[this.getRecipeMapIndex()].findByInputsAndOutputs(this.maxVoltage, itemInputs, itemOutputs, fluidInputs, fluidOutput);
                this.recipeMapWorkable.setRecipe(newRecipe, i);
                player.sendMessage(newRecipe != null ? this.displayRecipe(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.recipe.transfer.success", i + 1)), newRecipe, 1)
                        : new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.recipe.transfer.fail_2", i + 1)));
                return;
            }
        }
        player.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.recipe.transfer.fail")));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        long totalEnergyConsumption = this.getTotalEnergyConsumption();
        if (this.isStructureFormed()) {
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyContainer)
                    .energyInput(this.energyContainer.getEnergyStored() >= totalEnergyConsumption, totalEnergyConsumption)
                    .voltageTier(GAUtility.getTierByVoltage(this.maxVoltage))
                    .energyBonus(this.energyBonus, this.isStructureFormed() && this.energyBonus >= 0)
                    .recipeMap(this.getMultiblockRecipe());
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        if (this.recipeMapWorkable.isActive())
            return;
        this.recipeMapWorkable.previousRecipe.clear();
        this.recipeMapIndex = this.recipeMapIndex >= this.parallelRecipeMap.length - 1 ? 0 : this.recipeMapIndex + 1;
        if (!this.getWorld().isRemote) {
            this.writeCustomData(RECIPE_MAP_INDEX, buf -> buf.writeInt(this.recipeMapIndex));
            this.markDirty();
        }
    }

    private ITextComponent displayItemInputs(Recipe recipe, int parallel) {
        ITextComponent itemInputs = new TextComponentString(I18n.translateToLocal("tj.multiblock.parallel.advanced.itemInputs"));
        for (CountableIngredient item : recipe.getInputs()) {
            itemInputs.appendText("\n-");
            itemInputs.appendSibling(new TextComponentString("§6" + item.getIngredient().getMatchingStacks()[0].getDisplayName()));
            itemInputs.appendText(" §7{");
            itemInputs.appendText("§6" + String.format("%,d", item.getCount() * parallel) + "§7}");
        }
        return itemInputs;
    }

    private ITextComponent displayItemOutputs(Recipe recipe, int parallel) {
        ITextComponent itemOutputs = new TextComponentString(I18n.translateToLocal("tj.multiblock.parallel.advanced.itemOutputs"));
        for (ItemStack item : recipe.getOutputs()) {
            itemOutputs.appendText("\n-");
            itemOutputs.appendSibling(new TextComponentString("§6" + item.getDisplayName()));
            itemOutputs.appendText(" §7{");
            itemOutputs.appendText("§6" + String.format("%,d", item.getCount() * parallel) + "§7}");
        }
        for (Recipe.ChanceEntry entry : recipe.getChancedOutputs()) {
            itemOutputs.appendText("\n-");
            itemOutputs.appendSibling(new TextComponentString("§6" + entry.getItemStack().getDisplayName()));
            itemOutputs.appendText(" §7{");
            itemOutputs.appendText("§6" + String.format("%,d", entry.getItemStack().getCount() * parallel) + "§7}");
            itemOutputs.appendText(" §7{");
            itemOutputs.appendText("§6" + I18n.translateToLocalFormatted("gregtech.recipe.chance", entry.getChance() / 100, entry.getBoostPerTier() / 100) + "§7}");
        }
        return itemOutputs;
    }

    private ITextComponent displayFluids(List<FluidStack> fluidStacks, String fluidTextLocale, int parallel) {
        ITextComponent fluidInputs = new TextComponentString(I18n.translateToLocal(fluidTextLocale));
        for (FluidStack fluid : fluidStacks) {
            fluidInputs.appendText("\n-");
            fluidInputs.appendSibling(new TextComponentString("§b" + fluid.getLocalizedName()));
            fluidInputs.appendText(" §7{");
            fluidInputs.appendText("§b" + String.format("%,d", fluid.amount * parallel) + "L§7}");
        }
        return fluidInputs;
    }

    private ITextComponent displayRecipe(ITextComponent textComponent, Recipe recipe, int parallel) {
        return textComponent.appendText("\n")
                .appendSibling(this.displayItemInputs(recipe, parallel))
                .appendText("\n")
                .appendSibling(this.displayFluids(recipe.getFluidInputs(), "tj.multiblock.parallel.advanced.fluidInput", parallel))
                .appendText("\n")
                .appendSibling(this.displayItemOutputs(recipe, parallel))
                .appendText("\n")
                .appendSibling(this.displayFluids(recipe.getFluidOutputs(), "tj.multiblock.parallel.advanced.fluidOutput", parallel));
    }

    private void addWorkableDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.industrial_fusion_reactor.message", this.parallelLayer)));
        textList.add(new TextComponentTranslation("tj.multiblock.parallel.distinct")
                .appendText(" ")
                .appendSibling(this.recipeMapWorkable.isDistinct() ? withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.enabled"), "isDistinct")
                        : withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.disabled"), "notDistinct")));
        textList.add(new TextComponentTranslation("tj.multiblock.parallel.advanced")
                .appendText(" ")
                .appendSibling(this.advancedText ? withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.enabled"), "advanced")
                        : withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.disabled"), "basic")));
        textList.add(new TextComponentString(":")
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[<]"), "leftPage"))
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[>]"), "rightPage")));
        if (this.isStructureFormed()) {
            for (int i = this.pageIndex, recipeHandlerInstance = i + 1; i < this.pageIndex + this.pageSize; i++, recipeHandlerInstance++) {
                if (i < this.parallelLayer) {

                    int parallel = this.recipeMapWorkable.getParallel(i);
                    double progressPercent = this.recipeMapWorkable.getProgressPercent(i) * 100;
                    ITextComponent advancedTooltip = this.advancedText ? new TextComponentTranslation("tj.multiblock.parallel.advanced.on")
                            : new TextComponentTranslation("tj.multiblock.parallel.advanced.off").setStyle(new Style().setColor(TextFormatting.GRAY));
                    if (this.advancedText) {
                        Recipe recipe = this.recipeMapWorkable.getRecipe(i);
                        if (recipe != null) {
                            this.displayRecipe(advancedTooltip, recipe, parallel);
                        }
                    }
                    String isRunning = !this.recipeMapWorkable.isWorkingEnabled(i) ? I18n.translateToLocal("machine.universal.work_paused")
                            : !this.recipeMapWorkable.isInstanceActive(i) ? I18n.translateToLocal("machine.universal.idling")
                            : I18n.translateToLocal("machine.universal.running");

                    textList.add(new TextComponentString(": [§a" + recipeHandlerInstance + "§r] " + isRunning)
                            .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("tj.multiblock.parallel.status")
                                    .appendText(" ")
                                    .appendSibling(new TextComponentString(isRunning))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.handler", recipeHandlerInstance)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.eu", this.recipeMapWorkable.getRecipeEUt(i))))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.progress", (int) progressPercent)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.parallel", parallel)))
                                    .appendText("\n\n")
                                    .appendSibling(advancedTooltip))))
                            .appendText(" ")
                            .appendSibling(this.recipeMapWorkable.getLockingMode(i) ? withButton(new TextComponentTranslation("tj.multiblock.parallel.lock"), "lock:" + i)
                                    : withButton(new TextComponentTranslation("tj.multiblock.parallel.unlock"), "unlock:" + i))
                            .appendText(" ")
                            .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:" + i)));
                }
            }
        }
    }

    private void addDebugDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.parallel.debug.cache.capacity", this.recipeMapWorkable.previousRecipe.getCapacity())));
        textList.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.parallel.debug.cache.hit", this.recipeMapWorkable.previousRecipe.getCacheHit()))
                .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("tj.multiblock.parallel.debug.cache.hit.info")))));
        textList.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.parallel.debug.cache.miss", this.recipeMapWorkable.previousRecipe.getCacheMiss()))
                .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("tj.multiblock.parallel.debug.cache.miss.info")))));
    }

    private void handleWorkableDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "leftPage":
                if (this.pageIndex > 0)
                    this.pageIndex -= this.pageSize;
                return;
            case "rightPage":
                if (this.pageIndex < this.parallelLayer - this.pageSize)
                    this.pageIndex += this.pageSize;
                return;
            case "basic":
                this.advancedText = true;
                return;
            case "advanced":
                this.advancedText = false;
                return;
            case "isDistinct":
                this.recipeMapWorkable.setDistinct(false);
                return;
            case "notDistinct":
                this.recipeMapWorkable.setDistinct(true);
                return;
            default:
                if (componentData.startsWith("lock")) {
                    String[] lock = componentData.split(":");
                    int index = Integer.parseInt(lock[1]);
                    this.recipeMapWorkable.setLockingMode(false, index);

                } else if (componentData.startsWith("unlock")) {
                    String[] unlock = componentData.split(":");
                    int index = Integer.parseInt(unlock[1]);
                    this.recipeMapWorkable.setLockingMode(true, index);

                } else if (componentData.startsWith("remove")) {
                    String[] remove = componentData.split(":");
                    int index = Integer.parseInt(remove[1]);
                    this.recipeMapWorkable.setRecipe(null, index);
                }
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.initializeAbilities();
        int size = this.recipeMapWorkable.getSize();
        for (int i = 0; i < getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER).size(); i++) {
            MetaTileEntityMachineController controller = getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER).get(i);
            if (controller.isAutomatic() || controller.getId() >= size)
                controller.setID(Math.min(i, size - 1)).setController(this);
        }
    }

    @Override
    public void invalidateStructure() {
        for (MetaTileEntityMachineController controller : this.getAbilities(TJMultiblockAbility.REDSTONE_CONTROLLER))
            controller.setID(0).setController(null);
        super.invalidateStructure();
        this.resetTileAbilities();
        this.recipeMapWorkable.invalidate();
        this.maxVoltage = 0;
    }

    @Override
    protected void updateFormedValid() {
        if (!this.isWorkingEnabled)
            return;
        for (int i = 0; i < this.recipeMapWorkable.getSize(); i++) {
            this.recipeMapWorkable.update(i);
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
        this.energyContainer = new EnergyContainerList(Lists.newArrayList());;
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
        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();

        return maintenanceCount == 1 &&
                itemInputsCount >= this.parallelRecipeMap[this.getRecipeMapIndex()].getMinInputs() &&
                fluidInputsCount >= this.parallelRecipeMap[this.getRecipeMapIndex()].getMinFluidInputs() &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        this.getFrontOverlay().render(renderState, translation, pipeline, this.getFrontFacing(), this.recipeMapWorkable.isActive());
    }

    public void resetStructure() {
        this.invalidateStructure();
        this.structurePattern = this.createStructurePattern();
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (playerIn.getHeldItemMainhand().isItemEqual(MetaItems.SCREWDRIVER.getStackForm()))
            return false;
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    @Override
    public boolean onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        boolean removeLayer = false;
        boolean actionSuccess = false;
        ITextComponent textComponent;
        if (!playerIn.isSneaking()) {
            if (this.parallelLayer < this.getMaxParallel()) {
                this.parallelLayer++;
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.increment.success").appendSibling(new TextComponentString(" " + this.parallelLayer));
                actionSuccess = true;
            } else
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.increment.fail").appendSibling(new TextComponentString(" " + this.parallelLayer));
        } else {
            if (this.parallelLayer > 1) {
                this.parallelLayer--;
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.decrement.success").appendSibling(new TextComponentString(" " + this.parallelLayer));
                removeLayer = true;
                actionSuccess = true;
            } else
                textComponent = new TextComponentTranslation("tj.multiblock.parallel.layer.decrement.fail").appendSibling(new TextComponentString(" " + this.parallelLayer));
        }
        if (this.getWorld().isRemote)
            playerIn.sendMessage(textComponent);
        else {
            this.writeCustomData(PARALLEL_LAYER, buf -> buf.writeInt(this.parallelLayer));
            if (actionSuccess)
                this.recipeMapWorkable.setLayer(this.parallelLayer, removeLayer);
        }
        this.resetStructure();
        return true;
    }

    @Override
    public boolean onSawToolClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (playerIn.isSneaking()) {
            this.recipeMapWorkable.previousRecipe.clear();
            this.markDirty();
            playerIn.sendMessage(new TextComponentString("The recipe cache has been cleared."));
            return true;
        }
        boolean useOptimizedRecipeLookUp = this.recipeMapWorkable.toggleUseOptimizedRecipeLookUp();
        this.markDirty();
        if (useOptimizedRecipeLookUp) {
            playerIn.sendMessage(new TextComponentString("Using optimized recipe lookup, might fail to detects some of the recipes"));
        }
        else {
            playerIn.sendMessage(new TextComponentString("Using unoptimized recipe lookup, can detects all of the recipes but with poor performance"));
        }
        return true;
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == PARALLEL_LAYER) {
            this.parallelLayer = buf.readInt();
            this.structurePattern = this.createStructurePattern();
            this.scheduleRenderUpdate();
        }
        if (dataId == RECIPE_MAP_INDEX) {
            this.recipeMapIndex = buf.readInt();
            this.scheduleRenderUpdate();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeInt(this.parallelLayer);
        buf.writeByte(this.recipeMapIndex);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.parallelLayer = buf.readInt();
        this.recipeMapIndex = buf.readByte();
        this.structurePattern = this.createStructurePattern();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagCompound tagCompound = super.writeToNBT(data);
        tagCompound.setInteger("RecipeMapIndex", this.recipeMapIndex);
        tagCompound.setInteger("Parallel", this.parallelLayer);
        tagCompound.setBoolean("DistinctBus", this.isDistinctBus);
        tagCompound.setBoolean("UseOptimizedRecipeLookUp", this.recipeMapWorkable.getUseOptimizedRecipeLookUp());
        return tagCompound;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        this.recipeMapIndex = data.getInteger("RecipeMapIndex");
        this.isDistinctBus = data.getBoolean("DistinctBus");
        if (data.hasKey("Parallel")) {
            this.parallelLayer = data.getInteger("Parallel");
            this.structurePattern = this.createStructurePattern();
        }
        if (data.hasKey("UseOptimizedRecipeLookUp")) {
            this.recipeMapWorkable.setUseOptimizedRecipeLookUp(data.getBoolean("UseOptimizedRecipeLookUp"));
        }
        super.readFromNBT(data);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }
}
