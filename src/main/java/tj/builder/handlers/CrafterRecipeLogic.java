package tj.builder.handlers;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.capability.impl.AbstractWorkableHandler;
import tj.capability.impl.CraftingRecipeLRUCache;

//TODO WIP
public class CrafterRecipeLogic extends AbstractWorkableHandler<IItemHandlerModifiable, IMultipleTankHandler> {

    private final CraftingRecipeLRUCache previousRecipe = new CraftingRecipeLRUCache(10);

    public CrafterRecipeLogic(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    @Override
    protected boolean startRecipe() {
        if (++this.lastInputIndex == this.busCount)
            this.lastInputIndex = 0;
        return super.startRecipe();
    }
}
