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

import static com.johny.tj.TJRecipeMaps.PARALLEL_WIREMILL_RECIPES;
import static gregtech.api.recipes.RecipeMaps.WIREMILL_RECIPES;

public class MetaTileEntityParallelLargeWiremill extends ParallelRecipeMapMultiblockController {

    public MetaTileEntityParallelLargeWiremill(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_WIREMILL_RECIPES});
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargeWiremill(this.metaTileEntityId);
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
        return new RecipeMap[]{WIREMILL_RECIPES};
    }
}
