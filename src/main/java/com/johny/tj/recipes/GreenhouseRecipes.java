package com.johny.tj.recipes;

import gregtech.common.blocks.MetaBlocks;
import gregtech.common.blocks.wood.BlockGregLog;
import gregtech.common.items.MetaItems;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.johny.tj.TJRecipeMaps.GREENHOUSE_TREE_RECIPES;

public class GreenhouseRecipes {

    public static void init() {
        GREENHOUSE_TREE_RECIPES.recipeBuilder() //Sugar Cane
                .notConsumable(new ItemStack(Items.REEDS))
                .chancedOutput(new ItemStack(Items.REEDS, 5), 9500, 1000)
                .chancedOutput(new ItemStack(Items.REEDS), 5000, 100)
                .chancedOutput(new ItemStack(Items.SUGAR, 10), 5000, 1000)
                .duration(900)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Spectre Tree
                .notConsumable(new ItemStack(Item.getByNameOrId("randomthings:spectresapling")))
                .chancedOutput(new ItemStack(Item.getByNameOrId("randomthings:spectrelog"), 5), 9500, 1000)
                .chancedOutput(new ItemStack(Item.getByNameOrId("randomthings:spectresapling")), 5000, 1000)
                .chancedOutput(new ItemStack(Item.getByNameOrId("randomthings:ingredient"), 10, 2), 5000, 1000)
                .duration(900)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Spruce Tree
                .notConsumable(new ItemStack(Blocks.SAPLING, 1, 1))
                .chancedOutput(new ItemStack(Blocks.LOG, 5, 1), 9500, 1000)
                .chancedOutput(new ItemStack(Blocks.SAPLING, 1, 1), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Oak Tree
                .notConsumable(new ItemStack(Blocks.SAPLING, 1, 0))
                .chancedOutput(new ItemStack(Blocks.LOG, 5, 0), 9500, 1000)
                .chancedOutput(new ItemStack(Blocks.SAPLING, 1, 0), 5000, 1000)
                .chancedOutput(new ItemStack(Items.APPLE, 10), 5000, 100)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Birch Tree
                .notConsumable(new ItemStack(Blocks.SAPLING, 1, 2))
                .chancedOutput(new ItemStack(Blocks.LOG, 5, 2), 9500, 1000)
                .chancedOutput(new ItemStack(Blocks.SAPLING, 1, 2), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Jungle Tree
                .notConsumable(new ItemStack(Blocks.SAPLING, 1, 3))
                .chancedOutput(new ItemStack(Blocks.LOG, 5, 3), 9500, 1000)
                .chancedOutput(new ItemStack(Blocks.SAPLING, 1, 3), 5000, 1000)
                .chancedOutput(new ItemStack(Blocks.COCOA, 10), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Chorus Fruit
                .notConsumable(new ItemStack(Items.CHORUS_FRUIT))
                .chancedOutput(new ItemStack(Items.CHORUS_FRUIT, 5), 9500, 1000)
                .chancedOutput(new ItemStack(Items.CHORUS_FRUIT_POPPED, 10), 5000, 1000)
                .chancedOutput(new ItemStack(Items.ENDER_PEARL), 100, 100)
                .duration(6000)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Lightwood Tree
                .notConsumable(new ItemStack(Block.getBlockFromName("advancedrocketry:aliensapling")))
                .chancedOutput(new ItemStack(Block.getBlockFromName("advancedrocketry:alienwood"), 5), 9500, 1000)
                .chancedOutput(new ItemStack(Items.APPLE, 6), 7000, 1000)
                .chancedOutput(new ItemStack(Block.getBlockFromName("advancedrocketry:charcoallog"), 32), 6000, 1000)
                .chancedOutput(new ItemStack(Block.getBlockFromName("advancedrocketry:aliensapling")), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Acacia Tree
                .notConsumable(new ItemStack(Blocks.SAPLING, 1, 4))
                .chancedOutput(new ItemStack(Blocks.LOG2, 5, 0), 9500, 1000)
                .chancedOutput(new ItemStack(Blocks.SAPLING, 1, 2), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Dark Oak Tree
                .notConsumable(new ItemStack(Blocks.SAPLING, 1, 5))
                .chancedOutput(new ItemStack(Blocks.LOG2, 5, 1), 9500, 1000)
                .chancedOutput(new ItemStack(Blocks.SAPLING, 1, 3), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Rubber Tree
                .notConsumable(new ItemStack(MetaBlocks.SAPLING))
                .chancedOutput(new ItemStack(MetaBlocks.LOG.getItem(BlockGregLog.LogVariant.RUBBER_WOOD).getItem(), 5), 9500, 1000)
                .chancedOutput(new ItemStack(Items.APPLE, 6), 7000, 1000)
                .chancedOutput(MetaItems.RUBBER_DROP.getStackForm(6), 7000, 1000)
                .chancedOutput(new ItemStack(MetaBlocks.SAPLING), 5000, 1000)
                .duration(600)
                .buildAndRegister();

        GREENHOUSE_TREE_RECIPES.recipeBuilder() // Canola
                .notConsumable(new ItemStack(Item.getByNameOrId("actuallyadditions:item_canola_seed")))
                .chancedOutput(new ItemStack(Item.getByNameOrId("actuallyadditions:item_misc"), 5, 13), 9500, 1000)
                .chancedOutput(new ItemStack(Item.getByNameOrId("actuallyadditions:item_canola_seed"), 5), 9500, 1000)
                .duration(600)
                .buildAndRegister();
    }
}
