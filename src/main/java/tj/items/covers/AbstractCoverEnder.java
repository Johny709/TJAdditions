package tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.capability.IControllable;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverBehaviorUIFactory;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.HorizontalTabListRenderer;
import gregtech.common.covers.CoverPump;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.gui.uifactory.IPlayerUI;
import tj.gui.uifactory.PlayerHolder;
import tj.gui.widgets.*;
import tj.gui.widgets.impl.ButtonPopUpWidget;
import tj.gui.widgets.impl.ClickPopUpWidget;
import tj.textures.TJSimpleOverlayRenderer;
import tj.textures.TJTextures;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.HorizontalStartCorner.LEFT;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.VerticalLocation.TOP;
import static tj.gui.TJGuiTextures.*;

public abstract class AbstractCoverEnder<K, V> extends CoverBehavior implements CoverWithUI, IPlayerUI, ITickable, IControllable {

    protected String text = "";
    protected String channel;
    protected UUID ownerId;
    protected String searchPrompt = "";
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    protected int maxTransferRate;
    protected int transferRate = maxTransferRate;
    protected int timer;
    protected boolean isFilterPopUp;
    protected boolean isCaseSensitive;
    protected boolean hasSpaces;
    private int searchResults;
    private String renamePrompt = "";
    protected V handler;

    public AbstractCoverEnder(ICoverable coverHolder, EnumFacing attachedSide) {
        super(coverHolder, attachedSide);
    }

