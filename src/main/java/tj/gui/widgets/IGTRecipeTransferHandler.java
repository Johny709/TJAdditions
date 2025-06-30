package tj.gui.widgets;

import gregtech.api.gui.impl.ModularUIContainer;
import mezz.jei.api.gui.IGuiIngredient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Map;

public interface IGTRecipeTransferHandler {

    String transferRecipe(ModularUIContainer container, Map<Integer, IGuiIngredient<ItemStack>> itemIngredients, Map<Integer, IGuiIngredient<FluidStack>> fluidIngredients, EntityPlayer player, boolean maxTransfer, boolean doTransfer);
}
