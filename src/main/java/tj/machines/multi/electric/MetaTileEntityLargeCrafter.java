package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAUtility;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.TJConfig;
import tj.builder.handlers.CrafterRecipeLogic;
import tj.builder.handlers.IRecipeMapProvider;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.textures.TJTextures;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.conveyorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.robotArmPredicate;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.unification.material.Materials.Steel;
import static tj.machines.multi.electric.MetaTileEntityLargeGreenhouse.glassPredicate;

public class MetaTileEntityLargeCrafter extends TJMultiblockDisplayBase implements IRecipeMapProvider {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, INPUT_ENERGY, MAINTENANCE_HATCH};
    private final CrafterRecipeLogic recipeLogic = new CrafterRecipeLogic(this);
    private IItemHandlerModifiable importItemInventory;
    private IItemHandlerModifiable exportItemInventory;
    private IEnergyContainer energyContainer;
    private long maxVoltage;
    private int parallel;

    public MetaTileEntityLargeCrafter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.recipeLogic.setImportItems(() -> this.importItemInventory);
        this.recipeLogic.setExportItems(() -> this.exportItemInventory);
        this.recipeLogic.setImportEnergy(() -> this.energyContainer);
        this.recipeLogic.setInputBus((index) -> this.getAbilities(IMPORT_ITEMS).get(index));
        this.recipeLogic.setMaxVoltage(() -> this.maxVoltage);
        this.recipeLogic.setParallel(() -> this.parallel);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeCrafter(this.metaTileEntityId);
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();
        return maintenanceCount == 1 &&
                abilities.containsKey(IMPORT_ITEMS) &&
                abilities.containsKey(EXPORT_ITEMS) &&
                abilities.containsKey(INPUT_ENERGY);
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed())
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyContainer)
                    .voltageTier(GAUtility.getTierByVoltage(this.maxVoltage))
                    .energyInput(!this.recipeLogic.hasNotEnoughEnergy(), this.recipeLogic.getEUt())
                    .addTranslation("tj.multiblock.industrial_fusion_reactor.message", this.parallel)
                    .custom(text -> text.add(new TextComponentTranslation("gtadditions.multiblock.universal.distinct")
                            .appendText(" ")
                            .appendSibling(this.recipeLogic.isDistinct()
                                    ? withButton(new TextComponentTranslation("gtadditions.multiblock.universal.distinct.yes"), "distinctEnabled")
                                    : withButton(new TextComponentTranslation("gtadditions.multiblock.universal.distinct.no"), "distinctDisabled"))))
                    .isWorking(this.recipeLogic.isWorkingEnabled(), this.recipeLogic.isActive(), this.recipeLogic.getProgress(), this.recipeLogic.getMaxProgress());
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        this.recipeLogic.setDistinct(!componentData.equals("distinctEnabled"));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.importItemInventory = new ItemHandlerList(this.getAbilities(IMPORT_ITEMS));
        this.exportItemInventory = new ItemHandlerList(this.getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.energyContainer = new EnergyContainerList(this.getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.recipeLogic.initialize(this.getAbilities(IMPORT_ITEMS).size());
        int conveyor = context.getOrDefault("Conveyor", ConveyorCasing.CasingType.CONVEYOR_LV).getTier();
        int robotArm = context.getOrDefault("RobotArm", RobotArmCasing.CasingType.ROBOT_ARM_LV).getTier();
        int min = Math.min(conveyor, robotArm);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
        this.parallel = TJConfig.largeCrafter.stack * min;
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (this.getNumProblems() < 6)
            this.recipeLogic.update();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RIGHT, UP, BACK)
                .aisle("XXXXX", "FXXXF", "FXXXF", "FXXXF", "~XXX~")
                .aisle("XXXXX", "G#C#G", "GR#RG", "F#C#F", "~XXX~")
                .aisle("XXXXX", "G#C#G", "GR#RG", "F#C#F", "~XXX~")
                .aisle("XXXXX", "G#C#G", "GR#RG", "F#C#F", "~XXX~")
                .aisle("XXXXX", "FXXXF", "FXSXF", "FXXXF", "~XXX~")
                .where('S', this.selfPredicate())
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Steel).getDefaultState()))
                .where('G', glassPredicate())
                .where('C', conveyorPredicate())
                .where('R', robotArmPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.STEEL_SOLID);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return Textures.SOLID_STEEL_CASING;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.getFrontFacing(), this.recipeLogic.isActive(), this.recipeLogic.hasProblem(), this.recipeLogic.isWorkingEnabled());
    }

    @Override
    public Int2ObjectMap<IRecipe> getRecipeMap() {
        return null;
    }

    @Override
    public void clearRecipeCache() {
        this.recipeLogic.clearCache();
    }
}
