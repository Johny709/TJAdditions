package tj.gui.widgets;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.util.Position;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class PopUpWidgetGroup extends AbstractWidgetGroup {

    private final int width;
    private final int height;
    private final TextureArea textureArea;
    private boolean isEnabled;

    public PopUpWidgetGroup(int x, int y, int width, int height, TextureArea textureArea) {
        super(new Position(x, y));
        this.width = width;
        this.height = height;
        this.textureArea = textureArea;
    }

    public void setEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
        this.writeUpdateInfo(2, buffer -> buffer.writeBoolean(isEnabled));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible && this.isEnabled);
    }

    @Override
    public void addWidget(Widget widget) {
        super.addWidget(widget);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        if (this.isEnabled) {
            if (this.textureArea != null)
                this.textureArea.draw(getPosition().getX(), getPosition().getY(), width, height);
            super.drawInBackground(mouseX, mouseY, context);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 2) {
            this.isEnabled = buffer.readBoolean();
            this.setVisible(this.isEnabled);
        }
    }
}
