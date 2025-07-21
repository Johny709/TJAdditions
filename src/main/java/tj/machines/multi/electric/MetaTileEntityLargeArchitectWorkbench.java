package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAUtility;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.TJConfig;
import tj.builder.handlers.ArchitectWorkbenchWorkableHandler;
import tj.builder.multicontrollers.ExtendableMultiblockController;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.textures.TJTextures;
import tj.util.EnumFacingHelper;

import javax.annotation.Nullable;
import java.util.List;

import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.conveyorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.robotArmPredicate;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;

public class MetaTileEntityLargeArchitectWorkbench extends ExtendableMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, MAINTENANCE_HATCH};
    private final ArchitectWorkbenchWorkableHandler workbenchWorkableHandler = new ArchitectWorkbenchWorkableHandler(this, () -> this.itemInputs, () -> this.itemOutputs, () -> this.energyInput, this::getInputBus, () -> this.maxVoltage, () -> this.parallel);
    private ItemHandlerList itemInputs;
    private ItemHandlerList itemOutputs;
    private IEnergyContainer energyInput;
    private long maxVoltage;
    private int parallel;

    public MetaTileEntityLargeArchitectWorkbench(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeArchitectWorkbench(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.4", TJConfig.largeArchitectWorkbench.stack));
        tooltip.add(I18n.format("tj.multiblock.large_architect_workbench.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed())
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyInput)
                    .voltageTier(GAUtility.getTierByVoltage(this.maxVoltage))
                    .energyInput(!this.workbenchWorkableHandler.hasNotEnoughEnergy(), this.workbenchWorkableHandler.getEnergyPerTick())
                    .addTranslation("tj.multiblock.industrial_fusion_reactor.message", this.parallel)
                    .custom(text -> text.add(new TextComponentTranslation("gtadditions.multiblock.universal.distinct")
                            .appendText(" ")
                            .appendSibling(this.workbenchWorkableHandler.isDistinct()
                                    ? withButton(new TextComponentTranslation("gtadditions.multiblock.universal.distinct.yes"), "distinctEnabled")
                                    : withButton(new TextComponentTranslation("gtadditions.multiblock.universal.distinct.no"), "distinctDisabled"))))
                    .isWorking(this.workbenchWorkableHandler.isWorkingEnabled(), this.workbenchWorkableHandler.isActive(), this.workbenchWorkableHandler.getProgress(), this.workbenchWorkableHandler.getMaxProgress());
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        this.workbenchWorkableHandler.setDistinct(!componentData.equals("distinctEnabled"));
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (this.getNumProblems() < 6)
            this.workbenchWorkableHandler.update();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("XXX", "XXX", "~~~", "~~~");
            factoryPattern.aisle("XXX", "XcX", "C#C", "CrC");
        }
        return factoryPattern.aisle("XXX", "XSX", "~~~", "~~~")
                .where('S', selfPredicate())
                .where('C', statePredicate(getCasingState()))
                .where('X', statePredicate(getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('c', conveyorPredicate())
                .where('r', robotArmPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    protected IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        ConveyorCasing.CasingType conveyor = context.getOrDefault("Conveyor", ConveyorCasing.CasingType.CONVEYOR_LV);
        RobotArmCasing.CasingType robotArm = context.getOrDefault("RobotArm", RobotArmCasing.CasingType.ROBOT_ARM_LV);
        int min = Math.min(conveyor.getTier(), robotArm.getTier());
        this.itemInputs = new ItemHandlerList(this.getAbilities(IMPORT_ITEMS));
        this.itemOutputs = new ItemHandlerList(this.getAbilities(EXPORT_ITEMS));
        this.energyInput = new EnergyContainerList(this.getAbilities(INPUT_ENERGY));
        this.workbenchWorkableHandler.initialize(this.getAbilities(IMPORT_ITEMS).size());
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
        this.parallel = TJConfig.largeChiselWorkbench.stack * min;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.SOLID_STEEL_CASING;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_ASSEMBLER_OVERLAY.render(renderState, translation, pipeline, this.frontFacing, this.workbenchWorkableHandler.isActive(), this.workbenchWorkableHandler.hasProblem(), this.workbenchWorkableHandler.isWorkingEnabled());
        TJTextures.CHISEL_ARCHITECTURE.renderSided(EnumFacingHelper.getLeftFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.HAMMER.renderSided(EnumFacingHelper.getRightFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.SAW_BLADE.renderSided(this.frontFacing.getOpposite(), renderState, translation, pipeline);
    }

    @Override
    public void setWorkingEnabled(boolean isWorking) {
        this.workbenchWorkableHandler.setWorkingEnabled(isWorking);
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.workbenchWorkableHandler.isWorkingEnabled();
    }

    private IItemHandlerModifiable getInputBus(int index) {
        return this.getAbilities(IMPORT_ITEMS).get(index);
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.largeArchitectWorkbench.maximumSlices;
    }
}
