package tj.gui.widgets.impl;

import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import org.apache.commons.lang3.tuple.Pair;
import tj.gui.widgets.ButtonWidget;
import tj.gui.widgets.PopUpWidget;

import java.util.function.Predicate;

public class ButtonPopUpWidget<T extends ButtonPopUpWidget<T>> extends PopUpWidget<T> {

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
     * call {@link #addClosingButton(ButtonWidget)} before this to add closing buttons to close this popup.
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
        this.widgetMap.get(0).getRight().addWidget(button);
        this.addWidget(widgetGroup);
        this.pendingWidgets.clear();
        this.widgetMap.put(this.selectedIndex++, Pair.of(visible, widgetGroup));
        return (T) this;
    }

    /**
     * return true in the predicate for non-selected widgets to be visible but still can not be interacted. Adds a new popup every time this method is called.
     * call {@link #addClosingButton(ButtonWidget)} before this to add closing buttons to close this popup.
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
}
