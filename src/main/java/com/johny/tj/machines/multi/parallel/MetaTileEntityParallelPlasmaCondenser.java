package com.johny.tj.machines.multi.parallel;

import com.johny.tj.builder.ParallelRecipeMap;
import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import net.minecraft.util.ResourceLocation;

import static com.johny.tj.TJRecipeMaps.PARALLEL_PLASMA_CONDENSER_RECIPES;
import static gregicadditions.recipes.GARecipeMaps.PLASMA_CONDENSER_RECIPES;

public class MetaTileEntityParallelPlasmaCondenser extends ParallelRecipeMapMultiblockController {

    public MetaTileEntityParallelPlasmaCondenser(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_PLASMA_CONDENSER_RECIPES});
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelPlasmaCondenser(this.metaTileEntityId);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return null;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return null;
    }

    @Override
    public RecipeMap<?>[] getRecipeMaps() {
        return new RecipeMap[]{PLASMA_CONDENSER_RECIPES};
    }
}
