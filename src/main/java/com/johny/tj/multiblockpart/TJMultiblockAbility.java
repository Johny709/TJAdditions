package com.johny.tj.multiblockpart;

import com.johny.tj.multiblockpart.utility.MetaTileEntityMachineController;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import net.minecraftforge.items.IItemHandlerModifiable;

public class TJMultiblockAbility<T> extends MultiblockAbility<T> {
    public static final MultiblockAbility<IItemHandlerModifiable> CIRCUIT_SLOT = new TJMultiblockAbility<>();
    public static final MultiblockAbility<MetaTileEntityMachineController> REDSTONE_CONTROLLER = new TJMultiblockAbility<>();
}
