package com.johny.tj.builder.multicontrollers;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.util.text.TextFormatting.*;

public class MultiblockDisplayBuilder {

    private final List<ITextComponent> textList;

    public MultiblockDisplayBuilder(List<ITextComponent> textList) {
        this.textList = textList;
    }

    public static MultiblockDisplayBuilder start(List<ITextComponent> textList) {
        return new MultiblockDisplayBuilder(textList);
    }

    public MultiblockDisplayBuilder custom(Consumer<List<ITextComponent>> textList) {
        textList.accept(this.textList);
        return this;
    }

    public MultiblockDisplayBuilder voltageTier(IEnergyContainer energyContainer) {
        return this.voltageTier(energyContainer, true);
    }

    public MultiblockDisplayBuilder voltageTier(IEnergyContainer energyContainer, boolean isStructureFormed) {
        if (isStructureFormed && energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
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
