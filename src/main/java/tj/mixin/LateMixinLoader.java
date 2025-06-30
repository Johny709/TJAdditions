package tj.mixin;

import tj.TJ;
import gregtech.api.GTValues;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LateMixinLoader implements ILateMixinLoader {

    public static final List<String> MODIDs = Arrays.asList(GTValues.MODID);

    @Override
    public List<String> getMixinConfigs() {
        return MODIDs.stream()
                .map(mod -> "mixins." + TJ.MODID + "." + mod + ".json")
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
