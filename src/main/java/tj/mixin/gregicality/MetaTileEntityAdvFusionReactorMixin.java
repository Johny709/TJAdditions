package tj.mixin.gregicality;

import gregicadditions.machines.multi.advance.MetaTileEntityAdvFusionReactor;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.multiblock.BlockWorldState;
import gregtech.api.multiblock.FactoryBlockPattern;
import gregtech.api.recipes.RecipeMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tj.blocks.AdvEnergyPortCasings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = MetaTileEntityAdvFusionReactor.class, remap = false)
public abstract class MetaTileEntityAdvFusionReactorMixin extends RecipeMapMultiblockController implements IMetaTileEntityAdvFusionReactorMixin {

    public MetaTileEntityAdvFusionReactorMixin(ResourceLocation metaTileEntityId, RecipeMap<?> recipeMap) {
        super(metaTileEntityId, recipeMap);
    }

    @Redirect(method = "createStructurePattern", at = @At(value = "INVOKE", target = "Lgregtech/api/multiblock/FactoryBlockPattern;where(CLjava/util/function/Predicate;)Lgregtech/api/multiblock/FactoryBlockPattern;", ordinal = 4))
    private FactoryBlockPattern redirectCreateStructurePattern_divertor(FactoryBlockPattern pattern, char symbol, Predicate<BlockWorldState> blockMatcher) {
        pattern.where(symbol, blockMatcher.or(energyPortPredicate(getTier())));
        return pattern;
    }

    @Redirect(method = "createStructurePattern", at = @At(value = "INVOKE", target = "Lgregtech/api/multiblock/FactoryBlockPattern;where(CLjava/util/function/Predicate;)Lgregtech/api/multiblock/FactoryBlockPattern;", ordinal = 5))
    private FactoryBlockPattern redirectCreateStructurePattern_vacuum(FactoryBlockPattern pattern, char symbol, Predicate<BlockWorldState> blockMatcher) {
        pattern.where(symbol, blockMatcher.or(energyPortPredicate(getTier())));
        return pattern;
    }

    @Redirect(method = "createStructurePattern", at = @At(value = "INVOKE", target = "Lgregtech/api/multiblock/FactoryBlockPattern;where(CLjava/util/function/Predicate;)Lgregtech/api/multiblock/FactoryBlockPattern;", ordinal = 6))
    private FactoryBlockPattern redirectCreateStructurePattern_cryostat(FactoryBlockPattern pattern, char symbol, Predicate<BlockWorldState> blockMatcher) {
        pattern.where(symbol, blockMatcher.or(energyPortPredicate(getTier())));
        return pattern;
    }

    @Redirect(method = "createStructurePattern", at = @At(value = "INVOKE", target = "Lgregtech/api/multiblock/FactoryBlockPattern;where(CLjava/util/function/Predicate;)Lgregtech/api/multiblock/FactoryBlockPattern;", ordinal = 7))
    private FactoryBlockPattern redirectCreateStructurePattern_blanket(FactoryBlockPattern pattern, char symbol, Predicate<BlockWorldState> blockMatcher) {
        pattern.where(symbol, blockMatcher.or(energyPortPredicate(getTier())));
        pattern.setAmountAtMost('P', 16);
        pattern.where('P', energyPortPredicate(getTier()));
        return pattern;
    }

    @Unique
    public Predicate<BlockWorldState> energyPortPredicate(int tier) {
        return (blockWorldState) -> {
            IBlockState blockState = blockWorldState.getBlockState();
            if (blockState.getBlock() instanceof AdvEnergyPortCasings) {
                AdvEnergyPortCasings abilityCasings = (AdvEnergyPortCasings) blockState.getBlock();
                AdvEnergyPortCasings.AbilityType tieredCasingType = abilityCasings.getState(blockState);
                List<AdvEnergyPortCasings.AbilityType> currentCasing = blockWorldState.getMatchContext().getOrCreate("EnergyPort", ArrayList::new);
                currentCasing.add(tieredCasingType);
                return currentCasing.get(0).getName().equals(tieredCasingType.getName()) && currentCasing.get(0).getTier() >= tier;
            }
            return false;
        };
    }
}
