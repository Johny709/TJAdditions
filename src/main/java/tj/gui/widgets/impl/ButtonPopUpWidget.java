package tj.gui.widgets.impl;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import tj.gui.widgets.ButtonWidget;
import tj.gui.widgets.PopUpWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ButtonPopUpWidget<T extends ButtonPopUpWidget<T>> extends PopUpWidget<T> {

    protected final List<Widget> buttons = new ArrayList<>();

    public ButtonPopUpWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Call this before any of the {@link ButtonPopUpWidget#addPopup(Predicate)} methods. These widgets are bound to the popup defined by calling the {@link ButtonPopUpWidget#addPopup(Predicate) method} mentioned
     * @param button button widgets to close this popup.
     */
    public T addClosingButton(ButtonWidget<?> button) {
        button.setButtonId(String.valueOf(0));
        button.setButtonResponder(this::handleButtonPress);
        this.pendingWidgets.add(button);
        return (T) this;
    }

    /**
     * return true in the predicate for non-selected widgets to be visible but still can not be interacted. Adds a new popup every time this method is called.
     * @param x X offset of widget group.
     * @param y Y offset of widget group.
     * @param width width of widget group.
     * @param height height of widget group.
     * @param button button widget to activate this popup.
     * @param widgets widgets to add.
     */
    public T addPopup(int x, int y, int width, int height, ButtonWidget<?> button, Predicate<WidgetGroup> widgets) {
        button.setButtonId(String.valueOf(this.selectedIndex))
                .setButtonResponder(this::handleButtonPress);
        if (button instanceof TJToggleButtonWidget)
            ((TJToggleButtonWidget) button).setButtonSupplier(() -> this.selectedIndex == button.getButtonIdAsLong());
        WidgetGroup widgetGroup = new WidgetGroup(new Position(x, y), new Size(width, height));
        boolean visible = widgets.test(widgetGroup);
        for (Widget widget : this.pendingWidgets)
            widgetGroup.addWidget(widget);
        this.addWidget(widgetGroup);
        this.addWidget(button);
        this.buttons.add(button);
        this.pendingWidgets.clear();
        this.widgetMap.put(this.selectedIndex++, Pair.of(visible, widgetGroup));
        return (T) this;
    }

    public T addPopup(Predicate<WidgetGroup> widgets) {
        this.buttons.add(null);
        return super.addPopup(widgets);
    }

    /**
     * return true in the predicate for non-selected widgets to be visible but still can not be interacted. Adds a new popup every time this method is called.
     * @param button button widget to activate this popup.
     * @param widgets widgets to add.
     */
    public T addPopup(ButtonWidget<?> button, Predicate<WidgetGroup> widgets) {
        this.addPopup(0, 0, 0, 0, button, widgets);
        return (T) this;
    }

    protected void handleButtonPress(String buttonId) {
        try {
            this.selectedIndex = Integer.parseInt(buttonId);
            this.writeUpdateInfo(2, buffer -> buffer.writeInt(this.selectedIndex));
        } catch (NumberFormatException ignored) {}
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        for (Widget widget : this.buttons)
            if (widget != null)
                widget.updateScreen();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Widget widget : this.buttons)
            if (widget != null)
                widget.detectAndSendChanges();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        super.drawInForeground(mouseX, mouseY);
        for (Widget widget : this.buttons)
            if (widget != null)
                widget.drawInForeground(mouseX, mouseY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        super.drawInBackground(mouseX, mouseY, context);
        for (Widget widget : this.buttons)
            if (widget != null)
                widget.drawInBackground(mouseX, mouseY, context);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        for (Widget widget : this.buttons)
            if (widget != null)
                widget.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
