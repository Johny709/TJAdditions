package tj.blocks;

import gregtech.common.blocks.VariantBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public class BlockPipeCasings extends VariantBlock<BlockPipeCasings.PipeCasingType> {

    public BlockPipeCasings() {
        super(Material.IRON);
        setHardness(5.0f);
        setResistance(10.0f);
        setTranslationKey("pipe_casing");
        setSoundType(SoundType.METAL);
        setHarvestLevel("wrench", 2);
        setDefaultState(getState(PipeCasingType.STAINLESS_PIPE_CASING));
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }

    public enum PipeCasingType implements IStringSerializable {
        STAINLESS_PIPE_CASING("stainless_pipe");

        private final String name;

        PipeCasingType(String name) {
            this.name = name;
        }

        @Nonnull
        @Override
        public String getName() {
            return this.name;
        }
    }
}
