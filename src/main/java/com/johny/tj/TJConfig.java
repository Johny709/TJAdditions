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

        @Config.Name("EU/t Percentage")
        @Config.RequiresMcRestart
        public int eutPercentage = 90;

        @Config.Name("Duration Percentage")
        @Config.RequiresMcRestart
        public int durationPercentage = 80;

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

        @Config.Name("Recipe Parallel")
        @Config.Comment("Adjust the amount of recipes the machine can run simultaneously")
        @Config.RequiresMcRestart
        public int maximumRecipeParallel = 64;
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

    @Config.Comment("Large Greenhouse")
    public static LargeGreenhouse largeGreenhouse = new LargeGreenhouse();

    public static class LargeGreenhouse {

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

        @Config.Name("EU/t Percentage [Tree Mode]")
        @Config.RequiresMcRestart
        public int eutPercentageTree = 90;

        @Config.Name("Duration Percentage [Tree Mode]")
        @Config.RequiresMcRestart
        public int durationPercentageTree = 50;

        @Config.Name("Chance Percentage [Tree Mode]")
        @Config.RequiresMcRestart
        public int chancePercentageTree = 150;

        @Config.Name("Stack Size [Tree Mode]")
        @Config.RequiresMcRestart
        public int stackTree = 4;
    }

    @Config.Comment("Large Architect's Workbench")
    public static LargeArchitectWorkbench largeArchitectWorkbench = new LargeArchitectWorkbench();

    public static class LargeArchitectWorkbench {

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

        @Config.Name("Slice Limit")
        @Config.Comment("Adjust the maximum number of slices the Large Architect Workbench can have")
        @Config.RequiresMcRestart
        public int maximumSlices = 64;
    }

    @Config.Comment("Elite Large Miner")
    public static EliteLargeMiner eliteLargeMiner = new EliteLargeMiner();

    public static class EliteLargeMiner {

        @Config.Name("Elite Large Miner Chunk Diamater")
        @Config.RequiresMcRestart
        @Config.Comment("The length in chunks of the side of the square centered on the Miner that will be mined.")
        public int eliteMinerChunkDiamater = 9;

        @Config.Name("Elite Large Miner Fortune Level")
        @Config.RequiresMcRestart
        @Config.Comment("The level of fortune which will be applied to blocks that the Miner mines.")
        public int eliteMinerFortune = 12;

        @Config.Name("Elite Large Miner Drilling Fluid Consumption")
        @Config.RequiresMcRestart
        @Config.Comment("The amount of drilling fluid consumed per tick")
        public int eliteMinerDrillingFluid = 64;
    }

    @Config.Comment("Elite Large Miner")
    public static UltimateLargeMiner ultimateLargeMiner = new UltimateLargeMiner();

    public static class UltimateLargeMiner {

        @Config.Name("Elite Large Miner Chunk Diamater")
        @Config.RequiresMcRestart
        @Config.Comment("The length in chunks of the side of the square centered on the Miner that will be mined.")
        public int ultimateMinerChunkDiamater = 12;

        @Config.Name("Elite Large Miner Fortune Level")
        @Config.RequiresMcRestart
        @Config.Comment("The level of fortune which will be applied to blocks that the Miner mines.")
        public int ultimateMinerFortune = 15;

        @Config.Name("Elite Large Miner Drilling Fluid Consumption")
        @Config.RequiresMcRestart
        @Config.Comment("The amount of drilling fluid consumed per tick")
        public int ultimateMinerDrillingFluid = 128;
    }

    @Config.Comment("World Destroyer")
    public static WorldDestroyerMiner worldDestroyerMiner = new WorldDestroyerMiner();

    public static class WorldDestroyerMiner {

        @Config.Name("World Destroyer Fortune Level")
        @Config.RequiresMcRestart
        @Config.Comment("The level of fortune which will be applied to blocks that the Miner mines.")
        public int worldDestroyerFortune = 3;

        @Config.Name("World Destroyer Drilling Fluid Consumption")
        @Config.RequiresMcRestart
        @Config.Comment("The amount of drilling fluid consumed per tick")
        public int worldDestroyerDrillingFluid = 100;

        @Config.Name("World Destroyer Chunk Multiplier")
        @Config.RequiresMcRestart
        @Config.Comment("The amount chunk area multiplied per motor tier")
        public int worldDestroyerChunkMultiplier = 2;
    }
}
