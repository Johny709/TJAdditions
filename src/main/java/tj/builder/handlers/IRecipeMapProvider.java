package tj.builder.handlers;

import net.minecraft.item.crafting.IRecipe;

import java.util.Map;

public interface IRecipeMapProvider {

    Map<Integer, IRecipe> getRecipeMap();
}
