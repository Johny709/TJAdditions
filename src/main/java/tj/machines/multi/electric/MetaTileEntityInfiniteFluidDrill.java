package tj.machines.multi.electric;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregicadditions.capabilities.GregicAdditionsCapabilities;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.components.PumpCasing;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.AdvancedTextWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MTETrait;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import tj.blocks.BlockSolidCasings;
import tj.blocks.TJMetaBlocks;
import tj.builder.handlers.InfiniteFluidDrillWorkableHandler;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;
import tj.builder.multicontrollers.TJMultiblockDisplayBase;
import tj.gui.TJWidgetGroup;
import tj.textures.TJTextures;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static gregicadditions.GAMaterials.*;
import static net.minecraft.util.text.TextFormatting.RED;

public class MetaTileEntityInfiniteFluidDrill extends TJMultiblockDisplayBase {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {MultiblockAbility.IMPORT_FLUIDS, MultiblockAbility.EXPORT_FLUIDS,
            MultiblockAbility.INPUT_ENERGY, GregicAdditionsCapabilities.MAINTENANCE_HATCH};
    private final InfiniteFluidDrillWorkableHandler fluidDrillWorkableHandler = new InfiniteFluidDrillWorkableHandler(this, () -> this.inputFluid, () -> this.outputFluid, () -> this.energyContainer, () -> this.maxVoltage);
    private long maxVoltage;
    private int tier;
    private IMultipleTankHandler outputFluid;
    private IMultipleTankHandler inputFluid;
    private EnergyContainerList energyContainer;

    public MetaTileEntityInfiniteFluidDrill(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityInfiniteFluidDrill(this.metaTileEntityId);
    }

