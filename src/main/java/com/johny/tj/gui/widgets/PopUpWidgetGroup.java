package com.johny.tj.gui.widgets;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.util.Position;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.BooleanSupplier;

public class PopUpWidgetGroup extends AbstractWidgetGroup {

    private final int width;
    private final int height;
    private final TextureArea textureArea;
    private BooleanSupplier setEnabled;
    private boolean isEnabled;

    public PopUpWidgetGroup(int x, int y, int width, int height, TextureArea textureArea) {
        super(new Position(x, y));
        this.width = width;
        this.height = height;
        this.textureArea = textureArea;
    }

    public PopUpWidgetGroup setEnabled(BooleanSupplier setEnabled) {
        this.setEnabled = setEnabled;
        return this;
    }

    @Override
    public void addWidget(Widget widget) {
        super.addWidget(widget);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        if (isEnabled) {
            if (textureArea != null)
                textureArea.draw(getPosition().getX(), getPosition().getY(), width, height);
            super.drawInBackground(mouseX, mouseY, context);
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        writeUpdateInfo(2, buf -> buf.writeBoolean(setEnabled.getAsBoolean()));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 2) {
            this.isEnabled = buffer.readBoolean();
            setVisible(isEnabled);
        }
    }
}
