package tj.gui.widgets.impl;

import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.ScrollableListWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import org.apache.commons.lang3.tuple.Pair;
import tj.gui.widgets.TJAdvancedTextWidget;

import java.util.function.Predicate;

public class ClickPopUpWidget extends ButtonPopUpWidget<ClickPopUpWidget> {

    public ClickPopUpWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ClickPopUpWidget addWidgets(int x, int y, int width, int height, TJAdvancedTextWidget textWidget, Predicate<WidgetGroup> widgets) {
        WidgetGroup widgetGroup = new WidgetGroup(new Position(x, y), new Size(width, height));
        boolean visible = widgets.test(widgetGroup);
        ScrollableListWidget listWidget = new ScrollableListWidget(x, y, width, height) {
            @Override
            public boolean isWidgetClickable(Widget widget) {
                return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
            }
        };
        listWidget.addWidget(textWidget.setClickHandler(this::handleDisplayClick));
        widgetGroup.addWidget(listWidget);
        this.widgetMap.put(this.selectedIndex++, Pair.of(visible, widgetGroup));
        this.buttons.add(null);
        this.addWidget(widgetGroup);
        return this;
    }

    private void handleDisplayClick(String componentData, ClickData clickData) {

    }
}
