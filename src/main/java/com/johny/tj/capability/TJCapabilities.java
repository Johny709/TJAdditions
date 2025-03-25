package com.johny.tj.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class TJCapabilities {

    @CapabilityInject(IMultipleWorkable.class)
    public static Capability<IMultipleWorkable> CAPABILITY_MULTIPLEWORKABLE = null;

    @CapabilityInject(IMultiControllable.class)
    public static Capability<IMultiControllable> CAPABILITY_MULTICONTROLLABLE = null;

    @CapabilityInject(IParallelController.class)
    public static Capability<IParallelController> CAPABILITY_PARALLEL_CONTROLLER = null;

    @CapabilityInject(LinkPos.class)
    public static Capability<LinkPos> CAPABILITY_LINKPOS = null;

    @CapabilityInject(LinkInterDimPos.class)
    public static Capability<LinkInterDimPos> CAPABILITY_LINKPOS_INTERDIM = null;
}
