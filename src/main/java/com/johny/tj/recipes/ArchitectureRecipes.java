package com.johny.tj.recipes;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import static com.johny.tj.TJRecipeMaps.ARCHITECT_RECIPES;

public class ArchitectureRecipes {

    private static int inputQuantity;
    private static int outputQuantity;

    public static void init() {

        for (int i = 0; i < 89; i++) {
            int finalI = i;
            getQuantity(i);
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            nbtTagCompound.setInteger("Shape", finalI);
            nbtTagCompound.setString("BaseName", Blocks.PLANKS.getRegistryName().toString());
            nbtTagCompound.setInteger("BaseData", new ItemStack(Blocks.PLANKS).getMetadata());
            ItemStack oakCatalyst = new ItemStack(Item.getByNameOrId("architecturecraft:shape"));
            oakCatalyst.setTagCompound(nbtTagCompound);

            Block.REGISTRY.forEach(block -> {
                    String blockName = block.getRegistryName().toString();
                    NBTTagCompound tagCompound = new NBTTagCompound();
                    tagCompound.setInteger("Shape", finalI);
                    tagCompound.setString("BaseName", blockName);
                    tagCompound.setInteger("BaseData", block.getMetaFromState(block.getDefaultState()));

                    ItemStack architectStack = new ItemStack(Item.getByNameOrId("architecturecraft:shape"), outputQuantity);
                    architectStack.setTagCompound(tagCompound);

                    ARCHITECT_RECIPES.recipeBuilder()
                            .notConsumable(oakCatalyst)
                            .inputs(new ItemStack(block, inputQuantity))
                            .outputs(architectStack)
                            .EUt(30)
                            .duration(20)
                            .buildAndRegister();
                });}
    }

    public static void getQuantity(int shape) {
        switch (shape) {
            case 0, 4, 5, 7, 14, 29, 31, 32, 34, 39, 46, 47, 48, 49, 57, 58, 59, 62, 63, 66, 68, 69, 74, 81, 87:
                inputQuantity = 1;
                outputQuantity = 2;
                return;
            case 1, 8, 18, 75:
                inputQuantity = 1;
                outputQuantity = 3;
                return;
            case 2, 9:
                inputQuantity = 2;
                outputQuantity = 3;
                return;
            case 3, 16, 30, 35, 40, 41, 42, 43, 45, 44, 52, 50, 51, 56, 54, 53, 55, 76, 78:
                inputQuantity = 1;
                outputQuantity = 4;
                return;
            case 6, 10, 11, 12, 13, 15, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 33, 37, 38, 61, 64, 65, 67:
                inputQuantity = 1;
                outputQuantity = 1;
                return;
            case 17, 60:
                inputQuantity = 1;
                outputQuantity = 16;
                return;
            case 36, 80, 83, 82:
                inputQuantity = 1;
                outputQuantity = 8;
                return;
            case 70, 71, 72, 77:
                inputQuantity = 1;
                outputQuantity = 10;
                return;
            case 73, 86, 84, 85:
                inputQuantity = 1;
                outputQuantity = 5;
                return;
            case 79, 88:
                inputQuantity = 1;
                outputQuantity = 6;
        }
    }
}
