package tj.integration.jei;

import gregicadditions.Gregicality;
import gregtech.api.GregTechAPI;
import gregtech.api.gui.impl.ModularUIGui;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.RecipeMap;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import net.minecraft.util.ResourceLocation;
import tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import tj.integration.jei.recipe.GTRecipeTransferGuiHandler;

import java.util.ArrayList;
import java.util.List;

import static tj.machines.TJMetaTileEntities.INFINITE_FLUID_DRILL;

@mezz.jei.api.JEIPlugin
public class TJJEIPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        TJMultiblockInfoCategory.registerRecipes(registry);

        registry.addRecipeCatalyst(INFINITE_FLUID_DRILL.getStackForm(), Gregicality.MODID + ":drilling_rig");
        List<String> recipeCategoriesUIds = new ArrayList<>();

        for (ResourceLocation metaTileEntityId : GregTechAPI.META_TILE_ENTITY_REGISTRY.getKeys()) {
            MetaTileEntity metaTileEntity = GregTechAPI.META_TILE_ENTITY_REGISTRY.getObject(metaTileEntityId);
            if (metaTileEntity instanceof ParallelRecipeMapMultiblockController) {
                for (RecipeMap<?> recipeMap : ((ParallelRecipeMapMultiblockController) metaTileEntity).getRecipeMaps()) {
                    String recipeName = Gregicality.MODID + ":" + recipeMap.unlocalizedName;
                    registry.addRecipeCatalyst(metaTileEntity.getStackForm(), recipeName);
                    GTRecipeTransferGuiHandler gtRecipeTransferGuiHandler = new GTRecipeTransferGuiHandler(jeiHelpers.recipeTransferHandlerHelper());
                    registry.getRecipeTransferRegistry().addRecipeTransferHandler(gtRecipeTransferGuiHandler, recipeName);
                    recipeCategoriesUIds.add(recipeName);
                }
            }
        }
        String[] UIds = new String[recipeCategoriesUIds.size()];
        registry.addRecipeClickArea(ModularUIGui.class, 0, -20, 190, 20, recipeCategoriesUIds.toArray(UIds));

        TJMultiblockInfoCategory.getMultiblockRecipes().values().forEach(v -> {
            MultiblockInfoPage infoPage = v.getInfoPage();
            registry.addIngredientInfo(infoPage.getController().getStackForm(),
                    VanillaTypes.ITEM,
                    infoPage.getDescription());
        });
    }
}
