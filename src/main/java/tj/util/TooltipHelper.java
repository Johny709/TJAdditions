package tj.util;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TooltipHelper {

    public static String blinking(Color color, int ticks) {
        return FMLClientHandler.instance().getWorldClient().getTotalWorldTime() % ticks < ticks / 2 ? String.valueOf(color) : "";
    }

    public static String blinkingText(Color color, int ticks, String locale, Object... params) {
        return (FMLClientHandler.instance().getWorldClient().getTotalWorldTime() % ticks < ticks / 2 ? color : "") + I18n.format(locale, params);
    }

    public static String rainbow(int ticks) {
        int ordinal = (int) ((FMLClientHandler.instance().getWorldClient().getTotalWorldTime() % (Color.values().length * ticks)) / ticks);
        return String.valueOf(Color.values()[ordinal]);
    }

    public static String rainbowText(int ticks, String locale, Object... params) {
        int ordinal = (int) ((FMLClientHandler.instance().getWorldClient().getTotalWorldTime() % (Color.values().length * ticks)) / ticks);
        return Color.values()[ordinal] + I18n.format(locale, params);
    }
}
