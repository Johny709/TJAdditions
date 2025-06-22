package com.johny.tj.machines.multi.steam;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.builder.multicontrollers.MultiblockDisplayBuilder;
import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import com.johny.tj.capability.IGeneratorInfo;
import com.johny.tj.capability.IHeatInfo;
import com.johny.tj.capability.IItemFluidHandlerInfo;
import com.johny.tj.multiblockpart.TJMultiblockAbility;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IFuelInfo;
import gregtech.api.capability.IFuelable;
import gregtech.api.capability.IWorkable;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.johny.tj.capability.TJCapabilities.*;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_FUELABLE;
import static gregtech.api.capability.GregtechTileCapabilities.CAPABILITY_WORKABLE;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withHoverTextTranslate;
import static gregtech.api.unification.material.Materials.Steam;
import static gregtech.api.unification.material.Materials.Water;

public class MetaTileEntityMegaBoiler extends TJMultiblockDisplayBase implements IWorkable, IFuelable, IHeatInfo, IGeneratorInfo, IItemFluidHandlerInfo {

    private static final int CONSUMPTION_MULTIPLIER = 100;
    private static final int BOILING_TEMPERATURE = 100;
    private final int MAX_PROCESSES;

    public final MetaTileEntityLargeBoiler.BoilerType boilerType;
    private static final MultiblockAbility<?>[] OUTPUT_ABILITIES = {MultiblockAbility.EXPORT_FLUIDS, TJMultiblockAbility.STEAM_OUTPUT};

    private int currentTemperature;
    private int progress;
    private int maxProgress;
    private int throttlePercentage = 100;
    private boolean isActive;
    private boolean hasNoWater;
    private int waterConsumption;
    private int steamProduction;
    private String currentItemProcessed = "tj.multiblock.large_boiler.insert_burnable";
    private final List<ItemStack> currentItem = new ArrayList<>();
    private final List<FluidStack> currentFluid = new ArrayList<>();
    int currentItemsEngaged = 0;

    private FluidTankList fluidImportInventory;
    private ItemHandlerList itemImportInventory;
    private FluidTankList steamOutputTank;

