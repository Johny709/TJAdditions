package tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import org.apache.commons.lang3.ArrayUtils;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.gui.uifactory.IPlayerUI;
import tj.gui.uifactory.PlayerHolder;
import tj.gui.widgets.OnTextFieldWidget;
import tj.gui.widgets.TJAdvancedTextWidget;
import tj.gui.widgets.TJClickButtonWidget;
import tj.gui.widgets.TJTextFieldWidget;
import tj.textures.TJSimpleOverlayRenderer;
import tj.textures.TJTextures;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.HorizontalStartCorner.LEFT;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.VerticalLocation.TOP;
import static tj.gui.TJGuiTextures.*;

public abstract class AbstractCoverEnder<K, V> extends CoverBehavior implements CoverWithUI, IPlayerUI, ITickable, IControllable {

    protected String text = "";
    protected String searchPrompt = "";
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    protected int maxTransferRate;
    protected int transferRate = maxTransferRate;
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
        getOverlay().renderSided(attachedSide, renderState, translation, pipeline);

        renderState.baseColour = TJValues.VC[getTier()] << 8;
        TJTextures.INSIDE_OVERLAY_BASE.renderSided(attachedSide, renderState, translation, pipeline);

        renderState.baseColour = oldBaseColor;
        renderState.alphaOverride = oldAlphaOverride;
        TJTextures.OUTSIDE_OVERLAY_BASE.renderSided(attachedSide, renderState, translation, pipeline);
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (!playerIn.world.isRemote) {
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

    protected abstract TJSimpleOverlayRenderer getOverlay();

    protected abstract Map<K, V> getMap();

    protected abstract void addWidgets(Consumer<Widget> widget);

    protected abstract V createHandler();

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
                .addWidget(new LabelWidget(30, 4, "metaitem.ender_energy_cover_" + GAValues.VN[this.getTier()].toLowerCase() + ".name"))
                .addTab(this.getName(), this.getPickItem(), tab -> {
                    WidgetGroup widgetGroup = new WidgetGroup(), addWidgetGroup = new WidgetGroup();
                    ScrollableListWidget listWidget = new ScrollableListWidget(3, 61, 182, 80) {
                        @Override
                        public boolean isWidgetClickable(Widget widget) {
                            return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
                        }
                    };
                    listWidget.addWidget(new TJAdvancedTextWidget(2, 3, this::addDisplayText, 0xFFFFFF) {
                        @Override
                        public boolean mouseClicked(int mouseX, int mouseY, int button) {
                            if (!isFilterPopUp())
                                return super.mouseClicked(mouseX, mouseY, button);
                            return false;
                        }
                    }.setClickHandler(this::handleDisplayClick)
                            .setMaxWidthLimit(1000));
                    widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
                    widgetGroup.addWidget(new ImageWidget(30, 38, 115, 18, DISPLAY));
                    widgetGroup.addWidget(new ImageWidget(3, 61, 170, 80, DISPLAY));
                    widgetGroup.addWidget(new ImageWidget(30, 142, 115, 18, DISPLAY));
                    widgetGroup.addWidget(new ImageWidget(-25, 33, 28, 28, BORDERED_BACKGROUND_RIGHT));
                    widgetGroup.addWidget(new TJTextFieldWidget(32, 43, 112, 18, false, this::getTextID, this::setTextID)
                            .setTextLength(256)
                            .setTooltipText("machine.universal.toggle.current.entry")
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
                    widgetGroup.addWidget(new TJTextFieldWidget(32, 20, 112, 18, false, this::getTransferRate, this::setTransferRate)
                            .setTooltipText("metaitem.ender_cover.transfer")
                            .setTooltipFormat(this::getTooltipFormat)
                            .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
                    widgetGroup.addWidget(new TJTextFieldWidget(32, 147, 112, 18, false, this::getSearchPrompt, this::setSearchPrompt)
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
                    widgetGroup.addWidget(new CycleButtonWidget(30, 161, 115, 18, CoverPump.PumpMode.class, this::getPumpMode, this::setPumpMode));
                    widgetGroup.addWidget(new ToggleButtonWidget(7, 161, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                            .setTooltipText("machine.universal.toggle.run.mode"));
                    this.addWidgets(addWidgetGroup::addWidget);
                    tab.addWidget(widgetGroup);
                    tab.addWidget(listWidget);
                    tab.addWidget(addWidgetGroup);
                })
                .addTab("names", new ItemStack(Items.NAME_TAG), tab -> {
                    ScrollableListWidget listWidget = new ScrollableListWidget(3, 38, 182, 80) {
                        @Override
                        public boolean isWidgetClickable(Widget widget) {
                            return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
                        }
                    };
                    listWidget.addWidget(new TJAdvancedTextWidget(2, 3, this::addChannelDisplayText, 0xFFFFFF)
                            .setClickHandler(this::handleDisplayClick)
                            .setMaxWidthLimit(1000));
                    tab.addWidget(new ImageWidget(3, 15, 170, 18, DISPLAY));
                    tab.addWidget(new ImageWidget(3, 38, 170, 103, DISPLAY));
                    tab.addWidget(new TJClickButtonWidget(151, 15, 18, 18, "O", this::onAddEntry)
                            .setTooltipText("machine.universal.toggle.add.entry"));
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

    private String getSearchPrompt() {
        return this.searchPrompt;
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

    private CoverPump.PumpMode getPumpMode() {
        return this.pumpMode;
    }

    private void setTextID(String text) {
        this.text = text;
        this.handler = this.getMap().get((K) this.text);
        this.markAsDirty();
    }

    private String getTextID() {
        return this.text;
    }

    private void addChannelDisplayText(List<ITextComponent> textList) {

    }

    private void addDisplayText(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("machine.universal.entries") + "§r(§e" + this.searchResults + "§r/§e" + this.getMap().size() + "§r)"));
        for (Map.Entry<K, V> entry : getMap().entrySet()) {
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
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.select"), "select:" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "rename:" + text));
            textList.add(keyEntry);
            this.addEntryText(keyEntry, entry.getKey(), entry.getValue());
            searchResults++;
        }
        this.searchResults = searchResults;
    }

    protected abstract void addEntryText(ITextComponent keyEntry, K key, V value);

    private void handleDisplayClick(String componentData, Widget.ClickData clickData, EntityPlayer player) {
        if (componentData.startsWith("select")) {
            String[] select = componentData.split(":");
            this.setTextID(select[1]);

        } else if (componentData.startsWith("remove")) {
            String[] remove = componentData.split(":");
            this.getMap().remove(remove[1]);

        } else if (componentData.startsWith("rename")) {
            String[] rename = componentData.split(":");
            this.renamePrompt = rename[1];
            PlayerHolder holder = new PlayerHolder(player, this);
            holder.openUI();
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
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.isFilterPopUp);
    }

    @Override
    public void readInitialSyncData(PacketBuffer packetBuffer) {
        this.isFilterPopUp = packetBuffer.readBoolean();
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
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.pumpMode = CoverPump.PumpMode.values()[data.getInteger("PumpMode")];
        this.isWorkingEnabled = data.getBoolean("IsWorking");
        this.isCaseSensitive = data.getBoolean("CaseSensitive");
        this.hasSpaces = data.getBoolean("HasSpaces");
        this.transferRate = data.getInteger("TransferRate");
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
