package com.johny.tj.capability;

import static gregtech.api.capability.SimpleCapabilityManager.registerCapabilityWithNoDefault;

public class TJSimpleCapabilityManager {

    public static void init() {
        registerCapabilityWithNoDefault(IMultiControllable.class);
        registerCapabilityWithNoDefault(IMultipleWorkable.class);
        registerCapabilityWithNoDefault(IParallelController.class);
        registerCapabilityWithNoDefault(LinkPos.class);
        registerCapabilityWithNoDefault(LinkPosInterDim.class);
        registerCapabilityWithNoDefault(LinkEntity.class);
        registerCapabilityWithNoDefault(LinkEntityInterDim.class);
    }
}
