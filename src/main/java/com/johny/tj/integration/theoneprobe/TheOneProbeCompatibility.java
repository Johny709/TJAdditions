package com.johny.tj.integration.theoneprobe;

import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.ITheOneProbe;

public class TheOneProbeCompatibility {

    public static void registerCompatibility() {
        ITheOneProbe probe = TheOneProbe.theOneProbeImp;
        probe.registerProvider(new ParallelControllerInfoProvider());
        probe.registerProvider(new ParallelWorkableInfoProvider());
        probe.registerProvider(new LinkedPosInfoProvider());
    }
}
