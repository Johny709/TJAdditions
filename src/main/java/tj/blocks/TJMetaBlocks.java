package tj.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TJMetaBlocks {

    private TJMetaBlocks(){
    }

    public static BlockSolidCasings SOLID_CASING;
    public static BlockAbilityCasings ABILITY_CASING;
    public static BlockPipeCasings PIPE_CASING;
    public static BlockFusionCasings FUSION_CASING;
    public static BlockFusionGlass FUSION_GLASS;

    public static void init() {
        SOLID_CASING = new BlockSolidCasings();
        SOLID_CASING.setRegistryName("solid_casing");

        ABILITY_CASING = new BlockAbilityCasings();
        ABILITY_CASING.setRegistryName("ability_casing");

        PIPE_CASING = new BlockPipeCasings();
        PIPE_CASING.setRegistryName("pipe_casing");

        FUSION_CASING = new BlockFusionCasings();
        FUSION_CASING.setRegistryName("fusion_casing");

        FUSION_GLASS = new BlockFusionGlass();
        FUSION_GLASS.setRegistryName("fusion_glass");

    }

    @SideOnly(Side.CLIENT)
    public static void registerItemModels() {
        registerItemModel(SOLID_CASING);
        registerItemModel(ABILITY_CASING);
        registerItemModel(PIPE_CASING);
        registerItemModel(FUSION_CASING);
        registerItemModel(FUSION_GLASS);
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModel(Block block) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            //noinspection ConstantConditions
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),
                    block.getMetaFromState(state),
                    new ModelResourceLocation(block.getRegistryName(),
                            statePropertiesToString(state.getProperties())));
        }
    }

    private static String statePropertiesToString(Map<IProperty<?>, Comparable<?>> properties) {
        StringBuilder stringbuilder = new StringBuilder();

        List<Map.Entry<IProperty<?>, Comparable<?>>> entries = properties.entrySet().stream()
                .sorted(Comparator.comparing(c -> c.getKey().getName()))
                .collect(Collectors.toList());

        for (Map.Entry<IProperty<?>, Comparable<?>> entry : entries) {
            if (stringbuilder.length() != 0) {
                stringbuilder.append(",");
            }

            IProperty<?> property = entry.getKey();
            stringbuilder.append(property.getName());
            stringbuilder.append("=");
            stringbuilder.append(getPropertyName(property, entry.getValue()));
        }

        if (stringbuilder.length() == 0) {
            stringbuilder.append("normal");
        }

        return stringbuilder.toString();
    }
    private static <T extends Comparable<T>> String getPropertyName(IProperty<T> property, Comparable<?> value) {
        return property.getName((T) value);
    }

}
