package com.johny.tj.recipes;

import com.johny.tj.TJConfig;
import com.johny.tj.blocks.*;
import com.johny.tj.items.TJMetaItems;
import com.johny.tj.recipes.ct.*;
import gregicadditions.GAValues;
import gregicadditions.item.GAMetaBlocks;
import gregicadditions.item.GATransparentCasing;
import gregicadditions.item.fusion.GAFusionCasing;
import gregicadditions.item.metal.MetalCasing1;
import gregicadditions.machines.GATileEntities;
import gregtech.api.recipes.ModHandler;
import gregtech.api.recipes.ingredients.IntCircuitIngredient;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.UnificationEntry;
import gregtech.common.blocks.BlockMachineCasing;
import gregtech.common.blocks.BlockMultiblockCasing;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.items.MetaItems;
import gregtech.common.metatileentities.MetaTileEntities;
import gregtech.common.metatileentities.electric.MetaTileEntityAirCollector;
import gregtech.loaders.recipe.CraftingComponent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.johny.tj.TJValues.CIRCUIT_TIERS;
import static com.johny.tj.items.TJMetaItems.UNIVERSAL_CIRCUITS;
import static com.johny.tj.machines.TJMetaTileEntities.*;
import static gregicadditions.GAMaterials.*;
import static gregicadditions.machines.GATileEntities.AIR_COLLECTOR;
import static gregtech.api.recipes.RecipeMaps.ALLOY_SMELTER_RECIPES;
import static gregtech.api.recipes.RecipeMaps.PACKER_RECIPES;
import static gregtech.api.unification.material.MarkerMaterials.Tier.Basic;
import static gregtech.api.unification.material.Materials.*;

public class RecipeInit {

    public static MetaTileEntityAirCollector[] AIR_COLLECTORS = {MetaTileEntities.AIR_COLLECTOR[3], AIR_COLLECTOR[4], AIR_COLLECTOR[5]};

