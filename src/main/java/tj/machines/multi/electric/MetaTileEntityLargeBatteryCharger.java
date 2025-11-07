package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.CellCasing;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregtech.api.capability.*;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.MetaTileEntityUIFactory;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.ArrayUtils;
import tj.builder.WidgetTabBuilder;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.capability.IParallelController;
import tj.capability.LinkEntity;
import tj.capability.LinkEvent;
import tj.capability.TJCapabilities;
import tj.gui.TJGuiTextures;
import tj.gui.uifactory.IPlayerUI;
import tj.gui.uifactory.PlayerHolder;
import tj.gui.widgets.OnTextFieldWidget;
import tj.gui.widgets.TJAdvancedTextWidget;
import tj.gui.widgets.TJClickButtonWidget;
import tj.gui.widgets.TJTextFieldWidget;
import tj.items.TJMetaItems;
import tj.util.PlayerWorldIDData;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static gregicadditions.GAMaterials.Talonite;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.MetaTileEntityBatteryTower.cellPredicate;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.unification.material.Materials.Nitrogen;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;
import static tj.gui.TJGuiTextures.CASE_SENSITIVE_BUTTON;
import static tj.gui.TJGuiTextures.SPACES_BUTTON;
import static tj.machines.multi.electric.MetaTileEntityLargeBatteryCharger.TransferMode.INPUT;
import static tj.machines.multi.electric.MetaTileEntityLargeBatteryCharger.TransferMode.OUTPUT;

public class MetaTileEntityLargeBatteryCharger extends TJMultiblockDisplayBase implements LinkEntity, LinkEvent, IParallelController, IWorkable, IPlayerUI {

