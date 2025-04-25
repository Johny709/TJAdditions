package com.johny.tj;

import gregtech.api.unification.material.IMaterialHandler;

import static gregicadditions.GAMaterials.FullerenePolymerMatrix;
import static gregicadditions.GAMaterials.Periodicium;
import static gregtech.api.unification.material.type.DustMaterial.MatFlags.GENERATE_PLATE;
import static gregtech.api.unification.material.type.SolidMaterial.MatFlags.GENERATE_FRAME;

@IMaterialHandler.RegisterMaterialHandler
public class TJMaterials implements IMaterialHandler {

    @Override
    public void onMaterialsInit() {
        FullerenePolymerMatrix.addFlag(GENERATE_FRAME);
        Periodicium.addFlag(GENERATE_PLATE);
    }
}