    @Override
    protected boolean checkStructureComponents(List<IMultiblockPart> parts, Map<MultiblockAbility<Object>, List<Object>> abilities) {
        int fluidInputsCount = abilities.getOrDefault(MultiblockAbility.IMPORT_FLUIDS, Collections.emptyList()).size();
        int fluidOutputsCount = abilities.getOrDefault(MultiblockAbility.EXPORT_FLUIDS, Collections.emptyList()).size();
        int maintenanceCount = abilities.getOrDefault(GregicAdditionsCapabilities.MAINTENANCE_HATCH, Collections.emptyList()).size();

        return maintenanceCount == 1 &&
                fluidInputsCount >= 1 &&
                fluidOutputsCount >= 1 &&
                abilities.containsKey(MultiblockAbility.INPUT_ENERGY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        int drillingMud = (int) Math.pow(4, (9 - GAValues.EV)) * 10;
        int outputFluid = (int) Math.pow(4, (9 - GAValues.EV)) * 4000;
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.1"));
        tooltip.add(I18n.format("tj.multiblock.drilling_rig.voltage", GAValues.VN[9], GAValues.VN[14]));
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.void.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.drilling_rig.tooltip.4", outputFluid, GAValues.VN[9]));
        tooltip.add(I18n.format("tj.multiblock.drilling_rig.tooltip.drilling_mud", drillingMud, drillingMud));
        tooltip.add(I18n.format("gtadditions.multiblock.large_chemical_reactor.tooltip.1"));
    }

    @Override
    protected void addNewTabs(Consumer<Triple<String, ItemStack, AbstractWidgetGroup>> tabs, int extended) {
        super.addNewTabs(tabs, extended);
        TJWidgetGroup widgetFluidGroup = new TJWidgetGroup();
        tabs.accept(new ImmutableTriple<>("tj.multiblock.tab.fluid", new ItemStack(Items.WATER_BUCKET), fluidsTab(widgetFluidGroup::addWidgets)));
    }

    private AbstractWidgetGroup fluidsTab(Function<Widget, WidgetGroup> widgetGroup) {
        return widgetGroup.apply(new AdvancedTextWidget(10, -2, this::addFluidDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (!this.isStructureFormed())
            return;

        if (this.fluidDrillWorkableHandler.getVeinFluid() == null) {
            textList.add(new TextComponentTranslation("gtadditions.multiblock.drilling_rig.no_fluid").setStyle(new Style().setColor(RED)));
            return;
        }

        MultiblockDisplayBuilder.start(textList)
                .voltageIn(this.energyContainer)
                .voltageTier(this.tier)
                .energyInput(!this.fluidDrillWorkableHandler.hasNotEnoughEnergy(), this.maxVoltage)
                .addTranslation("gtadditions.multiblock.drilling_rig.fluid", this.fluidDrillWorkableHandler.getVeinFluid().getName())
                .isWorking(this.fluidDrillWorkableHandler.isWorkingEnabled(), this.fluidDrillWorkableHandler.isActive(), this.fluidDrillWorkableHandler.getProgress(), this.fluidDrillWorkableHandler.getMaxProgress());
    }

    private void addFluidDisplayText(List<ITextComponent> textList) {
        FluidStack drillingMud = DrillingMud.getFluid(this.fluidDrillWorkableHandler.getDrillingMudAmount());
        List<FluidStack> fluidOutputs = this.fluidDrillWorkableHandler.getFluidOutputs();

        MultiblockDisplayBuilder builder = new MultiblockDisplayBuilder(textList);
        builder.fluidInput(this.fluidDrillWorkableHandler.hasEnoughFluid(drillingMud, this.fluidDrillWorkableHandler.getDrillingMudAmount()), drillingMud);
        int index = 0;
        for (FluidStack fluidOutput : fluidOutputs) {
            int amount = fluidOutput.isFluidEqual(UsedDrillingMud.getFluid(this.fluidDrillWorkableHandler.getDrillingMudAmount()))
                    ? this.fluidDrillWorkableHandler.getDrillingMudAmount()
                    : this.fluidDrillWorkableHandler.getOutputVeinFluidAmount()[index++];
            builder.fluidOutput(this.fluidDrillWorkableHandler.canOutputFluid(fluidOutput, amount), fluidOutput);
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.energyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.INPUT_ENERGY));
        this.inputFluid = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputFluid = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));

        int motorTier = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        int pumpTier = context.getOrDefault("Pump", PumpCasing.CasingType.PUMP_LV).getTier();
        this.tier = Math.min(motorTier, pumpTier);
        this.maxVoltage = GAValues.VA[this.tier];
        this.fluidDrillWorkableHandler.initialize(this.tier);
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    protected void updateFormedValid() {
        if (this.tier > GAValues.UV && this.getNumProblems() < 6 && this.fluidDrillWorkableHandler.getVeinFluid() != null)
            this.fluidDrillWorkableHandler.update();
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("CF~FC", "CF~FC", "CCCCC", "~XXX~", "~~C~~", "~~C~~", "~~C~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("F~~~F", "F~~~F", "CCMCC", "X###X", "~C#C~", "~C#C~", "~C#C~", "~FCF~", "~FFF~", "~FFF~", "~~F~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("~~~~~", "~~~~~", "CMPMC", "X#T#X", "C#T#C", "C#T#C", "C#T#C", "~CCC~", "~FCF~", "~FFF~", "~FFF~", "~~F~~", "~~F~~", "~~F~~")
                .aisle("F~~~F", "F~~~F", "CCMCC", "X###X", "~C#C~", "~C#C~" ,"~C#C~", "~FCF~", "~FFF~", "~FFF~", "~~F~~", "~~~~~", "~~~~~", "~~~~~")
                .aisle("CF~FC", "CF~FC", "CCCCC", "~XSX~", "~~C~~", "~~C~~", "~~C~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('F', statePredicate(MetaBlocks.FRAMES.get(Seaborgium).getDefaultState()))
                .where('T', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('M', LargeSimpleRecipeMapMultiblockController.motorPredicate())
                .where('P', LargeSimpleRecipeMapMultiblockController.pumpPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return TJMetaBlocks.SOLID_CASING.getState(BlockSolidCasings.SolidCasingType.SEABORGIUM_CASING);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return TJTextures.SEABORGIUM;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.frontFacing, this.fluidDrillWorkableHandler.isActive(), this.fluidDrillWorkableHandler.hasProblem(), this.fluidDrillWorkableHandler.isWorkingEnabled());
    }
}
