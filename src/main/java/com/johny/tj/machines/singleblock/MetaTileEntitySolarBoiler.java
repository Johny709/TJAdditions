package com.johny.tj.machines.singleblock;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.textures.TJTextures;
import gregtech.api.capability.impl.FilteredFluidHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.recipes.ModHandler;
import gregtech.api.render.SimpleSidedCubeRenderer;
import gregtech.api.render.Textures;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static com.johny.tj.gui.TJGuiTextures.*;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.render.Textures.*;
import static gregtech.api.unification.material.Materials.Steam;

public class MetaTileEntitySolarBoiler extends MetaTileEntity {

    private float temp;
    private boolean isActive;
    private boolean hadWater;

    private final IFluidTank waterTank;
    private final IFluidTank steamTank;
    private final SolarBoilerType boilerType;
    private static final EnumFacing[] STEAM_PUSH_DIRECTIONS = ArrayUtils.add(EnumFacing.HORIZONTALS, EnumFacing.UP);

    public MetaTileEntitySolarBoiler(ResourceLocation metaTileEntityId, SolarBoilerType boilerType) {
        super(metaTileEntityId);
        this.boilerType = boilerType;
        this.waterTank = new FilteredFluidHandler(16000).setFillPredicate(ModHandler::isWater);
        this.steamTank = new FluidTank(16000);
        initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntitySolarBoiler(metaTileEntityId, boilerType);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gregtech.machine.steam_boiler.tooltip_produces", boilerType.steamProduction, boilerType.ticks));
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new ItemStackHandler(1);
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new ItemStackHandler(1);
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return new FluidTankList(true, waterTank);
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return new FluidTankList(true, steamTank);
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        WidgetGroup widgetGroup = new WidgetGroup();
        widgetGroup.addWidget(new ProgressWidget(this::getTicksUntilMax, 95, 15, 15, 60) {
            private float displayTemp;
            private int displaySteamGeneration;
            private boolean displayCanGenerateSteam;
            @Override
            public void drawInForeground(int mouseX, int mouseY) {
                if(isMouseOverElement(mouseX, mouseY)) {
                    List<String> hoverList = Arrays.asList(I18n.format("machine.boiler.display.temperature", this.displayTemp),
                            I18n.format("machine.boiler.display.steam", this.displayCanGenerateSteam ? this.displaySteamGeneration : 0, boilerType.ticks),
                            I18n.format("machine.boiler.display.steam.description"));
                    drawHoveringText(ItemStack.EMPTY, hoverList, 300, mouseX, mouseY);
                }
            }
            @Override
            public void detectAndSendChanges() {
                super.detectAndSendChanges();
                float displayTemp = (getTicksUntilMax() * 5) * 100;
                int displaySteamGeneration = (int) (boilerType.steamProduction * getTicksUntilMax());
                boolean displayCanGenerateSteam = canGenerateSteam();
                writeUpdateInfo(1, buffer -> buffer.writeFloat(displayTemp));
                writeUpdateInfo(2, buffer -> buffer.writeInt(displaySteamGeneration));
                writeUpdateInfo(3, buffer -> buffer.writeBoolean(displayCanGenerateSteam));
            }

            @Override
            public void readUpdateInfo(int id, PacketBuffer buffer) {
                super.readUpdateInfo(id, buffer);
                if (id == 1) {
                    this.displayTemp = buffer.readFloat();
                }
                if (id == 2) {
                    this.displaySteamGeneration = buffer.readInt();
                }
                if (id == 3) {
                    this.displayCanGenerateSteam = buffer.readBoolean();
                }
            }

        }.setProgressBar(boilerType.progressBar, BAR_HEAT, ProgressWidget.MoveType.VERTICAL));
        widgetGroup.addWidget(new TankWidget(waterTank, 78, 15, 15, 60).setBackgroundTexture(boilerType.progressBar));
        widgetGroup.addWidget(new TankWidget(steamTank, 61, 15, 15, 60).setBackgroundTexture(boilerType.progressBar));
        widgetGroup.addWidget(new SlotWidget(importItems, 0, 36, 15).setBackgroundTexture(boilerType.slot, boilerType.in));
        widgetGroup.addWidget(new SlotWidget(exportItems, 0, 36, 55).setBackgroundTexture(boilerType.slot, boilerType.out));
        widgetGroup.addWidget(new ImageWidget(120, 15, 40, 40, isActive ? boilerType.sun : boilerType.moon));
        widgetGroup.addWidget(new ImageWidget(36, 35, 18, 18, boilerType.canister));
        return ModularUI.builder(boilerType.background, 176, 167)
                .label(10, 5, getMetaFullName())
                .widget(widgetGroup)
                .bindPlayerInventory(player.inventory, boilerType.slot, 7, 85)
                .build(getHolder(), player);
    }

