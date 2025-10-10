package tj.builder.handlers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.item.crafting.IRecipe;


public interface IRecipeMapProvider {

    Int2ObjectMap<IRecipe> getRecipeMap();

    default void clearRecipeCache() {}
}