    private long totalEnergyPerTick;
    private long energyPerTick;
    private int tier;
    private int fluidConsumption;
    private final int pageSize = 4;
    private int pageIndex;
    private boolean isActive;
    private boolean transferToOutput;
    private int progress;
    private int maxProgress = 1;
    private TransferMode transferMode = INPUT;
    private IItemHandlerModifiable importItemHandler;
    private IItemHandlerModifiable exportItemHandler;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer inputEnergyContainer;
    private IEnergyContainer outputEnergyContainer;
    private EntityPlayer[] linkedPlayers;
    private UUID[] linkedPlayersID;
    private String[] entityLinkName;
    private int[] entityLinkWorld;
    private int linkedWorldsCount;
    private String renamePrompt = "";
    private String searchPrompt = "";
    private boolean isCaseSensitive;
    private boolean hasSpaces;
    private int searchResults;
    private NBTTagCompound linkData;

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, OUTPUT_ENERGY, IMPORT_FLUIDS, MAINTENANCE_HATCH};

    public MetaTileEntityLargeBatteryCharger(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeBatteryCharger(this.metaTileEntityId);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_battery_charger.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed())
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.inputEnergyContainer)
                    .voltageTier(this.tier)
                    .energyStored(this.getEnergyStored(), this.getEnergyCapacity())
                    .energyInput(this.hasEnoughEnergy(this.energyPerTick), this.energyPerTick, this.maxProgress)
                    .fluidInput(this.hasEnoughFluid(this.fluidConsumption), Nitrogen.getPlasma(this.fluidConsumption))
                    .custom(text -> {
                        text.add(new TextComponentTranslation("machine.universal.item.output.transfer")
                                .appendText(" ")
                                .appendSibling(transferToOutput ? withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.enabled"), "transferEnabled")
                                        : withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.disabled"), "transferDisabled")));
                        text.add(new TextComponentTranslation("machine.universal.mode.transfer")
                                .appendText(" ")
                                .appendSibling(transferMode == INPUT ? withButton(new TextComponentTranslation("machine.universal.mode.transfer.input"), "input")
                                        : withButton(new TextComponentTranslation("machine.universal.mode.transfer.output"), "output")));
                    })
                    .isWorking(this.isWorkingEnabled, this.isActive, this.progress, this.maxProgress);
    }

    private void addDisplayLinkedPlayersText(List<ITextComponent> textList) {
        textList.add(new TextComponentString("§l" + net.minecraft.util.text.translation.I18n.translateToLocal("machine.universal.linked.players") + "§r(§e" + this.searchResults + "§r/§e" + this.entityLinkName.length + "§r)"));
    }

    private void addDisplayLinkedPlayersText2(List<ITextComponent> textList) {
        int searchResults = 0;
        for (int i = 0; i < this.entityLinkName.length; i++) {
            String name = this.entityLinkName[i] != null ? this.entityLinkName[i] : net.minecraft.util.text.translation.I18n.translateToLocal("machine.universal.empty");
            String result = name, result2 = name;

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

            EntityPlayer player = this.linkedPlayers[i];
            String dimensionName = player != null ? player.world.provider.getDimensionType().getName() : "";
            int dimensionID = player != null ? this.getDimension(i) : 0;
            int x = player != null ? (int) player.posX : Integer.MIN_VALUE;
            int y = player != null ? (int) player.posY : Integer.MIN_VALUE;
            int z = player != null ? (int) player.posZ : Integer.MIN_VALUE;
            long totalEnergyStored = 0;
            long totalEnergyCapacity = 0;

            if (player != null) {
                for (ItemStack stack : player.inventory.armorInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored() / 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                for (ItemStack stack : player.inventory.mainInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored() / 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                for (ItemStack stack : player.inventory.offHandInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored() / 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }
            }

            textList.add(new TextComponentString(": [§a" + (++searchResults) + "§r] ")
                    .appendSibling(new TextComponentString(name))
                    .setStyle(new Style()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(name)
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.energy.stored", totalEnergyStored, totalEnergyCapacity)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.dimension", dimensionName, dimensionID)))
                                    .appendText("\n")
                                    .appendSibling(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("machine.universal.linked.pos", x, y, z))))))
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:" + i))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "rename:" + name)));
        }
        this.searchResults = searchResults;
    }

    @Override
    protected int getExtended() {
        return 18;
    }

    @Override
    public ModularUI createUI(PlayerHolder holder, EntityPlayer player) {
        ModularUI.Builder builder = ModularUI.builder(BORDERED_BACKGROUND, 176, 80);
        builder.widget(new ImageWidget(10, 10, 156, 18, DISPLAY));
        OnTextFieldWidget onTextFieldWidget = new OnTextFieldWidget(15, 15, 151, 18, false, this::getRename, this::setRename);
        onTextFieldWidget.setTooltipText("machine.universal.set.name");
        onTextFieldWidget.setBackgroundText("machine.universal.set.name");
        onTextFieldWidget.setTextLength(256);
        onTextFieldWidget.setValidator(str -> Pattern.compile(".*").matcher(str).matches());
        builder.widget(onTextFieldWidget);
        builder.widget(new TJClickButtonWidget(10, 38, 156, 18, "OK", onTextFieldWidget::onResponder)
                .setClickHandler(this::onPlayerPressed));
        return builder.build(holder, player);
    }

    @Override
    protected void addTabs(WidgetTabBuilder tabBuilder) {
        super.addTabs(tabBuilder);
        tabBuilder.addTab("tj.multiblock.tab.linked_entities_display", TJMetaItems.LINKING_DEVICE.getStackForm(), linkedPlayersTab -> {
            ScrollableListWidget scrollWidget = new ScrollableListWidget(10, -8, 178, 117) {
                @Override
                public boolean isWidgetClickable(Widget widget) {
                    return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
                }
            };
            scrollWidget.addWidget(new TJAdvancedTextWidget(0, 0, this::addDisplayLinkedPlayersText2, 0xFFFFFF)
                    .setClickHandler(this::handleLinkedPlayersClick)
                    .setMaxWidthLimit(1000));
            linkedPlayersTab.addWidget(new AdvancedTextWidget(10, -20, this::addDisplayLinkedPlayersText, 0xFFFFFF));
            linkedPlayersTab.addWidget(scrollWidget);
            linkedPlayersTab.addWidget(new ToggleButtonWidget(172, 133, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                    .setTooltipText("machine.universal.case_sensitive"));
            linkedPlayersTab.addWidget(new ToggleButtonWidget(172, 151, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                    .setTooltipText("machine.universal.spaces"));
            linkedPlayersTab.addWidget(new ImageWidget(7, 112, 162, 18, DISPLAY));
            linkedPlayersTab.addWidget(new TJClickButtonWidget(172, 112, 18, 18, "", this::onClear)
                    .setTooltipText("machine.universal.toggle.clear")
                    .setButtonTexture(BUTTON_CLEAR_GRID));
            linkedPlayersTab.addWidget(new TJTextFieldWidget(12, 117, 157, 18, false, this::getSearchPrompt, this::setSearchPrompt)
                    .setTextLength(256)
                    .setBackgroundText("machine.universal.search")
                    .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
        });
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

    private String getRename() {
        return this.renamePrompt;
    }

    private void setRename(String name) {
        String finalName = this.checkDuplicateNames(name, 1);
        IntStream.range(0, this.entityLinkName.length)
                .filter(i -> this.entityLinkName[i].equals(this.renamePrompt))
                .forEach(i -> this.entityLinkName[i] = finalName);
    }

    private void onPlayerPressed(Widget.ClickData clickData, EntityPlayer player) {
        MetaTileEntityUIFactory.INSTANCE.openUI(this.getHolder(), (EntityPlayerMP) player);
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

    private String getSearchPrompt() {
        return this.searchPrompt;
    }

    private void setSearchPrompt(String searchPrompt) {
        this.searchPrompt = searchPrompt;
        this.markDirty();
    }

    private void onClear(Widget.ClickData clickData) {
        this.setSearchPrompt("");
    }

    private boolean isCaseSensitive() {
        return this.isCaseSensitive;
    }

    private void setCaseSensitive(Boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
        this.markDirty();
    }

    private boolean hasSpaces() {
        return this.hasSpaces;
    }

    private void setSpaces(Boolean hasSpaces) {
        this.hasSpaces = hasSpaces;
        this.markDirty();
    }

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        Arrays.fill(this.linkedPlayers, null);
        Arrays.fill(this.linkedPlayersID, null);
        Arrays.fill(this.entityLinkName, null);
        Arrays.fill(this.entityLinkWorld, Integer.MIN_VALUE);
        this.linkData.setInteger("I", this.getPosSize());
        this.updateTotalEnergyPerTick();
        this.updateFluidConsumption();
    }

    private void handleLinkedPlayersClick(String componentData, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.equals("leftPage") && this.pageIndex > 0) {
            this.pageIndex -= this.pageSize;

        } else if (componentData.equals("rightPage") && this.pageIndex < this.linkedPlayers.length - this.pageSize) {
            this.pageIndex += this.pageSize;

        } else if (componentData.startsWith("remove")) {
            String[] remove = componentData.split(":");
            int i = Integer.parseInt(remove[1]);
            int index = this.linkData.getInteger("I");
            this.linkData.setInteger("I", index + 1);
            this.entityLinkName[i] = null;
            this.linkedPlayers[i] = null;
            this.linkedPlayersID[i] = null;
            this.entityLinkWorld[i] = Integer.MIN_VALUE;
            this.updateTotalEnergyPerTick();

        } else if (componentData.startsWith("rename")) {
            String[] rename = componentData.split(":");
            this.renamePrompt = rename[1];
            PlayerHolder holder = new PlayerHolder(player, this);
            holder.openUI();
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "transferEnabled":
                this.transferToOutput = false;
                break;
            case "transferDisabled":
                this.transferToOutput = true;
                break;
            case "input":
                this.transferMode = OUTPUT;
                break;
            default:
                this.transferMode = INPUT;
        }
    }

    @Override
    protected void updateFormedValid() {
        if (!this.isWorkingEnabled || !this.hasEnoughEnergy(totalEnergyPerTick) || this.getNumProblems() >= 6 || this.maxProgress < 1) {
            if (this.isActive)
                setActive(false);
            return;
        }

        if (this.progress > 0 && !this.isActive)
            this.setActive(true);

        if (this.getOffsetTimer() % 200 == 0)
            this.playerLinkUpdate();

        if (this.progress >= this.maxProgress) {
            for (EntityPlayer linkedPlayer : this.linkedPlayers) {
                if (linkedPlayer == null)
                    continue;

                if (this.getWorld().provider.getDimension() != linkedPlayer.world.provider.getDimension()) {
                    if (!hasEnoughFluid(this.fluidConsumption))
                        continue;
                    this.consumeFluid();
                }

                for (ItemStack stack : linkedPlayer.inventory.armorInventory) {
                    if (stack.isEmpty())
                        continue;
                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    this.transferRF((int) this.energyPerTick, RFContainer, this.transferMode, stack, false);

                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    this.transferEU(this.energyPerTick, EUContainer, this.transferMode, stack, false);
                }

                for (ItemStack stack : linkedPlayer.inventory.mainInventory) {
                    if (stack.isEmpty())
                        continue;
                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    this.transferRF((int) this.energyPerTick, RFContainer, this.transferMode, stack, false);

                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    this.transferEU(this.energyPerTick, EUContainer, this.transferMode, stack, false);
                }

                for (ItemStack stack : linkedPlayer.inventory.offHandInventory) {
                    if (stack.isEmpty())
                        continue;
                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    this.transferRF((int) this.energyPerTick, RFContainer, this.transferMode, stack, false);

                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    this.transferEU(this.energyPerTick, EUContainer, this.transferMode, stack, false);
                }
            }
            for (int i = 0; i < this.importItemHandler.getSlots(); i++) {
                ItemStack stack = this.importItemHandler.getStackInSlot(i);
                if (stack.isEmpty())
                    continue;

                IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                this.transferRF((int) this.energyPerTick, RFContainer, this.transferMode, stack, this.transferToOutput);

                IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                this.transferEU(this.energyPerTick, EUContainer, this.transferMode, stack, this.transferToOutput);
            }
            this.calculateMaintenance(this.maxProgress);
            this.progress = 0;
            if (this.isActive)
                this.setActive(false);
        }

        if (this.progress <= 0) {
            this.progress = 1;
            if (!this.isActive)
                this.setActive(true);
        } else {
            this.progress++;
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.importItemHandler = new ItemHandlerList(this.getAbilities(IMPORT_ITEMS));
        this.exportItemHandler = new ItemHandlerList(this.getAbilities(EXPORT_ITEMS));
        this.importFluidHandler = new FluidTankList(true, this.getAbilities(IMPORT_FLUIDS));
        this.inputEnergyContainer = new EnergyContainerList(this.getAbilities(INPUT_ENERGY));
        this.outputEnergyContainer = new EnergyContainerList(this.getAbilities(OUTPUT_ENERGY));
        this.tier = context.getOrDefault("CellType", CellCasing.CellType.CELL_EV).getTier();
        this.linkedPlayers = this.linkedPlayers != null ? Arrays.copyOf(this.linkedPlayers, this.tier) : new EntityPlayer[this.tier];
        this.linkedPlayersID = this.linkedPlayersID != null ? Arrays.copyOf(this.linkedPlayersID, this.tier) : new UUID[this.tier];
        this.entityLinkName = this.entityLinkName != null ? Arrays.copyOf(this.entityLinkName, this.tier) : new String[this.tier];
        this.entityLinkWorld = this.entityLinkWorld != null ? Arrays.copyOf(this.entityLinkWorld, this.tier) : new int[this.tier];
        this.energyPerTick = (long) (Math.pow(4, this.tier) * 8);
        this.updateTotalEnergyPerTick();
        this.updateFluidConsumption();
    }

    private void transferToOutput(ItemStack stack, boolean transferToOutput) {
        if (transferToOutput && !this.getAbilities(EXPORT_ITEMS).isEmpty()) {
            for (int i = 0; i < this.exportItemHandler.getSlots(); i++) {
                if (this.exportItemHandler.getStackInSlot(i).isEmpty()) {
                    ItemStack newStack = stack.copy();
                    this.exportItemHandler.setStackInSlot(i, newStack);
                    stack.setCount(0);
                }
            }
        }
    }

    private void transferRF(int energyToAdd, IEnergyStorage RFContainer, TransferMode transferMode, ItemStack stack, boolean transferToOutput) {
        if (RFContainer == null)
            return;
        if (transferMode == INPUT) {
            int energyRemainingToFill = RFContainer.getMaxEnergyStored() - RFContainer.getEnergyStored();
            if (RFContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
                int energyInserted = RFContainer.receiveEnergy(Math.min(Integer.MAX_VALUE, energyRemainingToFill >= energyToAdd ? (energyToAdd * 4) : energyRemainingToFill), false);
                this.inputEnergyContainer.removeEnergy(energyInserted / 4);
            } else {
                this.transferToOutput(stack, transferToOutput);
            }
        } else {
            long energyRemainingToFill = (this.outputEnergyContainer.getEnergyCapacity() - this.outputEnergyContainer.getEnergyStored());
            if (this.outputEnergyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
                int energyExtracted = RFContainer.extractEnergy((int) Math.min(Integer.MAX_VALUE, Math.min(energyToAdd * 4L, energyRemainingToFill)), false);
                this.outputEnergyContainer.addEnergy(energyExtracted / 4);
            }
            if (RFContainer.getEnergyStored() < 1)
                this.transferToOutput(stack, transferToOutput);
        }
    }

    private void transferEU(long energyToAdd, IElectricItem EUContainer, TransferMode transferMode, ItemStack stack, boolean transferToOutput) {
        if (EUContainer == null)
            return;
        if (transferMode == INPUT) {
            long energyRemainingToFill = EUContainer.getMaxCharge() - EUContainer.getCharge();
            if (EUContainer.getCharge() < 1 || energyRemainingToFill != 0) {
                long energyInserted = EUContainer.charge(Math.min(energyRemainingToFill, energyToAdd), this.tier, true, false);
                this.inputEnergyContainer.removeEnergy(Math.abs(energyInserted));
            } else {
                this.transferToOutput(stack, transferToOutput);
            }
        } else {
            long energyRemainingToFill = this.outputEnergyContainer.getEnergyCapacity() - this.outputEnergyContainer.getEnergyStored();
            if (this.outputEnergyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
                long energyExtracted = EUContainer.discharge(Math.min(energyRemainingToFill, energyToAdd), tier, true, true,false);
                this.outputEnergyContainer.addEnergy(energyExtracted);
            }
            if (EUContainer.getCharge() < 1)
                this.transferToOutput(stack, transferToOutput);
        }
    }

    private void playerLinkUpdate() {
        for (int i = 0; i < this.linkedPlayersID.length; i++) {
            if (this.linkedPlayersID[i] == null)
                continue;

            int worldID = PlayerWorldIDData.getINSTANCE().getPlayerWorldIdMap().get(this.linkedPlayersID[i]);
            this.linkedPlayers[i] = DimensionManager.getWorld(worldID).getPlayerEntityByUUID(this.linkedPlayersID[i]);
            this.entityLinkWorld[i] = worldID;
        }
        this.updateFluidConsumption();
    }

    private void consumeFluid() {
        int fluidToConsume = this.fluidConsumption / this.linkedWorldsCount;
        this.importFluidHandler.drain(Nitrogen.getPlasma(fluidToConsume), true);
    }

    private void updateFluidConsumption() {
        int dimensionID = getWorld().provider.getDimension();
        this.linkedWorldsCount = (int) Arrays.stream(this.entityLinkWorld).filter(id -> id != dimensionID && id != Integer.MIN_VALUE).count();
        this.fluidConsumption = 10 * this.linkedWorldsCount;
    }

    private void updateTotalEnergyPerTick() {
        int slots = this.importItemHandler.getSlots();
        long amps = slots + Arrays.stream(this.linkedPlayers).filter(Objects::nonNull).count();
        this.totalEnergyPerTick = (long) (Math.pow(4, this.tier) * 8) * amps;
    }

    protected boolean hasEnoughEnergy(long amount) {
        return this.inputEnergyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(Nitrogen.getPlasma(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("HHHHH", "~HHH~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH" ,"HHHHH", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "HHHHH", "~BFB~", "~BFB~", "~BFB~", "~BFB~", "~BFB~", "~CFC~", "~~F~~", "~~F~~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("HHHHH", "HHHHH", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "CFBFC", "~CCC~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("HHHHH", "~HSH~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~C~C~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('H', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('B', cellPredicate())
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Talonite).getDefaultState()))
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.TALONITE);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.TALONITE_CASING;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.getFrontFacing(), this.isActive);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList linkList = new NBTTagList();
        for (int i = 0; i < this.linkedPlayersID.length; i++) {
            if (this.linkedPlayersID[i] != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Index", i);
                tag.setUniqueId("PlayerID", this.linkedPlayersID[i]);
                tag.setString("Name", this.entityLinkName[i]);
                tag.setInteger("World", this.entityLinkWorld[i]);
                linkList.appendTag(tag);
            }
        }
        data.setTag("Links", linkList);
        data.setInteger("TransferMode", this.transferMode.ordinal());
        data.setInteger("Progress", this.progress);
        data.setInteger("MaxProgress", this.maxProgress);
        data.setBoolean("TransferToOutput", this.transferToOutput);
        data.setInteger("LinkPlayersSize", this.linkedPlayers.length);
        if (this.linkData != null)
            data.setTag("Link.XYZ", this.linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.transferMode = TransferMode.values()[data.getInteger("TransferMode")];
        this.transferToOutput = data.getBoolean("TransferToOutput");
        this.linkedPlayers = new EntityPlayer[data.getInteger("LinkPlayersSize")];
        this.linkedPlayersID = new UUID[data.getInteger("LinkPlayersSize")];
        this.entityLinkName = new String[data.getInteger("LinkPlayersSize")];
        this.entityLinkWorld = new int[data.getInteger("LinkPlayersSize")];
        this.maxProgress = data.hasKey("MaxProgress") ? data.getInteger("MaxProgress") : 1;
        this.progress = data.getInteger("Progress");
        NBTTagList linkList = data.getTagList("Links", Constants.NBT.TAG_COMPOUND);
        for (NBTBase nbtBase : linkList) {
            NBTTagCompound tag = (NBTTagCompound) nbtBase;
            int i = tag.getInteger("Index");
            this.linkedPlayersID[i] = tag.getUniqueId("PlayerID");
            this.entityLinkName[i] = tag.getString("Name");
            this.entityLinkWorld[i] = tag.getInteger("World");
        }
        if (data.hasKey("Link.XYZ"))
            this.linkData = data.getCompoundTag("Link.XYZ");
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        this.markDirty();
        if (!this.getWorld().isRemote) {
            this.writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
        }
        this.getHolder().scheduleChunkForRenderUpdate();
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public long getEnergyStored() {
        return this.inputEnergyContainer != null && transferMode == INPUT ? this.inputEnergyContainer.getEnergyStored()
                : this.outputEnergyContainer != null && transferMode == OUTPUT ? this.outputEnergyContainer.getEnergyStored()
                : 0;
    }

    @Override
    public long getEnergyCapacity() {
        return this.inputEnergyContainer != null && transferMode == INPUT ? this.inputEnergyContainer.getEnergyCapacity()
                : this.outputEnergyContainer != null && transferMode == OUTPUT ? this.outputEnergyContainer.getEnergyCapacity()
                : 0;
    }

    @Override
    public long getMaxEUt() {
        return this.inputEnergyContainer != null && transferMode == INPUT ? this.inputEnergyContainer.getInputVoltage()
                : this.outputEnergyContainer != null && transferMode == OUTPUT ? this.outputEnergyContainer.getInputVoltage()
                : 0;
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergyConsumption() {
        return this.totalEnergyPerTick;
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[this.tier];
    }

    @Override
    public int dimensionID() {
        return this.getWorld().provider.getDimension();
    }

    @Override
    public boolean isInterDimensional() {
        return true;
    }

    @Override
    public int getDimension(int index) {
        return this.linkedPlayers[index].world.provider.getDimension();
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return this.linkedPlayers.length;
    }

    @Override
    public Entity getEntity(int index) {
        return this.linkedPlayers[index];
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        name = this.checkDuplicateNames(name, 1);
        this.entityLinkName[index] = name;
        this.entityLinkWorld[index] = world.provider.getDimension();
        this.linkedPlayers[index] = player;
        this.linkedPlayersID[index] = this.linkedPlayers[index].getUniqueID();
    }

    private String checkDuplicateNames(String name, int count) {
        if (!Arrays.asList(this.entityLinkName).contains(name))
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
        return this.linkData;
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.linkData = linkData;
    }

    @Override
    public int getPageIndex() {
        return this.pageIndex;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public void onLink(MetaTileEntity tileEntity) {
        this.updateTotalEnergyPerTick();
        this.updateFluidConsumption();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_LINK_ENTITY)
            return TJCapabilities.CAPABILITY_LINK_ENTITY.cast(this);
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }

    public enum TransferMode {
        INPUT,
        OUTPUT
    }
}
