package tj.machines.multi.parallel;

import gregicadditions.GAUtility;
import gregicadditions.client.ClientHandler;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.item.metal.MetalCasing2;
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
import gregtech.api.render.Textures;
import gregtech.common.blocks.BlockBoilerCasing;
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
import tj.TJConfig;
import tj.builder.ParallelRecipeMap;
import tj.builder.handlers.ParallelElectricBlastFurnaceRecipeLogic;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static gregicadditions.capabilities.GregicAdditionsCapabilities.MAINTENANCE_HATCH;
import static gregicadditions.machines.multi.TileEntityAlloyBlastFurnace.heatingCoilPredicate;
import static gregicadditions.machines.multi.override.MetaTileEntityElectricBlastFurnace.heatingCoilPredicate2;
import static gregicadditions.recipes.GARecipeMaps.BLAST_ALLOY_RECIPES;
import static gregtech.api.metatileentity.multiblock.MultiblockAbility.*;
import static gregtech.api.multiblock.BlockPattern.RelativeDirection.*;
import static tj.TJRecipeMaps.PARALLEL_BLAST_ALLOY_RECIPES;
import static tj.multiblockpart.TJMultiblockAbility.REDSTONE_CONTROLLER;


public class MetaTileEntityParallelAlloyBlastSmelter extends ParallelRecipeMapMultiblockController {

    private int blastFurnaceTemperature;
    private int bonusTemperature;
    private static final MultiblockAbility<?>[] ALLOWED_ABILITIES = {IMPORT_ITEMS, IMPORT_FLUIDS, EXPORT_FLUIDS, INPUT_ENERGY, MAINTENANCE_HATCH, REDSTONE_CONTROLLER};

    public MetaTileEntityParallelAlloyBlastSmelter(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_BLAST_ALLOY_RECIPES});
        this.recipeMapWorkable = new ParallelElectricBlastFurnaceRecipeLogic(this, () -> this.blastFurnaceTemperature);
        this.recipeMapWorkable.setMaxVoltage(this::getMaxVoltage);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelAlloyBlastSmelter(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gtadditions.multiblock.universal.tooltip.1", BLAST_ALLOY_RECIPES.getLocalizedName()));
        tooltip.add(I18n.format("tj.multiblock.parallel.tooltip.2", this.getMaxParallel()));
        tooltip.add(I18n.format("tj.multiblock.parallel.description"));
        tooltip.add(I18n.format("gtadditions.multiblock.electric_blast_furnace.tooltip.1"));
        tooltip.add(I18n.format("gtadditions.multiblock.electric_blast_furnace.tooltip.2"));
        tooltip.add(I18n.format("gtadditions.multiblock.electric_blast_furnace.tooltip.3"));
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.blast_furnace.max_temperature", this.blastFurnaceTemperature));
            textList.add(new TextComponentTranslation("gtadditions.multiblock.blast_furnace.additional_temperature", this.bonusTemperature));
        }
    }

    @Override
    protected BlockPattern createStructurePattern() {
        FactoryBlockPattern factoryPattern = FactoryBlockPattern.start(RIGHT, FRONT, DOWN);
        for (int layer = 0; layer < this.parallelLayer; layer++) {
            if (layer % 4 == 0) {
                String mufflerMM = layer == 0 ? "XXXXXXX" : "XXXPXXX";
                factoryPattern.aisle("~XXXXX~", "XXXXXXX", "XXXXXXX", mufflerMM, "XXXXXXX", "XXXXXXX", "~XXXXX~");
                factoryPattern.aisle("~AAAAA~", "ACCCCCA", "AC#C#CA", "ACCPCCA", "AC#C#CA", "ACCCCCA", "~AAAAA~");
                factoryPattern.aisle("~AAAAA~", "ACCCCCA", "AC#C#CA", "ACCPCCA", "AC#C#CA", "ACCCCCA", "~AAAAA~");
            }
        }
        return factoryPattern.aisle("~XXSXX~", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "~XXXXX~")
                .where('S', this.selfPredicate())
                .where('X', statePredicate(this.getCasingState()).or(abilityPartPredicate(ALLOWED_ABILITIES)))
                .where('A', statePredicate(GAMetaBlocks.METAL_CASING_2.getState(MetalCasing2.CasingType.STABALLOY)))
                .where('P', statePredicate(MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.TUNGSTENSTEEL_PIPE)))
                .where('C', heatingCoilPredicate().or(heatingCoilPredicate2()))
                .where('#', isAirPredicate())
                .where('~', tile -> true)
                .build();
    }

    private IBlockState getCasingState() {
        return GAMetaBlocks.METAL_CASING_1.getState(MetalCasing1.CasingType.ZIRCONIUM_CARBIDE);
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.ZIRCONIUM_CARBIDE_CASING;
    }

    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.PRIMITIVE_BLAST_FURNACE_OVERLAY;
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.maxVoltage = this.getAbilities(INPUT_ENERGY).stream()
                .mapToLong(IEnergyContainer::getInputVoltage)
                .max()
                .orElse(0);
        long amps = this.getAbilities(INPUT_ENERGY).stream()
                .filter(energy -> energy.getInputVoltage() == this.maxVoltage)
                .mapToLong(IEnergyContainer::getInputAmperage)
                .sum() / this.parallelLayer;
        amps = Math.min(1024, amps);
        while (amps >= 4) {
            amps /= 4;
            this.maxVoltage *= 4;
        }
        int energyTier = GAUtility.getTierByVoltage(this.maxVoltage);
        this.bonusTemperature = Math.max(0, 100 * (energyTier - 2));
        this.blastFurnaceTemperature = context.getOrDefault("blastFurnaceTemperature", 0);
        this.blastFurnaceTemperature += this.bonusTemperature;
    }

    @Override
    public int getMaxParallel() {
        return TJConfig.parallelAlloyBlastSmelter.maximumParallel;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{BLAST_ALLOY_RECIPES};
    }
}
