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

import static com.johny.tj.TJRecipeMaps.PARALLEL_PACKER_RECIPES;
import static com.johny.tj.TJRecipeMaps.PARALLEL_UNPACKER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.PACKER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.UNPACKER_RECIPES;

public class MetaTileEntityParallelLargePacker extends ParallelRecipeMapMultiblockController {

    public MetaTileEntityParallelLargePacker(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new ParallelRecipeMap[]{PARALLEL_PACKER_RECIPES, PARALLEL_UNPACKER_RECIPES});
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityParallelLargePacker(this.metaTileEntityId);
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
        return new RecipeMap[]{PACKER_RECIPES, UNPACKER_RECIPES};
    }
}
