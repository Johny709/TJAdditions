package com.johny.tj.machines.multi.steam;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IFuelInfo;
import gregtech.api.capability.IFuelable;
import gregtech.api.capability.impl.*;
import gregtech.api.capability.tool.ISoftHammerItem;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipes.FuelRecipe;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockFireboxCasing;
import gregtech.common.metatileentities.multi.MetaTileEntityLargeBoiler;
import gregtech.common.tools.DamageValues;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withHoverTextTranslate;

public class MetaTileEntityMegaBoiler extends TJMultiblockDisplayBase implements IFuelable {

    private static final int CONSUMPTION_MULTIPLIER = 100;
    private static final int BOILING_TEMPERATURE = 100;
    private final int MAX_PROCESSES;

    public final MetaTileEntityLargeBoiler.BoilerType boilerType;

    private int currentTemperature;
    private int fuelBurnTicksLeft;
    private int throttlePercentage = 100;
    private boolean isActive;
    private boolean wasActiveAndNeedsUpdate;
    private boolean hasNoWater;
    private int lastTickSteamOutput;
    String currentItemProcessed = "tj.multiblock.large_boiler.insert_burnable";
    int currentItemsEngaged = 0;

    private FluidTankList fluidImportInventory;
    private ItemHandlerList itemImportInventory;
    private FluidTankList steamOutputTank;

