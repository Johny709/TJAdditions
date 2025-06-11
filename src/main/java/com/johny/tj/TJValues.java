package com.johny.tj;

import gregtech.api.unification.material.type.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;

import static gregicadditions.GAMaterials.*;
import static gregtech.api.unification.material.MarkerMaterials.Tier.*;

public class TJValues {

    public static final int[] VC = {0x6b5f55, 0x9b9b9b, 0xf78b2c, 0xffb900, 0x656565, 0xffffff, 0xffb0b0, 0xffe9e9, 0xc6ffad, 0xffc4f8, 0x2776ff, 0xf0ff00, 0x9500cd, 0xff6565, 0xfff7f7};
    public static final String[] VCC = {"§0", "§7", "§c", "§6", "§8", "§f", "§d", "§b", "§2", "§9", "§1", "§e", "§5", "§4", "§k"};

    public static final Material[] CIRCUIT_TIERS = {Primitive, Basic, Good, Advanced, Extreme, Elite, Master, Ultimate, Superconductor, Infinite, UEV, UIV, UMV, UXV, MAX};

    public static final BlockPos DUMMY_POS = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    public static final IFluidTank DUMMY_TANK = new FluidTank(0);
}