    @Override
    public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline, Cuboid6 cuboid6, BlockRenderLayer blockRenderLayer) {
        int oldBaseColor = renderState.baseColour;
        int oldAlphaOverride = renderState.alphaOverride;

        renderState.baseColour = getPortalColor() << 8;
        renderState.alphaOverride = 0xFF;
        this.getOverlay().renderSided(attachedSide, renderState, translation, pipeline);

        renderState.baseColour = TJValues.VC[getTier()] << 8;
        TJTextures.INSIDE_OVERLAY_BASE.renderSided(attachedSide, renderState, translation, pipeline);

        renderState.baseColour = oldBaseColor;
        renderState.alphaOverride = oldAlphaOverride;
        TJTextures.OUTSIDE_OVERLAY_BASE.renderSided(attachedSide, renderState, translation, pipeline);
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (!playerIn.world.isRemote) {
            if (this.ownerId == null) {
                this.ownerId = playerIn.getUniqueID();
                this.writeUpdateData(2, buffer -> buffer.writeUniqueId(this.ownerId));
            }
            this.openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }

    protected int getPortalColor() {
        return 0xffffff;
    }

    protected int getTier() {
        return 0;
    }

    protected String getName() {
        return "Null";
    }

    protected CoverEnderProfile getEnderProfile()  {
        return this.getPlayerMap().getOrDefault(this.channel, this.getPlayerMap().get(null)).getLeft();
    }

    protected abstract TJSimpleOverlayRenderer getOverlay();

    protected abstract Map<String, Pair<CoverEnderProfile, Map<K, V>>> getPlayerMap();

    protected abstract Map<K, V> getMap();

    protected abstract void addWidgets(Consumer<Widget> widget);

    protected void addToPopUpWidget(ButtonPopUpWidget<?> buttonPopUpWidget) {}

    protected abstract V createHandler();

    protected void addChannel(Widget.ClickData clickData) {
        this.getPlayerMap().putIfAbsent(this.channel, Pair.of(new CoverEnderProfile(this.ownerId), new Object2ObjectOpenHashMap<>()));
    }

    protected void onAddEntry(Widget.ClickData clickData) {
        this.getMap().putIfAbsent((K) this.text, this.createHandler());
    }

    protected void onClear(Widget.ClickData clickData) {
        if (this.getMap().containsKey((K) this.text)) {
            this.getMap().put((K) this.text, this.createHandler());
        }
    }

    private String getRename() {
        return this.renamePrompt;
    }

    private void setRename(String name) {
        V value = this.getMap().get((K) this.renamePrompt);
        this.getMap().remove((K) this.renamePrompt);
        this.handler = this.getMap().put((K) name, value);
    }

    private void onPlayerPressed(Widget.ClickData clickData, EntityPlayer player) {
        CoverBehaviorUIFactory.INSTANCE.openUI(this, (EntityPlayerMP) player);
    }

    @Override
    public ModularUI createUI(PlayerHolder holder, EntityPlayer player) {
        ModularUI.Builder builder = ModularUI.builder(BORDERED_BACKGROUND, 176, 80);
        builder.widget(new ImageWidget(10, 10, 156, 18, DISPLAY));
        OnTextFieldWidget onTextFieldWidget = new OnTextFieldWidget(15, 15, 151, 18, false, this::getRename, this::setRename);
        onTextFieldWidget.setTooltipText("machine.universal.set.name");
        onTextFieldWidget.setBackgroundText("machine.universal.set.name");
        onTextFieldWidget.setTextLength(256);
        onTextFieldWidget.setValidator(str -> Pattern.compile(".*").matcher(str).matches());
        builder.widget(onTextFieldWidget);
        builder.widget(new TJClickButtonWidget(10, 38, 156, 18, "OK", onTextFieldWidget::onResponder)
                .setClickHandler(this::onPlayerPressed));
        return builder.build(holder, player);
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetTabBuilder tabBuilder = new WidgetTabBuilder()
                .setTabListRenderer(() -> new HorizontalTabListRenderer(LEFT, TOP))
                .addWidget(new LabelWidget(30, 4, this.getName()))
                .addTab(this.getName(), this.getPickItem(), tab -> {
                    ClickPopUpWidget buttonPopUpWidget = new ClickPopUpWidget(0, 0, 0, 0)
                            .addWidgets(widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(30, 38, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(3, 61, 170, 80, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(30, 142, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(-25, 33, 28, 28, BORDERED_BACKGROUND_RIGHT));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 43, 112, 18, false)
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                        .setBackgroundText("machine.universal.toggle.current.entry")
                                        .setTooltipText("machine.universal.toggle.current.entry")
                                        .setTextResponder(this::setTextID)
                                        .setTextSupplier(() -> this.text)
                                        .setMaxStringLength(256));
                                widgetGroup.addWidget(new TJTextFieldWidget(32, 20, 112, 18, false, this::getTransferRate, this::setTransferRate)
                                        .setTooltipText("metaitem.ender_cover.transfer")
                                        .setTooltipFormat(this::getTooltipFormat)
                                        .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
                                widgetGroup.addWidget(new TJTextFieldWidget(32, 147, 112, 18, false, () -> this.searchPrompt, this::setSearchPrompt)
                                        .setTextLength(256)
                                        .setBackgroundText("machine.universal.search")
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
                                widgetGroup.addWidget(new TJClickButtonWidget(151, 38, 18, 18, "O", this::onAddEntry)
                                        .setTooltipText("machine.universal.toggle.add.entry"));
                                widgetGroup.addWidget(new TJClickButtonWidget(151, 15, 18, 18, "+", this::onIncrement)
                                        .setTooltipText("machine.universal.toggle.increment.disabled"));
                                widgetGroup.addWidget(new TJClickButtonWidget(7, 15, 18, 18, "-", this::onDecrement)
                                        .setTooltipText("machine.universal.toggle.decrement.disabled"));
                                widgetGroup.addWidget(new TJClickButtonWidget(-20, 38, 18, 18, "", this::onClear)
                                        .setTooltipText("machine.universal.toggle.clear")
                                        .setButtonTexture(BUTTON_CLEAR_GRID));
                                widgetGroup.addWidget(new ToggleButtonWidget(7, 142, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                                        .setTooltipText("machine.universal.case_sensitive"));
                                widgetGroup.addWidget(new ToggleButtonWidget(151, 142, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                                        .setTooltipText("machine.universal.spaces"));
                                widgetGroup.addWidget(new CycleButtonWidget(30, 161, 115, 18, CoverPump.PumpMode.class, () -> this.pumpMode, this::setPumpMode));
                                widgetGroup.addWidget(new ToggleButtonWidget(7, 161, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                                        .setTooltipText("machine.universal.toggle.run.mode"));
                                widgetGroup.addWidget(listWidget);
                                this.addWidgets(widgetGroup::addWidget);
                                return true;
                            }).addWidgets(3, 61, 182, 80, new TJAdvancedTextWidget(2, 3, this::addDisplayText, 0xFFFFFF)
                                    .setClickHandler(this::handleDisplayClick)
                                    .setMaxWidthLimit(1000), widgetGroup -> );
                    this.addToPopUpWidget(buttonPopUpWidget);
                    tab.addWidget(buttonPopUpWidget);
                })
                .addTab("machine.universal.channels", new ItemStack(Item.getByNameOrId("appliedenergistics2:part"), 1, 76),tab -> {
                    ScrollableListWidget listWidget = new ScrollableListWidget(3, 38, 182, 103) {
                        @Override
                        public boolean isWidgetClickable(Widget widget) {
                            return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
                        }
                    };
                    listWidget.addWidget(new TJAdvancedTextWidget(2, 3, this::addChannelDisplayText, 0xFFFFFF)
                            .setClickHandler(this::handleDisplayClick)
                            .setMaxWidthLimit(1000));
                    tab.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
                    tab.addWidget(new ImageWidget(3, 38, 170, 103, DISPLAY));
                    tab.addWidget(new NewTextFieldWidget<>(32, 20, 112, 18)
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                            .setBackgroundText("machine.universal.toggle.current.channel")
                            .setTooltipText("machine.universal.toggle.current.channel")
                            .setTextSupplier(() -> this.channel)
                            .setTextResponder(this::setChannel)
                            .setMaxStringLength(256));
                    tab.addWidget(new TJClickButtonWidget(151, 15, 18, 18, "O", this::addChannel)
                            .setTooltipText("machine.universal.toggle.add.channel"));
                    tab.addWidget(new ToggleButtonWidget(7, 15, 18, 18, UNLOCK_LOCK, () -> this.getEnderProfile().isPublic(), this::setPublic)
                            .setTooltipText("metaitem.ender_cover.private"));
                    tab.addWidget(new LabelWidget(3, 150, "machine.universal.owner", this.ownerId));
                    tab.addWidget(listWidget);
                });
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 262)
                .bindPlayerInventory(player.inventory, 181)
                .widget(tabBuilder.build())
                .widget(tabBuilder.buildWidgetGroup())
                .build(this, player);
    }

    private void setSearchPrompt(String searchPrompt) {
        this.searchPrompt = searchPrompt;
        this.markAsDirty();
    }

    private String[] getTooltipFormat() {
        return ArrayUtils.toArray(getTransferRate());
    }

    private void setTransferRate(String amount) {
        this.transferRate = Math.min(Integer.parseInt(amount), this.maxTransferRate);
        this.markAsDirty();
    }

    public String getTransferRate() {
        return String.valueOf(this.transferRate);
    }

    private void onIncrement(Widget.ClickData clickData) {
        this.transferRate = MathHelper.clamp(this.transferRate * 2, 1, this.maxTransferRate);
        this.markAsDirty();
    }

    private void onDecrement(Widget.ClickData clickData) {
        this.transferRate = MathHelper.clamp(this.transferRate / 2, 1, this.maxTransferRate);
        this.markAsDirty();
    }

    private void setPumpMode(CoverPump.PumpMode pumpMode) {
        this.pumpMode = pumpMode;
        this.markAsDirty();
    }

    private void setPublic(boolean isPublic) {
        this.getPlayerMap().get(this.channel).getLeft().setPublic(isPublic);
        this.markAsDirty();
    }

    private void setChannel(String channel) {
        this.channel = channel;
        this.handler = this.getMap().get((K) this.text);
        this.markAsDirty();
    }

    private void setTextID(String text) {
        this.text = text;
        this.handler = this.getMap().get((K) this.text);
        this.markAsDirty();
    }

    private void addChannelDisplayText(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("machine.universal.channels") + "§r(§e" + this.searchResults + "§r/§e" + this.getPlayerMap().size() + "§r)"));
        for (Map.Entry<String, Pair<CoverEnderProfile, Map<K, V>>> entry : this.getPlayerMap().entrySet()) {
            String text =  entry.getKey() != null ? entry.getKey() : "PUBLIC";
            String result = text;

            if (!this.isCaseSensitive)
                result = result.toLowerCase();

            if (!this.hasSpaces)
                result = result.replace(" ", "");

            if (!result.isEmpty() && !result.contains(this.searchPrompt))
                continue;

            textList.add(new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.select"), "select:channel:" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:channel:" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "rename:channel:" + text))
                    .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.owner", entry.getValue().getLeft().getOwner())))));
        }
    }

    private void addDisplayText(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("machine.universal.entries") + "§r(§e" + this.searchResults + "§r/§e" + this.getMap().size() + "§r)"));
        for (Map.Entry<K, V> entry : this.getMap().entrySet()) {
            String text = (String) entry.getKey();
            String result = text;

            if (!this.isCaseSensitive)
                result = result.toLowerCase();

            if (!this.hasSpaces)
                result = result.replace(" ", "");

            if (!result.isEmpty() && !result.contains(this.searchPrompt))
                continue;

            ITextComponent keyEntry = new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.select"), "select:entry:" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:entry:" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "rename:entry:" + text));
            textList.add(keyEntry);
            this.addEntryText(keyEntry, entry.getKey(), entry.getValue());
            searchResults++;
        }
        this.searchResults = searchResults;
    }

    protected abstract void addEntryText(ITextComponent keyEntry, K key, V value);

    private void handleDisplayClick(String componentData, Widget.ClickData clickData, EntityPlayer player) {
        String[] components = componentData.split(":");
        if (this.ownerId != null && !this.ownerId.equals(player.getUniqueID()))
            return;
        switch (components[0]) {
            case "select":
                if (components[1].equals("entry"))
                    this.setTextID(components[2]);
                else this.setChannel(components[2]);
                break;
            case "remove":
                if (components[1].equals("entry"))
                    this.getMap().remove(components[2]);
                else this.getPlayerMap().remove(components[2]);
                break;
            case "rename":
                this.renamePrompt = components[2];
                PlayerHolder holder = new PlayerHolder(player, this);
                holder.openUI();
                break;
        }
    }

    protected void setFilterPopUp(boolean isFilterPopUp) {
        this.isFilterPopUp = isFilterPopUp;
        this.writeUpdateData(1, buffer -> buffer.writeBoolean(this.isFilterPopUp));
        this.markAsDirty();
    }

    protected boolean isFilterPopUp() {
        return this.isFilterPopUp;
    }

    @Override
    public void readUpdateData(int id, PacketBuffer packetBuffer) {
        if (id == 1) {
            this.isFilterPopUp = packetBuffer.readBoolean();
        } else if (id == 2) {
            this.ownerId = packetBuffer.readUniqueId();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.isFilterPopUp);
        packetBuffer.writeBoolean(this.ownerId != null);
        if (this.ownerId != null)
            packetBuffer.writeUniqueId(this.ownerId);
    }

    @Override
    public void readInitialSyncData(PacketBuffer packetBuffer) {
        this.isFilterPopUp = packetBuffer.readBoolean();
        if (packetBuffer.readBoolean())
            this.ownerId = packetBuffer.readUniqueId();
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("PumpMode", this.pumpMode.ordinal());
        data.setBoolean("IsWorking", this.isWorkingEnabled);
        data.setBoolean("CaseSensitive", this.isCaseSensitive);
        data.setBoolean("HasSpaces", this.hasSpaces);
        data.setInteger("TransferRate", this.transferRate);
        data.setString("Text", this.text);
        if (this.channel != null)
            data.setString("channel", this.channel);
        if (this.ownerId != null)
            data.setUniqueId("ownerId", this.ownerId);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.pumpMode = CoverPump.PumpMode.values()[data.getInteger("PumpMode")];
        this.isWorkingEnabled = data.getBoolean("IsWorking");
        this.isCaseSensitive = data.getBoolean("CaseSensitive");
        this.hasSpaces = data.getBoolean("HasSpaces");
        this.transferRate = data.getInteger("TransferRate");
        if (data.hasKey("channel"))
            this.channel = data.getString("channel");
        if (data.hasKey("ownerId"))
            this.ownerId = data.getUniqueId("ownerId");
        if (data.hasKey("Text")) {
            this.text = data.getString("Text");
            this.handler = this.getMap().get((K) this.text);
        }
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingEnabled) {
        this.isWorkingEnabled = isWorkingEnabled;
        this.markAsDirty();
    }

    private boolean isCaseSensitive() {
        return this.isCaseSensitive;
    }

    private void setCaseSensitive(Boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
        this.markAsDirty();
    }

    private boolean hasSpaces() {
        return this.hasSpaces;
    }

    private void setSpaces(Boolean hasSpaces) {
        this.hasSpaces = hasSpaces;
        this.markAsDirty();
    }
}
