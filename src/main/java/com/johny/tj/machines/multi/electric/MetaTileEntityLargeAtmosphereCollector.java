package com.johny.tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.blocks.BlockPipeCasings;
import com.johny.tj.blocks.TJMetaBlocks;
import com.johny.tj.builder.handlers.LargeAtmosphereCollectorWorkableHandler;
import com.johny.tj.builder.multicontrollers.TJRotorHolderMultiblockController;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.GAMetaItems;
import gregtech.api.GTValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.FuelRecipeLogic;
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
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import gregtech.common.metatileentities.multi.electric.generator.MetaTileEntityLargeTurbine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class MetaTileEntityLargeAtmosphereCollector extends TJRotorHolderMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS, GregicAdditionsCapabilities.MAINTENANCE_HATCH, MultiblockAbility.IMPORT_ITEMS};
    public final MetaTileEntityLargeTurbine.TurbineType turbineType;
    public IFluidHandler exportFluidHandler;
    public ItemHandlerList importItemHandler;

    public MetaTileEntityLargeAtmosphereCollector(ResourceLocation metaTileEntityId, MetaTileEntityLargeTurbine.TurbineType turbineType) {
        super(metaTileEntityId, turbineType.recipeMap, GTValues.V[4]);
        this.turbineType = turbineType;
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
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();
        boolean hasInputFluid = abilities.containsKey(MultiblockAbility.IMPORT_FLUIDS);
        boolean hasOutputFluid = abilities.containsKey(MultiblockAbility.EXPORT_FLUIDS);

        return maintenanceCount == 1 && hasInputFluid && hasOutputFluid;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
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

            if (!isRotorFaceFree()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.turbine.obstructed")
                        .setStyle(new Style().setColor(TextFormatting.RED)));
            }
            if (!workableHandler.isWorkingEnabled()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.work_paused"));
            } else if (workableHandler.isActive()) {
                textList.add(new TextComponentTranslation("gregtech.multiblock.running"));
                textList.add(new TextComponentTranslation("tj.multiblock.large_atmosphere_collector.air", workableHandler.getRecipeOutputVoltage()));
            } else {
                textList.add(new TextComponentTranslation("gregtech.multiblock.idling"));
            }
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.exportFluidHandler = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        this.importItemHandler = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.exportFluidHandler = null;
        this.importItemHandler = new ItemHandlerList(Collections.emptyList());
    }

    @Override
    protected void updateFormedValid() {
        super.updateFormedValid();
        if (isStructureFormed()) {
            if (getOffsetTimer() % 20 == 0) {;
                for (MetaTileEntityRotorHolder rotorHolder : getAbilities(ABILITY_ROTOR_HOLDER)) {
                    if (rotorHolder.hasRotorInInventory())
                        continue;
                    ItemStack rotorReplacementStack = checkAndConsumeItem();
                    if (rotorReplacementStack != null) {
                        rotorHolder.getRotorInventory().setStackInSlot(0, rotorReplacementStack);
                    }
                }
            }
        }
    }

    private ItemStack checkAndConsumeItem() {
        int getItemSlots = importItemHandler.getSlots();
        for (int slotIndex = 0; slotIndex < getItemSlots; slotIndex++) {
            ItemStack item = importItemHandler.getStackInSlot(slotIndex);
            boolean hugeRotorStack = GAMetaItems.HUGE_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean largeRotorStack = GAMetaItems.LARGE_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean mediumRotorStack = GAMetaItems.MEDIUM_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);
            boolean smallRotorStack = GAMetaItems.SMALL_TURBINE_ROTOR.getStackForm().isItemEqualIgnoreDurability(item);

            // check if slot has either small, medium, large, huge rotor. if not then skip to next slot
            if(!hugeRotorStack && !largeRotorStack && !mediumRotorStack && !smallRotorStack)
                continue;

            ItemStack getItemFromSlot = item.getItem().getContainerItem(item);
            item.setCount(0); // sets stacksize to 0. effectively voiding the item
            importItemHandler.setStackInSlot(slotIndex, item);
            return getItemFromSlot;
        }
        return null;
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return turbineType == null ? null : FactoryBlockPattern.start(LEFT, BACK, DOWN)
                .aisle("CPPPCCC", "CPPPXXC", "CPPPCCC")
                .aisle("CPPPXXC", "R#####F", "CPPPSXC")
                .aisle("CPPPCCC", "CPPPXXC", "CPPPCCC")
                .where('S', selfPredicate())
                .where('C', statePredicate(turbineType.casingState))
                .where('P', statePredicate(getPipeState()))
                .where('X', statePredicate(turbineType.casingState).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', abilityPartPredicate(MultiblockAbility.EXPORT_FLUIDS))
                .where('R', abilityPartPredicate(ABILITY_ROTOR_HOLDER))
                .where('#', isAirPredicate())
                .build();
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return turbineType.casingRenderer;
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
    public boolean isWorkingEnabled() {
        return this.workableHandler.isWorkingEnabled();
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        this.workableHandler.setWorkingEnabled(isWorking);
    }

    @Override
    public int getRotorSpeedIncrement() {
        return 3;
    }

    @Override
    public int getRotorSpeedDecrement() {
        return 1;
    }
}
