package tj.gui.widgets;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IWidgetGroup {

    @SideOnly(Side.CLIENT)
    int getTimer();

    @SideOnly(Side.CLIENT)
    void setTimer(int timer);

}
