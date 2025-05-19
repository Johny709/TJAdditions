package com.johny.tj.gui.widgets;

import com.google.common.base.Preconditions;
import gregtech.api.gui.widgets.ClickButtonWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TJClickButtonWidget extends ClickButtonWidget {

    private String tooltipText;
    private Supplier<String[]> formatSupplier;
    private String[] format;

    public TJClickButtonWidget(int xPosition, int yPosition, int width, int height, String displayText, Consumer<ClickData> onPressed) {
        super(xPosition, yPosition, width, height, displayText, onPressed);
    }

    /**
     * Translates the passed in String for display when cursor is hovering over this widget.
     * @param tooltipText The String text to translate
     */
    public TJClickButtonWidget setTooltipText(String tooltipText) {
        Preconditions.checkNotNull(tooltipText, "tooltipText");
        this.tooltipText = tooltipText;
        return this;
    }

    /**
     * The format args used for translating series of text for TooltipText. Text is constantly updated by the supplier.
     *
     * @apiNote
     * <p>Very similar to I18n.format() where Object... param is the series of text being translated. See setTooltipText for String translateKey param.
     * <pre>{@code
     *     I18n.format(String translateKey, Object... parameters)
     * }</pre>
     *
     * @param formatSupplier translate series of text
     */
    public TJClickButtonWidget setTooltipFormat(Supplier<String[]> formatSupplier) {
        this.formatSupplier = formatSupplier;
        this.format = new String[formatSupplier.get().length];
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        if (isMouseOverElement(mouseX, mouseY) && this.tooltipText != null) {
            String tooltipHoverString = this.tooltipText;
            String[] format = this.format != null ? this.format : ArrayUtils.toArray("");
            List<String> hoverList = Arrays.asList(I18n.format(tooltipHoverString, format).split("/n"));
            drawHoveringText(ItemStack.EMPTY, hoverList, 300, mouseX, mouseY);
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (this.formatSupplier != null)
            IntStream.range(0, this.formatSupplier.get().length)
                    .forEach(i -> writeUpdateInfo(i + 2, buffer -> buffer.writeString(this.formatSupplier.get()[i])));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (this.formatSupplier != null)
            IntStream.range(0, this.formatSupplier.get().length).forEach(i -> {
                if (i + 2 == id)
                    this.format[i] = buffer.readString(Short.MAX_VALUE);
            });
    }
}
