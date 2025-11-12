package tj.gui.widgets.impl;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.resources.SizedTextureArea;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tj.gui.widgets.ButtonWidget;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;

public class TJToggleButtonWidget extends ButtonWidget<TJToggleButtonWidget> {

    private boolean useToggleTexture;
    private boolean isPressed;
    private TextureArea toggleTexture;
    private TextureArea activeTexture;
    private TextureArea baseTexture;
    private BooleanSupplier isPressedCondition;

    public TJToggleButtonWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Supplier to get the state of button.
     * @param isPressedCondition is button pressed
     */
    @Nonnull
    public TJToggleButtonWidget setPressedCondition(BooleanSupplier isPressedCondition) {
        this.isPressedCondition = isPressedCondition;
        return this;
    }

    /**
     * Toggle this mode to use button texture with On-Off state. Default: false.
     * @param useToggleTexture set to use toggle button texture
     */
    public TJToggleButtonWidget useToggleTexture(boolean useToggleTexture) {
        this.useToggleTexture = useToggleTexture;
        return this;
    }

    /**
     * Set texture for button with an On-Off state.
     * @param toggleTexture toggle button texture
     */
    public TJToggleButtonWidget setToggleTexture(TextureArea toggleTexture) {
        this.toggleTexture = toggleTexture;
        return this;
    }

    /**
     * The texture shown when the button is pressed.
     * @param activeTexture active button texture
     */
    public TJToggleButtonWidget setActiveTexture(TextureArea activeTexture) {
        this.activeTexture = activeTexture;
        return this;
    }

    /**
     * The texture shown when the button is not pressed.
     * @param baseTexture base button texture
     */
    public TJToggleButtonWidget setBaseTexture(TextureArea baseTexture) {
        this.baseTexture = baseTexture;
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        Position pos = this.getPosition();
        Size size = this.getSize();
        if (!this.useToggleTexture) {
            if (this.isPressedCondition.getAsBoolean())
                this.activeTexture.draw(pos.getX(), pos.getY(), size.getWidth(), size.getHeight());
            else this.baseTexture.draw(pos.getX(), pos.getY(), size.getWidth(), size.getHeight());
        } else if (this.toggleTexture instanceof SizedTextureArea) {
            ((SizedTextureArea) this.toggleTexture).drawHorizontalCutSubArea(pos.x, pos.y, size.width, size.height, this.isPressed ? 0.5 : 0.0, 0.5);
        } else {
            this.toggleTexture.drawSubArea(pos.x, pos.y, size.width, size.height, 0.0, this.isPressed ? 0.5 : 0.0, 1.0, 0.5);
        }
        super.drawInBackground(mouseX, mouseY, context);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!this.isPressed && this.isMouseOverElement(mouseX, mouseY)) {
            this.playButtonClickSound();
            this.isPressed = true;
            this.writeClientAction(1, buffer -> {
                buffer.writeString(this.buttonId != null ? this.buttonId : "");
                buffer.writeBoolean(this.isPressed);
                buffer.writeInt(mouseX);
                buffer.writeInt(mouseY);
                buffer.writeInt(button);
            });
            return true;
        }
        return false;
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        if (id == 1) {
            String buttonId = buffer.readString(Short.MAX_VALUE);
            this.isPressed = buffer.readBoolean();
            int mouseX = buffer.readInt();
            int mouseY = buffer.readInt();
            int button = buffer.readInt();
            if (this.buttonResponder != null)
                this.buttonResponder.accept(buttonId);
            if (this.textResponderWithMouse != null)
                this.textResponderWithMouse.accept(buttonId, mouseX, mouseY, button);
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (this.isPressedCondition != null) {
            this.isPressed = this.isPressedCondition.getAsBoolean();
            this.writeUpdateInfo(3, buffer -> buffer.writeBoolean(this.isPressed));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 3)
            this.isPressed = buffer.readBoolean();
    }
}
