package tj.gui.widgets;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.AdoptableTextureArea;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

public class PopUpWidget<R extends PopUpWidget<R>> extends AbstractWidgetGroup {

    protected final Int2ObjectMap<Pair<Boolean, Widget>> widgetMap = new Int2ObjectOpenHashMap<>();
    protected Rectangle clickArea;
    protected AdoptableTextureArea textureArea;
    protected IntSupplier indexSupplier;
    protected int selectedIndex;

    public PopUpWidget(int x, int y, int width, int height) {
        super(new Position(x, y), new Size(width, height));
    }

    /**
     * Set resizable texture to render in the background.
     * @param textureArea resizable texture
     */
    public R setTexture(AdoptableTextureArea textureArea) {
        this.textureArea = textureArea;
        return (R) this;
    }

    /**
     * Supplier to update index.
     * @param indexSupplier index supplier
     */
    public R setIndexSupplier(IntSupplier indexSupplier) {
        this.indexSupplier = indexSupplier;
        return (R) this;
    }

    /**
     * return true in the predicate for non-selected widgets to be visible but still can not be interacted.
     * @param widgets widgets to add.
     */
    public R addWidgets(Predicate<WidgetGroup> widgets) {
        WidgetGroup widgetGroup = new WidgetGroup();
        boolean visible = widgets.test(widgetGroup);
        this.widgetMap.put(this.selectedIndex++ ,Pair.of(visible, widgetGroup));
        this.widgets.add(widgetGroup);
        return (R) this;
    }

    @Override
    public void initWidget() {
        super.initWidget();
        this.selectedIndex = 0;
    }

    @Override
    public void detectAndSendChanges() {
        if (this.indexSupplier != null) {
            this.selectedIndex = this.indexSupplier.getAsInt();
            this.writeUpdateInfo(2, buffer -> buffer.writeInt(this.selectedIndex));
        }
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey())
                widget.getValue().getRight().detectAndSendChanges();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateScreen() {
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey() || widget.getValue().getLeft())
                widget.getValue().getRight().updateScreen();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey() || widget.getValue().getLeft())
                widget.getValue().getRight().drawInForeground(mouseX, mouseY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        if (this.textureArea != null)
            this.textureArea.draw(this.getPosition().getX(), this.getPosition().getY(), this.getSize().getWidth(), this.getSize().getHeight());
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey() || widget.getValue().getLeft())
                widget.getValue().getRight().drawInBackground(mouseX, mouseY, context);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseWheelMove(int mouseX, int mouseY, int wheelDelta) {
        boolean success = false;
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey())
                success = success || widget.getValue().getRight().mouseWheelMove(mouseX, mouseY, wheelDelta);
        return success;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.clickArea != null && !this.clickArea.contains(mouseX, mouseY)) {
            this.selectedIndex = 0;
            this.writeClientAction(2, buffer -> buffer.writeInt(this.selectedIndex));
        }
        boolean success = false;
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey())
                success = success || widget.getValue().getRight().mouseClicked(mouseX, mouseY, button);
        return success;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseDragged(int mouseX, int mouseY, int button, long timeDragged) {
        boolean success = false;
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey())
                success = success || widget.getValue().getRight().mouseDragged(mouseX, mouseY, button, timeDragged);
        return success;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        boolean success = false;
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey())
                success = success || widget.getValue().getRight().mouseReleased(mouseX, mouseY, button);
        return success;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean keyTyped(char charTyped, int keyCode) {
        boolean success = false;
        for (Int2ObjectMap.Entry<Pair<Boolean, Widget>> widget : this.widgetMap.int2ObjectEntrySet())
            if (this.selectedIndex == widget.getIntKey())
                success = success || widget.getValue().getRight().keyTyped(charTyped, keyCode);
        return success;
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        super.handleClientAction(id, buffer);
        if (id == 2) {
            this.selectedIndex = buffer.readInt();
        }
    }

    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 2) {
            this.selectedIndex = buffer.readInt();
        }
    }
}
