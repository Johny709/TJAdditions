package tj.gui.widgets;

import gregtech.api.gui.widgets.AdvancedTextWidget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.List;
import java.util.function.Consumer;

public class TJAdvancedTextWidget extends AdvancedTextWidget {

    protected TriConsumer<String, ClickData, EntityPlayer> playerClickHandler;

    public TJAdvancedTextWidget(int xPosition, int yPosition, Consumer<List<ITextComponent>> text, int color) {
        super(xPosition, yPosition, text, color);
    }

    public TJAdvancedTextWidget setClickHandler(TriConsumer<String, ClickData, EntityPlayer> playerClickHandler) {
        this.playerClickHandler = playerClickHandler;
        return this;
    }

    @Override
    public void handleClientAction(int id, PacketBuffer buffer) {
        if (id == 1) {
            ClickData clickData = ClickData.readFromBuf(buffer);
            String componentData = buffer.readString(128);
            EntityPlayer player = gui.entityPlayer;
            if (playerClickHandler != null) {
                playerClickHandler.accept(componentData, clickData, player);
            }
        }
    }
}
