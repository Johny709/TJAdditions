package com.johny.tj;

import gregtech.api.unification.material.IMaterialHandler;

import static gregicadditions.GAMaterials.Periodicium;
import static gregtech.api.unification.material.type.DustMaterial.MatFlags.GENERATE_PLATE;
import static gregtech.api.unification.material.type.IngotMaterial.MatFlags.GENERATE_BOLT_SCREW;

@IMaterialHandler.RegisterMaterialHandler
public class TJMaterials implements IMaterialHandler {

    @Override
    public void onMaterialsInit() {
        Periodicium.addFlag(GENERATE_PLATE, GENERATE_BOLT_SCREW);
    }
}
