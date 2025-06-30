package tj.gui;

import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.WidgetGroup;

public class TJWidgetGroup extends WidgetGroup {

    public TJWidgetGroup addWidgets(Widget widget) {
        super.addWidget(widget);
        return this;
    }
}
