package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IFluidHandlerInfo;
import com.johny.tj.capability.IHeatInfo;
import com.johny.tj.capability.IItemHandlerInfo;
import com.johny.tj.capability.TJCapabilities;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.IWorkable;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.AdvancedTextWidget;
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
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.johny.tj.textures.TJTextures.HEAVY_QUARK_DEGENERATE_MATTER;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.recipes.categories.handlers.VoidMinerHandler.ORES_3;
import static net.minecraft.util.text.TextFormatting.*;

public class MetaTileEntityVoidMOreMiner extends TJMultiblockDisplayBase implements IHeatInfo, IWorkable, IFluidHandlerInfo, IItemHandlerInfo {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private static final int CONSUME_START = 100;
    private IEnergyContainer energyContainer;
    private IMultipleTankHandler importFluidHandler;
    private IMultipleTankHandler exportFluidHandler;
    protected IItemHandlerModifiable outputInventory;
    private boolean isActive;
    private boolean overheat;
    private long maxTemperature;
    private long temperature = 0;
    private double currentDrillingFluid = CONSUME_START;
    private long energyDrain;
    private int tier;
    private int progress;
    private final int maxProgress = 20;
    private final List<FluidStack> fluidInputs = new ArrayList<>();
    private final List<FluidStack> fluidOutputs = new ArrayList<>();
    private final List<ItemStack> oreOutputs = new ArrayList<>();

