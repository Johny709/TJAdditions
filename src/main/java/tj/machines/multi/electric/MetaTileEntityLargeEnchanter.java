package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAUtility;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.Widget;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.TJConfig;
import tj.builder.handlers.EnchanterWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.textures.TJTextures;
import tj.util.EnumFacingHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.fieldGenPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.sensorPredicate;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;


public class MetaTileEntityLargeEnchanter extends TJMultiblockDisplayBase {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, IMPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH};
    private final EnchanterWorkableHandler workableHandler = new EnchanterWorkableHandler(this);
    private IItemHandlerModifiable itemInputs;
    private IItemHandlerModifiable itemOutputs;
    private IMultipleTankHandler fluidInputs;
    private IEnergyContainer energyInput;
    private long maxVoltage;
    private int parallel;

    public MetaTileEntityLargeEnchanter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.workableHandler.setImportItems(() -> this.itemInputs);
        this.workableHandler.setExportItems(() -> this.itemOutputs);
        this.workableHandler.setImportFluids(() -> this.fluidInputs);
        this.workableHandler.setImportEnergy(() -> this.energyInput);
        this.workableHandler.setInputBus(this::getInputBus);
        this.workableHandler.setMaxVoltage(() -> this.maxVoltage);
        this.workableHandler.setParallel(() -> parallel);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeEnchanter(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.4", TJConfig.largeEnchanter.stack));
        tooltip.add(I18n.format("tj.multiblock.large_enchanter.description"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed())
            MultiblockDisplayBuilder.start(textList)
                    .voltageIn(this.energyInput)
                    .voltageTier(GAUtility.getTierByVoltage(this.maxVoltage))
                    .energyInput(!this.workableHandler.hasNotEnoughEnergy(), this.workableHandler.getEUt())
                    .addTranslation("tj.multiblock.industrial_fusion_reactor.message", this.parallel)
                    .custom(text -> text.add(new TextComponentTranslation("gtadditions.multiblock.universal.distinct")
                            .appendText(" ")
                            .appendSibling(this.workableHandler.isDistinct()
                                    ? withButton(new TextComponentTranslation("gtadditions.multiblock.universal.distinct.yes"), "distinctEnabled")
                                    : withButton(new TextComponentTranslation("gtadditions.multiblock.universal.distinct.no"), "distinctDisabled"))))
                    .isWorking(this.workableHandler.isWorkingEnabled(), this.workableHandler.isActive(), this.workableHandler.getProgress(), this.workableHandler.getMaxProgress());
    }

    @Override
    protected void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        this.workableHandler.setDistinct(!componentData.equals("distinctEnabled"));
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (this.getNumProblems() < 6)
            this.workableHandler.update();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start(RIGHT, FRONT, DOWN)
                .aisle("~DDD~", "DXXXD", "DXXXD", "DXXXD", "~DDD~")
                .aisle("DBBBD", "B###B", "B#s#B", "B###B", "DBBBD")
                .aisle("DBSBD", "B#s#B", "BsFsB", "B#s#B", "DBBBD")
                .aisle("DBBBD", "B###B", "B#s#B", "B###B", "DBBBD")
                .aisle("~DDD~", "DXXXD", "DXXXD", "DXXXD", "~DDD~")
                .where('S', this.selfPredicate())
                .where('X', blockPredicate(Blocks.OBSIDIAN).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('D', blockPredicate(Blocks.DIAMOND_BLOCK))
                .where('B', this::bookshelfPredicate)
                .where('F', fieldGenPredicate())
                .where('s', sensorPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private boolean bookshelfPredicate(BlockWorldState blockWorldState) {
        Block block = blockWorldState.getBlockState().getBlock();
        return block == Block.getBlockFromName("apotheosis:hellshelf");
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int fieldGen = context.getOrDefault("FieldGen", ConveyorCasing.CasingType.CONVEYOR_LV).getTier();
        int sensor = context.getOrDefault("Sensor", RobotArmCasing.CasingType.ROBOT_ARM_LV).getTier();
        int min = Math.min(fieldGen, sensor);
        this.itemInputs = new ItemHandlerList(this.getAbilities(IMPORT_ITEMS));
        this.itemOutputs = new ItemHandlerList(this.getAbilities(EXPORT_ITEMS));
        this.fluidInputs = new FluidTankList(true, this.getAbilities(IMPORT_FLUIDS));
        this.energyInput = new EnergyContainerList(this.getAbilities(INPUT_ENERGY));
        this.workableHandler.initialize(this.getAbilities(IMPORT_ITEMS).size());
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
        this.parallel = TJConfig.largeEnchanter.stack * min;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return TJTextures.OBSIDIAN;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.frontFacing, this.workableHandler.isActive(), this.workableHandler.hasProblem(), this.workableHandler.isWorkingEnabled());
        TJTextures.ENCHANTED_BOOK.renderSided(this.frontFacing.getOpposite(), renderState, translation, pipeline);
        TJTextures.ENCHANTED_BOOK.renderSided(EnumFacingHelper.getLeftFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.ENCHANTED_BOOK.renderSided(EnumFacingHelper.getRightFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.ENCHANTING_TABLE.renderSided(EnumFacingHelper.getTopFacingFrom(this.frontFacing), renderState, translation, pipeline);
    }

    private IItemHandlerModifiable getInputBus(int index) {
        return this.getAbilities(IMPORT_ITEMS).get(index);
    }
}
