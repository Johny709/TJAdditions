package tj.machines.multi.parallel;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.render.ICubeRenderer;
import net.minecraft.util.ResourceLocation;
import tj.builder.ParallelRecipeMap;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;

public class MetaTileEntityParallelVacuumFreezer extends ParallelRecipeMapMultiblockController {

    public MetaTileEntityParallelVacuumFreezer(ResourceLocation metaTileEntityId, ParallelRecipeMap[] recipeMap) {
        super(metaTileEntityId, recipeMap);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return null;
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
        return new RecipeMap[0];
    }
}
