package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IParallelController;
import com.johny.tj.capability.LinkEntity;
import com.johny.tj.capability.LinkEvent;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJGuiTextures;
import com.johny.tj.gui.TJWidgetGroup;
import com.johny.tj.items.TJMetaItems;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.CellCasing;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregtech.api.capability.IElectricItem;
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
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.johny.tj.machines.multi.electric.MetaTileEntityLargeBatteryCharger.TransferMode.INPUT;
import static com.johny.tj.machines.multi.electric.MetaTileEntityLargeBatteryCharger.TransferMode.OUTPUT;
import static gregicadditions.GAMaterials.Talonite;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.MetaTileEntityBatteryTower.cellPredicate;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.unification.material.Materials.Nitrogen;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;

public class MetaTileEntityLargeBatteryCharger extends TJMultiblockDisplayBase implements LinkEntity, LinkEvent, IParallelController {

    private long totalEnergyPerTick;
    private long energyPerTick;
    private int tier;
    private int fluidConsumption;
    private final int pageSize = 4;
    private int pageIndex;
    private boolean isActive;
    private boolean transferToOutput;
    private TransferMode transferMode = INPUT;
    private IItemHandlerModifiable importItemHandler;
    private IItemHandlerModifiable exportItemHandler;
    private IMultipleTankHandler importFluidHandler;
    private IEnergyContainer inputEnergyContainer;
    private IEnergyContainer outputEnergyContainer;
    private EntityPlayer[] linkedPlayers;
    private UUID[] linkedPlayersID;
    private int[] entityLinkWorld;
    private int linkedWorldsCount;
    private NBTTagCompound linkData;

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, OUTPUT_ENERGY, IMPORT_FLUIDS, MAINTENANCE_HATCH};

    public MetaTileEntityLargeBatteryCharger(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeBatteryCharger(metaTileEntityId);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("tj.multiblock.large_battery_charger.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            Style style = new Style().setColor(TextFormatting.GREEN);
            textList.add(hasEnoughEnergy(totalEnergyPerTick) ? new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", getMaxEUt())
                    .appendText("\n")
                    .appendSibling(new TextComponentTranslation("tj.multiblock.parallel.sum", totalEnergyPerTick))
                    .appendText("\n")
                    .appendSibling(new TextComponentTranslation("machine.universal.tooltip.voltage_tier")
                            .appendText(" ")
                            .appendSibling(new TextComponentString(String.valueOf(getVoltageTier())).setStyle(style))
                            .appendText(" (")
                            .appendSibling(new TextComponentString(String.valueOf(GAValues.VN[GTUtility.getGATierByVoltage(getVoltageTier())])).setStyle(style))
                            .appendText(")"))
                    : new TextComponentTranslation("gregtech.multiblock.not_enough_energy")
                    .setStyle(new Style().setColor(TextFormatting.RED)));
            textList.add(isWorkingEnabled ? (isActive ? new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN))
                    : new TextComponentTranslation("gregtech.multiblock.idling"))
                    : new TextComponentTranslation("gregtech.multiblock.work_paused").setStyle(new Style().setColor(TextFormatting.YELLOW)));
            textList.add(hasEnoughFluid(fluidConsumption) || fluidConsumption == 0 ? new TextComponentTranslation("tj.multiblock.enough_fluid")
                    .appendText(" ")
                    .appendSibling(new TextComponentTranslation(Nitrogen.getPlasma(fluidConsumption).getUnlocalizedName())
                            .setStyle(new Style().setColor(TextFormatting.DARK_AQUA)))
                    .appendText(" ")
                    .appendText("§3" + fluidConsumption)
                    .appendText("§7 L/t")
                    : new TextComponentTranslation("tj.multiblock.not_enough_fluid").setStyle(new Style().setColor(TextFormatting.RED)));
            textList.add(new TextComponentTranslation("machine.universal.item.output.transfer")
                    .appendText(" ")
                    .appendSibling(transferToOutput ? withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.enabled"), "transferEnabled")
                            : withButton(new TextComponentTranslation("machine.universal.toggle.run.mode.disabled"), "transferDisabled")));
            textList.add(new TextComponentTranslation("machine.universal.mode.transfer")
                    .appendText(" ")
                    .appendSibling(transferMode == INPUT ? withButton(new TextComponentTranslation("machine.universal.mode.transfer.input"), "input")
                            : withButton(new TextComponentTranslation("machine.universal.mode.transfer.output"), "output")));
        } else {
            super.addDisplayText(textList);
        }
    }

    private void addDisplayLinkedPlayersText(List<ITextComponent> textList) {
        textList.add(new TextComponentTranslation("machine.universal.linked.players")
                .setStyle(new Style().setBold(true).setUnderlined(true)));
        textList.add(new TextComponentString(":")
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[<]"), "leftPage"))
                .appendText(" ")
                .appendSibling(withButton(new TextComponentString("[>]"), "rightPage")));

        for (int i = pageIndex, linkedEntitiesPos = i + 1; i < pageIndex + pageSize; i++, linkedEntitiesPos++) {
            if (i < linkedPlayers.length && linkedPlayers[i] != null) {

                String name = linkedPlayers[i].getName();
                String customName = linkedPlayers[i].getCustomNameTag();
                String inDimensionName = linkedPlayers[i].world.provider.getDimensionType().getName();
                boolean hasCustomName = linkedPlayers[i].hasCustomName();
                long totalEnergyStored = 0;
                long totalEnergyCapacity = 0;

                for (ItemStack stack : linkedPlayers[i].inventory.armorInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored()/ 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                for (ItemStack stack : linkedPlayers[i].inventory.mainInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored()/ 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                for (ItemStack stack : linkedPlayers[i].inventory.offHandInventory) {
                    if (stack.isEmpty())
                        continue;

                    IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                    IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                    if (RFContainer != null) {
                        totalEnergyStored += RFContainer.getEnergyStored() / 4;
                        totalEnergyCapacity += RFContainer.getMaxEnergyStored()/ 4;
                    }
                    if (EUContainer != null) {
                        totalEnergyStored += EUContainer.getCharge();
                        totalEnergyCapacity += EUContainer.getMaxCharge();
                    }
                }

                textList.add(new TextComponentString(": [" + linkedEntitiesPos + "] ")
                        .appendSibling(new TextComponentTranslation(hasCustomName ? customName : name))
                            .setStyle(new Style()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(hasCustomName ? customName : name)
                                        .appendText("\n")
                                        .appendSibling(new TextComponentTranslation("machine.universal.linked.dimension", inDimensionName, getDimension(i)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentTranslation("machine.universal.energy.stored", totalEnergyStored, totalEnergyCapacity))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("X: ").appendSibling(new TextComponentString(String.valueOf(linkedPlayers[i].posX))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("Y: ").appendSibling(new TextComponentString(String.valueOf(linkedPlayers[i].posY))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true)))
                                        .appendText("\n")
                                        .appendSibling(new TextComponentString("Z: ").appendSibling(new TextComponentString(String.valueOf(linkedPlayers[i].posZ))
                                                .setStyle(new Style().setColor(TextFormatting.YELLOW))).setStyle(new Style().setBold(true))))))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove" + i)));

            }
        }
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        TJWidgetGroup widgetLinkedPlayersGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab_linked_players_display", TJMetaItems.LINKING_DEVICE.getStackForm(), linkedPlayersTab(widgetLinkedPlayersGroup::addWidgets)));
    }

    @Override
    protected AbstractWidgetGroup mainDisplayTab(Function<Widget, WidgetGroup> widgetGroup) {
        super.mainDisplayTab(widgetGroup);
        return widgetGroup.apply(new ToggleButtonWidget(172, 151, 18, 18, TJGuiTextures.RESET_BUTTON, this::isReset, this::setReset)
                .setTooltipText("machine.universal.toggle.reset"));
    }

    private AbstractWidgetGroup linkedPlayersTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addDisplayLinkedPlayersText, 0xFFFFFF)
                .setMaxWidthLimit(180).setClickHandler(this::handleLinkedPlayersClick));
    }

    private boolean isReset() {
        return false;
    }

    private void setReset(boolean reset) {
        Arrays.fill(linkedPlayers, null);
        Arrays.fill(linkedPlayersID, null);
        Arrays.fill(entityLinkWorld, Integer.MIN_VALUE);
        linkData.setInteger("I", getPosSize());
        updateTotalEnergyPerTick();
        updateFluidConsumption();
    }

    private void handleLinkedPlayersClick(String componentData, Widget.ClickData clickData) {
        if (componentData.equals("leftPage") && pageIndex > 0) {
            pageIndex -= pageSize;
            return;
        }
        if (componentData.equals("rightPage") && pageIndex < linkedPlayers.length - pageSize) {
            pageIndex += pageSize;
            return;
        }
        for (int i = 0; i < linkedPlayers.length; i++) {
            if (componentData.equals("remove" + i)) {
                int index = linkData.getInteger("I");
                linkData.setInteger("I", index + 1);
                linkedPlayers[i] = null;
                linkedPlayersID[i] = null;
                entityLinkWorld[i] = Integer.MIN_VALUE;
                updateTotalEnergyPerTick();
                break;
            }
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        switch (componentData) {
            case "transferEnabled":
                transferToOutput = false;
                break;
            case "transferDisabled":
                transferToOutput = true;
                break;
            case "input":
                transferMode = OUTPUT;
                break;
            default:
                transferMode = INPUT;
        }
    }

    @Override
    protected void updateFormedValid() {
        if (!isWorkingEnabled || !hasEnoughEnergy(totalEnergyPerTick) || getNumProblems() >= 6) {
            if (isActive)
                setActive(false);
            return;
        }
        if (!isActive)
            setActive(true);
        calculateMaintenance(1);

        if (getOffsetTimer() % 100 == 0)
            playerLinkUpdate();

        for (EntityPlayer linkedPlayer : linkedPlayers) {
            if (linkedPlayer == null)
                continue;

            if (getWorld().provider.getDimension() != linkedPlayer.world.provider.getDimension()) {
                if (!hasEnoughFluid(fluidConsumption))
                    continue;
                consumeFluid();
            }

            for (ItemStack stack : linkedPlayer.inventory.armorInventory) {
                if (stack.isEmpty())
                    continue;
                IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                transferRF((int) energyPerTick, RFContainer, transferMode, stack, false);

                IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                transferEU(energyPerTick, EUContainer, transferMode, stack, false);
            }

            for (ItemStack stack : linkedPlayer.inventory.mainInventory) {
                if (stack.isEmpty())
                    continue;
                IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                transferRF((int) energyPerTick, RFContainer, transferMode, stack, false);

                IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                transferEU(energyPerTick, EUContainer, transferMode, stack, false);
            }

            for (ItemStack stack : linkedPlayer.inventory.offHandInventory) {
                if (stack.isEmpty())
                    continue;
                IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
                transferRF((int) energyPerTick, RFContainer, transferMode, stack, false);

                IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
                transferEU(energyPerTick, EUContainer, transferMode, stack, false);
            }
        }
        for (int i = 0; i < importItemHandler.getSlots(); i++) {
            ItemStack stack = importItemHandler.getStackInSlot(i);
            if (stack.isEmpty())
                continue;

            IEnergyStorage RFContainer = stack.getCapability(ENERGY, null);
            transferRF((int) energyPerTick, RFContainer, transferMode, stack, transferToOutput);

            IElectricItem EUContainer = stack.getCapability(CAPABILITY_ELECTRIC_ITEM, null);
            transferEU(energyPerTick, EUContainer, transferMode, stack, transferToOutput);
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        importItemHandler = new ItemHandlerList(getAbilities(IMPORT_ITEMS));
        exportItemHandler = new ItemHandlerList(getAbilities(EXPORT_ITEMS));
        importFluidHandler = new FluidTankList(true, getAbilities(IMPORT_FLUIDS));
        inputEnergyContainer = new EnergyContainerList(getAbilities(INPUT_ENERGY));
        outputEnergyContainer = new EnergyContainerList(getAbilities(OUTPUT_ENERGY));
        tier = context.getOrDefault("CellType", CellCasing.CellType.CELL_EV).getTier();
        linkedPlayers = linkedPlayers != null ? Arrays.copyOf(linkedPlayers, tier) : new EntityPlayer[tier];
        linkedPlayersID = linkedPlayersID != null ? Arrays.copyOf(linkedPlayersID, tier) : new UUID[tier];
        entityLinkWorld = entityLinkWorld != null ? Arrays.copyOf(entityLinkWorld, tier) : new int[tier];
        energyPerTick = (long) (Math.pow(4, tier) * 8);
        updateTotalEnergyPerTick();
        updateFluidConsumption();
    }

    private void transferToOutput(ItemStack stack, boolean transferToOutput) {
        if (transferToOutput && !getAbilities(EXPORT_ITEMS).isEmpty()) {
            for (int i = 0; i < exportItemHandler.getSlots(); i++) {
                if (exportItemHandler.getStackInSlot(i).isEmpty()) {
                    ItemStack newStack = stack.copy();
                    exportItemHandler.setStackInSlot(i, newStack);
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
                inputEnergyContainer.removeEnergy(energyInserted / 4);
            } else
                transferToOutput(stack, transferToOutput);
        } else {
            long energyRemainingToFill = (outputEnergyContainer.getEnergyCapacity() - outputEnergyContainer.getEnergyStored());
            if (outputEnergyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
                int energyExtracted = RFContainer.extractEnergy((int) Math.min(Integer.MAX_VALUE, Math.min(energyToAdd * 4L, energyRemainingToFill)), false);
                outputEnergyContainer.addEnergy(energyExtracted / 4);
            } else
                transferToOutput(stack, transferToOutput);
        }
    }

    private void transferEU(long energyToAdd, IElectricItem EUContainer, TransferMode transferMode, ItemStack stack, boolean transferToOutput) {
        if (EUContainer == null)
            return;
        if (transferMode == INPUT) {
            long energyRemainingToFill = EUContainer.getMaxCharge() - EUContainer.getCharge();
            if (EUContainer.getCharge() < 1 || energyRemainingToFill != 0) {
                long energyInserted = EUContainer.charge(Math.min(energyRemainingToFill, energyToAdd), tier, true, false);
                inputEnergyContainer.removeEnergy(Math.abs(energyInserted));
            } else
                transferToOutput(stack, transferToOutput);
        } else {
            long energyRemainingToFill = outputEnergyContainer.getEnergyCapacity() - outputEnergyContainer.getEnergyStored();
            if (outputEnergyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
                long energyExtracted = EUContainer.discharge(Math.min(energyRemainingToFill, energyToAdd), tier, true, true,false);
                outputEnergyContainer.addEnergy(energyExtracted);
            } else
                transferToOutput(stack, transferToOutput);
        }
    }

    private void playerLinkUpdate() {
        for (int i = 0; i < linkedPlayersID.length; i++) {
            if (linkedPlayersID[i] == null)
                continue;
            if (linkedPlayers[i] == null)
                linkedPlayers[i] = DimensionManager.getWorld(entityLinkWorld[i]).getPlayerEntityByUUID(linkedPlayersID[i]);
            else
                entityLinkWorld[i] = linkedPlayers[i].world.provider.getDimension();
        }
        updateFluidConsumption();
    }

    private void consumeFluid() {
        int fluidToConsume = fluidConsumption / linkedWorldsCount;
        importFluidHandler.drain(Nitrogen.getPlasma(fluidToConsume), true);
    }

    private void updateFluidConsumption() {
        int dimensionID = getWorld().provider.getDimension();
        linkedWorldsCount = (int) Arrays.stream(entityLinkWorld).filter(id -> id != dimensionID && id != Integer.MIN_VALUE).count();
        fluidConsumption = 10 * linkedWorldsCount;
    }

    private void updateTotalEnergyPerTick() {
        int slots = importItemHandler.getSlots();
        long amps = slots + Arrays.stream(linkedPlayers).filter(Objects::nonNull).count();
        totalEnergyPerTick = (long) (Math.pow(4, tier) * 8) * amps;
    }

    protected boolean hasEnoughEnergy(long amount) {
        return inputEnergyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughFluid(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(Nitrogen.getPlasma(amount), false);
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
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('H', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
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
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        for (int i = 0; i < linkedPlayersID.length; i++) {
            if (linkedPlayersID[i] != null && linkedPlayers[i] != null) {
                data.setUniqueId("PlayerID" + i, linkedPlayersID[i]);
                data.setInteger("EntityWorld" + i, linkedPlayers[i].world.provider.getDimension());
            }
        }
        data.setInteger("TransferMode", transferMode.ordinal());
        data.setBoolean("TransferToOutput", transferToOutput);
        data.setInteger("LinkPlayersSize", linkedPlayers.length);
        if (linkData != null)
            data.setTag("Link.XYZ", linkData);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        linkData = data.hasKey("Link.XYZ") ? data.getCompoundTag("Link.XYZ") : null;
        transferMode = TransferMode.values()[data.getInteger("TransferMode")];
        transferToOutput = data.getBoolean("TransferToOutput");
        linkedPlayers = new EntityPlayer[data.getInteger("LinkPlayersSize")];
        linkedPlayersID = new UUID[data.getInteger("LinkPlayersSize")];
        entityLinkWorld = new int[data.getInteger("LinkPlayersSize")];
        for (int i = 0; i < linkedPlayersID.length; i++) {
            if (data.hasUniqueId("PlayerID" + i)) {
                linkedPlayersID[i] = data.getUniqueId("PlayerID" + i);
                entityLinkWorld[i] = data.getInteger("EntityWorld" + i);
            } else {
                entityLinkWorld[i] = Integer.MIN_VALUE;
            }
        }
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        markDirty();
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
        }
        getHolder().scheduleChunkForRenderUpdate();
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public long getMaxEUt() {
        return inputEnergyContainer != null ? inputEnergyContainer.getInputVoltage()
                : outputEnergyContainer != null ? outputEnergyContainer.getInputVoltage()
                : 0;
    }

    @Override
    public int getEUBonus() {
        return -1;
    }

    @Override
    public long getTotalEnergyConsumption() {
        return totalEnergyPerTick;
    }

    @Override
    public long getVoltageTier() {
        return GAValues.V[tier];
    }

    @Override
    public int dimensionID() {
        return getWorld().provider.getDimension();
    }

    @Override
    public boolean isInterDimensional() {
        return true;
    }

    @Override
    public int getDimension(int index) {
        return linkedPlayers[index].world.provider.getDimension();
    }

    @Override
    public int getRange() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPosSize() {
        return linkedPlayers.length;
    }

    @Override
    public Entity getEntity(int index) {
        return linkedPlayers[index];
    }

    @Override
    public void setPos(String name, BlockPos pos, EntityPlayer player, World world, int index) {
        entityLinkWorld[index] = world.provider.getDimensionType().getId();
        linkedPlayers[index] = player;
        linkedPlayersID[index] = linkedPlayers[index].getUniqueID();
    }

    @Override
    public World world() {
        return getWorld();
    }

    @Override
    public NBTTagCompound getLinkData() {
        return linkData;
    }

    @Override
    public void setLinkData(NBTTagCompound linkData) {
        this.linkData = linkData;
    }

    @Override
    public int getPageIndex() {
        return pageIndex;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void onLink() {
        updateTotalEnergyPerTick();
        updateFluidConsumption();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_LINK_ENTITY)
            return TJCapabilities.CAPABILITY_LINK_ENTITY.cast(this);
        if (capability == TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER)
            return TJCapabilities.CAPABILITY_PARALLEL_CONTROLLER.cast(this);
        return super.getCapability(capability, side);
    }

    public enum TransferMode {
        INPUT,
        OUTPUT
    }
}
