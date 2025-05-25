package com.johny.tj.builder.multicontrollers;

import com.johny.tj.TJValues;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.util.text.TextFormatting.*;

public class MultiblockDisplayBuilder {

    private final List<ITextComponent> textList;
    private final boolean isStructureFormed;

    public MultiblockDisplayBuilder(List<ITextComponent> textList, boolean isStructureFormed) {
        this.textList = textList;
        this.isStructureFormed = isStructureFormed;
    }

    public static MultiblockDisplayBuilder start(List<ITextComponent> textList, boolean isStructureFormed) {
        return new MultiblockDisplayBuilder(textList, isStructureFormed);
    }

    public MultiblockDisplayBuilder custom(Consumer<List<ITextComponent>> textList) {
        textList.accept(this.textList);
        return this;
    }

    public MultiblockDisplayBuilder voltageTier(int tier) {
        if (this.isStructureFormed && tier > 0) {
            String color = TJValues.VCC[tier];
            this.textList.add(new TextComponentTranslation("machine.universal.tooltip.voltage_tier")
                    .appendText(" ")
                    .appendSibling(new TextComponentString(color + GAValues.V[tier] + "§r"))
                    .appendText(" (")
                    .appendSibling(new TextComponentString(color + GAValues.VN[tier] + "§r"))
                    .appendText(")"));
        }
        return this;
    }

    public MultiblockDisplayBuilder voltageIn(IEnergyContainer energyContainer) {
        if (this.isStructureFormed && energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
            long maxVoltage = energyContainer.getInputVoltage();
            String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
            textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
        }
        return this;
    }

    public MultiblockDisplayBuilder fluidInput(boolean hasEnoughAmount, FluidStack fluidStack, int amount) {
        String fluidName = fluidStack.getLocalizedName();
        boolean hasEnoughFluid = hasEnoughAmount || amount == 0;
        ITextComponent fluidInputText = hasEnoughFluid ? new TextComponentTranslation("machine.universal.fluid.input.sec", fluidName, amount)
                : new TextComponentTranslation("tj.multiblock.not_enough_fluid", fluidName, amount);
        fluidInputText.getStyle().setColor(hasEnoughFluid ? WHITE : RED);
        textList.add(fluidInputText);
        return this;
    }

    public MultiblockDisplayBuilder isWorking(boolean isWorkingEnabled, boolean isActive, int progress, int maxProgress) {
        if (!this.isStructureFormed)
            return this;
        int currentProgress = (int) Math.floor(progress / (maxProgress * 1.0) * 100);
        ITextComponent isWorkingText = !isWorkingEnabled ? new TextComponentTranslation("gregtech.multiblock.work_paused")
                : !isActive ? new TextComponentTranslation("gregtech.multiblock.idling")
                : new TextComponentTranslation("gregtech.multiblock.running");
        isWorkingText.getStyle().setColor(!isWorkingEnabled ? YELLOW : !isActive ? WHITE : GREEN);
        textList.add(isWorkingText);
        if (isActive)
            textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));
        return this;
    }
}