    public MetaTileEntityVoidMOreMiner(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityVoidMOreMiner(metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.void_miner.description.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.void_miner.description.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.void_miner.description.3"));
        tooltip.add(I18n.format("gtadditions.multiblock.void_miner.description.4"));
        tooltip.add(I18n.format("gtadditions.multiblock.void_miner.description.5"));
        tooltip.add(I18n.format("gtadditions.multiblock.void_miner.description.6"));
        tooltip.add(I18n.format("tj.multiblock.void_more_miner.description"));
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs) {
        super.addNewTabs(tabs);
        WidgetGroup widgetFluidGroup = new WidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.fluid", new ItemStack(Items.WATER_BUCKET), fluidsTab(widget -> {widgetFluidGroup.addWidget(widget); return widgetFluidGroup;})));
    }

    private AbstractWidgetGroup fluidsTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addFluidDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed() && !this.hasProblems()) {
            if (energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
                long maxVoltage = energyContainer.getInputVoltage();
                String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
                textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
            }
            int currentProgress = (int) Math.floor(progress / (maxProgress * 1.0) * 100);
            boolean hasEnoughEnergy = hasEnoughEnergy(energyDrain);

            ITextComponent temperatureText = new TextComponentTranslation("gregtech.multiblock.large_boiler.temperature", temperature, maxTemperature);
            ITextComponent energyText = hasEnoughEnergy ? new TextComponentTranslation("gregtech.multiblock.universal.energy_used", energyDrain)
                    : new TextComponentTranslation("gregtech.multiblock.not_enough_energy");
            ITextComponent isWorkingText = !isWorkingEnabled ? new TextComponentTranslation("gregtech.multiblock.work_paused")
                    : !isActive ? new TextComponentTranslation("gregtech.multiblock.idling")
                    : new TextComponentTranslation("gregtech.multiblock.running");

            energyText.getStyle().setColor(hasEnoughEnergy ? WHITE : RED);
            isWorkingText.getStyle().setColor(!isWorkingEnabled ? YELLOW : !isActive ? WHITE : GREEN);

            textList.addAll(Arrays.asList(temperatureText, energyText, isWorkingText));
            if (isActive)
                textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
            if (overheat)
                textList.add(new TextComponentTranslation("gregtech.multiblock.universal.overheat").setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    private void addFluidDisplayText(List<ITextComponent> textList) {
        String pyrotheumName = Pyrotheum.getFluid((int) currentDrillingFluid).getLocalizedName();
        String cryotheumName = Cryotheum.getFluid((int) currentDrillingFluid).getLocalizedName();
        String drillingMudName = DrillingMud.getFluid((int) currentDrillingFluid).getLocalizedName();
        String usedDrillingMudName =  UsedDrillingMud.getFluid((int) currentDrillingFluid).getLocalizedName();

        boolean hasEnoughPyrotheum = hasEnoughPyrotheum((int) currentDrillingFluid);
        boolean hasEnoughCryotheum = hasEnoughCryotheum((int) currentDrillingFluid);
        boolean hasEnoughDrillingMud = hasEnoughDrillingMud((int) currentDrillingFluid);
        boolean canFillDrillingMudOutput = canOutputUsedDrillingMud((int) currentDrillingFluid);

        ITextComponent pyrotheumInputText = hasEnoughPyrotheum ? new TextComponentTranslation("machine.universal.fluid.input.sec", pyrotheumName, (int) currentDrillingFluid)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", pyrotheumName, (int) currentDrillingFluid);
        ITextComponent cryotheumInputText = hasEnoughCryotheum ? new TextComponentTranslation("machine.universal.fluid.input.sec", cryotheumName, (int) currentDrillingFluid)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", cryotheumName, (int) currentDrillingFluid);
        ITextComponent drillingMudInputText = hasEnoughDrillingMud ? new TextComponentTranslation("machine.universal.fluid.input.sec", drillingMudName, (int) currentDrillingFluid)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", drillingMudName, (int) currentDrillingFluid);
        ITextComponent drillingMudOutputText = canFillDrillingMudOutput ? new TextComponentTranslation("machine.universal.fluid.output.sec", usedDrillingMudName, (int) currentDrillingFluid)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid.space", usedDrillingMudName, (int) currentDrillingFluid);

        pyrotheumInputText.getStyle().setColor(hasEnoughPyrotheum ? WHITE : RED);
        cryotheumInputText.getStyle().setColor(hasEnoughCryotheum ? WHITE : RED);
        drillingMudInputText.getStyle().setColor(hasEnoughDrillingMud ? WHITE : RED);
        drillingMudOutputText.getStyle().setColor(canFillDrillingMudOutput ? WHITE : RED);

        textList.addAll(Arrays.asList(pyrotheumInputText, cryotheumInputText, drillingMudInputText, drillingMudOutputText));
    }

    @Override
    protected void updateFormedValid() {
        if (tier >= GTValues.UV) {

            if (overheat || !isWorkingEnabled || getNumProblems() >= 6) {
                if (temperature > 0) {
                    temperature--;
                }
                if (temperature == 0) {
                    overheat = false;
                }
                if (currentDrillingFluid > CONSUME_START) {
                    currentDrillingFluid--;
                }
                if (currentDrillingFluid < CONSUME_START) {
                    currentDrillingFluid = CONSUME_START;
                }
                if (isActive)
                    setActive(false);
                return;
            }

            calculateMaintenance(1);
            if (progress > 0 && !isActive)
                setActive(true);

            if (progress >= maxProgress) {
                if (addItemsToItemHandler(outputInventory, true, oreOutputs))
                    addItemsToItemHandler(outputInventory, false, oreOutputs);
                progress = 0;
                fluidInputs.clear();
                fluidOutputs.clear();
                oreOutputs.clear();
                if (isActive)
                    setActive(false);
            }

            if (hasEnoughEnergy(energyDrain)) {
                if (progress <= 0) {
                    boolean canMineOres = false;
                    if (hasEnoughPyrotheum((int) currentDrillingFluid) && hasEnoughCryotheum((int) currentDrillingFluid)) {
                        fluidInputs.add(importFluidHandler.drain(Pyrotheum.getFluid((int) currentDrillingFluid), true));
                        fluidInputs.add(importFluidHandler.drain(Cryotheum.getFluid((int) currentDrillingFluid), true));
                        canMineOres = true;
                    } else if (hasEnoughPyrotheum((int) currentDrillingFluid)) {
                        fluidInputs.add(importFluidHandler.drain(Pyrotheum.getFluid((int) currentDrillingFluid), true));
                        temperature += (long) (currentDrillingFluid / 100.0);
                        currentDrillingFluid *= 1.02;
                        canMineOres = true;
                    } else if (hasEnoughCryotheum((int) currentDrillingFluid)) {
                        fluidInputs.add(importFluidHandler.drain(Cryotheum.getFluid((int) currentDrillingFluid), true));
                        currentDrillingFluid /= 1.02;
                        temperature -= (long) (currentDrillingFluid / 100.0);
                    } else {
                        return; // prevent energy consumption if either fluids are not consumed
                    }

                    if (temperature < 0) {
                        temperature = 0;
                    }
                    if (currentDrillingFluid < CONSUME_START) {
                        currentDrillingFluid = CONSUME_START;
                    }
                    if (temperature > maxTemperature) {
                        overheat = true;
                        currentDrillingFluid = CONSUME_START;
                        return;
                    }

                    currentDrillingFluid += this.getNumProblems();

                    if (hasEnoughDrillingMud((int) currentDrillingFluid) && canOutputUsedDrillingMud((int) currentDrillingFluid)) {
                        fluidInputs.add(importFluidHandler.drain(DrillingMud.getFluid((int) currentDrillingFluid), true));
                        int outputAmount = exportFluidHandler.fill(UsedDrillingMud.getFluid((int) currentDrillingFluid), true);
                        fluidOutputs.add(new FluidStack(UsedDrillingMud.getFluid(outputAmount), outputAmount));
                        long nbOres = temperature / 1000;

                        if (nbOres != 0 && canMineOres) {
                            List<ItemStack> ores = getOres();
                            Collections.shuffle(ores);
                            oreOutputs.addAll(ores.stream()
                                    .limit(10)
                                    .peek(itemStack -> itemStack.setCount(getWorld().rand.nextInt((int) (nbOres * nbOres)) + 1))
                                    .collect(Collectors.toCollection(ArrayList::new)));

                            oreOutputs.forEach(ore -> ore.getItem().setMaxStackSize(Integer.MAX_VALUE));
                        }
                    }
                    progress = 1;
                    if (!isActive)
                        setActive(true);
                } else {
                    progress++;
                }
                energyContainer.removeEnergy(energyDrain);
            } else {
                if (progress > 1)
                    progress--;
            }
        }
    }

    private boolean hasEnoughEnergy(long amount) {
        return energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughDrillingMud(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(DrillingMud.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean hasEnoughPyrotheum(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(Pyrotheum.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean hasEnoughCryotheum(int amount) {
        FluidStack fluidStack = importFluidHandler.drain(Cryotheum.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean canOutputUsedDrillingMud(int amount) {
        int fluidStack = exportFluidHandler.fill(UsedDrillingMud.getFluid(amount), false);
        return fluidStack == amount;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.importFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.exportFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.tier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.energyDrain = (long) (Math.pow(4, tier) * 8);
        int startTier = tier - GTValues.ZPM;
        int multiplier = (startTier + 2) * 100;
        int multiplier2 = Math.min((startTier + 2) * 10, 40);
        int multiplier3 = startTier > 2 ? (int) Math.pow(2.8, startTier - 2) : 1;
        this.maxTemperature = multiplier * ((long) multiplier2 * multiplier3);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.importFluidHandler = new FluidTankList(true);
        this.exportFluidHandler = new FluidTankList(true);
        this.outputInventory = new ItemStackHandler(0);
        this.energyContainer = new EnergyContainerList(Lists.newArrayList());
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("CCCCCCCCC", "CCCCCCCCC", "C#######C", "C#######C", "C#######C", "CCCCCCCCC", "CFFFFFFFC", "CFFFFFFFC", "C#######C", "C#######C")
                .aisle("C#######C", "C#######C", "#########", "#########", "#########", "C###D###C", "F##DDD##F", "F##DDD##F", "###DDD###", "#########")
                .aisle("C#######C", "C#######C", "#########", "####D####", "###DDD###", "C##DDD##C", "F#DD#DD#F", "F#D###D#F", "##D###D##", "#########")
                .aisle("C###D###C", "C###D###C", "###DDD###", "###D#D###", "##DD#DD##", "C#D###D#C", "FDD###DDF", "FD#####DF", "#D#####D#", "#########")
                .aisle("C##DMD##C", "C##DMD##C", "###DMD###", "##D###D##", "##D###D##", "CDD###DDC", "FD#####DF", "FD#####DF", "#D#####D#", "#########")
                .aisle("C###D###C", "C###D###C", "###DDD###", "###D#D###", "##DD#DD##", "C#D###D#C", "FDD###DDF", "FD#####DF", "#D#####D#", "#########")
                .aisle("C#######C", "C#######C", "#########", "####D####", "###DDD###", "C##DDD##C", "F#DD#DD#F", "F#D###D#F", "##D###D##", "#########")
                .aisle("C#######C", "C#######C", "#########", "#########", "#########", "C###D###C", "F##DDD##F", "F##DDD##F", "###DDD###", "#########")
                .aisle("CCCCCCCCC", "CCCCSCCCC", "C#######C", "C#######C", "C#######C", "CCCCCCCCC", "CFFFFFFFC", "CFFFFFFFC", "C#######C", "C#######C")
                .where('S', selfPredicate())
                .where('C', statePredicate(TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.HEAVY_QUARK_DEGENERATE_MATTER)).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('D', statePredicate(TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.PERIODICIUM)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(QCDMatter).getDefaultState()))
                .where('M', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('#', (tile) -> true)
                .build();
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return HEAVY_QUARK_DEGENERATE_MATTER;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive);
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
        if (dataId == 1)
            this.isActive = buf.readBoolean();
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
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Progress", progress);
        data.setLong("Temperature", temperature);
        data.setDouble("CurrentDrillingFluid", currentDrillingFluid);
        data.setBoolean("Overheat", overheat);
        data.setBoolean("Active", isActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        progress = data.getInteger("Progress");
        temperature = data.getLong("Temperature");
        currentDrillingFluid = data.getDouble("CurrentDrillingFluid");
        overheat = data.getBoolean("Overheat");
        isActive = data.getBoolean("Active");
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_HEAT)
            return TJCapabilities.CAPABILITY_HEAT.cast(this);
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_ITEM_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_HANDLING.cast(this);
        if (capability == TJCapabilities.CAPABILITY_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_FLUID_HANDLING.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public long heat() {
        return temperature;
    }

    @Override
    public long maxHeat() {
        return maxTemperature;
    }

    private static List<ItemStack> getOres() {
        return ORES_3;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return maxProgress;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return fluidInputs;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    @Override
    public List<ItemStack> getItemInputs() {
        return null;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return oreOutputs;
    }
}
