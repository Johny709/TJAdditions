package com.johny.tj.integration.jei;

import com.google.common.collect.ImmutableMap;
import com.johny.tj.TJ;
import com.johny.tj.TJConfig;
import com.johny.tj.integration.jei.multi.*;
import com.johny.tj.machines.TJMetaTileEntities;
import gregtech.integration.jei.multiblock.MultiblockInfoRecipeWrapper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.gui.recipes.RecipeLayout;
import net.minecraft.client.resources.I18n;


public class TJMultiblockInfoCategory implements IRecipeCategory<MultiblockInfoRecipeWrapper> {
    private final IDrawable background;
    private final IGuiHelper guiHelper;
    private static ImmutableMap<String, MultiblockInfoRecipeWrapper> multiblockRecipes;

    public TJMultiblockInfoCategory(IJeiHelpers helpers) {
        this.guiHelper = helpers.getGuiHelper();
        this.background = this.guiHelper.createBlankDrawable(176, 166);
    }

    public static ImmutableMap<String, MultiblockInfoRecipeWrapper> getMultiblockRecipes() {
        if (multiblockRecipes == null) {
            ImmutableMap.Builder<String, MultiblockInfoRecipeWrapper> multiblockRecipes = new ImmutableMap.Builder<>();

                    if (TJConfig.machines.replaceCTMultis) {
                    multiblockRecipes.put("primitive_alloy", new MultiblockInfoRecipeWrapper(new PrimitiveAlloyInfo()))
                                .put("coke_oven", new MultiblockInfoRecipeWrapper(new CokeOvenInfo()))
                                .put("heat_exchanger", new MultiblockInfoRecipeWrapper(new HeatExchangerInfo()))
                                .put("armor_infuser", new MultiblockInfoRecipeWrapper(new ArmorInfuserInfo()))
                                .put("mega_coke_oven", new MultiblockInfoRecipeWrapper(new MegaCokeOvenInfo()))
                                .put("dragon_egg_replicator", new MultiblockInfoRecipeWrapper(new DragonReplicatorInfo()))
                                .put("chaos_replicator", new MultiblockInfoRecipeWrapper(new ChaosReplicatorInfo()))
                                .put("large_powered_spawner", new MultiblockInfoRecipeWrapper(new LargePoweredSpawnerInfo()))
                                .put("large_vial_processor", new MultiblockInfoRecipeWrapper(new LargeVialProcessorInfo()));
                    }

                    multiblockRecipes.put("mega_boiler", new MultiblockInfoRecipeWrapper(new MegaBoilerInfo()))
                    .put("xl_turbine.steam", new MultiblockInfoRecipeWrapper(new XLTurbineInfo(TJMetaTileEntities.XL_STEAM_TURBINE)))
                    .put("xl_turbine.gas", new MultiblockInfoRecipeWrapper(new XLTurbineInfo(TJMetaTileEntities.XL_GAS_TURBINE)))
                    .put("xl_turbine.plasma", new MultiblockInfoRecipeWrapper(new XLTurbineInfo(TJMetaTileEntities.XL_PLASMA_TURBINE)))
                    .put("xl_turbine.coolant", new MultiblockInfoRecipeWrapper(new XLHotCoolantTurbineInfo(TJMetaTileEntities.XL_COOLANT_TURBINE)))
                    .put("large_decay_chamber", new MultiblockInfoRecipeWrapper(new LargeDecayChamberInfo()))
                    .put("large_alloy_smelter", new MultiblockInfoRecipeWrapper(new LargeAlloySmelterInfo()))
                    .put("industrial_fusion_reactor.uv", new MultiblockInfoRecipeWrapper(new IndustrialFusionReactorInfo()))
                    .put("parallel_chemical_reactor", new MultiblockInfoRecipeWrapper(new ParallelChemicalReactorInfo()))
                    .put("large_greenhouse", new MultiblockInfoRecipeWrapper(new LargeGreenhouseInfo()));

                    return TJMultiblockInfoCategory.multiblockRecipes = multiblockRecipes.build();
        }
        return multiblockRecipes;
    }

    public static void registerRecipes(IModRegistry registry) {
        registry.addRecipes(getMultiblockRecipes().values(), "gregtech:multiblock_info");
    }
    @Override
    public String getUid() {
        return "tj:multiblock_info";
    }
    @Override
    public String getTitle() {
        return I18n.format("gregtech.multiblock.title");
    }
    @Override
    public String getModName() {
        return TJ.MODID;
    }
    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, MultiblockInfoRecipeWrapper recipeWrapper, IIngredients ingredients) {
        recipeWrapper.setRecipeLayout((RecipeLayout) iRecipeLayout, guiHelper);

        IGuiItemStackGroup itemStackGroup = iRecipeLayout.getItemStacks();
        itemStackGroup.addTooltipCallback(recipeWrapper::addBlockTooltips);
    }
}
