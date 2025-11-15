package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.metatileentity.MTETrait;
import tj.builder.WidgetTabBuilder;
import tj.builder.handlers.TeleporterWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IParallelController;
import tj.capability.LinkPos;
import tj.capability.TJCapabilities;
import tj.gui.TJGuiTextures;
import tj.gui.uifactory.PlayerHolder;
import tj.gui.widgets.TJAdvancedTextWidget;
import tj.gui.widgets.TJClickButtonWidget;
import tj.gui.widgets.TJTextFieldWidget;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.item.GAMultiblockCasing;
import gregicadditions.item.GAMultiblockCasing2;
import gregicadditions.item.components.FieldGenCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static tj.gui.TJGuiTextures.SPACES_BUTTON;
import static tj.textures.TJTextures.FUSION_MK2;
import static tj.textures.TJTextures.TELEPORTER_OVERLAY;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate;
import static gregicadditions.machines.multi.mega.MegaMultiblockRecipeMapController.frameworkPredicate2;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.IMPORT_FLUIDS;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.INPUT_ENERGY;


public class MetaTileEntityTeleporter extends TJMultiblockDisplayBase implements IParallelController, LinkPos {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH};
    private final TeleporterWorkableHandler workableHandler = new TeleporterWorkableHandler(this);
    private IEnergyContainer energyContainer;
    private IMultipleTankHandler inputFluidHandler;
    private NBTTagCompound linkData;
    private int tier;

    public MetaTileEntityTeleporter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.workableHandler.setImportEnergySupplier(this::getEnergyContainer)
                .setImportFluidsSupplier(this::getInputFluidHandler)
                .setTierSupplier(this::getTier);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityTeleporter(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(net.minecraft.client.resources.I18n.format("tj.multiblock.teleporter.description"));
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        this.workableHandler.update();
    }

    private boolean hasEnoughEnergy(long amount) {
        return this.energyContainer.getEnergyStored() >= amount;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int framework = 0, framework2 = 0;
        if (context.get("framework") instanceof GAMultiblockCasing.CasingType) {
            framework = ((GAMultiblockCasing.CasingType) context.get("framework")).getTier();
        }
        if (context.get("framework2") instanceof GAMultiblockCasing2.CasingType) {
            framework2 = ((GAMultiblockCasing2.CasingType) context.get("framework2")).getTier();
        }
        int fieldGen = context.getOrDefault("FieldGen", FieldGenCasing.CasingType.FIELD_GENERATOR_LV).getTier();
        this.tier = Math.min(fieldGen, Math.max(framework, framework2));
        boolean energyHatchTierMatches = this.getAbilities(INPUT_ENERGY).stream()
                .allMatch(energyContainer -> GAUtility.getTierByVoltage(energyContainer.getInputVoltage()) <= this.tier);
        if (!energyHatchTierMatches) {
            this.invalidateStructure();
            return;
        }
        this.energyContainer = new EnergyContainerList(this.getAbilities(INPUT_ENERGY));
        this.inputFluidHandler = new FluidTankList(true, this.getAbilities(IMPORT_FLUIDS));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("~CFC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CHHHC", "~HHH~", "~C#C~", "~C#C~", "~C#C~", "~HHH~", "~~H~~")
                .aisle("FHfHF", "~HSH~", "~###~", "~###~", "~###~", "~HFH~", "~HfH~")
                .aisle("CHHHC", "~HHH~", "~C#C~", "~C#C~", "~C#C~", "~HHH~", "~~H~~")
                .aisle("~CFC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('H', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', frameworkPredicate().or(frameworkPredicate2()))
                .where('f', LargeSimpleRecipeMapMultiblockController.fieldGenPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return MetaBlocks.MUTLIBLOCK_CASING.getState(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return FUSION_MK2;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TELEPORTER_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), this.workableHandler.isActive());
    }

    @Override
    protected int getExtended() {
        return 18;
    }

    @Override
    protected void addTabs(WidgetTabBuilder tabBuilder) {
        super.addTabs(tabBuilder);
        tabBuilder.addTab("tj.multiblock.tab.pos", new ItemStack(Items.COMPASS), blockPosTab -> this.addScrollWidgets(blockPosTab, this::addPosDisplayText, this::addPosDisplayText2, this::getSearchPrompt, this::setSearchPrompt, this::onClear));
        tabBuilder.addTab("tj.multiblock.tab.queue", MetaItems.CONVEYOR_MODULE_ZPM.getStackForm(), queueTab -> this.addScrollWidgets(queueTab, this::addQueueDisplayText, this::addQueueDisplayText2, this::getQueuePrompt, this::setQueuePrompt, this::onClear2));
    }

    @Override
    protected void mainDisplayTab(WidgetGroup widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        widgetGroup.addWidget(new ImageWidget(28, 112, 141, 18, DISPLAY));
        widgetGroup.addWidget(new TJTextFieldWidget(33, 117, 136, 18, false, this::getTickSpeed, this::setTickSpeed)
                .setTooltipText("machine.universal.tick.speed")
                .setTooltipFormat(this::getTickSpeedFormat)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new ClickButtonWidget(7, 112, 18, 18, "+", this::onIncrement));
        widgetGroup.addWidget(new ClickButtonWidget(172, 112, 18, 18, "-", this::onDecrement));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    private void addScrollWidgets(WidgetGroup widgetGroup, Consumer<List<ITextComponent>> displayText, Consumer<List<ITextComponent>> displayText2, Supplier<String> searchSupplier, Consumer<String> searchResponder, Consumer<Widget.ClickData> onClear) {
        ScrollableListWidget scrollWidget = new ScrollableListWidget(10, -8, 178, 117) {
            @Override
            public boolean isWidgetClickable(Widget widget) {
                return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
            }
        };
        scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, displayText2, 0xFFFFFF)
                .addClickHandler(this::handlePosDisplayClick)
                .setMaxWidthLimit(1000));
        widgetGroup.addWidget(new AdvancedTextWidget(10, -20, displayText, 0xFFFFFF));
        widgetGroup.addWidget(scrollWidget);
        widgetGroup.addWidget(new ToggleButtonWidget(172, 133, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                .setTooltipText("machine.universal.case_sensitive"));
        widgetGroup.addWidget(new ToggleButtonWidget(172, 151, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                .setTooltipText("machine.universal.spaces"));
        widgetGroup.addWidget(new ImageWidget(7, 112, 162, 18, DISPLAY));
        widgetGroup.addWidget(new TJClickButtonWidget(172, 112, 18, 18, "", onClear)
                .setTooltipText("machine.universal.toggle.clear")
                .setButtonTexture(BUTTON_CLEAR_GRID));
        widgetGroup.addWidget(new TJTextFieldWidget(12, 117, 157, 18, false, searchSupplier, searchResponder)
                .setTextLength(256)
                .setBackgroundText("machine.universal.search")
                .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed()) {
            Pair<Integer, BlockPos> selectedPos = this.workableHandler.getPosMap().get(this.workableHandler.getSelectedPosName());
            World world;
            int worldID;
            BlockPos pos;
            long distance;
            long distanceEU;
            if (selectedPos != null) {
                worldID = selectedPos.getLeft();
                pos = selectedPos.getRight();
                world = DimensionManager.getWorld(worldID);
                boolean interdimensional = worldID != this.getWorld().provider.getDimension();
                int x = Math.abs(interdimensional ? pos.getX() : pos.getX() - this.getPos().getX());
                int y = Math.abs(interdimensional ? pos.getY() : pos.getY() - this.getPos().getY());
                int z = Math.abs(interdimensional ? pos.getZ() : pos.getZ() - this.getPos().getZ());
                distance = x + y + z;
                distanceEU = 1000000 + distance * 1000L;
            } else {
                world = null;
                worldID = Integer.MIN_VALUE;
                pos = null;
                distance = 0;
                distanceEU = 0;
            }
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyContainer)
                    .voltageTier(this.tier)
                    .custom(text -> {
                        if (selectedPos != null) {
                            text.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.teleporter.selected.world", world != null ? world.provider.getDimensionType().getName() : "Null", worldID)));
                            text.add(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.teleporter.selected.pos", pos.getX(), pos.getY(), pos.getZ())));
                            text.add(new TextComponentString(I18n.translateToLocalFormatted("metaitem.linking.device.range", distance)));
                        }
                    }).energyInput(hasEnoughEnergy(distanceEU), distanceEU, this.workableHandler.getMaxProgress())
                    .isWorking(this.workableHandler.isWorkingEnabled(), this.workableHandler.isActive(), this.workableHandler.getProgress(), this.workableHandler.getMaxProgress());
        }
    }

    private void addPosDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.pos") + "§r(§e" + this.searchResults + "§r/§e" + this.posMap.size() + "§r)"));
    }

    private void addPosDisplayText2(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
        for (Map.Entry<String, Pair<Integer, BlockPos>> posEntry : this.posMap.entrySet()) {
            String key = posEntry.getKey();
            String result = key, result2 = key;

            if (!this.isCaseSensitive) {
                result = result.toLowerCase();
                result2 = result2.toUpperCase();
            }

            if (!this.hasSpaces) {
                result = result.replace(" ", "");
                result2 = result2.replace(" ", "");
            }

            if (!result.isEmpty() && !result.contains(this.searchPrompt) && !result2.contains(this.searchPrompt))
                continue;

            World world = DimensionManager.getWorld(posEntry.getValue().getLeft());
            String worldName = world != null ? world.provider.getDimensionType().getName() : "Null";
            int worldID = posEntry.getValue().getLeft();

            BlockPos pos = posEntry.getValue().getValue();

            String tp = "tp" + "w" + worldID + "x" + pos.getX() + "y" + pos.getY() + "z" + pos.getZ();
            String select = "select:" + key;
            String remove = "remove:" + key;
            String rename = "rename:" + key;

            ITextComponent keyPos = new TextComponentString(": [§a" + (++count) + "§r] " + key + "§r")
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentString("[TP]"), tp))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.select"), select))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), remove))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), rename));

            ITextComponent blockPos = new TextComponentString(count + ": " + key + "\n")
                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("machine.universal.linked.dimension", worldName, worldID)))
                    .appendText("\n")
                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("machine.universal.linked.pos", pos.getX(), pos.getY(), pos.getZ())));

            keyPos.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, blockPos));
            textList.add(keyPos);
            searchResults++;
        }
        this.searchResults = searchResults;
    }

    private void addQueueDisplayText(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.queue") + "§r(§e" + this.queueCount + "§r/§e" + this.workableHandler.getQueueTeleport().size() + "§r)"));
    }

    private void addQueueDisplayText2(List<ITextComponent> textList) {
        int count = 0, queueCount = 0;
        for (Triple<Entity, Integer, BlockPos> queueEntry : this.workableHandler.getQueueTeleport()) {
            String key = queueEntry.getLeft().getName();
            String result = key, result2 = key;

            if (!this.isCaseSensitive) {
                result = result.toLowerCase();
                result2 = result2.toUpperCase();
            }

            if (!this.hasSpaces) {
                result = result.replace(" ", "");
                result2 = result2.replace(" ", "");
            }

            if (!result.isEmpty() && !result.contains(this.queuePrompt) && !result2.contains(this.queuePrompt))
                continue;

            BlockPos pos = queueEntry.getRight();
            World world = DimensionManager.getWorld(queueEntry.getMiddle());
            String worldName = world != null ? world.provider.getDimensionType().getName() : "Null";
            int worldID = queueEntry.getMiddle();

            String position = I18n.translateToLocal("machine.universal.linked.pos") + " X: §e" + pos.getX() + "§r Y: §e" + pos.getY() + "§r Z: §e" + pos.getZ();

            ITextComponent keyPos = new TextComponentString("[§e" + (++count) + "§r] " + key + "§r");

            ITextComponent blockPos = new TextComponentString(count + ": " + key + "\n")
                    .appendSibling(new TextComponentString(I18n.translateToLocalFormatted("machine.universal.linked.dimension", worldName, worldID)))
                    .appendText("\n")
                    .appendSibling(new TextComponentString(position));

            keyPos.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, blockPos));
            textList.add(keyPos);
            queueCount++;
        }
        this.queueCount = queueCount;
    }

    private void handlePosDisplayClick(String componentData, String textId, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.startsWith("tp")) {
            String[] world = componentData.split("w");
            String[] pos = world[1].split("x");
            String[] x = pos[1].split("y");
            String[] yz = x[1].split("z");

            int worldID = Integer.parseInt(pos[0]);
            int posX = Integer.parseInt(x[0]);
            int posY = Integer.parseInt(yz[0]);
            int posZ = Integer.parseInt(yz[1]);

            if (DimensionManager.getWorld(worldID) == null) {
                DimensionManager.initDimension(worldID);
                DimensionManager.keepDimensionLoaded(worldID, true);
            }

            BlockPos blockPos = new BlockPos(posX, posY, posZ);
            player.sendMessage(new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.teleporter.queue", player.getName())));
            this.workableHandler.getQueueTeleport().add(new ImmutableTriple<>(player, worldID, blockPos));

        } else if (componentData.startsWith("select")) {
            String[] selectedPos = componentData.split(":");
            this.selectedPosName = selectedPos[1];

        } else if (componentData.startsWith("remove")) {
            String[] selectedName = componentData.split(":");
            this.posMap.remove(selectedName[1]);

        } else if (componentData.startsWith("rename")) {
            String[] rename = componentData.split(":");
            this.renamePrompt = rename[1];
            PlayerHolder playerHolder = new PlayerHolder(player, this);
            playerHolder.openUI();
        }
    }

    private String[] getTickSpeedFormat() {
        return ArrayUtils.toArray(String.valueOf(this.maxProgress));
    }

    private void onIncrement(Widget.ClickData clickData) {
        this.maxProgress = MathHelper.clamp(this.maxProgress * 2, 1, Integer.MAX_VALUE);
        this.markDirty();
    }

    private void onDecrement(Widget.ClickData clickData) {
        this.maxProgress = MathHelper.clamp(this.maxProgress / 2, 1, Integer.MAX_VALUE);
        this.markDirty();
    }

    private String getTickSpeed() {
        return String.valueOf(this.maxProgress);
    }

    private void setTickSpeed(String maxProgress) {
        this.maxProgress = maxProgress.isEmpty() ? 1 : Integer.parseInt(maxProgress);
        this.markDirty();
    }

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        this.posMap.clear();
        this.linkData.setInteger("I", this.getPosSize());
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        if (capability == TJCapabilities.CAPABILITY_LINK_POS)
            return TJCapabilities.CAPABILITY_LINK_POS.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public long getEnergyStored() {
        return this.energyContainer.getEnergyStored();
    }

    @Override
    public long getEnergyCapacity() {
        return this.energyContainer.getEnergyCapacity();
    }

    @Override
    public long getMaxEUt() {
        return this.workableHandler.getEnergyPerTick();
    }

    @Override
    public long getTotalEnergyConsumption() {
        return this.workableHandler.getEnergyPerTick();
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
    }

    @Override
    public boolean isInterDimensional() {
        return true;
    }

    @Override
    public int dimensionID() {
        return this.getWorld().provider.getDimension();
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return 1;
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        name = this.checkDuplicateNames(name, 1);
        int worldID = world.provider.getDimension();
        this.workableHandler.getPosMap().put(name, new ImmutablePair<>(worldID, pos));
        this.linkData.setInteger("I", 1);
        this.markDirty();
    }

    private String checkDuplicateNames(String name, int count) {
        if (!this.workableHandler.getPosMap().containsKey(name))
            return name;
        if (count > 1) {
            String[] split = name.split(" ");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < split.length - 1; i++) {
                builder.append(split[i]);
                if (i < split.length - 2)
                    builder.append(" ");
            }
            name = builder.toString();
        }
        name = name + " (" + count + ")";
        return this.checkDuplicateNames(name, ++count);
    }

    @Override
    public World world() {
        return this.getWorld();
    }

    @Override
    public NBTTagCompound getLinkData() {
        return this.workableHandler.getLinkData();
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.workableHandler.setLinkData(linkData);
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.workableHandler.setWorkingEnabled(isActivationAllowed);
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.workableHandler.isWorkingEnabled();
    }

    private IEnergyContainer getEnergyContainer() {
        return this.energyContainer;
    }

    private IMultipleTankHandler getInputFluidHandler() {
        return this.inputFluidHandler;
    }

    public int getTier() {
        return this.tier;
    }
}
