package tj.mixin.gregicality;

import gregicadditions.machines.multi.TileEntityFusionReactor;
import gregtech.api.GTValues;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.recipes.RecipeMap;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tj.blocks.EnergyPortCasings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = TileEntityFusionReactor.class, remap = false)
public abstract class TileEntityFusionReactorMixin extends RecipeMapMultiblockController implements ITileEntityFusionReactorMixin {

    public TileEntityFusionReactorMixin(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId, recipeMap);
    }

    @Shadow
    protected abstract IBlockState getCasingState();

    @Redirect(method = "createStructurePattern", at = @At(value = "INVOKE", target = "Lgregtech/api/multiblock/FactoryBlockPattern;where(CLjava/util/function/Predicate;)Lgregtech/api/multiblock/FactoryBlockPattern;", ordinal = 4))
    private FactoryBlockPattern redirectCreateStructurePattern(FactoryBlockPattern pattern, char symbol, Predicate<BlockWorldState> blockMatcher) {
        pattern.where('E', MultiblockControllerBase.statePredicate(getCasingState()).or(MultiblockControllerBase.tilePredicate((state, tile) -> {
            for (int i = getTier(); i < GTValues.V.length; i++) {
                if (tile.metaTileEntityId.equals(MetaTileEntities.ENERGY_INPUT_HATCH[i].metaTileEntityId))
                    return true;
            }
            return false;
        })).or(energyPortPredicate(getTier())));
        return pattern;
    }

    @Unique
    public Predicate<BlockWorldState> energyPortPredicate(int tier) {
        return (blockWorldState) -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (blockState.getBlock() instanceof EnergyPortCasings) {
                EnergyPortCasings abilityCasings = (EnergyPortCasings) blockState.getBlock();
                EnergyPortCasings.AbilityType tieredCasingType = abilityCasings.getState(blockState);
                List<EnergyPortCasings.AbilityType> currentCasing = blockWorldState.getMatchContext().getOrCreate("EnergyPort", ArrayList::new);
                currentCasing.add(tieredCasingType);
                return currentCasing.get(0).getName().equals(tieredCasingType.getName()) && currentCasing.get(0).getTier() >= tier;
            }
            return false;
        };
    }
}
