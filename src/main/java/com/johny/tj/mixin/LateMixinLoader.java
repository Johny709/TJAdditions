package com.johny.tj.mixin;

import com.johny.tj.TJ;
import gregicadditions.Gregicality;
import gregtech.api.GTValues;
import mcjty.theoneprobe.TheOneProbe;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LateMixinLoader implements ILateMixinLoader {

    public static final List<String> MODIDs = Arrays.asList(GTValues.MODID, Gregicality.MODID, TheOneProbe.MODID);

    @Override
    public List<String> getMixinConfigs() {
        return MODIDs.stream()
                .map(mod -> "mixin." + TJ.MODID + "." + mod + ".json")
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
