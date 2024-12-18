package com.johny.tj.materials;

import com.google.common.collect.ImmutableList;
import gregtech.api.unification.material.IMaterialHandler;
import gregtech.api.unification.material.type.FluidMaterial;

import static gregtech.api.unification.material.MaterialIconSet.FLUID;
import static gregtech.api.unification.material.type.FluidMaterial.MatFlags.GENERATE_FLUID_BLOCK;
import static gregtech.api.unification.material.type.Material.MatFlags.DISABLE_DECOMPOSITION;
import static gregtech.api.unification.material.type.Material.MatFlags.NO_RECYCLING;


@IMaterialHandler.RegisterMaterialHandler
public class TJMaterials {

    public static final FluidMaterial PahoehoeLava = new FluidMaterial(550, "pahoehoe", 0x964B00, FLUID, ImmutableList.of(), NO_RECYCLING | GENERATE_FLUID_BLOCK | DISABLE_DECOMPOSITION);
    //public static final IngotMaterial Soularium = new IngotMaterial(534, "soularium", 0x3b2a15, DULL, 10, ImmutableList.of(), GENERATE_PLATE | SMELT_INTO_FLUID | GENERATE_DENSE | GENERATE_ROD | GENERATE_FRAME | GENERATE_PLASMA | GENERATE_BOLT_SCREW, 13.0f, 6, 512);
    //public static final IngotMaterial Draconium = new IngotMaterial(518, "draconium", 0x573d85, DULL, 10, ImmutableList.of(), GENERATE_PLATE | SMELT_INTO_FLUID | GENERATE_DENSE | GENERATE_ORE | GENERATE_ROD | GENERATE_FRAME | GENERATE_BOLT_SCREW, null, 30.0f, 6, 12_800, 9200);
    //public static final IngotMaterial AwakenedDraconium = new IngotMaterial(519, "awaken_draconium", 0xff571a, SHINY, 10, ImmutableList.of(), GENERATE_PLATE | SMELT_INTO_FLUID | GENERATE_DENSE | GENERATE_ROD | GENERATE_FRAME | GENERATE_PLASMA | GENERATE_BOLT_SCREW, 40.0f, 6, 128_000);
    //public static final IngotMaterial Chaos = new IngotMaterial(520, "chaos", 0x696969, DULL, 10, ImmutableList.of(), GENERATE_PLATE | SMELT_INTO_FLUID | GENERATE_DENSE | GENERATE_ROD | GENERATE_FRAME | GENERATE_PLASMA | GENERATE_FINE_WIRE | GENERATE_BOLT_SCREW, 50.0f, 6, 1_280_000);
    //public static final IngotMaterial ChaosAlloy = new IngotMaterial(521, "chaosalloy", 0x696969, DULL, 10, ImmutableList.of(), GENERATE_PLATE | SMELT_INTO_FLUID | GENERATE_DENSE | GENERATE_PLASMA | GENERATE_FINE_WIRE | GENERATE_BOLT_SCREW, 60.0f, 6, 1_280_000);

}