    public static void init() {

        craftingRecipes();
        GreenhouseRecipes.init();
        AssemblerRecipes.init();
        AssemblyLineRecipes.init();
        RockBreakerRecipes.init();
        CokeOvenRecipes.init();

        if (TJConfig.machines.loadArchitectureRecipes)
            ArchitectureRecipes.init();

        if (TJConfig.machines.replaceCTMultis) {
            PrimitiveAlloySmelterRecipes.init();
            HeatExchangerRecipes.init();
            LargePoweredSpawnerRecipes.init();
            LargeVialProcessorRecipes.init();
            ArmorInfuserRecipes.init();
            DragonEggReplicatorRecipes.init();
            ChaosReplicatorRecipes.init();
        }
    }
    private static void craftingRecipes() {

        ALLOY_SMELTER_RECIPES.recipeBuilder()
                .input(Items.CLAY_BALL)
                .input(Blocks.SAND)
                .outputs(MetaItems.COKE_OVEN_BRICK.getStackForm(2))
                .EUt(64)
                .duration(600)
                .buildAndRegister();

        ModHandler.addShapedRecipe("large_decay_chamber", LARGE_DECAY_CHAMBER.getStackForm(), "LCL", "FMF", "LCL",
                'L', new UnificationEntry(OrePrefix.plateDense, Lead),
                'C', CraftingComponent.CIRCUIT.getIngredient(6),
                'F', CraftingComponent.FIELD_GENERATOR.getIngredient(6),
                'M', GATileEntities.DECAY_CHAMBER[5].getMetaTileEntity().getStackForm());

        ModHandler.addShapedRecipe("large_alloy_smelter", LARGE_ALLOY_SMELTER.getStackForm(), "PCP", "WSW", "PCP",
                'P', new UnificationEntry(OrePrefix.plate, ZirconiumCarbide),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'W', new UnificationEntry(OrePrefix.cableGtOctal, Naquadah),
                'S', GATileEntities.ALLOY_SMELTER[4].getMetaTileEntity().getStackForm());

        ModHandler.addShapedRecipe("large_greenhouse", LARGE_GREENHOUSE.getStackForm(), "PCP", "RSR", "GCG",
                'P', new UnificationEntry(OrePrefix.plate, StainlessSteel),
                'G', new UnificationEntry(OrePrefix.gear, StainlessSteel),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'R', CraftingComponent.PUMP.getIngredient(5),
                'S', GATileEntities.GREEN_HOUSE[4].getMetaTileEntity().getStackForm());

        ModHandler.addShapedRecipe("large_architect_workbench", LARGE_ARCHITECT_WORKBENCH.getStackForm(), "GCG", "RSB", "GCG",
                'G', new UnificationEntry(OrePrefix.gear, Steel),
                'C', CraftingComponent.CIRCUIT.getIngredient(4),
                'R', CraftingComponent.ROBOT_ARM.getIngredient(4),
                'B', CraftingComponent.CONVEYOR.getIngredient(4),
                'S', new ItemStack(Item.getByNameOrId("architecturecraft:sawbench")));

        ModHandler.addShapedRecipe("elite_large_miner", ELITE_LARGE_MINER.getStackForm(), "GCG", "THT", "SCS",
                'G', new UnificationEntry(OrePrefix.gear, Duranium),
                'C', CraftingComponent.CIRCUIT.getIngredient(7),
                'T', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(),
                'H', CraftingComponent.HULL.getIngredient(7),
                'S', CraftingComponent.SENSOR.getIngredient(7));

        ModHandler.addShapedRecipe("ultimate_large_miner", ULTIMATE_LARGE_MINER.getStackForm(), "GCG", "THT", "SCS",
                'G', new UnificationEntry(OrePrefix.gear, Seaborgium),
                'C', CraftingComponent.CIRCUIT.getIngredient(8),
                'T', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(),
                'H', CraftingComponent.HULL.getIngredient(8),
                'S', CraftingComponent.SENSOR.getIngredient(8));

        ModHandler.addShapedRecipe("world_destroyer", WORLD_DESTROYER.getStackForm(), "GCG", "DTD", "SCS",
                'G', new UnificationEntry(OrePrefix.gear, TungstenTitaniumCarbide),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'D', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm(),
                'T', MetaTileEntities.BLOCK_BREAKER[3].getStackForm(),
                'S', CraftingComponent.SENSOR.getIngredient(5));

        ModHandler.addShapedRecipe("large_rock_breaker", LARGE_ROCK_BREAKER.getStackForm(), "PCP", "USU", "GBG",
                'P', CraftingComponent.PISTON.getIngredient(5),
                'C', CraftingComponent.CIRCUIT.getIngredient(5),
                'U', CraftingComponent.PIPE.getIngredient(5),
                'S', GATileEntities.ROCK_BREAKER[4].getStackForm(),
                'G', GAMetaBlocks.TRANSPARENT_CASING.getItemVariant(GATransparentCasing.CasingType.CHROME_GLASS),
                'B', MetaItems.COMPONENT_GRINDER_TUNGSTEN.getStackForm());

        ModHandler.addShapedRecipe("stainless_pipe_casing", TJMetaBlocks.PIPE_CASING.getItemVariant(BlockPipeCasings.PipeCasingType.STAINLESS_PIPE_CASING, 3), "PTP", "TFT", "PTP",
                'P', new UnificationEntry(OrePrefix.plate, StainlessSteel),
                'T', new UnificationEntry(OrePrefix.pipeMedium, StainlessSteel),
                'F', new UnificationEntry(OrePrefix.frameGt, StainlessSteel));

        ModHandler.addShapelessRecipe("bronze_solar_boiler", SOLAR_BOILER[0].getStackForm(), MetaTileEntities.STEAM_BOILER_SOLAR_BRONZE.getStackForm());
        ModHandler.addShapelessRecipe("bronze_coal_boiler", COAL_BOILER[0].getStackForm(), MetaTileEntities.STEAM_BOILER_COAL_BRONZE.getStackForm());
        ModHandler.addShapelessRecipe("bronze_fluid_boiler", FLUID_BOILER[0].getStackForm(), MetaTileEntities.STEAM_BOILER_LAVA_BRONZE.getStackForm());

        ModHandler.addShapedRecipe("steel_solar_boiler", SOLAR_BOILER[1].getStackForm(), "GGG", "SSS", "PBP",
                'G', Blocks.GLASS,
                'S', new UnificationEntry(OrePrefix.plate, Silver),
                'P', new UnificationEntry(OrePrefix.pipeMedium, Steel),
                'B', MetaBlocks.MACHINE_CASING.getItemVariant(BlockMachineCasing.MachineCasingType.STEEL_BRICKS_HULL));
        ModHandler.addShapelessRecipe("steel_coal_boiler", COAL_BOILER[1].getStackForm(), MetaTileEntities.STEAM_BOILER_COAL_STEEL.getStackForm());
        ModHandler.addShapelessRecipe("steel_fluid_boiler", FLUID_BOILER[1].getStackForm(), MetaTileEntities.STEAM_BOILER_LAVA_STEEL.getStackForm());

        ModHandler.addShapedRecipe("lv_solar_boiler", SOLAR_BOILER[2].getStackForm(), "GGG", "SSS", "PBP",
                'G', Blocks.GLASS,
                'S', new UnificationEntry(OrePrefix.plate, Silver),
                'P', new UnificationEntry(OrePrefix.pipeLarge, Steel),
                'B', MetaTileEntities.HULL[1].getStackForm());

        ModHandler.addShapedRecipe("lv_coal_boiler", COAL_BOILER[2].getStackForm(), "PPP", "PHP", "BFB",
                'P', new UnificationEntry(OrePrefix.plate, Steel),
                'H', MetaTileEntities.HULL[1].getStackForm(),
                'B', Blocks.BRICK_BLOCK,
                'F', Blocks.FURNACE);

        ModHandler.addShapedRecipe("lv_fluid_boiler", FLUID_BOILER[2].getStackForm(), "PPP", "GGG", "PHP",
                'P', new UnificationEntry(OrePrefix.plate, Steel),
                'G', Blocks.GLASS,
                'H', MetaTileEntities.HULL[1].getStackForm());

        for (int i = 0; i < AIR_COLLECTORS.length; i++) {
            ModHandler.addShapedRecipe("large_atmosphere_collector." + i, LARGE_ATMOSPHERE_COLLECTOR[i].getStackForm(), "CRC", "RSR", "PRP",
                    'C', CraftingComponent.CIRCUIT.getIngredient(5 + i),
                    'R', CraftingComponent.ROTOR.getIngredient(5 + i),
                    'S', AIR_COLLECTORS[i].getStackForm(),
                    'P', CraftingComponent.PIPE.getIngredient(5 + i));
        }

        ModHandler.addShapedRecipe("duranium_casing", TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.DURANIUM_CASING, 3), "PhP", "PFP", "PwP",
                'P', new UnificationEntry(OrePrefix.plate, Duranium),
                'F', new UnificationEntry(OrePrefix.frameGt, Duranium));