    public MetaTileEntityMegaBoiler(ResourceLocation metaTileEntityId, MetaTileEntityLargeBoiler.BoilerType boilerType, int maxProcess) {
        super(metaTileEntityId);
        this.boilerType = boilerType;
        this.MAX_PROCESSES = maxProcess;
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMegaBoiler(metaTileEntityId, boilerType, MAX_PROCESSES);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.fluidImportInventory = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.itemImportInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.steamOutputTank = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.fluidImportInventory = new FluidTankList(true);
        this.itemImportInventory = new ItemHandlerList(Collections.emptyList());
        this.steamOutputTank = new FluidTankList(true);
        this.currentTemperature = 0; //reset temperature
        this.fuelBurnTicksLeft = 0;
        this.hasNoWater = false;
        this.isActive = false;
        this.throttlePercentage = 100;
        replaceFireboxAsActive(false);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (!getWorld().isRemote && isStructureFormed()) {
            replaceFireboxAsActive(false);
        }
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.temperature", currentTemperature, boilerType.maxTemperature));
            textList.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.steam_output", lastTickSteamOutput, boilerType.baseSteamOutput));

            ITextComponent heatEffText = new TextComponentTranslation("gregtech.multiblock.large_boiler.heat_efficiency", (int) (getHeatEfficiencyMultiplier() * 100));
            withHoverTextTranslate(heatEffText, "gregtech.multiblock.large_boiler.heat_efficiency.tooltip");
            textList.add(heatEffText);

            ITextComponent throttleText = new TextComponentTranslation("gregtech.multiblock.large_boiler.throttle", throttlePercentage, (int)(getThrottleEfficiency() * 100));
            withHoverTextTranslate(throttleText, "gregtech.multiblock.large_boiler.throttle.tooltip");
            textList.add(throttleText);

            ITextComponent buttonText = new TextComponentTranslation("gregtech.multiblock.large_boiler.throttle_modify");
            buttonText.appendText(" ");
            buttonText.appendSibling(withButton(new TextComponentString("[-]"), "sub"));
            buttonText.appendText(" ");
            buttonText.appendSibling(withButton(new TextComponentString("[+]"), "add"));
            textList.add(buttonText);
            ITextComponent itemDisplayText;
            if (!isActive) {
                itemDisplayText = new TextComponentTranslation(currentItemProcessed + ".name").appendSibling(new TextComponentString(" (" + currentItemsEngaged + "/" + MAX_PROCESSES + ")"));
                withHoverTextTranslate(itemDisplayText, "tj.multiblock.large_boiler.items_process_hover");
            }
            else {
                itemDisplayText = new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN));
            }
            textList.add(itemDisplayText);
            ITextComponent itemClear = new TextComponentTranslation("tj.multiblock.large_boiler.clear_item");
            itemClear.appendText(" ");
            itemClear.appendSibling(withButton(new TextComponentString("[O]"), "clear"));
            textList.add(itemClear);

        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        super.handleDisplayClick(componentData, clickData);
        //int modifier = componentData.equals("add") ? 1 : -1;
        int modifier = 0;
        if (componentData.equals("add")) {
            modifier = 1;
        }
        else if (componentData.equals("sub")) {
            modifier = -1;
        }
        else {
            currentItemsEngaged = 0;
            currentItemProcessed = "tj.multiblock.large_boiler.insert_burnable";
        }

        int result = (clickData.isShiftClick ? 1 : 5) * modifier;
        this.throttlePercentage = MathHelper.clamp(throttlePercentage + result, 20, 100);
    }

    private double getHeatEfficiencyMultiplier() {
        double temperature = currentTemperature / (boilerType.maxTemperature * 1.0);
        return 1.0 + Math.round(boilerType.temperatureEffBuff * temperature) / 100.0;
    }

    @Override
    protected void updateFormedValid() {
        if (fuelBurnTicksLeft > 0 && currentTemperature < boilerType.maxTemperature) {
            --this.fuelBurnTicksLeft;
            if (getOffsetTimer() % 20 == 0) {
                this.currentTemperature++;
            }
            if (fuelBurnTicksLeft == 0) {
                this.wasActiveAndNeedsUpdate = true;
            }
        } else if (currentTemperature > 0 && getOffsetTimer() % 20 == 0) {
            --this.currentTemperature;
        }

        this.lastTickSteamOutput = 0;
        if (currentTemperature >= BOILING_TEMPERATURE) {
            boolean doWaterDrain = getOffsetTimer() % 20 == 0;
            FluidStack drainedWater = fluidImportInventory.drain(ModHandler.getWater(1), doWaterDrain);
            if (drainedWater == null || drainedWater.amount == 0) {
                drainedWater = fluidImportInventory.drain(ModHandler.getDistilledWater(1), doWaterDrain);
            }
            if (drainedWater != null && drainedWater.amount > 0) {
                if (currentTemperature > BOILING_TEMPERATURE && hasNoWater) {
                    float explosionPower = currentTemperature / (float)BOILING_TEMPERATURE * 2.0f;
                    getWorld().setBlockToAir(getPos());
                    getWorld().createExplosion(null, getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
                            explosionPower, true);
                }
                this.hasNoWater = false;
                if (currentTemperature >= BOILING_TEMPERATURE) {
                    double outputMultiplier = currentTemperature / (boilerType.maxTemperature * 1.0) * getThrottleMultiplier() * getThrottleEfficiency();
                    int steamOutput = (int) (boilerType.baseSteamOutput * MAX_PROCESSES * outputMultiplier);
                    FluidStack steamStack = ModHandler.getSteam(steamOutput);
                    steamOutputTank.fill(steamStack, true);
                    this.lastTickSteamOutput = steamOutput;
                }
            } else {
                this.hasNoWater = true;
            }
        } else {
            this.hasNoWater = false;
        }

        if (fuelBurnTicksLeft == 0) {
            double heatEfficiency = getHeatEfficiencyMultiplier();
            int fuelMaxBurnTime = (int) Math.round(setupRecipeAndConsumeInputs() * heatEfficiency);
            if (fuelMaxBurnTime > 0) {
                this.fuelBurnTicksLeft = fuelMaxBurnTime;
                if (wasActiveAndNeedsUpdate) {
                    this.wasActiveAndNeedsUpdate = false;
                } else setActive(true);
                markDirty();
            }
        }

        if (wasActiveAndNeedsUpdate) {
            this.wasActiveAndNeedsUpdate = false;
            setActive(false);
        }
    }

    private int setupRecipeAndConsumeInputs() {
        for (IFluidTank fluidTank : fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);
            if (fuelStack == null || ModHandler.isWater(fuelStack))
                continue; //ignore empty tanks and water
            FuelRecipe dieselRecipe = RecipeMaps.DIESEL_GENERATOR_FUELS.findRecipe(GTValues.V[9], fuelStack);
            if (dieselRecipe != null) {
                int fuelAmountToConsume = (int) Math.ceil(dieselRecipe.getRecipeFluid().amount * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                if (fuelStack.amount >= fuelAmountToConsume) {
                    fluidTank.drain(fuelAmountToConsume, true);
                    long recipeVoltage = FuelRecipeLogic.getTieredVoltage(dieselRecipe.getMinVoltage());
                    int voltageMultiplier = (int) Math.max(1L, recipeVoltage / GTValues.V[GTValues.LV]);
                    return (int) Math.ceil(dieselRecipe.getDuration() * CONSUMPTION_MULTIPLIER * MAX_PROCESSES / 2.0 * voltageMultiplier * getThrottleMultiplier());
                } else continue;
            }
            FuelRecipe denseFuelRecipe = RecipeMaps.SEMI_FLUID_GENERATOR_FUELS.findRecipe(GTValues.V[9], fuelStack);
            if (denseFuelRecipe != null) {
                int fuelAmountToConsume = (int) Math.ceil(denseFuelRecipe.getRecipeFluid().amount * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                if (fuelStack.amount >= fuelAmountToConsume) {
                    fluidTank.drain(fuelAmountToConsume, true);
                    long recipeVoltage = FuelRecipeLogic.getTieredVoltage(denseFuelRecipe.getMinVoltage());
                    int voltageMultiplier = (int) Math.max(1L, recipeVoltage / GTValues.V[GTValues.LV]);
                    return (int) Math.ceil(denseFuelRecipe.getDuration() * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * 2 * voltageMultiplier * getThrottleMultiplier());
                }
            }
        }
        for (int slotIndex = 0; slotIndex < itemImportInventory.getSlots(); slotIndex++) {
            ItemStack itemStack = itemImportInventory.getStackInSlot(slotIndex);
            int fuelBurnValue = (int) Math.ceil(TileEntityFurnace.getItemBurnTime(itemStack) / (50.0 * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier()));
            if (fuelBurnValue > 0 ) {
                if (currentItemProcessed.equals("tj.multiblock.large_boiler.insert_burnable")) {
                    currentItemProcessed = itemStack.getTranslationKey();
                }
                if (itemStack.getTranslationKey().equals(currentItemProcessed)) {
                    currentItemsEngaged += itemStack.getCount();
                    if (currentItemsEngaged >= MAX_PROCESSES) {
                        int takeAmountFromStack = Math.abs((currentItemsEngaged - MAX_PROCESSES) - itemStack.getCount());
                        itemStack.shrink(takeAmountFromStack);
                        itemImportInventory.setStackInSlot(slotIndex, itemStack);
                    } else {
                        ItemStack containerItem = itemStack.getItem().getContainerItem(itemStack);
                        itemImportInventory.setStackInSlot(slotIndex, containerItem);
                    }
                }
                if (currentItemsEngaged < MAX_PROCESSES)
                    continue;

                currentItemsEngaged = 0;
                currentItemProcessed = "tj.multiblock.large_boiler.insert_burnable";
                return fuelBurnValue;
            }
        }
        return 0;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("CurrentTemperature", currentTemperature);
        data.setInteger("FuelBurnTicksLeft", fuelBurnTicksLeft);
        data.setBoolean("HasNoWater", hasNoWater);
        data.setInteger("ThrottlePercentage", throttlePercentage);
        data.setString("CurrentItemProcessed", currentItemProcessed);
        data.setInteger("CurrentItemsEngaged", currentItemsEngaged);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.currentTemperature = data.getInteger("CurrentTemperature");
        this.fuelBurnTicksLeft = data.getInteger("FuelBurnTicksLeft");
        this.hasNoWater = data.getBoolean("HasNoWater");
        if (data.hasKey("CurrentItemProcessed")) {
            this.currentItemProcessed = data.getString("CurrentItemProcessed");
        }
        this.currentItemsEngaged = data.getInteger("CurrentItemsEngaged");
        if(data.hasKey("ThrottlePercentage")) {
            this.throttlePercentage = data.getInteger("ThrottlePercentage");
        }
        this.isActive = fuelBurnTicksLeft > 0;
    }

    private void setActive(boolean active) {
        this.isActive = active;
        if (!getWorld().isRemote) {
            if (isStructureFormed()) {
                replaceFireboxAsActive(active);
            }
            writeCustomData(100, buf -> buf.writeBoolean(isActive));
            markDirty();
        }
    }

    private double getThrottleMultiplier() {
        return throttlePercentage / 100.0;
    }

    private double getThrottleEfficiency() {
        return MathHelper.clamp(1.0 + 0.3*Math.log(getThrottleMultiplier()), 0.4, 1.0);
    }

    private void replaceFireboxAsActive(boolean isActive) {
        BlockPos centerPos = getPos().offset(getFrontFacing().getOpposite()).down();
        for (int x = -13; x <= 13; x++) {
            for (int z = -13; z <= 13; z++) {
                BlockPos blockPos = centerPos.add(x, -1, z);
                IBlockState blockState = getWorld().getBlockState(blockPos);
                if (blockState.getBlock() instanceof BlockFireboxCasing) {
                    blockState = blockState.withProperty(BlockFireboxCasing.ACTIVE, isActive);
                    getWorld().setBlockState(blockPos, blockState);
                }
            }
        }
    }

    @Override
    public int getLightValueForPart(IMultiblockPart sourcePart) {
        return sourcePart == null ? 0 : (isActive ? 15 : 0);
    }

    public int getMAX_PROCESSES() {
        return MAX_PROCESSES;
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
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 100) {
            this.isActive = buf.readBoolean();
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return boilerType == null ? null : FactoryBlockPattern.start()
                .aisle("XXXXXXXXXXXXXXX", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CPPPPPPPPPPPPPC", "CCCCCCCCCCCCCCC")
                .aisle("XXXXXXXXXXXXXXX", "CCCCCCCCCCCCCCC", "CCCCCCCSCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC", "CCCCCCCCCCCCCCC")
                .setAmountAtLeast('X', 200)
                .setAmountAtLeast('C', 200)
                .where('S', selfPredicate())
                .where('P', statePredicate(boilerType.pipeState))
                .where('X', state -> statePredicate(GTUtility.getAllPropertyValues(boilerType.fireboxState, BlockFireboxCasing.ACTIVE))
                        .or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, GregicAdditionsCapabilities.MAINTENANCE_HATCH)).test(state))
                .where('C', statePredicate(boilerType.casingState).or(abilityPartPredicate(
                        MultiblockAbility.EXPORT_FLUIDS)))
                .build();
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        this.getFrontOverlay().render(renderState, translation, pipeline, getFrontFacing(), isActive);
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return boilerType.frontOverlay;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.1", RecipeMaps.DIESEL_GENERATOR_FUELS.getLocalizedName() + ", " + RecipeMaps.SEMI_FLUID_GENERATOR_FUELS.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.universal.tooltip.2", MAX_PROCESSES));
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        //noinspection SuspiciousMethodCalls
        int importFluidsSize = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();
        //noinspection SuspiciousMethodCalls
        return importFluidsSize >= 1 && (importFluidsSize >= 2 ||
                abilities.containsKey(MultiblockAbility.IMPORT_ITEMS)) &&
                abilities.containsKey(MultiblockAbility.EXPORT_FLUIDS);
    }

    private boolean isFireboxPart(IMultiblockPart sourcePart) {
        return isStructureFormed() && (((MetaTileEntity) sourcePart).getPos().getY() < getPos().getY());
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        if (sourcePart != null && isFireboxPart(sourcePart)) {
            return isActive ? boilerType.firefoxActiveRenderer : boilerType.fireboxIdleRenderer;
        }
        return boilerType.solidCasingRenderer;
    }

    @Override
    public boolean shouldRenderOverlay(IMultiblockPart sourcePart) {
        return sourcePart == null || !isFireboxPart(sourcePart);
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        ItemStack itemStack = playerIn.getHeldItem(hand);
        if(!itemStack.isEmpty() && itemStack.hasCapability(GregtechCapabilities.CAPABILITY_MALLET, null)) {
            ISoftHammerItem softHammerItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_MALLET, null);

            if (getWorld().isRemote) {
                return true;
            }
            if(!softHammerItem.damageItem(DamageValues.DAMAGE_FOR_SOFT_HAMMER, false)) {
                return false;
            }
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        T result = super.getCapability(capability, side);
        if (result != null)
            return result;
        if (capability == GregtechCapabilities.CAPABILITY_FUELABLE) {
            return GregtechCapabilities.CAPABILITY_FUELABLE.cast(this);
        }
        return null;
    }

    @Override
    public Collection<IFuelInfo> getFuels() {
        if (!isStructureFormed())
            return Collections.emptySet();
        final LinkedHashMap<Object, IFuelInfo> fuels = new LinkedHashMap<Object, IFuelInfo>();
        int fluidCapacity = 0; // fluid capacity is all non water tanks
        for (IFluidTank fluidTank : fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);
            if (!ModHandler.isWater(fuelStack))
                fluidCapacity += fluidTank.getCapacity();
        }
        for (IFluidTank fluidTank : fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);
            if (fuelStack == null || ModHandler.isWater(fuelStack))
                continue;
            FuelRecipe dieselRecipe = RecipeMaps.DIESEL_GENERATOR_FUELS.findRecipe(GTValues.V[9], fuelStack);
            if (dieselRecipe != null) {
                long recipeVoltage = FuelRecipeLogic.getTieredVoltage(dieselRecipe.getMinVoltage());
                int voltageMultiplier = (int) Math.max(1L, recipeVoltage / GTValues.V[GTValues.LV]);
                int burnTime = (int) Math.ceil(dieselRecipe.getDuration() * CONSUMPTION_MULTIPLIER * MAX_PROCESSES / 2.0 * voltageMultiplier * getThrottleMultiplier());
                int fuelAmountToConsume = (int) Math.ceil(dieselRecipe.getRecipeFluid().amount * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                final long fuelBurnTime = (fuelStack.amount * burnTime) / fuelAmountToConsume;
                FluidFuelInfo fluidFuelInfo = (FluidFuelInfo) fuels.get(fuelStack.getUnlocalizedName());
                if (fluidFuelInfo == null) {
                    fluidFuelInfo = new FluidFuelInfo(fuelStack, fuelStack.amount, fluidCapacity, fuelAmountToConsume, fuelBurnTime);
                    fuels.put(fuelStack.getUnlocalizedName(), fluidFuelInfo);
                }
                else {
                    fluidFuelInfo.addFuelRemaining(fuelStack.amount);
                    fluidFuelInfo.addFuelBurnTime(fuelBurnTime);
                }
            }
            FuelRecipe denseFuelRecipe = RecipeMaps.SEMI_FLUID_GENERATOR_FUELS.findRecipe(GTValues.V[9], fuelStack);
            if (denseFuelRecipe != null) {
                long recipeVoltage = FuelRecipeLogic.getTieredVoltage(denseFuelRecipe.getMinVoltage());
                int voltageMultiplier = (int) Math.max(1L, recipeVoltage / GTValues.V[GTValues.LV]);
                int burnTime = (int) Math.ceil(denseFuelRecipe.getDuration() * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * 2 * voltageMultiplier * getThrottleMultiplier());
                int fuelAmountToConsume = (int) Math.ceil(denseFuelRecipe.getRecipeFluid().amount * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                final long fuelBurnTime = (fuelStack.amount * burnTime) / fuelAmountToConsume;
                FluidFuelInfo fluidFuelInfo = (FluidFuelInfo) fuels.get(fuelStack.getUnlocalizedName());
                if (fluidFuelInfo == null) {
                    fluidFuelInfo = new FluidFuelInfo(fuelStack, fuelStack.amount, fluidCapacity, fuelAmountToConsume, fuelBurnTime);
                    fuels.put(fuelStack.getUnlocalizedName(), fluidFuelInfo);
                }
                else {
                    fluidFuelInfo.addFuelRemaining(fuelStack.amount);
                    fluidFuelInfo.addFuelBurnTime(fuelBurnTime);
                }
            }
        }
        int itemCapacity = 0; // item capacity is all slots
        for (int slotIndex = 0; slotIndex < itemImportInventory.getSlots(); slotIndex++) {
            itemCapacity += itemImportInventory.getSlotLimit(slotIndex);
        }
        for (int slotIndex = 0; slotIndex < itemImportInventory.getSlots(); slotIndex++) {
            ItemStack itemStack = itemImportInventory.getStackInSlot(slotIndex);
            final long burnTime = (int) Math.ceil(TileEntityFurnace.getItemBurnTime(itemStack) / (50.0 * this.boilerType.fuelConsumptionMultiplier * MAX_PROCESSES * getThrottleMultiplier()));
            if (burnTime > 0) {
                ItemFuelInfo itemFuelInfo = (ItemFuelInfo) fuels.get(itemStack.getTranslationKey());
                if (itemFuelInfo == null) {
                    itemFuelInfo = new ItemFuelInfo(itemStack, itemStack.getCount(), itemCapacity, 1, itemStack.getCount() * burnTime);
                    fuels.put(itemStack.getDisplayName(), itemFuelInfo);
                }
                else {
                    itemFuelInfo.addFuelRemaining(itemStack.getCount());
                    itemFuelInfo.addFuelBurnTime(itemStack.getCount() * burnTime);
                }
            }
        }
        return fuels.values();
    }

}