    public MetaTileEntityMegaBoiler(ResourceLocation metaTileEntityId, MetaTileEntityLargeBoiler.BoilerType boilerType, int maxProcess) {
        super(metaTileEntityId);
        this.boilerType = boilerType;
        this.MAX_PROCESSES = maxProcess;
        this.reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityMegaBoiler(this.metaTileEntityId, this.boilerType, MAX_PROCESSES);
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
        boolean hasInputFluid = abilities.containsKey(MultiblockAbility.IMPORT_FLUIDS);
        boolean hasSteamOutput = abilities.containsKey(TJMultiblockAbility.STEAM_OUTPUT);
        boolean hasOutputFluid = abilities.containsKey(MultiblockAbility.EXPORT_FLUIDS);
        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();

        return maintenanceCount == 1 && hasInputFluid && (hasOutputFluid || hasSteamOutput);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        List<IFluidTank> fluidTanks = new ArrayList<>();
        fluidTanks.addAll(this.getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        fluidTanks.addAll(this.getAbilities(TJMultiblockAbility.STEAM_OUTPUT));

        this.fluidImportInventory = new FluidTankList(true, this.getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.itemImportInventory = new ItemHandlerList(this.getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.steamOutputTank = new FluidTankList(true, fluidTanks);
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.fluidImportInventory = new FluidTankList(true);
        this.itemImportInventory = new ItemHandlerList(Collections.emptyList());
        this.steamOutputTank = new FluidTankList(true);
        this.currentTemperature = 0; //reset temperature
        this.progress = 0;
        this.hasNoWater = false;
        this.isActive = false;
        this.throttlePercentage = 100;
        this.replaceFireboxAsActive(false);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (!this.getWorld().isRemote && this.isStructureFormed()) {
            this.replaceFireboxAsActive(false);
        }
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed()) {
            MultiblockDisplayBuilder.start(textList)
                    .temperature(this.currentTemperature, this.boilerType.maxTemperature)
                    .fluidInput(this.hasEnoughWater(this.waterConsumption), Water.getFluid(this.waterConsumption))
                    .custom(text -> {
                        text.add(new TextComponentTranslation("gregtech.multiblock.large_boiler.steam_output", this.steamProduction, this.boilerType.baseSteamOutput));

                        ITextComponent heatEffText = new TextComponentTranslation("gregtech.multiblock.large_boiler.heat_efficiency", (int) (this.getHeatEfficiencyMultiplier() * 100));
                        withHoverTextTranslate(heatEffText, "gregtech.multiblock.large_boiler.heat_efficiency.tooltip");
                        text.add(heatEffText);

                        ITextComponent throttleText = new TextComponentTranslation("gregtech.multiblock.large_boiler.throttle", this.throttlePercentage, (int) (this.getThrottleEfficiency() * 100));
                        withHoverTextTranslate(throttleText, "gregtech.multiblock.large_boiler.throttle.tooltip");
                        text.add(throttleText);

                        ITextComponent buttonText = new TextComponentTranslation("gregtech.multiblock.large_boiler.throttle_modify");
                        buttonText.appendText(" ");
                        buttonText.appendSibling(withButton(new TextComponentString("[-]"), "sub"));
                        buttonText.appendText(" ");
                        buttonText.appendSibling(withButton(new TextComponentString("[+]"), "add"));
                        text.add(buttonText);

                        if (!this.isActive) {
                            ITextComponent itemDisplayText = new TextComponentTranslation(currentItemProcessed + ".name").appendSibling(new TextComponentString(" (" + currentItemsEngaged + "/" + MAX_PROCESSES + ")"));
                            withHoverTextTranslate(itemDisplayText, "tj.multiblock.large_boiler.items_process_hover");
                            text.add(itemDisplayText);
                        }

                        ITextComponent itemClear = new TextComponentTranslation("tj.multiblock.large_boiler.clear_item");
                        itemClear.appendText(" ");
                        itemClear.appendSibling(withButton(new TextComponentString("[O]"), "clear"));
                        textList.add(itemClear);
                    })
                    .isWorking(this.isWorkingEnabled, this.isActive, this.progress, this.maxProgress);
        }
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        super.handleDisplayClick(componentData, clickData);
        //int modifier = componentData.equals("add") ? 1 : -1;
        int modifier = 0;
        if (componentData.equals("add")) {
            modifier = 1;
        } else if (componentData.equals("sub")) {
            modifier = -1;
        } else {
            this.currentItemsEngaged = 0;
            this.currentItemProcessed = "tj.multiblock.large_boiler.insert_burnable";
        }

        int result = (clickData.isShiftClick ? 1 : 5) * modifier;
        this.throttlePercentage = MathHelper.clamp(this.throttlePercentage + result, 20, 100);
    }

    private double getHeatEfficiencyMultiplier() {
        double temperature = this.currentTemperature / (this.boilerType.maxTemperature * 1.0);
        return 1.0 + Math.round(this.boilerType.temperatureEffBuff * temperature) / 100.0;
    }

    @Override
    protected void updateFormedValid() {
        if (this.getOffsetTimer() < 40)
            return;
        if (this.getOffsetTimer() % 20 == 0) {
            double outputMultiplier = this.currentTemperature / (this.boilerType.maxTemperature * 1.0) * this.getThrottleMultiplier() * this.getThrottleEfficiency();
            this.steamProduction = (int) (this.boilerType.baseSteamOutput * MAX_PROCESSES * outputMultiplier);

            if (!this.isWorkingEnabled || !this.isActive || this.getNumProblems() >= 6) {
                if (this.isActive)
                    this.setActive(false);
                if (this.currentTemperature > 0)
                    this.currentTemperature--;
            } else {
                if (this.currentTemperature < this.boilerType.maxTemperature)
                    this.currentTemperature++;
            }
        }

        if (this.progress > 0 && !this.isActive)
            this.setActive(true);

        if (this.progress >= this.maxProgress) {
            this.calculateMaintenance(this.maxProgress);
            this.progress = 0;
            this.currentItem.clear();
            this.currentFluid.clear();
            this.setActive(false);
        }

        if (this.progress <= 0) {
            double heatEfficiency = this.getHeatEfficiencyMultiplier();
            int fuelMaxBurnTime = (int) Math.round(this.setupRecipeAndConsumeInputs() * heatEfficiency);
            if (fuelMaxBurnTime > 0) {
                this.maxProgress = fuelMaxBurnTime;
                this.progress = 1;
                this.setActive(true);
            }
        } else {
            this.progress++;
        }
        if (!this.canGenerateSteam()) {
            this.hasNoWater = false;
            return;
        }
        if (this.progress > 0) {
            this.waterConsumption = Math.round((float) this.steamProduction / 160);
            boolean hasEnoughWater = this.hasEnoughWater(this.waterConsumption);
            if (hasEnoughWater && this.hasNoWater) {
                this.getWorld().setBlockToAir(this.getPos());
                this.getWorld().createExplosion(null,
                        this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5,
                        2.0f, true);
            } else {
                if (hasEnoughWater) {
                    this.steamOutputTank.fill(Steam.getFluid(this.steamProduction), true);
                    this.fluidImportInventory.drain(Water.getFluid(this.waterConsumption), true);
                } else {
                    this.hasNoWater = true;
                }
            }
        }
    }

    private boolean hasEnoughWater(int amount) {
        FluidStack waterStack = this.fluidImportInventory.drain(Water.getFluid(amount), false);
        return waterStack != null && waterStack.amount == amount;
    }

    private boolean canGenerateSteam() {
        return this.currentTemperature >= BOILING_TEMPERATURE;
    }

    private int setupRecipeAndConsumeInputs() {
        for (IFluidTank fluidTank : this.fluidImportInventory.getFluidTanks()) {
            FluidStack fuelStack = fluidTank.drain(Integer.MAX_VALUE, false);
            if (fuelStack == null || ModHandler.isWater(fuelStack))
                continue; //ignore empty tanks and water
            FuelRecipe dieselRecipe = RecipeMaps.DIESEL_GENERATOR_FUELS.findRecipe(GTValues.V[9], fuelStack);
            if (dieselRecipe != null) {
                int fuelAmountToConsume = (int) Math.ceil(dieselRecipe.getRecipeFluid().amount * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                if (fuelStack.amount >= fuelAmountToConsume) {
                    this.currentFluid.add(fluidTank.drain(fuelAmountToConsume, true));
                    long recipeVoltage = FuelRecipeLogic.getTieredVoltage(dieselRecipe.getMinVoltage());
                    int voltageMultiplier = (int) Math.max(1L, recipeVoltage / GTValues.V[GTValues.LV]);
                    return (int) Math.ceil(dieselRecipe.getDuration() * CONSUMPTION_MULTIPLIER / 2.0 * voltageMultiplier * this.getThrottleMultiplier());
                } else continue;
            }
            FuelRecipe denseFuelRecipe = RecipeMaps.SEMI_FLUID_GENERATOR_FUELS.findRecipe(GTValues.V[9], fuelStack);
            if (denseFuelRecipe != null) {
                int fuelAmountToConsume = (int) Math.ceil(denseFuelRecipe.getRecipeFluid().amount * CONSUMPTION_MULTIPLIER * MAX_PROCESSES * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                if (fuelStack.amount >= fuelAmountToConsume) {
                    this.currentFluid.add(fluidTank.drain(fuelAmountToConsume, true));
                    long recipeVoltage = FuelRecipeLogic.getTieredVoltage(denseFuelRecipe.getMinVoltage());
                    int voltageMultiplier = (int) Math.max(1L, recipeVoltage / GTValues.V[GTValues.LV]);
                    return (int) Math.ceil(denseFuelRecipe.getDuration() * CONSUMPTION_MULTIPLIER * 2 * voltageMultiplier * this.getThrottleMultiplier());
                }
            }
        }
        for (int slotIndex = 0; slotIndex < this.itemImportInventory.getSlots(); slotIndex++) {
            ItemStack itemStack = this.itemImportInventory.getStackInSlot(slotIndex);
            ItemStack displayStack = itemStack.copy();
            int fuelBurnValue = (int) Math.ceil(TileEntityFurnace.getItemBurnTime(itemStack) / (50.0 * this.boilerType.fuelConsumptionMultiplier * this.getThrottleMultiplier()));
            if (fuelBurnValue > 0 ) {
                if (this.currentItemProcessed.equals("tj.multiblock.large_boiler.insert_burnable")) {
                    this.currentItemProcessed = itemStack.getTranslationKey();
                }
                if (itemStack.getTranslationKey().equals(this.currentItemProcessed)) {
                    this.currentItemsEngaged += itemStack.getCount();
                    if (this.currentItemsEngaged >= MAX_PROCESSES) {
                        int takeAmountFromStack = Math.abs((this.currentItemsEngaged - MAX_PROCESSES) - itemStack.getCount());
                        itemStack.shrink(takeAmountFromStack);
                        this.itemImportInventory.setStackInSlot(slotIndex, itemStack);
                    } else {
                        ItemStack containerItem = itemStack.getItem().getContainerItem(itemStack);
                        this.itemImportInventory.setStackInSlot(slotIndex, containerItem);
                    }
                }
                if (this.currentItemsEngaged < MAX_PROCESSES)
                    continue;
                displayStack.setCount(this.currentItemsEngaged);
                this.currentItem.add(displayStack);

                this.currentItemsEngaged = 0;
                this.currentItemProcessed = "tj.multiblock.large_boiler.insert_burnable";
                return fuelBurnValue;
            }
        }
        return 0;
    }

    private double getThrottleMultiplier() {
        return this.throttlePercentage / 100.0;
    }

    private double getThrottleEfficiency() {
        return MathHelper.clamp(1.0 + 0.3*Math.log(this.getThrottleMultiplier()), 0.4, 1.0);
    }

    private void replaceFireboxAsActive(boolean isActive) {
        BlockPos centerPos = this.getPos().offset(this.getFrontFacing().getOpposite()).down();
        for (int x = -13; x <= 13; x++) {
            for (int z = -13; z <= 13; z++) {
                BlockPos blockPos = centerPos.add(x, -1, z);
                IBlockState blockState = this.getWorld().getBlockState(blockPos);
                if (blockState.getBlock() instanceof BlockFireboxCasing) {
                    blockState = blockState.withProperty(BlockFireboxCasing.ACTIVE, isActive);
                    this.getWorld().setBlockState(blockPos, blockState);
                }
            }
        }
    }

    @Override
    public int getLightValueForPart(IMultiblockPart sourcePart) {
        return sourcePart == null ? 0 : (this.isActive ? 15 : 0);
    }

    public int getMAX_PROCESSES() {
        return MAX_PROCESSES;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return this.boilerType == null ? null : FactoryBlockPattern.start()
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
                .where('S', this.selfPredicate())
                .where('P', statePredicate(boilerType.pipeState))
                .where('X', state -> statePredicate(GTUtility.getAllPropertyValues(boilerType.fireboxState, BlockFireboxCasing.ACTIVE))
                        .or(abilityPartPredicate(MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.IMPORT_ITEMS, GregicAdditionsCapabilities.MAINTENANCE_HATCH)).test(state))
                .where('C', statePredicate(boilerType.casingState).or(abilityPartPredicate(OUTPUT_ABILITIES)))
                .build();
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        this.getFrontOverlay().render(renderState, translation, pipeline, this.getFrontFacing(), this.isActive);
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return this.boilerType.frontOverlay;
    }

    private boolean isFireboxPart(IMultiblockPart sourcePart) {
        return this.isStructureFormed() && (((MetaTileEntity) sourcePart).getPos().getY() < getPos().getY());
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        if (sourcePart != null && isFireboxPart(sourcePart)) {
            return this.isActive ? this.boilerType.firefoxActiveRenderer : this.boilerType.fireboxIdleRenderer;
        }
        return this.boilerType.solidCasingRenderer;
    }

    @Override
    public boolean shouldRenderOverlay(IMultiblockPart sourcePart) {
        return sourcePart == null || !this.isFireboxPart(sourcePart);
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        ItemStack itemStack = playerIn.getHeldItem(hand);
        if(!itemStack.isEmpty() && itemStack.hasCapability(GregtechCapabilities.CAPABILITY_MALLET, null)) {
            ISoftHammerItem softHammerItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_MALLET, null);

            if (this.getWorld().isRemote) {
                return true;
            }
            if(!softHammerItem.damageItem(DamageValues.DAMAGE_FOR_SOFT_HAMMER, false)) {
                return false;
            }
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
    }

    private void setActive(boolean active) {
        this.isActive = active;
        if (!this.getWorld().isRemote) {
            this.writeCustomData(100, buf -> buf.writeBoolean(isActive));
            this.replaceFireboxAsActive(isActive);
            this.markDirty();
        }
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
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 100) {
            this.isActive = buf.readBoolean();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("CurrentTemperature", this.currentTemperature);
        data.setInteger("MaxProgress", this.maxProgress);
        data.setInteger("Progress", this.progress);
        data.setBoolean("HasNoWater", this.hasNoWater);
        data.setBoolean("Active", this.isActive);
        data.setInteger("ThrottlePercentage", this.throttlePercentage);
        data.setString("CurrentItemProcessed", this.currentItemProcessed);
        data.setInteger("CurrentItemsEngaged", this.currentItemsEngaged);
        if (!currentItem.isEmpty())
            data.setTag("CurrentItem", this.currentItem.get(0).writeToNBT(new NBTTagCompound()));
        if (!currentFluid.isEmpty())
            data.setTag("CurrentFluid", this.currentFluid.get(0).writeToNBT(new NBTTagCompound()));
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.currentTemperature = data.getInteger("CurrentTemperature");
        this.maxProgress = data.getInteger("MaxProgress");
        this.progress = data.getInteger("Progress");
        this.hasNoWater = data.getBoolean("HasNoWater");
        this.isActive = data.getBoolean("Active");
        this.currentItemsEngaged = data.getInteger("CurrentItemsEngaged");
        if (data.hasKey("CurrentItemProcessed"))
            this.currentItemProcessed = data.getString("CurrentItemProcessed");
        if (data.hasKey("ThrottlePercentage"))
            this.throttlePercentage = data.getInteger("ThrottlePercentage");
        if (data.hasKey("CurrentItem"))
            this.currentItem.add(new ItemStack(data.getCompoundTag("CurrentItem")));
        if (data.hasKey("CurrentFluid"))
            this.currentFluid.add(FluidStack.loadFluidStackFromNBT(data.getCompoundTag("CurrentFluid")));
    }

    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == CAPABILITY_WORKABLE)
            return CAPABILITY_WORKABLE.cast(this);
        if (capability == CAPABILITY_FUELABLE)
            return CAPABILITY_FUELABLE.cast(this);
        if (capability == CAPABILITY_HEAT)
            return CAPABILITY_HEAT.cast(this);
        if (capability == CAPABILITY_GENERATOR)
            return CAPABILITY_GENERATOR.cast(this);
        if (capability == CAPABILITY_ITEM_FLUID_HANDLING)
            return CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability, side);
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
                int burnTime = (int) Math.ceil(dieselRecipe.getDuration() * CONSUMPTION_MULTIPLIER  / 2.0 * voltageMultiplier * getThrottleMultiplier());
                int fuelAmountToConsume = (int) Math.ceil(dieselRecipe.getRecipeFluid().amount * MAX_PROCESSES * CONSUMPTION_MULTIPLIER * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                final long fuelBurnTime = ((long) fuelStack.amount * burnTime) / fuelAmountToConsume;
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
                int burnTime = (int) Math.ceil(denseFuelRecipe.getDuration() * CONSUMPTION_MULTIPLIER * 2 * voltageMultiplier * getThrottleMultiplier());
                int fuelAmountToConsume = (int) Math.ceil(denseFuelRecipe.getRecipeFluid().amount * MAX_PROCESSES * CONSUMPTION_MULTIPLIER * boilerType.fuelConsumptionMultiplier * getThrottleMultiplier());
                final long fuelBurnTime = ((long) fuelStack.amount * burnTime) / fuelAmountToConsume;
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

    @Override
    public long heat() {
        return currentTemperature;
    }

    @Override
    public long maxHeat() {
        return boilerType.maxTemperature;
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
    public List<ItemStack> getItemInputs() {
        return currentItem;
    }

    @Override
    public List<FluidStack> getFluidInputs() {
        return currentFluid;
    }

    @Override
    public long getConsumption() {
        return waterConsumption;
    }

    @Override
    public long getProduction() {
        return steamProduction;
    }

    @Override
    public String[] consumptionInfo() {
        return ArrayUtils.toArray("machine.universal.consumption", "§9 ", "suffix", "machine.universal.liters.short",  "§r ", Water.getUnlocalizedName(), "machine.universal.tick");
    }

    @Override
    public String[] productionInfo() {
        return ArrayUtils.toArray("machine.universal.producing", "§7 ", "suffix", "machine.universal.liters.short", "§r ", Steam.getUnlocalizedName(), "machine.universal.tick");
    }
}