        ModHandler.addShapedRecipe("seaborgium_casing", TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.SEABORGIUM_CASING, 3), "PhP", "PFP", "PwP",
                'P', new UnificationEntry(OrePrefix.plate, Seaborgium),
                'F', new UnificationEntry(OrePrefix.frameGt, Seaborgium));

        ModHandler.addShapedRecipe("tungsten_titanium_carbide_casing", TJMetaBlocks.SOLID_CASING.getItemVariant(BlockSolidCasings.SolidCasingType.TUNGSTEN_TITANIUM_CARBIDE_CASING, 3), "PhP", "PFP", "PwP",
                'P', new UnificationEntry(OrePrefix.plate, TungstenTitaniumCarbide),
                'F', new UnificationEntry(OrePrefix.frameGt, TungstenTitaniumCarbide));

        ModHandler.addShapedRecipe("linking_device", TJMetaItems.LINKING_DEVICE.getStackForm(), "SIS", "RLR", "CIC",
                'S', CraftingComponent.SENSOR.getIngredient(5),
                'I', new UnificationEntry(OrePrefix.cableGtSingle, IVSuperconductor),
                'R', new UnificationEntry(OrePrefix.ring, HSSE),
                'L', new UnificationEntry(OrePrefix.stickLong, Osmium),
                'C', CraftingComponent.CIRCUIT.getIngredient(6));

        ModHandler.addShapedRecipe("industrial_stean_engine", INDUSTRIAL_STEAM_ENGINE.getStackForm(), "PCP", "BHB", "GOG",
                'P', new UnificationEntry(OrePrefix.pipeLarge, Bronze),
                'C', CraftingComponent.CIRCUIT.getIngredient(2),
                'B', new UnificationEntry(OrePrefix.plate, Brass),
                'H', GAMetaBlocks.METAL_CASING_1.getItemVariant(MetalCasing1.CasingType.TUMBAGA),
                'G', new UnificationEntry(OrePrefix.gear, Bronze),
                'O', new UnificationEntry(OrePrefix.gear, Steel));

        ModHandler.addShapedRecipe("void_plunger", TJMetaItems.VOID_PLUNGER.getStackForm(), " OO", " SO", "S  ",
                'O', new ItemStack(Item.getByNameOrId("enderio:block_reinforced_obsidian")),
                'S', new UnificationEntry(OrePrefix.stick, Steel));

        ModHandler.addShapedRecipe("nbt_reader", TJMetaItems.NBT_READER.getStackForm(), "PPP", "PCP", "PPP",
                'P', new ItemStack(Items.PAPER),
                'C', new UnificationEntry(OrePrefix.circuit, Basic));

        ModHandler.addShapedRecipe("rotor_holder_umv", ROTOR_HOLDER_UMV.getStackForm(), "MQM", "QHQ", "MQM",
                'M', new UnificationEntry(OrePrefix.gearSmall, MetastableHassium),
                'Q', new UnificationEntry(OrePrefix.gear, Quantum),
                'H', GATileEntities.GA_HULLS[3].getStackForm());

        ModHandler.addShapedRecipe("coolant_rotor_holder_umv", COOLANT_ROTOR_HOLDER_UMV.getStackForm(), "MVM", "VHV", "MVM",
                'M', new UnificationEntry(OrePrefix.gearSmall, MetastableOganesson),
                'V', new UnificationEntry(OrePrefix.gear, Vibranium),
                'H', GATileEntities.GA_HULLS[3].getStackForm());

        for (int i = 0; i < UNIVERSAL_CIRCUITS.length; i++) {
            ModHandler.addShapelessRecipe(GAValues.VN[i].toLowerCase() + "_universal_circuit", UNIVERSAL_CIRCUITS[i].getStackForm(), new UnificationEntry(OrePrefix.circuit, CIRCUIT_TIERS[i]));

            PACKER_RECIPES.recipeBuilder()
                    .input(OrePrefix.circuit, CIRCUIT_TIERS[i])
                    .notConsumable(new IntCircuitIngredient(0))
                    .outputs(UNIVERSAL_CIRCUITS[i].getStackForm())
                    .EUt(2)
                    .duration(20)
                    .buildAndRegister();
        }

        BlockFusionGlass.GlassType[] fusionGlass = BlockFusionGlass.GlassType.values();
        ItemStack[] fusionCasing = {MetaBlocks.MUTLIBLOCK_CASING.getItemVariant(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING), MetaBlocks.MUTLIBLOCK_CASING.getItemVariant(BlockMultiblockCasing.MultiblockCasingType.FUSION_CASING_MK2),
                GAMetaBlocks.FUSION_CASING.getItemVariant(GAFusionCasing.CasingType.FUSION_3), TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_CASING_UHV),
                TJMetaBlocks.FUSION_CASING.getItemVariant(BlockFusionCasings.FusionType.FUSION_CASING_UEV)};
        for (int i = 0; i < fusionGlass.length; i++) {
            ModHandler.addShapelessRecipe("fusion_glass" + fusionCasing[i].getTranslationKey(), TJMetaBlocks.FUSION_GLASS.getItemVariant(fusionGlass[i]), fusionCasing[i], new ItemStack(Blocks.GLASS));
            ModHandler.addShapelessRecipe("fusion_casing" + fusionCasing[i].getTranslationKey(), fusionCasing[i], TJMetaBlocks.FUSION_GLASS.getItemVariant(fusionGlass[i]));
        }
    }
}
