package com.johny.tj.builder.multicontrollers;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import java.util.List;

public class MultiblockDisplaysBuilder {

    public static MultiblockDisplaysBuilder start() {
        return new MultiblockDisplaysBuilder();
    }

    public MultiblockDisplaysBuilder maintenanceDisplay(List<ITextComponent> textList, byte maintenanceProblems, boolean hasProblems) {
        if (hasProblems) {
            textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.has_problems")
                    .setStyle(new Style().setColor(TextFormatting.DARK_RED)));

            // Wrench
            if (((maintenanceProblems) & 1) == 0) {
                textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.problem.wrench")
                        .setStyle(new Style().setColor(TextFormatting.RED)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new TextComponentTranslation("gtadditions.multiblock.universal.problem.wrench.tooltip")))));
            }

            // Screwdriver
            if (((maintenanceProblems >> 1) & 1) == 0) {
                textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.problem.screwdriver")
                        .setStyle(new Style().setColor(TextFormatting.RED)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new TextComponentTranslation("gtadditions.multiblock.universal.problem.screwdriver.tooltip")))));
            }

            // Soft Hammer
            if (((maintenanceProblems >> 2) & 1) == 0) {
                textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.problem.softhammer")
                        .setStyle(new Style().setColor(TextFormatting.RED)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new TextComponentTranslation("gtadditions.multiblock.universal.problem.softhammer.tooltip")))));
            }

            // Hard Hammer
            if (((maintenanceProblems >> 3) & 1) == 0) {
                textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.problem.hardhammer")
                        .setStyle(new Style().setColor(TextFormatting.RED)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new TextComponentTranslation("gtadditions.multiblock.universal.problem.hardhammer.tooltip")))));
            }

            // Wirecutter
            if (((maintenanceProblems >> 4) & 1) == 0) {
                textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.problem.wirecutter")
                        .setStyle(new Style().setColor(TextFormatting.RED)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new TextComponentTranslation("gtadditions.multiblock.universal.problem.wirecutter.tooltip")))));
            }

            // Crowbar
            if (((maintenanceProblems >> 5) & 1) == 0) {
                textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.problem.crowbar")
                        .setStyle(new Style().setColor(TextFormatting.RED)
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new TextComponentTranslation("gtadditions.multiblock.universal.problem.crowbar.tooltip")))));
            }
        } else {
            textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.no_problems")
                    .setStyle(new Style().setColor(TextFormatting.GREEN)));
        }
        return this;
    }

    public MultiblockDisplaysBuilder mufflerDisplay(List < ITextComponent > textList,boolean isMufflerFaceFree) {
        if (!isMufflerFaceFree)
            textList.add(new TextComponentTranslation("gtadditions.multiblock.universal.muffler_obstructed")
                    .setStyle(new Style().setColor(TextFormatting.RED)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new TextComponentTranslation("gtadditions.multiblock.universal.muffler_obstructed.tooltip")))));
        return this;
    }

    public MultiblockDisplaysBuilder isInvalid(List < ITextComponent > textList,boolean isStructureFormed) {
        if (!isStructureFormed) {
            ITextComponent tooltip = new TextComponentTranslation("gregtech.multiblock.invalid_structure.tooltip");
            tooltip.setStyle(new Style().setColor(TextFormatting.GRAY));
            textList.add(new TextComponentTranslation("gregtech.multiblock.invalid_structure")
                    .setStyle(new Style().setColor(TextFormatting.RED)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
        return this;
    }

    public MultiblockDisplaysBuilder recipeMapWorkable(List < ITextComponent > textList, boolean isStructureFormed, MultiblockRecipeLogic recipeLogic) {
        if (isStructureFormed) {
            IEnergyContainer energyContainer = recipeLogic.getEnergyContainer();
            if (energyContainer != null && energyContainer.getEnergyCapacity() > 0) {
                long maxVoltage = energyContainer.getInputVoltage();
                String voltageName = GAValues.VN[GAUtility.getTierByVoltage(maxVoltage)];
                textList.add(new TextComponentTranslation("gregtech.multiblock.max_energy_per_tick", maxVoltage, voltageName));
            }

            textList.add(recipeLogic.isWorkingEnabled() ? (recipeLogic.isActive() ? new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN))
                    : new TextComponentTranslation("gregtech.multiblock.idling"))
                    : new TextComponentTranslation("gregtech.multiblock.work_paused").setStyle(new Style().setColor(TextFormatting.YELLOW)));
            int currentProgress = (int) (recipeLogic.getProgressPercent() * 100);
            textList.add(new TextComponentTranslation("gregtech.multiblock.progress", currentProgress));

            if (recipeLogic.isHasNotEnoughEnergy()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.not_enough_energy").setStyle(new Style().setColor(TextFormatting.RED)));
            }
        }
        return this;
    }

    public MultiblockDisplaysBuilder recipeMapWorkable (List < ITextComponent > textList, boolean isStructureFormed, FuelRecipeLogic recipeLogic) {
        if (isStructureFormed) {
            textList.add(recipeLogic.isWorkingEnabled() ? (recipeLogic.isActive() ? new TextComponentTranslation("gregtech.multiblock.running").setStyle(new Style().setColor(TextFormatting.GREEN))
                    : new TextComponentTranslation("gregtech.multiblock.idling"))
                    : new TextComponentTranslation("gregtech.multiblock.work_paused").setStyle(new Style().setColor(TextFormatting.YELLOW)));
        }
        return this;
    }
}
