package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.MultiblockDisplayBuilder;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IHeatInfo;
import com.johny.tj.capability.IItemFluidHandlerInfo;
import com.johny.tj.capability.TJCapabilities;
import com.johny.tj.gui.TJWidgetGroup;
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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.johny.tj.textures.TJTextures.HEAVY_QUARK_DEGENERATE_MATTER;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.recipes.categories.handlers.VoidMinerHandler.ORES_3;

public class MetaTileEntityVoidMOreMiner extends TJMultiblockDisplayBase implements IHeatInfo, IWorkable, IItemFluidHandlerInfo {

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
        return new MetaTileEntityVoidMOreMiner(this.metaTileEntityId);
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
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs, int extended) {
        super.addNewTabs(tabs, extended);
        TJWidgetGroup widgetFluidGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.fluid", new ItemStack(Items.WATER_BUCKET), fluidsTab(widgetFluidGroup::addWidgets)));
    }

    private AbstractWidgetGroup fluidsTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, 18, this::addFluidDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed()) {
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyContainer)
                    .temperature(this.temperature, this.maxTemperature)
                    .energyInput(this.hasEnoughEnergy(this.energyDrain), this.energyDrain)
                    .isWorking(this.isWorkingEnabled, this.isActive, this.progress, this.maxProgress);
            if (this.overheat)
                textList.add(new TextComponentTranslation("gregtech.multiblock.universal.overheat").setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }

    private void addFluidDisplayText(List<ITextComponent> textList) {
        MultiblockDisplayBuilder.start(textList)
                .fluidInput(this.hasEnoughPyrotheum((int) this.currentDrillingFluid), Pyrotheum.getFluid((int) this.currentDrillingFluid))
                .fluidInput(this.hasEnoughCryotheum((int) this.currentDrillingFluid), Cryotheum.getFluid((int) this.currentDrillingFluid))
                .fluidInput(this.hasEnoughDrillingMud((int) this.currentDrillingFluid), DrillingMud.getFluid((int) this.currentDrillingFluid))
                .fluidInput(this.canOutputUsedDrillingMud((int) this.currentDrillingFluid), UsedDrillingMud.getFluid((int) this.currentDrillingFluid));
    }

    @Override
    protected void updateFormedValid() {
        if (this.tier >= GTValues.UV) {

            if (this.overheat || !this.isWorkingEnabled || this.getNumProblems() >= 6) {
                if (this.temperature > 0) {
                    this.temperature--;
                }
                if (this.temperature == 0) {
                    this.overheat = false;
                }
                if (this.currentDrillingFluid > CONSUME_START) {
                    this.currentDrillingFluid--;
                }
                if (this.currentDrillingFluid < CONSUME_START) {
                    this.currentDrillingFluid = CONSUME_START;
                }
                if (this.isActive)
                    this.setActive(false);
                return;
            }

            if (progress > 0 && !isActive)
                this.setActive(true);

            if (this.progress >= this.maxProgress) {
                if (addItemsToItemHandler(this.outputInventory, true, this.oreOutputs))
                    addItemsToItemHandler(this.outputInventory, false, this.oreOutputs);
                this.calculateMaintenance(this.maxProgress);
                this.progress = 0;
                this.fluidInputs.clear();
                this.fluidOutputs.clear();
                this.oreOutputs.clear();
                if (this.isActive)
                    this.setActive(false);
            }

            if (this.hasEnoughEnergy(this.energyDrain)) {
                if (this.progress <= 0) {
                    boolean canMineOres = false;
                    if (hasEnoughPyrotheum((int) this.currentDrillingFluid) && this.hasEnoughCryotheum((int) this.currentDrillingFluid)) {
                        this.fluidInputs.add(this.importFluidHandler.drain(Pyrotheum.getFluid((int) this.currentDrillingFluid), true));
                        this.fluidInputs.add(this.importFluidHandler.drain(Cryotheum.getFluid((int) this.currentDrillingFluid), true));
                        canMineOres = true;
                    } else if (hasEnoughPyrotheum((int) this.currentDrillingFluid)) {
                        this.fluidInputs.add(this.importFluidHandler.drain(Pyrotheum.getFluid((int) this.currentDrillingFluid), true));
                        this.temperature += (long) (this.currentDrillingFluid / 100.0);
                        this.currentDrillingFluid *= 1.02;
                        canMineOres = true;
                    } else if (hasEnoughCryotheum((int) this.currentDrillingFluid)) {
                        this.fluidInputs.add(this.importFluidHandler.drain(Cryotheum.getFluid((int) this.currentDrillingFluid), true));
                        this.currentDrillingFluid /= 1.02;
                        this.temperature -= (long) (this.currentDrillingFluid / 100.0);
                    } else {
                        return; // prevent energy consumption if either fluids are not consumed
                    }

                    if (this.temperature < 0) {
                        this.temperature = 0;
                    }
                    if (this.currentDrillingFluid < CONSUME_START) {
                        this.currentDrillingFluid = CONSUME_START;
                    }
                    if (this.temperature > this.maxTemperature) {
                        this.overheat = true;
                        this.currentDrillingFluid = CONSUME_START;
                        return;
                    }

                    this.currentDrillingFluid += this.getNumProblems();

                    if (hasEnoughDrillingMud((int) this.currentDrillingFluid) && this.canOutputUsedDrillingMud((int) this.currentDrillingFluid)) {
                        this.fluidInputs.add(this.importFluidHandler.drain(DrillingMud.getFluid((int) this.currentDrillingFluid), true));
                        int outputAmount = this.exportFluidHandler.fill(UsedDrillingMud.getFluid((int) this.currentDrillingFluid), true);
                        this.fluidOutputs.add(new FluidStack(UsedDrillingMud.getFluid(outputAmount), outputAmount));
                        long nbOres = this.temperature / 1000;

                        if (nbOres != 0 && canMineOres) {
                            List<ItemStack> ores = getOres();
                            Collections.shuffle(ores);
                            this.oreOutputs.addAll(ores.stream()
                                    .limit(10)
                                    .peek(itemStack -> itemStack.setCount(getWorld().rand.nextInt((int) (nbOres * nbOres)) + 1))
                                    .collect(Collectors.toCollection(ArrayList::new)));

                            this.oreOutputs.forEach(ore -> ore.getItem().setMaxStackSize(Integer.MAX_VALUE));
                        }
                    }
                    this.progress = 1;
                    if (!this.isActive)
                        this.setActive(true);
                } else {
                    this.progress++;
                }
                this.energyContainer.removeEnergy(this.energyDrain);
            } else {
                if (this.progress > 1)
                    this.progress--;
            }
        }
    }

    private boolean hasEnoughEnergy(long amount) {
        return this.energyContainer.getEnergyStored() >= amount;
    }

    private boolean hasEnoughDrillingMud(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(DrillingMud.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean hasEnoughPyrotheum(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(Pyrotheum.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean hasEnoughCryotheum(int amount) {
        FluidStack fluidStack = this.importFluidHandler.drain(Cryotheum.getFluid(amount), false);
        return fluidStack != null && fluidStack.amount == amount;
    }

    private boolean canOutputUsedDrillingMud(int amount) {
        int fluidStack = this.exportFluidHandler.fill(UsedDrillingMud.getFluid(amount), false);
        return fluidStack == amount;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.importFluidHandler = new FluidTankList(true, this.getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.exportFluidHandler = new FluidTankList(true, this.getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(this.getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.energyContainer = new EnergyContainerList(this.getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.tier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.energyDrain = (long) (Math.pow(4, tier) * 8);
        int startTier = this.tier - GTValues.ZPM;
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
                .where('S', this.selfPredicate())
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
        Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.getFrontFacing(), this.isActive);
    }

    protected void setActive(boolean active) {
        this.isActive = active;
        if (!this.getWorld().isRemote) {
            this.writeCustomData(1, buf -> buf.writeBoolean(active));
            this.markDirty();
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
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("Progress", this.progress);
        data.setLong("Temperature", this.temperature);
        data.setDouble("CurrentDrillingFluid", this.currentDrillingFluid);
        data.setBoolean("Overheat", this.overheat);
        data.setBoolean("Active", this.isActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.progress = data.getInteger("Progress");
        this.temperature = data.getLong("Temperature");
        this.currentDrillingFluid = data.getDouble("CurrentDrillingFluid");
        this.overheat = data.getBoolean("Overheat");
        this.isActive = data.getBoolean("Active");
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_HEAT)
            return TJCapabilities.CAPABILITY_HEAT.cast(this);
        if (capability == GregtechTileCapabilities.CAPABILITY_WORKABLE)
            return GregtechTileCapabilities.CAPABILITY_WORKABLE.cast(this);
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public long heat() {
        return this.temperature;
    }

    @Override
    public long maxHeat() {
        return this.maxTemperature;
    }

    private static List<ItemStack> getOres() {
        return ORES_3;
    }

    @Override
    public boolean isActive() {
        return this.isActive;
    }

    @Override
    public int getProgress() {
        return this.progress;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProgress;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return this.fluidInputs;
    }

    @Override
    public List<FluidStack> getFluidOutputs() {
        return this.fluidOutputs;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return this.oreOutputs;
    }
}
