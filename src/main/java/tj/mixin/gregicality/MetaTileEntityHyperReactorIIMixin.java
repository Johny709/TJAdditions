package tj.mixin.gregicality;

import gregicadditions.GAConfig;
import gregicadditions.machines.multi.GAFueledMultiblockController;
import gregicadditions.machines.multi.advance.hyper.MetaTileEntityHyperReactorII;
import gregicadditions.recipes.GARecipeMaps;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FuelRecipeLogic;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.TJConfig;
import tj.builder.handlers.TJFuelRecipeLogic;
import tj.builder.multicontrollers.MultiblockDisplayBuilder;

import javax.annotation.Nonnull;
import java.util.List;

@Mixin(value = MetaTileEntityHyperReactorII.class, remap = false)
public abstract class MetaTileEntityHyperReactorIIMixin extends GAFueledMultiblockController implements IMetaTileEntityHyperReactorIIMixin {

    @Shadow
    @Nonnull
    protected abstract FluidStack getBooster();

    public MetaTileEntityHyperReactorIIMixin(ResourceLocation metaTileEntityId, long maxVoltage) {
        super(metaTileEntityId, GARecipeMaps.HYPER_REACTOR_FUELS, maxVoltage);
    }

    @Inject(method = "createWorkable", at = @At("HEAD"), cancellable = true)
    private void injectCreateWorkable(long maxVoltage, CallbackInfoReturnable<FuelRecipeLogic> cir) {
        if (TJConfig.machines.generatorWorkableHandlerOverrides) {
            MetaTileEntityHyperReactorII tileEntity = (MetaTileEntityHyperReactorII) (Object) this;
            cir.setReturnValue(new TJFuelRecipeLogic(tileEntity, this.recipeMap, this::getEnergyContainer, this::getImportFluidHandler, this::getBooster, this::getFuelMultiplier, this::getEUMultiplier, maxVoltage));
        }
    }

    @Inject(method = "addDisplayText", at = @At("HEAD"), cancellable = true)
    private void injectAddDisplayText(List<ITextComponent> textList, CallbackInfo ci) {
        if (TJConfig.machines.generatorWorkableHandlerOverrides) {
            if (!this.isStructureFormed()) {
                super.addDisplayText(textList);
                ci.cancel();
                return;
            }
            TJFuelRecipeLogic workableHandler = (TJFuelRecipeLogic) this.workableHandler;
            MultiblockDisplayBuilder.start(textList)
                    .custom(text -> {
                        FluidStack booster = importFluidHandler.drain(this.getBoosterFluid(), false);
                        FluidStack fuelStack = workableHandler.getFuelStack();
                        boolean isBoosted = workableHandler.isBoosted();
                        int boosterAmount = booster == null ? 0 : booster.amount;
                        int fuelAmount = fuelStack == null ? 0 : fuelStack.amount;

                        if (fuelStack == null)
                            text.add(new TextComponentTranslation("gregtech.multiblock.large_rocket_engine.no_fuel").setStyle(new Style().setColor(TextFormatting.RED)));
                        else text.add(new TextComponentString(String.format("%s: %dmb", fuelStack.getLocalizedName(), fuelAmount)).setStyle(new Style().setColor(TextFormatting.GREEN)));

                        if (isBoosted) {
                            text.add(new TextComponentTranslation("gregtech.multiblock.large_rocket_engine.boost").setStyle(new Style().setColor(TextFormatting.GREEN)));
                            if (booster != null)
                                text.add(new TextComponentString(String.format("%s: %dmb", booster.getLocalizedName(), boosterAmount)).setStyle(new Style().setColor(TextFormatting.AQUA)));
                        }
                        text.add(new TextComponentString(net.minecraft.util.text.translation.I18n.translateToLocalFormatted("tj.multiblock.extreme_turbine.energy", workableHandler.getProduction())));
                    }).isWorking(workableHandler.isWorkingEnabled(), workableHandler.isActive(), workableHandler.getProgress(), workableHandler.getMaxProgress());
            ci.cancel();
        }
    }

    @Unique
    private IEnergyContainer getEnergyContainer() {
        return this.energyContainer;
    }

    @Unique
    private IMultipleTankHandler getImportFluidHandler() {
        return this.importFluidHandler;
    }

    @Unique
    private int getFuelMultiplier() {
        return GAConfig.multis.hyperReactors.boostedFuelAmount[0];
    }

    @Unique
    private int getEUMultiplier() {
        return GAConfig.multis.hyperReactors.boostedEuAmount[0];
    }
}
