package tj.items.item;

import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;

public class TJItems {

    public static Item UNBREAKABLE_AXE;
    public static Item UNBREAKABLE_HOE;
    public static Item UNBREAKABLE_SHEARS;

    public static void init(IForgeRegistry<Item> registry) {
        UNBREAKABLE_AXE = registerItem(registry, new UnbreakableAxe(Item.ToolMaterial.DIAMOND));
        UNBREAKABLE_HOE = registerItem(registry, new UnbreakableHoe(Item.ToolMaterial.DIAMOND));
        UNBREAKABLE_SHEARS = registerItem(registry, new UnbreakableShears());
    }

    private static Item registerItem(IForgeRegistry<Item> registry, Item item) {
        registry.register(item);
        return item;
    }
}
