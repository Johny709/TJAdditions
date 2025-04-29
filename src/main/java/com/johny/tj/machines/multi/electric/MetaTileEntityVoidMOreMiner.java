package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import com.johny.tj.blocks.BlockSolidCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IHeatInfo;
import com.johny.tj.capability.TJCapabilities;
import gregicadditions.GAMaterials;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.johny.tj.textures.TJTextures.HEAVY_QUARK_DEGENERATE_MATTER;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.recipes.categories.handlers.VoidMinerHandler.ORES_3;

public class MetaTileEntityVoidMOreMiner extends TJMultiblockDisplayBase implements IHeatInfo {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.EXPORT_ITEMS, MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private static final int CONSUME_START = 100;
    private IEnergyContainer energyContainer;
    private IMultipleTankHandler importFluidHandler;
    private IMultipleTankHandler exportFluidHandler;
    protected IItemHandlerModifiable outputInventory;
    private boolean isActive = false;
    private boolean overheat = false;
    private boolean usingPyrotheum = true;
    private long maxTemperature;
    private long temperature = 0;
    private double currentDrillingFluid = CONSUME_START;
    private long energyDrain;
    private int tier;

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
    protected void addDisplayText(List<ITextComponent> textList) {
        if (this.isStructureFormed() && !this.hasProblems()) {
            if (energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
                long maxVoltage = energyContainer.getInputVoltage();
                String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
                textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
            }
            textList.add(new TextComponentTranslation("gregtech.multiblock.universal.energy_used", energyDrain));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.temperature", temperature, maxTemperature));
            textList.add(new TextComponentTranslation("gregtech.multiblock.universal.drilling_fluid_amount", (int) currentDrillingFluid));
            if (overheat) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.universal.overheat").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }

        super.addDisplayText(textList);
    }

    public boolean drainEnergy() {
        if (energyContainer.getEnergyStored() >= energyDrain) {
            energyContainer.removeEnergy(energyDrain);
            return true;
        }
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (getNumProblems() < 6 && tier >= GTValues.UV) {
            if (!isWorkingEnabled || overheat || !drainEnergy()) {
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

            if (getOffsetTimer() % 20 == 0) {
                FluidStack pyrotheumFluid = GAMaterials.Pyrotheum.getFluid((int) currentDrillingFluid);
                FluidStack cryotheumFluid = GAMaterials.Cryotheum.getFluid((int) currentDrillingFluid);
                FluidStack drillingMudFluid = DrillingMud.getFluid((int) currentDrillingFluid);
                FluidStack usedDrillingMudFluid = UsedDrillingMud.getFluid((int) currentDrillingFluid);
                FluidStack canDrainPyrotheum = importFluidHandler.drain(pyrotheumFluid, false);
                FluidStack canDrainCryotheum = importFluidHandler.drain(cryotheumFluid, false);
                FluidStack canDrainDrillingMud = importFluidHandler.drain(drillingMudFluid, false);
                int canFillUsedDrillingMud = exportFluidHandler.fill(usedDrillingMudFluid, false);
                boolean hasConsume = false;
                //consume fluid
                if (canDrainDrillingMud != null && canDrainDrillingMud.amount == (int) currentDrillingFluid &&
                        canFillUsedDrillingMud != 0 && canFillUsedDrillingMud == (int) currentDrillingFluid) {
                    importFluidHandler.drain(drillingMudFluid, true);
                    exportFluidHandler.fill(usedDrillingMudFluid, true);
                } else {
                    setActive(false);
                    return;
                }

                if (!isActive)
                    setActive(true);

                calculateMaintenance(20);

                if ((usingPyrotheum && canDrainPyrotheum != null && canDrainPyrotheum.amount == (int) currentDrillingFluid) && (temperature > 0 && canDrainCryotheum != null && canDrainCryotheum.amount == (int) currentDrillingFluid)) {
                    importFluidHandler.drain(cryotheumFluid, true);
                    importFluidHandler.drain(pyrotheumFluid, true);
                    hasConsume = true;

                } else if ((usingPyrotheum && canDrainPyrotheum != null && canDrainPyrotheum.amount == (int) currentDrillingFluid)  && (canDrainCryotheum == null)) {
                    importFluidHandler.drain(pyrotheumFluid, true);
                    temperature += (int)(currentDrillingFluid / 100.0);
                    currentDrillingFluid = currentDrillingFluid * 1.02;
                    hasConsume = true;
                } else if ((temperature > 0 && canDrainCryotheum != null && canDrainCryotheum.amount == (int) currentDrillingFluid) && canDrainPyrotheum == null) {
                    importFluidHandler.drain(cryotheumFluid, true);
                    currentDrillingFluid = currentDrillingFluid / 1.02;
                    temperature -= (int) (currentDrillingFluid / 100.0);
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
                usingPyrotheum = !usingPyrotheum;

                currentDrillingFluid += this.getNumProblems();

                //mine

                long nbOres = temperature / 1000;

                if (nbOres == 0 || !hasConsume) {
                    return;
                }

                List<ItemStack> ores = getOres();
                Collections.shuffle(ores);
                ores.stream().limit(10).peek(itemStack -> itemStack.setCount(getWorld().rand.nextInt((int) (nbOres * nbOres)) + 1)).forEach(itemStack ->
                        addItemsToItemHandler(outputInventory, false, Collections.singletonList(itemStack)));
            }
        }
    }

    private static List<ItemStack> getOres() {
        return ORES_3;
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
        data.setLong("Temperature", temperature);
        data.setDouble("CurrentDrillingFluid", currentDrillingFluid);
        data.setBoolean("Overheat", overheat);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        temperature = data.getLong("Temperature");
        currentDrillingFluid = data.getDouble("CurrentDrillingFluid");
        overheat = data.getBoolean("Overheat");
    }

    @Override
    public long heat() {
        return temperature;
    }

    @Override
    public long maxHeat() {
        return maxTemperature;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == TJCapabilities.CAPABILITY_HEAT)
            return TJCapabilities.CAPABILITY_HEAT.cast(this);
        return super.getCapability(capability, side);
    }
}
