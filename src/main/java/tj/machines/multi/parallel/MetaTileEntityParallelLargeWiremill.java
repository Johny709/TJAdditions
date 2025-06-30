package tj.machines.multi.parallel;

import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.MotorCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.OrientedOverlayRenderer;
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockTurbineCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

import static tj.TJRecipeMaps.PARALLEL_WIREMILL_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.motorPredicate;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.WIREMILL_RECIPES;

public class MetaTileEntityParallelLargeWiremill extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, MAINTENANCE_HATCH, INPUT_ENERGY, REDSTONE_CONTROLLER};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargeWiremill(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_WIREMILL_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, TJConfig.parallelLargeWiremill.eutPercentage, TJConfig.parallelLargeWashingMachine.durationPercentage,
                TJConfig.parallelLargeWiremill.chancePercentage, TJConfig.parallelLargeWiremill.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeWiremill(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                WIREMILL_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargeWiremill.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargeWiremill.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargeWiremill.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargeWiremill.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("XXXXX", "~XXX~", "~~~~~", "~~~~~", "~~~~~");
            factoryPattern.aisle("XXXXX", "XMGMX", "C###C", "CMGMC", "CCCCC");
        }
        return factoryPattern.aisle("XXXXX", "~XSX~", "~~~~~", "~~~~~", "~~~~~")
                .where('S', this.selfPredicate())
                .where('C', statePredicate(this.getCasingState()))
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('G', statePredicate(MetaBlocks.TURBINE_CASING.getState(BlockTurbineCasing.TurbineCasingType.TITANIUM_GEARBOX)))
                .where('M', motorPredicate())
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.MARAGING_STEEL_250);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV).getTier();
        this.maxVoltage = (long) (Math.pow(4, motor) * 8);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.MARAGING_STEEL_250_CASING;
    }

    @Override
    protected @NotNull OrientedOverlayRenderer getFrontOverlay() {
        return Textures.WIREMILL_OVERLAY;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargeWiremill.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{WIREMILL_RECIPES};
    }
}
