package com.johny.tj;

import gregtech.api.unification.material.IMaterialHandler;

import static gregicadditions.GAMaterials.Periodicium;
import static gregtech.api.unification.material.Materials.EnderEye;
import static gregtech.api.unification.material.Materials.EnderPearl;
import static gregtech.api.unification.material.type.DustMaterial.MatFlags.GENERATE_PLATE;
import static gregtech.api.unification.material.type.DustMaterial.MatFlags.SMELT_INTO_FLUID;
import static gregtech.api.unification.material.type.IngotMaterial.MatFlags.GENERATE_BOLT_SCREW;
import static gregtech.api.unification.material.type.SolidMaterial.MatFlags.GENERATE_FRAME;

@IMaterialHandler.RegisterMaterialHandler
public class TJMaterials implements IMaterialHandler {

    @Override
    public void onMaterialsInit() {
        Periodicium.addFlag(GENERATE_PLATE, GENERATE_BOLT_SCREW, GENERATE_FRAME);
        EnderPearl.addFlag(SMELT_INTO_FLUID);
        EnderEye.addFlag(SMELT_INTO_FLUID);
    }
}
