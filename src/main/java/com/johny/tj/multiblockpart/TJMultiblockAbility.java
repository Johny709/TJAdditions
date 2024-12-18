package com.johny.tj.multiblockpart;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import net.minecraftforge.items.IItemHandlerModifiable;

public class TJMultiblockAbility<T> extends MultiblockAbility<T> {
    public static final MultiblockAbility<IItemHandlerModifiable> CIRCUIT_SLOT = new TJMultiblockAbility<>();
}
