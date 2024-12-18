package com.johny.tj;

import net.minecraftforge.common.config.Config;

@Config(modid = TJ.MODID)
public class TJConfig {

    @Config.Comment("Configure Machines")
    public static Machines machines = new Machines();

    public static class Machines {

        @Config.Name("Replace CT Multis")
        @Config.Comment("replaces Multis registered by MultiblockTweaker")
        @Config.RequiresMcRestart
        public boolean replaceCTMultis = false;

    }

    @Config.Comment("Industrial Fusion Reactor")
    public static IndustrialFusionReactor industrialFusionReactor = new IndustrialFusionReactor();

    public static class IndustrialFusionReactor {

        @Config.Name("Slice Limit")
        @Config.Comment("Adjust the maximum number of slices the fusion reactor can have")
        @Config.RequiresMcRestart
        public int maximumSlices = 64;
    }

    @Config.Comment("Parallel Large Chemical Reactor")
    public static ParallelLCR parallelLCR = new ParallelLCR();

    public static class ParallelLCR {

        @Config.Name("Layer Limit")
        @Config.Comment("Adjust the maximum number of layers the chemical reactor can have")
        @Config.RequiresMcRestart
        public int maximumLayers = 64;
    }

    @Config.Comment("Decay Chamber")
    public static DecayChamber decayChamber = new DecayChamber();

    public static class DecayChamber {

        @Config.Name("EU/t Percentage")
        @Config.RequiresMcRestart
        public int eutPercentage = 90;

        @Config.Name("Duration Percentage")
        @Config.RequiresMcRestart
        public int durationPercentage = 80;

        @Config.Name("Chance Percentage")
        @Config.RequiresMcRestart
        public int chancePercentage = 100;

        @Config.Name("Stack Size")
        @Config.RequiresMcRestart
        public int stack = 16;
    }

    @Config.Comment("Large Alloy Smelter")
    public static LargeAlloySmelter largeAlloySmelter = new LargeAlloySmelter();

    public static class LargeAlloySmelter {

        @Config.Name("EU/t Percentage")
        @Config.RequiresMcRestart
        public int eutPercentage = 90;

        @Config.Name("Duration Percentage")
        @Config.RequiresMcRestart
        public int durationPercentage = 80;

        @Config.Name("Chance Percentage")
        @Config.RequiresMcRestart
        public int chancePercentage = 100;

        @Config.Name("Stack Size")
        @Config.RequiresMcRestart
        public int stack = 16;
    }

}