    @Override
    public void update() {
        super.update();
        if (!getWorld().isRemote && getOffsetTimer() % boilerType.ticks == 0) {
            if (getWorld().isDaytime() && getWorld().canBlockSeeSky(getPos().up()) && !getWorld().isRaining()) {
                if (!isActive)
                    setActive(true);
                temp = (float) MathHelper.clamp(temp + (1.5 * boilerType.ticks), 0, 12000);
            } else {
                if (isActive)
                    setActive(false);
                temp = MathHelper.clamp(temp - boilerType.ticks, 0, 12000);
            }
            fillInternalTankFromFluidContainer(importItems, exportItems, 0, 0);
            pushFluidsIntoNearbyHandlers(STEAM_PUSH_DIRECTIONS);
            if (!canGenerateSteam()) {
                hadWater = false;
                return;
            }
            int waterToConsume = Math.round((float) boilerType.steamProduction / 160);
            FluidStack waterStack = importFluids.drain(waterToConsume, false);
            boolean hasEnoughWater = waterStack != null && waterStack.amount == waterToConsume;
            if (hasEnoughWater && hadWater) {
                getWorld().setBlockToAir(this.getPos());
                getWorld().createExplosion(null,
                        getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
                        2.0f, true);
            } else {
                if (hasEnoughWater) {
                    steamTank.fill(Steam.getFluid((int) (boilerType.steamProduction * getTicksUntilMax())), true);
                    waterTank.drain(waterToConsume, true);
                } else
                    hadWater = true;
            }
        }
    }

    public float getTicksUntilMax() {
        return (float) (temp / (12000 * 1.0));
    }

    public boolean isActive() {
        return isActive;
    }

    private boolean canGenerateSteam() {
        return temp >= 2400;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        getBaseRenderer().render(renderState, translation, pipeline);
        Textures.SOLAR_BOILER_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), true);
        TJTextures.BOILER_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), isActive());
    }

    @SideOnly(Side.CLIENT)
    protected SimpleSidedCubeRenderer getBaseRenderer() {
        return boilerType.casing;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(getBaseRenderer().getParticleSprite(), getPaintingColor());
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 1) {
            this.isActive = buf.readBoolean();
            scheduleRenderUpdate();
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

    protected void setActive(boolean active) {
        this.isActive = active;
        if (!getWorld().isRemote) {
            writeCustomData(1, buf -> buf.writeBoolean(active));
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setFloat("Temp", temp);
        data.setBoolean("HadWater", hadWater);
        data.setBoolean("IsActive", isActive);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.temp = data.getFloat("Temp");
        this.hadWater = data.getBoolean("HadWater");
        this.isActive = data.getBoolean("IsActive");
    }

    public enum SolarBoilerType {
        BRONZE(25, 150, BAR_STEAM, SUN_BRONZE, MOON_BRONZE, BRONZE_BACKGROUND, BRONZE_SLOT, BRONZE_IN, BRONZE_OUT, CANISTER_OVERLAY, STEAM_BRICKED_CASING_BRONZE),
        STEEL(10, 150, BAR_STEEL, SUN_STEEL, MOON_STEEL, STEEL_BACKGROUND, STEEL_SLOT, STEEL_IN, STEEL_OUT, DARK_CANISTER_OVERLAY, STEAM_BRICKED_CASING_STEEL),
        LV(10, 300, BAR_STEEL, SUN_STEEL, MOON_STEEL, BACKGROUND, SLOT, STEEL_IN, STEEL_OUT, DARK_CANISTER_OVERLAY, VOLTAGE_CASINGS[1]);

        SolarBoilerType(int ticks, int steamProduction, TextureArea progressBar, TextureArea sun, TextureArea moon, TextureArea background, TextureArea slot, TextureArea in, TextureArea out, TextureArea canister, SimpleSidedCubeRenderer casing) {
            this.ticks = ticks;
            this.steamProduction = steamProduction;
            this.progressBar = progressBar;
            this.sun = sun;
            this.moon = moon;
            this.background = background;
            this.slot = slot;
            this.in = in;
            this.out = out;
            this.canister = canister;
            this.casing = casing;
        }
        private final int ticks;
        private final int steamProduction;
        private final TextureArea progressBar;
        private final TextureArea sun;
        private final TextureArea moon;
        private final TextureArea background;
        private final TextureArea slot;
        private final TextureArea in;
        private final TextureArea out;
        private final TextureArea canister;
        private final SimpleSidedCubeRenderer casing;
    }
}
