package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.blocks.BlockPipeCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.handlers.LargeAtmosphereCollectorWorkableHandler;
import com.johny.tj.gui.TJGuiTextures;
import gregtech.api.capability.impl.FuelRecipeLogic;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class MetaTileEntityLargeAtmosphereCollector extends MetaTileEntityLargeTurbine {

    protected boolean doStructureCheck;
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS};

    public MetaTileEntityLargeAtmosphereCollector(ResourceLocation metaTileEntityId, TurbineType turbineType) {
        super(metaTileEntityId, turbineType);
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeAtmosphereCollector(metaTileEntityId, turbineType);
    }

    @Override
    protected FuelRecipeLogic createWorkable(long maxVoltage) {
        return new LargeAtmosphereCollectorWorkableHandler(this, recipeMap, () -> energyContainer, () -> importFluidHandler);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.multiblock.large_atmosphere_collector.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            MetaTileEntityRotorHolder rotorHolder = getRotorHolder();
            FluidStack fuelStack = ((LargeAtmosphereCollectorWorkableHandler) workableHandler).getFuelStack();
            int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

            ITextComponent fuelName = new TextComponentTranslation(fuelAmount == 0 ? "gregtech.fluid.empty" : fuelStack.getUnlocalizedName());
            textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.fuel_amount", fuelAmount, fuelName));

            if (rotorHolder.getRotorEfficiency() > 0.0) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.rotor_speed", rotorHolder.getCurrentRotorSpeed(), rotorHolder.getMaxRotorSpeed()));
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.rotor_efficiency", (int) (rotorHolder.getRotorEfficiency() * 100)));
                int rotorDurability = (int) (rotorHolder.getRotorDurability() * 100);
                if (rotorDurability > 10) {
                    textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.rotor_durability", rotorDurability));
                } else {
                    textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.low_rotor_durability",
                            10, rotorDurability).setStyle(new Style().setColor(TextFormatting.RED)));
                }
            }

            if(!isRotorFaceFree()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.obstructed")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            }
            if (!workableHandler.isWorkingEnabled()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.work_paused"));
            } else if (workableHandler.isActive()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.running"));
                textList.add(new TextComponentTranslation("gregtech.multiblock.generation_eu", workableHandler.getRecipeOutputVoltage()));
            } else {
                textList.add(new TextComponentTranslation("gregtech.multiblock.idling"));
            }
        } else {
            ITextComponent tooltip = new TextComponentTranslation("gregtech.multiblock.invalid_structure.tooltip");
            tooltip.setStyle(new Style().setColor(TextFormatting.GRAY));
            textList.add(new TextComponentTranslation("gregtech.multiblock.invalid_structure")
                    .setStyle(new Style().setColor(TextFormatting.RED)
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        return abilities.containsKey(MultiblockAbility.IMPORT_FLUIDS);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return turbineType == null ? null : FactoryBlockPattern.start(LEFT, BACK, DOWN)
                .aisle("CPPPCC", "CPPPXC", "CPPPCC")
                .aisle("CPPPXC", "R####F", "CPPPSC")
                .aisle("CPPPCC", "CPPPXC", "CPPPCC")
                .where('S', selfPredicate())
                .where('C', statePredicate(turbineType.casingState))
                .where('P', statePredicate(getPipeState()))
                .where('X', statePredicate(turbineType.casingState).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', abilityPartPredicate(MultiblockAbility.EXPORT_FLUIDS))
                .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
                .where('#', isAirPredicate())
                .build();
    }

    public IBlockState getPipeState() {
        switch (turbineType) {
            case STEAM: return MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.STEEL_PIPE);
            case GAS: return TJMetaBlocks.PIPE_CASING.getState(BlockPipeCasings.PipeCasingType.STAINLESS_PIPE_CASING);
            default: return MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE);
        }
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
                Textures.FILTER_OVERLAY.renderSided(facing, renderState, translation, pipeline);
            } else {
                Textures.AIR_VENT_OVERLAY.renderSided(facing, renderState, translation, pipeline);
            }
            Textures.MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, getFrontFacing(), workableHandler.isActive());
        }
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.extendedBuilder();
        builder.image(-10, 0, 195, 217, TJGuiTextures.NEW_MULTIBLOCK_DISPLAY);
        builder.label(1, 9, getMetaFullName(), 0xFFFFFF);
        builder.widget(new AdvancedTextWidget(1, 19, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180)
                .setClickHandler(this::handleDisplayClick));
        builder.widget(new ToggleButtonWidget(162, 170, 18, 18, TJGuiTextures.POWER_BUTTON, this::getToggleMode, this::setToggleRunning)
                .setTooltipText("machine.universal.toggle.run.mode"));
        builder.widget(new ToggleButtonWidget(162, 152, 18, 18, TJGuiTextures.CAUTION_BUTTON, this::getDoStructureCheck, this::setDoStructureCheck)
                .setTooltipText("machine.universal.toggle.check.mode"));
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT ,-3, 134);
        return builder;
    }

    protected boolean getToggleMode() {
        return this.workableHandler.isWorkingEnabled();
    }

    protected void setToggleRunning(boolean running) {
        this.workableHandler.setWorkingEnabled(running);
    }

    protected boolean getDoStructureCheck() {
        if (isStructureFormed())
            this.doStructureCheck = false;
        return this.doStructureCheck;
    }

    protected void setDoStructureCheck(boolean check) {
        if (isStructureFormed()) {
            this.doStructureCheck = true;
            invalidateStructure();
            this.structurePattern = createStructurePattern();
        }
    }
}
