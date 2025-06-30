package tj.machines.multi.parallel;

import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.components.ConveyorCasing;
import gregicadditions.item.components.RobotArmCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
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

import static tj.TJRecipeMaps.PARALLEL_PACKER_RECIPES;
import static tj.TJRecipeMaps.PARALLEL_UNPACKER_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.conveyorPredicate;
import static gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.robotArmPredicate;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.PACKER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.UNPACKER_RECIPES;
import static gregtech.api.render.Textures.PACKER_OVERLAY;
import static gregtech.api.render.Textures.UNPACKER_OVERLAY;

public class MetaTileEntityParallelLargePackager extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, MAINTENANCE_HATCH, INPUT_ENERGY, REDSTONE_CONTROLLER};
    private static final DecimalFormat formatter = new DecimalFormat("#0.00");

    public MetaTileEntityParallelLargePackager(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_PACKER_RECIPES, PARALLEL_UNPACKER_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, TJConfig.parallelLargePackager.eutPercentage, TJConfig.parallelLargePackager.durationPercentage,
                TJConfig.parallelLargePackager.chancePercentage, TJConfig.parallelLargePackager.stack) {
            @Override
            protected long getMaxVoltage() {
                return this.controller.getMaxVoltage();
            }
        };
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargePackager(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1",
                PACKER_RECIPES.getLocalizedName() + ", " + UNPACKER_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelLargePackager.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelLargePackager.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelLargePackager.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.5", TJConfig.parallelLargePackager.chancePercentage));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, UP, BACK);
        factoryPattern.aisle("XXX", "XXX", "XXX");
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            factoryPattern.aisle("XXX", "XCX", "XRX");
        }
        return factoryPattern.aisle("XXX", "XSX", "XXX")
                .where('S', this.selfPredicate())
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('C', conveyorPredicate())
                .where('R', robotArmPredicate())
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.HG_1223);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        int conveyor = context.getOrDefault("Conveyor", ConveyorCasing.CasingType.CONVEYOR_LV).getTier();
        int robotArm = context.getOrDefault("RobotArm", RobotArmCasing.CasingType.ROBOT_ARM_LV).getTier();
        int min = Math.min(conveyor, robotArm);
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.HG_1223_CASING;
    }

    @Override
    protected @NotNull OrientedOverlayRenderer getFrontOverlay() {
        return this.getRecipeMapIndex() == 0 ? PACKER_OVERLAY : UNPACKER_OVERLAY;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelLargePackager.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return GATileEntities.LARGE_PACKAGER.getRecipeMaps();
    }
}
