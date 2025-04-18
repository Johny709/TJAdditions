package com.johny.tj.items.covers;

import com.johny.tj.builder.handlers.BasicEnergyHandler;
import com.johny.tj.textures.TJSimpleOverlayRenderer;
import com.johny.tj.util.EnderWorldData;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.common.covers.CoverPump;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.johny.tj.gui.TJGuiTextures.BAR_HEAT;
import static com.johny.tj.gui.TJGuiTextures.BAR_STEEL;
import static com.johny.tj.textures.TJTextures.PORTAL_OVERLAY;
import static gregtech.api.capability.GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
import static gregtech.api.gui.widgets.ProgressWidget.MoveType.VERTICAL;

public class CoverEnderEnergy extends AbstractCoverEnder<String, BasicEnergyHandler> {

    private final IEnergyContainer energyContainer;
    private final long capacity;
    private final int tier;

    public CoverEnderEnergy(ICoverable coverHolder, EnumFacing attachedSide, int tier) {
        super(coverHolder, attachedSide);
        this.tier = tier;
        this.energyContainer = this.coverHolder.getCapability(CAPABILITY_ENERGY_CONTAINER, null);
        this.capacity = (long) (Math.pow(4, tier) * 1000);
        this.maxTransferRate = (int) Math.min(Math.pow(4, tier) * 8, Integer.MAX_VALUE);
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CAPABILITY_ENERGY_CONTAINER, this.attachedSide) != null;
    }

    @Override
    protected TJSimpleOverlayRenderer getOverlay() {
        return PORTAL_OVERLAY;
    }

    @Override
    protected int getPortalColor() {
        return 0x9fff2c;
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    protected Map<String, BasicEnergyHandler> getMap() {
        return EnderWorldData.getEnergyContainerMap();
    }

    @Override
    protected void addWidgets(Consumer<Widget> widget) {
        widget.accept(new LabelWidget(30, 4, "metaitem.ender_energy_cover_" + GAValues.VN[tier].toLowerCase() + ".name"));
        widget.accept(new ProgressWidget(this::getEnergyStored, 7, 38, 18, 18) {
            private long energyStored;
            private long energyCapacity;

            @Override
            public void drawInForeground(int mouseX, int mouseY) {
                if(isMouseOverElement(mouseX, mouseY)) {
                    List<String> hoverList = Collections.singletonList(I18n.format("machine.universal.energy.stored", this.energyStored, this.energyCapacity));
                    drawHoveringText(ItemStack.EMPTY, hoverList, 300, mouseX, mouseY);
                }
            }

            @Override
            public void detectAndSendChanges() {
                super.detectAndSendChanges();
                BasicEnergyHandler energyHandler = getMap().get(text);
                if (energyHandler != null) {
                    long energyStored = energyHandler.getStored();
                    long energyCapacity = energyHandler.getCapacity();
                    writeUpdateInfo(1, buffer -> buffer.writeLong(energyStored));
                    writeUpdateInfo(2, buffer -> buffer.writeLong(energyCapacity));
                }
            }

            @Override
            public void readUpdateInfo(int id, PacketBuffer buffer) {
                super.readUpdateInfo(id, buffer);
                if (id == 1) {
                    this.energyStored = buffer.readLong();
                }
                if (id == 2) {
                    this.energyCapacity = buffer.readLong();
                }
            }

        }.setProgressBar(BAR_STEEL, BAR_HEAT, VERTICAL));
    }

    private double getEnergyStored() {
        BasicEnergyHandler energyHandler = getMap().get(text);
        if (energyHandler == null)
            return 0;
        return (double) energyHandler.getStored() / energyHandler.getCapacity();
    }

    @Override
    protected void onAddEntry(Widget.ClickData clickData) {
        EnderWorldData.getEnergyContainerMap().putIfAbsent(text, new BasicEnergyHandler(capacity));
    }

    @Override
    protected void onClear(Widget.ClickData clickData) {
        EnderWorldData.getEnergyContainerMap().put(text, new BasicEnergyHandler(capacity));
    }

    @Override
    public void update() {
        if (isWorkingEnabled) {
            BasicEnergyHandler enderEnergyContainer = getMap().get(text);
            if (enderEnergyContainer == null)
                return;
            if (pumpMode == CoverPump.PumpMode.IMPORT) {
                importEnergy(enderEnergyContainer);
            } else {
                exportEnergy(enderEnergyContainer);
            }
        }
    }

    private void importEnergy(BasicEnergyHandler enderEnergyContainer) {
        long energyRemainingToFill = enderEnergyContainer.getCapacity() - enderEnergyContainer.getStored();
        if (enderEnergyContainer.getStored() < 1 || energyRemainingToFill != 0) {
            long energyExtracted = energyContainer.removeEnergy(Math.min(energyRemainingToFill, transferRate));
            enderEnergyContainer.addEnergy(Math.abs(energyExtracted));
        }
    }

    private void exportEnergy(BasicEnergyHandler enderEnergyContainer) {
        long energyRemainingToFill = energyContainer.getEnergyCapacity() - energyContainer.getEnergyStored();
        if (energyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            long energyExtracted = enderEnergyContainer.removeEnergy(Math.min(energyRemainingToFill, transferRate));
            energyContainer.acceptEnergyFromNetwork(this.attachedSide, Math.abs(energyExtracted), 1);
        }
    }
}
