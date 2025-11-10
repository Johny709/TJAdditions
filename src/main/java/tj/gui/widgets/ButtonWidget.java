package tj.gui.widgets;

import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import tj.util.consumers.QuadConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ButtonWidget<R extends ButtonWidget<R>> extends Widget {

    protected QuadConsumer<String, Integer, Integer, Integer> textResponderWithMouse;
    protected Consumer<String> buttonResponder;
    protected TextureArea[] backgroundTextures;
    protected String[] format;
    protected String buttonId;
    protected String tooltipText;
    protected long buttonIdAsLong;

    public ButtonWidget(int x, int y, int width, int height) {
        super(new Position(x, y), new Size(width, height));
    }

    /**
     * Set responder for when this button gets pressed. This respond with the buttonId along with mouse click values.
     * @param textResponderWithMouse (buttonId, mouseX, mouseY, button) ->
     */
    public R setTextResponderWithMouse(QuadConsumer<String, Integer, Integer, Integer> textResponderWithMouse) {
        this.textResponderWithMouse = textResponderWithMouse;
        return (R) this;
    }

    /**
     * Set responder for when this button gets pressed. This respond with the buttonId.
     * @param buttonResponder buttonId ->
     */
    public R setButtonResponder(Consumer<String> buttonResponder) {
        this.buttonResponder = buttonResponder;
        return (R) this;
    }

    /**
     * Set buttonId that will be used to determine response type. Null will be treated as empty string. A Long version type of buttonId is available if only numbers are passed in.
     * @param buttonId button
     */
    public R setButtonId(String buttonId) {
        this.buttonId = buttonId;
        try {
            this.buttonIdAsLong = Long.parseLong(buttonId);
        } catch (NumberFormatException ignored) {}
        return (R) this;
    }

    /**
     * Add textures to render in background. Last texture passed in will be rendered on top of all the others.
     * @param backgroundTextures textures
     */
    public R setBackgroundTextures(TextureArea... backgroundTextures) {
        this.backgroundTextures = backgroundTextures;
        return (R) this;
    }

    public long getButtonIdAsLong() {
        return this.buttonIdAsLong;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInForeground(int mouseX, int mouseY) {
        if (isMouseOverElement(mouseX, mouseY) && this.tooltipText != null) {
            String tooltipHoverString = this.tooltipText;
            String[] format = this.format != null ? this.format : ArrayUtils.toArray("");
            List<String> hoverList = Arrays.asList(I18n.format(tooltipHoverString, format).split("/n"));
            this.drawHoveringText(ItemStack.EMPTY, hoverList, 300, mouseX, mouseY);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInBackground(int mouseX, int mouseY, IRenderContext context) {
        if (this.backgroundTextures != null)
            for (TextureArea textureArea : this.backgroundTextures)
                textureArea.draw(this.getPosition().getX(), this.getPosition().getY(), this.getSize().getWidth(), this.getSize().getHeight());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (this.isMouseOverElement(mouseX, mouseY)) {
            this.gui.entityPlayer.playSound(SoundEvents.UI_BUTTON_CLICK, 1, 1);
            this.writeClientAction(1, buffer -> {
                buffer.writeString(this.buttonId != null ? this.buttonId : "");
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
            int mouseX = buffer.readInt();
            int mouseY = buffer.readInt();
            int button = buffer.readInt();
            if (this.buttonResponder != null)
                this.buttonResponder.accept(buttonId);
            if (this.textResponderWithMouse != null)
                this.textResponderWithMouse.accept(buttonId, mouseX, mouseY, button);
        }
    }
}
