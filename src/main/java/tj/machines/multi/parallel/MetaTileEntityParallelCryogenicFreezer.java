package tj.machines.multi.parallel;

import gregicadditions.GAUtility;
import gregicadditions.GAValues;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregtech.api.capability.IEnergyContainer;
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
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.MetaBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.capability.impl.ParallelGAMultiblockRecipeLogic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static gregicadditions.GAMaterials.Cryotheum;
import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static gregtech.api.recipes.RecipeMaps.VACUUM_RECIPES;
import static tj.TJRecipeMaps.PARALLEL_VACUUM_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;


public class MetaTileEntityParallelCryogenicFreezer extends ParallelRecipeMapMultiblockController {

    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, EXPORT_ITEMS, IMPORT_FLUIDS, EXPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH, REDSTONE_CONTROLLER};
    private int cryoConsumeAmount;

    public MetaTileEntityParallelCryogenicFreezer(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_VACUUM_RECIPES});
        this.recipeMapWorkable = new ParallelGAMultiblockRecipeLogic(this, () -> TJConfig.parallelCryogenicFreezer.eutPercentage,
                () -> TJConfig.parallelCryogenicFreezer.durationPercentage, () -> TJConfig.parallelCryogenicFreezer.chancePercentage, () -> TJConfig.parallelCryogenicFreezer.stack) {

            @Override
            protected boolean drawEnergy(int recipeEUt) {
                FluidStack cryotheum = this.getInputTank().drain(Cryotheum.getFluid(cryoConsumeAmount), false);
                if (cryotheum != null && cryotheum.amount == cryoConsumeAmount)
                    this.getInputTank().drain(Cryotheum.getFluid(cryoConsumeAmount), true);
                else return false;
                return super.drawEnergy(recipeEUt);
            }
        };
        this.recipeMapWorkable.setMaxVoltage(this::getMaxVoltage);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelCryogenicFreezer(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1", VACUUM_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.2", formatter.format(TJConfig.parallelCryogenicFreezer.eutPercentage / 100.0)));
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.3", formatter.format(TJConfig.parallelCryogenicFreezer.durationPercentage / 100.0)));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.1", TJConfig.parallelCryogenicFreezer.stack));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("gregtech.multiblock.vol_cryo.description"));
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, FRONT, DOWN);
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            String entityP = layer == 0 ? "XXXXX" : "XXPXX";
            if (layer % 4 == 0) {
                String entityS = layer >= this.parallelLayer - 4 ? "~XSX~" : "~XXX~";
                factoryPattern.aisle("~XXX~", "XXXXX", entityP, "XXXXX", "~XXX~");
                factoryPattern.aisle(entityS, "X#P#X", "XPPPX", "X#P#X", "~XXX~");
            }
        }
        return factoryPattern.aisle("~XXX~", "XXXXX", "XXXXX", "XXXXX", "~XXX~")
                .setAmountAtLeast('L', 16)
                .where('S', this.selfPredicate())
                .where('L', statePredicate(this.getCasingState()))
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.INCOLOY_MA956);
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.maxVoltage = this.getAbilities(INPUT_ENERGY).stream()
                .mapToLong(IEnergyContainer::getInputVoltage)
                .filter(voltage -> voltage <= GAValues.V[7])
                .max()
                .orElse(GAValues.V[7]);
        this.cryoConsumeAmount = (int) Math.pow(2, GAUtility.getTierByVoltage(this.maxVoltage));
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.INCOLOY_MA956_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return ClientHandler.FREEZER_OVERLAY;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelCryogenicFreezer.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{VACUUM_RECIPES};
    }
}
