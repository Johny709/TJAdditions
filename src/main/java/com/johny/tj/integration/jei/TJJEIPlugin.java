package com.johny.tj.integration.jei;

import com.johny.tj.builder.multicontrollers.ParallelRecipeMapMultiblockController;
import gregicadditions.Gregicality;
import gregtech.api.GregTechAPI;
import gregtech.api.gui.impl.ModularUIGuiHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.RecipeMap;
import gregtech.integration.jei.multiblock.MultiblockInfoPage;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import net.minecraft.util.ResourceLocation;

import static com.johny.tj.machines.TJMetaTileEntities.INFINITE_FLUID_DRILL;

@mezz.jei.api.JEIPlugin
public class TJJEIPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();

        TJMultiblockInfoCategory.registerRecipes(registry);

        ModularUIGuiHandler modularUIGuiHandler = new ModularUIGuiHandler(jeiHelpers.recipeTransferHandlerHelper());
        registry.addAdvancedGuiHandlers(modularUIGuiHandler);
        registry.addGhostIngredientHandler(modularUIGuiHandler.getGuiContainerClass(), modularUIGuiHandler);

        registry.addRecipeCatalyst(INFINITE_FLUID_DRILL.getStackForm(), Gregicality.MODID + ":drilling_rig");

        for (ResourceLocation metaTileEntityId : GregTechAPI.META_TILE_ENTITY_REGISTRY.getKeys()) {
            MetaTileEntity metaTileEntity = GregTechAPI.META_TILE_ENTITY_REGISTRY.getObject(metaTileEntityId);
            if (metaTileEntity instanceof ParallelRecipeMapMultiblockController) {
                for (RecipeMap<?> recipeMap : ((ParallelRecipeMapMultiblockController) metaTileEntity).getRecipeMaps()) {
                    String recipeName = recipeMap.unlocalizedName;
                    registry.addRecipeCatalyst(metaTileEntity.getStackForm(), Gregicality.MODID + ":" + recipeName);
                }
            }
        }

        TJMultiblockInfoCategory.getMultiblockRecipes().values().forEach(v -> {
            MultiblockInfoPage infoPage = v.getInfoPage();
            registry.addIngredientInfo(infoPage.getController().getStackForm(),
                    VanillaTypes.ITEM,
                    infoPage.getDescription());
        });
    }

}
