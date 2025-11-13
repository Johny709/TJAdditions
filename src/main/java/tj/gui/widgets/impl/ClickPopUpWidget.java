package tj.gui.widgets.impl;

import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.tuple.Pair;
import tj.gui.widgets.TJAdvancedTextWidget;

import java.util.function.Predicate;

public class ClickPopUpWidget extends ButtonPopUpWidget<ClickPopUpWidget> {

    public ClickPopUpWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * return true in the predicate for non-selected widgets to be visible but still can not be interacted. Adds a new popup every time this method is called.
     * @param x X offset of widget group.
     * @param y Y offset of widget group.
     * @param width width of widget group.
     * @param height height of widget group.
     * @param textWidget text widget to activate this popup upon certain click conditions.
     * @param add set to add this text widget to this widget group
     * @param widgets widgets to add.
     */
    public ClickPopUpWidget addPopup(int x, int y, int width, int height, TJAdvancedTextWidget textWidget, boolean add, Predicate<WidgetGroup> widgets) {
        WidgetGroup widgetGroup = new WidgetGroup(new Position(x, y), new Size(width, height));
        boolean visible = widgets.test(widgetGroup);
        textWidget.setTextId(String.valueOf(this.selectedIndex))
                .addClickHandler(this::handleDisplayClick);
        if (add)
            widgetGroup.addWidget(textWidget);
        for (Widget widget : this.pendingWidgets)
            widgetGroup.addWidget(widget);
        this.addWidget(widgetGroup);
        this.pendingWidgets.clear();
        this.widgetMap.put(this.selectedIndex++, Pair.of(visible, widgetGroup));
        return this;
    }

    private void handleDisplayClick(String componentData, String textId, ClickData clickData, EntityPlayer player) {
        String[] component = componentData.split(":");
        if (component[0].equals("@Popup"))
            this.handleButtonPress(textId);
    }
}
