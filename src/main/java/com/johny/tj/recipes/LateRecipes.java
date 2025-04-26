package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.util.ResourceLocation;

import static com.johny.tj.machines.TJMetaTileEntities.COKE_OVEN;
import static com.johny.tj.machines.TJMetaTileEntities.MEGA_COKE_OVEN;
import static gregtech.api.recipes.RecipeMaps.ASSEMBLER_RECIPES;

public class LateRecipes {

    public static void init() {
        MetaTileEntity cokeOven = TJConfig.machines.replaceCTMultis ? COKE_OVEN : GregTechAPI.META_TILE_ENTITY_REGISTRY.getObject(new ResourceLocation("multiblocktweaker", "coke_oven_2"));
        ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(cokeOven.getStackForm(64))
                .inputs(cokeOven.getStackForm(64))
                .inputs(cokeOven.getStackForm(64))
                .inputs(cokeOven.getStackForm(64))
                .outputs(MEGA_COKE_OVEN.getStackForm(1))
                .EUt(30)
                .duration(1200)
                .buildAndRegister();
    }
}
