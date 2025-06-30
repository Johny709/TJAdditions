package tj.blocks;

import gregtech.common.blocks.VariantBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockAbilityCasings extends VariantBlock<BlockAbilityCasings.AbilityType> {

    public BlockAbilityCasings() {
        super(Material.IRON);
        setHardness(5.0f);
        setResistance(10.0f);
        setTranslationKey("ability_casing");
        setSoundType(SoundType.METAL);
        setHarvestLevel("wrench", 2);
        setDefaultState(getState(AbilityType.ENERGY_PORT_LUV));
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("tile.ability_casing.energy_port.description"));
    }

    public enum AbilityType implements IStringSerializable {

        ENERGY_PORT_LUV("energy_port_luv", 6),
        ENERGY_PORT_ZPM("energy_port_zpm", 7),
        ENERGY_PORT_UV("energy_port_uv", 8),
        ENERGY_PORT_UHV("energy_port_uhv", 9),
        ENERGY_PORT_UEV("energy_port_uev", 10);

        private final String name;
        private final int tier;

        AbilityType(String name, int tier) {
            this.name = name;
            this.tier = tier;
        }

        @Override
        public String getName() {
            return this.name;
        }

        public int getTier() {
            return this.tier;
        }
    }
}
