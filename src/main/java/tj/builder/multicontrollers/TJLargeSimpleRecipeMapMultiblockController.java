package tj.builder.multicontrollers;

import gregicadditions.machines.GATileEntities;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.ItemTabInfo;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.Position;
import gregtech.common.items.MetaItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import tj.gui.TJGuiTextures;
import tj.gui.TJHorizontoalTabListRenderer;
import tj.gui.TJTabGroup;
import tj.gui.TJWidgetGroup;
import tj.multiblockpart.TJMultiblockAbility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TJLargeSimpleRecipeMapMultiblockController extends LargeSimpleRecipeMapMultiblockController implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    protected boolean doStructureCheck = false;

    public TJLargeSimpleRecipeMapMultiblockController(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
        super(metaTileEntityId, recipeMap, EUtPercentage, durationPercentage, chancePercentage, stack);
    }

    public TJLargeSimpleRecipeMapMultiblockController(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap, int EUtPercentage, int durationPercentage, int chancePercentage, int stack, boolean hasMuffler, boolean hasMaintenance, boolean canDistinct) {
        super(metaTileEntityId, recipeMap, EUtPercentage, durationPercentage, chancePercentage, stack, hasMuffler, hasMaintenance, canDistinct);
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new ItemStackHandler(1) {
            @Override
            @Nonnull
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (!stack.isItemEqual(MetaItems.INTEGRATED_CIRCUIT.getStackForm()))
                    return stack;
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Override
    public MultiblockAbility<IItemHandlerModifiable> getAbility() {
        return TJMultiblockAbility.CIRCUIT_SLOT;
    }

    @Override
    public void registerAbilities(List<IItemHandlerModifiable> abilityList) {
        abilityList.add(this.importItems);
    }

    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.initializeAbilities();
    }

    private void initializeAbilities() {
        List<IItemHandlerModifiable> itemHandlerCollection = new ArrayList<>();
        itemHandlerCollection.addAll(getAbilities(TJMultiblockAbility.CIRCUIT_SLOT));
        itemHandlerCollection.addAll(getAbilities(MultiblockAbility.IMPORT_ITEMS));

        this.inputInventory = new ItemHandlerList(itemHandlerCollection);
        this.inputFluidInventory = new FluidTankList(allowSameFluidFillForOutputs(), getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.outputFluidInventory = new FluidTankList(allowSameFluidFillForOutputs(), getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(-10, 0, 195, 217, TJGuiTextures.NEW_MULTIBLOCK_DISPLAY);
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT ,-3, 134);
        builder.widget(new LabelWidget(0, 7, getMetaFullName(), 0xFFFFFF));

        TJTabGroup tabGroup = new TJTabGroup(() -> new TJHorizontoalTabListRenderer(TJHorizontoalTabListRenderer.HorizontalStartCorner.LEFT, TJHorizontoalTabListRenderer.VerticalLocation.BOTTOM), new Position(-10, 1));
        List<Triple<String, ItemStack, AbstractWidgetGroup>> tabList = new ArrayList<>();
        addNewTabs(tabList::add);
        tabList.forEach(tabs -> tabGroup.addTab(new ItemTabInfo(tabs.getLeft(), tabs.getMiddle()), tabs.getRight()));
        builder.widget(tabGroup);
        return builder;
    }

    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        TJWidgetGroup widgetDisplayGroup = new TJWidgetGroup(), widgetMaintenanceGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.display", this.getStackForm(), mainDisplayTab(widgetDisplayGroup::addWidgets)));
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.maintenance", GATileEntities.MAINTENANCE_HATCH[0].getStackForm(), maintenanceTab(widgetMaintenanceGroup::addWidgets)));
    }

    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        widgetGroup.apply(new AdvancedTextWidget(10, 19, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180)
                .setClickHandler(this::handleDisplayClick));
        widgetGroup.apply(new SlotWidget(this.importItems, 0, 172, 191));
        widgetGroup.apply(new ImageWidget(171, 190, 20, 20, GuiTextures.INT_CIRCUIT_OVERLAY));
        widgetGroup.apply(new ToggleButtonWidget(172, 169, 18, 18, TJGuiTextures.POWER_BUTTON, this::getToggleMode, this::setToggleRunning)
                .setTooltipText("machine.universal.toggle.run.mode"));
        widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.DISTINCT_BUTTON, this::getDistinctMode, this::setDistinctMode)
                .setTooltipText("machine.universal.toggle.distinct.mode"));
        return widgetGroup.apply(new ToggleButtonWidget(172, 133, 18, 18, TJGuiTextures.CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.recipeMapWorkable(textList, isStructureFormed(), recipeMapWorkable);
        MultiblockDisplaysUtility.isInvalid(textList, isStructureFormed());
    }

    protected AbstractWidgetGroup maintenanceTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addMaintenanceDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    protected void addMaintenanceDisplayText(List<ITextComponent> textList) {
        MultiblockDisplaysUtility.mufflerDisplay(textList, !hasMufflerHatch() || isMufflerFaceFree());
        MultiblockDisplaysUtility.maintenanceDisplay(textList, maintenance_problems, hasProblems());
    }

    protected boolean getToggleMode() {
        return this.recipeMapWorkable.isWorkingEnabled();
    }

    protected void setToggleRunning(boolean running) {
        this.recipeMapWorkable.setWorkingEnabled(running);
    }

    protected boolean getDoStructureCheck() {
        if (isStructureFormed())
            this.doStructureCheck = false;
        return this.doStructureCheck;
    }

    protected void setDoStructureCheck(boolean check) {
        if (isStructureFormed()) {
            this.doStructureCheck = true;
            invalidateStructure();
            this.structurePattern = createStructurePattern();
        }
    }

    protected boolean getDistinctMode() {
        return isDistinct;
    }

    protected void setDistinctMode(boolean distinct) {
        isDistinct = distinct;
        markDirty();
    }

    @Override
    public boolean isAttachedToMultiBlock() {
        return false;
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase multiblockControllerBase) {

    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase multiblockControllerBase) {

    }
}
