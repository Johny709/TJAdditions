package com.johny.tj.builder.multicontrollers;

import com.johny.tj.TJValues;
import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.recipes.RecipeMap;
import net.minecraft.util.text.*;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;

import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static net.minecraft.util.text.TextFormatting.*;

;

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

    public MultiblockDisplayBuilder energyStored(long energyStored, long energyCapacity) {
        this.textList.add(new TextComponentString(I18n.translateToLocalFormatted("machine.universal.energy.stored", energyStored, energyCapacity)));
        return this;
    }

    public MultiblockDisplayBuilder energyInput(boolean hasEnoughEnergy, long amount) {
        return this.energyInput(hasEnoughEnergy, amount, 1);
    }

    public MultiblockDisplayBuilder energyInput(boolean hasEnoughEnergy, long amount, int maxProgress) {
        ITextComponent textComponent = !hasEnoughEnergy ? new TextComponentTranslation("gregtech.multiblock.not_enough_energy")
                : maxProgress > 1 ? new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.parallel.sum.2", amount, maxProgress))
                : new TextComponentString(I18n.translateToLocalFormatted("tj.multiblock.parallel.sum", amount)) ;
        textComponent.getStyle().setColor(hasEnoughEnergy ? WHITE : RED);
        this.textList.add(textComponent);
        return this;
    }

    public MultiblockDisplayBuilder voltageTier(int tier) {
        if (tier > 0) {
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
        if (energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
            long maxVoltage = energyContainer.getInputVoltage();
            String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
            textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
        }
        return this;
    }

    public MultiblockDisplayBuilder energyBonus(int energyBonus, boolean enabled) {
        if (enabled)
            this.textList.add(new TextComponentTranslation("gregtech.multiblock.universal.energy_usage", 100 - energyBonus).setStyle(new Style().setColor(TextFormatting.AQUA)));
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

    public MultiblockDisplayBuilder recipeMap(RecipeMap<?> recipeMap) {
        this.textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.tooltip.1")
                .appendSibling(withButton(new TextComponentString("[" + I18n.translateToLocal("recipemap." + recipeMap.getUnlocalizedName() + ".name") + "]"), recipeMap.getUnlocalizedName())));
        return this;
    }
}
