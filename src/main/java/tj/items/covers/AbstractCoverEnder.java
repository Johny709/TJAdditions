package tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.capability.IControllable;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.HorizontalTabListRenderer;
import gregtech.common.covers.CoverPump;
import it.unimi.dsi.fastutil.objects.*;
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
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.gui.widgets.*;
import tj.gui.widgets.impl.ButtonPopUpWidget;
import tj.gui.widgets.impl.ClickPopUpWidget;
import tj.gui.widgets.impl.ScrollableTextWidget;
import tj.gui.widgets.impl.TJToggleButtonWidget;
import tj.textures.TJSimpleOverlayRenderer;
import tj.textures.TJTextures;
import tj.util.consumers.QuadConsumer;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.HorizontalStartCorner.LEFT;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.VerticalLocation.TOP;
import static net.minecraft.util.text.TextFormatting.GRAY;
import static net.minecraft.util.text.TextFormatting.YELLOW;
import static tj.gui.TJGuiTextures.*;

public abstract class AbstractCoverEnder<K, V> extends CoverBehavior implements CoverWithUI, ITickable, IControllable {

    protected String text = "";
    protected String channel;
    protected UUID ownerId;
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    protected int maxTransferRate;
    protected int transferRate = maxTransferRate;
    protected int timer;
    protected boolean isFilterPopUp;
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

    protected void addChannel(String name, String id) {
        this.getPlayerMap().putIfAbsent(name, Pair.of(new CoverEnderProfile(this.ownerId), new Object2ObjectOpenHashMap<>()));
    }

    protected void addEntry(String name, String id) {
        if (name != null)
            this.getMap().putIfAbsent((K) name, this.createHandler());
    }

    protected void onClear(Widget.ClickData clickData) {
        if (this.getMap().containsKey((K) this.text)) {
            this.getMap().put((K) this.text, this.createHandler());
        }
    }

    private void renameChannel(String name, String id) {
        Pair<CoverEnderProfile, Map<K, V>> map = this.getPlayerMap().get(id);
        if (map != null && map.getLeft().getOwner() != null && map.getLeft().getOwner().equals(this.ownerId)) {
            this.getPlayerMap().remove(id);
            this.getPlayerMap().put(name, map);
        }
    }

    private void renameEntry(String name, String id) {
        V value = this.getMap().get(id);
        if (value != null) {
            this.text = name;
            this.getMap().remove(id);
            this.handler = this.getMap().put((K) name, value);
        }
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        List<Integer> searchResults = Arrays.asList(0, 0, 0);
        List<Integer> patternFlags = Arrays.asList(0, 0, 0);
        List<String> search = Arrays.asList("", "", "");
        WidgetTabBuilder tabBuilder = new WidgetTabBuilder()
                .setTabListRenderer(() -> new HorizontalTabListRenderer(LEFT, TOP))
                .addWidget(new LabelWidget(30, 4, this.getName()))
                .addTab(this.getName(), this.getPickItem(), tab -> {
                    NewTextFieldWidget<?> textFieldWidgetRename = new NewTextFieldWidget<>(12, 20, 159, 13)
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                            .setBackgroundText("machine.universal.toggle.rename.entry")
                            .setTooltipText("machine.universal.toggle.rename.entry")
                            .setTextResponder(this::renameEntry)
                            .setMaxStringLength(256);
                    NewTextFieldWidget<?> textFieldWidgetEntry = new NewTextFieldWidget<>(12, 20, 159, 13)
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                            .setBackgroundText("machine.universal.toggle.add.entry")
                            .setTooltipText("machine.universal.toggle.add.entry")
                            .setTextResponder(this::addEntry)
                            .setMaxStringLength(256);
                    TJAdvancedTextWidget textWidget = new TJAdvancedTextWidget(2, 3, this.addEntryDisplayText(searchResults, patternFlags, search), 0xFFFFFF)
                            .addClickHandler(this.handleDisplayClick(textFieldWidgetRename));
                    textWidget.setMaxWidthLimit(1000);
                    ClickPopUpWidget clickPopUpWidget = new ClickPopUpWidget(0, 0, 0, 0)
                            .addPopup(widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(30, 38, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(3, 61, 170, 80, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(30, 142, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(-25, 33, 28, 28, BORDERED_BACKGROUND_RIGHT));
                                widgetGroup.addWidget(new ScrollableTextWidget(3, 61, 182, 80)
                                        .addTextWidget(textWidget));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 43, 112, 13, false)
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                        .setBackgroundText("machine.universal.toggle.current.entry")
                                        .setTooltipText("machine.universal.toggle.current.entry")
                                        .setTextResponder(this::setTextID)
                                        .setTextSupplier(() -> this.text)
                                        .setMaxStringLength(256)
                                        .setUpdateOnTyping(true));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 20, 112, 13, false)
                                        .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches())
                                        .setBackgroundText("metaitem.ender_cover.transfer")
                                        .setTooltipText("metaitem.ender_cover.transfer")
                                        .setTooltipFormat(this::getTooltipFormat)
                                        .setTextResponder(this::setTransferRate)
                                        .setTextSupplier(this::getTransferRate)
                                        .setUpdateOnTyping(true));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 147, 112, 13, false)
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                        .setBackgroundText("machine.universal.search")
                                        .setTextSupplier(() -> search.get(0))
                                        .setTextResponder((result, id) -> search.set(0, result))
                                        .setMaxStringLength(256)
                                        .setUpdateOnTyping(true));
                                widgetGroup.addWidget(new TJClickButtonWidget(151, 15, 18, 18, "+", this::onIncrement)
                                        .setTooltipText("machine.universal.toggle.increment.disabled"));
                                widgetGroup.addWidget(new TJClickButtonWidget(7, 15, 18, 18, "-", this::onDecrement)
                                        .setTooltipText("machine.universal.toggle.decrement.disabled"));
                                widgetGroup.addWidget(new TJClickButtonWidget(-20, 38, 18, 18, "", this::onClear)
                                        .setTooltipText("machine.universal.toggle.clear")
                                        .setButtonTexture(BUTTON_CLEAR_GRID));
                                widgetGroup.addWidget(new CycleButtonWidget(30, 161, 115, 18, CoverPump.PumpMode.class, () -> this.pumpMode, this::setPumpMode));
                                widgetGroup.addWidget(new ToggleButtonWidget(7, 161, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                                        .setTooltipText("machine.universal.toggle.run.mode"));
                                this.addWidgets(widgetGroup::addWidget);
                                return true;
                            }).addPopup(112, 61, 60, 78, new TJToggleButtonWidget(151, 142, 18, 18)
                                    .setTooltipText("machine.universal.search.settings")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .useToggleTexture(true)
                                    .setDisplayText("⚙"), widgetGroup -> {
                                    widgetGroup.addWidget(new ImageWidget(0, 0, 60, 78, BORDERED_BACKGROUND));
                                    widgetGroup.addWidget(new ImageWidget(3, 57, 54, 18, DISPLAY));
                                    widgetGroup.addWidget(new AdvancedTextWidget(5, 62, textList -> textList.add(new TextComponentTranslation("string.regex.flag", patternFlags.get(0))), 0x404040));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(3, 3, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.UNIX_LINES))
                                            .setDisplayText("string.regex.pattern.unix_lines.flag")
                                            .setTooltipText("string.regex.pattern.unix_lines")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(21, 3, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.CASE_INSENSITIVE))
                                            .setDisplayText("string.regex.pattern.case_insensitive.flag")
                                            .setTooltipText("string.regex.pattern.case_insensitive")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(39, 3, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.COMMENTS))
                                            .setDisplayText("string.regex.pattern.comments.flag")
                                            .setTooltipText("string.regex.pattern.comments")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(3, 21, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.MULTILINE))
                                            .setDisplayText("string.regex.pattern.multiline.flag")
                                            .setTooltipText("string.regex.pattern.multiline")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(21, 21, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.LITERAL))
                                            .setDisplayText("string.regex.pattern.literal.flag")
                                            .setTooltipText("string.regex.pattern.literal")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(39, 21, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.DOTALL))
                                            .setDisplayText("string.regex.pattern.dotall.flag")
                                            .setTooltipText("string.regex.pattern.dotall")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(3, 39, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.UNICODE_CASE))
                                            .setDisplayText("string.regex.pattern.unicode_case.flag")
                                            .setTooltipText("string.regex.pattern.unicode_case")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(21, 39, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.CANON_EQ))
                                            .setDisplayText("string.regex.pattern.canon_eq.flag")
                                            .setTooltipText("string.regex.pattern.canon_eq")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    widgetGroup.addWidget(new TJToggleButtonWidget(39, 39, 18, 18)
                                            .setButtonResponder(s -> patternFlags.set(0, Pattern.UNICODE_CHARACTER_CLASS))
                                            .setDisplayText("string.regex.pattern.unicode_character_class.flag")
                                            .setTooltipText("string.regex.pattern.unicode_character_class")
                                            .setToggleTexture(TOGGLE_BUTTON_BACK)
                                            .setPressedCondition(() -> false)
                                            .useToggleTexture(true));
                                    return false;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetRename::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addPopup(0, 61, 182, 60, textWidget, false, widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 60, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(10, 15, 162, 18, DISPLAY));
                                widgetGroup.addWidget(new AdvancedTextWidget(45, 4, textList -> textList.add(new TextComponentTranslation("machine.universal.renaming", textFieldWidgetRename.getTextId())), 0x404040));
                                widgetGroup.addWidget(textFieldWidgetRename);
                                return false;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetEntry::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addPopup(0, 61, 182, 60, new TJToggleButtonWidget(151, 38, 18, 18)
                                    .setTooltipText("machine.universal.toggle.add.entry")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .useToggleTexture(true)
                                    .setDisplayText("O"), widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 60, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(10, 15, 162, 18, DISPLAY));
                                widgetGroup.addWidget(new AdvancedTextWidget(55, 4, textList -> textList.add(new TextComponentTranslation("machine.universal.toggle.add.entry")), 0x404040));
                                widgetGroup.addWidget(textFieldWidgetEntry);
                                return false;
                            });
                    this.addToPopUpWidget(clickPopUpWidget);
                    tab.addWidget(clickPopUpWidget);
                }).addTab("machine.universal.channels", new ItemStack(Item.getByNameOrId("appliedenergistics2:part"), 1, 76), tab -> {
                    NewTextFieldWidget<?> textFieldWidgetRename = new NewTextFieldWidget<>(12, 20, 159, 13)
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                            .setBackgroundText("machine.universal.toggle.rename.channel")
                            .setTooltipText("machine.universal.toggle.rename.channel")
                            .setTextResponder(this::renameChannel)
                            .setMaxStringLength(256);
                    NewTextFieldWidget<?> textFieldWidgetChannel = new NewTextFieldWidget<>(12, 20, 159, 13)
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                            .setBackgroundText("machine.universal.toggle.add.channel")
                            .setTooltipText("machine.universal.toggle.add.channel")
                            .setTextResponder(this::addChannel)
                            .setMaxStringLength(256);
                    TJAdvancedTextWidget textWidget = new TJAdvancedTextWidget(2, 3, this.addChannelDisplayText(searchResults, patternFlags, search), 0xFFFFFF)
                            .addClickHandler(this.handleDisplayClick(textFieldWidgetRename));
                    textWidget.setMaxWidthLimit(1000);
                    ClickPopUpWidget clickPopUpWidget = new ClickPopUpWidget(0, 0, 0, 0)
                            .addPopup(widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(3, 38, 170, 103, DISPLAY));
                                widgetGroup.addWidget(new ScrollableTextWidget(3, 38, 182, 103)
                                        .addTextWidget(textWidget));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 20, 112, 18)
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                        .setBackgroundText("machine.universal.toggle.current.channel")
                                        .setTooltipText("machine.universal.toggle.current.channel")
                                        .setTextSupplier(() -> this.channel)
                                        .setTextResponder(this::setChannel)
                                        .setMaxStringLength(256)
                                        .setUpdateOnTyping(true));
                                widgetGroup.addWidget(new ToggleButtonWidget(7, 15, 18, 18, UNLOCK_LOCK, () -> this.getEnderProfile().isPublic(), this::setPublic)
                                        .setTooltipText("metaitem.ender_cover.private"));
                                widgetGroup.addWidget(new LabelWidget(3, 170, "machine.universal.owner", this.ownerId));
                                return true;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetRename::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addPopup(0, 61, 182, 60, textWidget, false, widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 60, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(10, 15, 162, 18, DISPLAY));
                                widgetGroup.addWidget(new AdvancedTextWidget(45, 4, textList -> textList.add(new TextComponentTranslation("machine.universal.renaming", textFieldWidgetRename.getTextId())), 0x404040));
                                widgetGroup.addWidget(textFieldWidgetRename);
                                return false;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetChannel::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setPressedCondition(() -> false)
                                    .useToggleTexture(true))
                            .addPopup(0, 61, 182, 60, new TJToggleButtonWidget(151, 15, 18, 18)
                                    .setTooltipText("machine.universal.toggle.add.channel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .useToggleTexture(true)
                                    .setDisplayText("O"), widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 60, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(10, 15, 162, 18, DISPLAY));
                                widgetGroup.addWidget(new AdvancedTextWidget(55, 4, textList -> textList.add(new TextComponentTranslation("machine.universal.toggle.add.channel")), 0x404040));
                                widgetGroup.addWidget(textFieldWidgetChannel);
                                return false;
                            }).addPopup(0, 38, 182, 103, new TJToggleButtonWidget(151, 145, 18, 18)
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setBackgroundTextures(LIST_OVERLAY)
                                    .useToggleTexture(true), widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 103, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(3, 15, 176, 80, DISPLAY));
                                widgetGroup.addWidget(new ScrollableTextWidget(3, 15, 185, 80)
                                        .addTextWidget(new TJAdvancedTextWidget(2, 3, this::addPlayerDisplayText, 0xFFFFFF)
                                                .addClickHandler(this::handlePlayerDisplayClick)));
                                widgetGroup.addWidget(new AdvancedTextWidget(55, 4, textList -> textList.add(new TextComponentTranslation("machine.universal.list.players")), 0x404040));
                                return false;
                            });
                    tab.addWidget(clickPopUpWidget);
                });
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 262)
                .bindPlayerInventory(player.inventory, 181)
                .widget(tabBuilder.build())
                .widget(tabBuilder.buildWidgetGroup())
                .build(this, player);
    }

    private QuadConsumer<String, String, Widget.ClickData, EntityPlayer> handleDisplayClick(NewTextFieldWidget<?> textFieldWidget) {
        return (componentData, textId, clickData, player) -> {
            String[] components = componentData.split(":");
            if (components[0].equals("@Popup"))
                textFieldWidget.setTextId(components[1]);
            if (components.length < 3 || this.ownerId != null && !this.ownerId.equals(player.getUniqueID()))
                return;
            switch (components[0]) {
                case "select":
                    if (components[1].equals("entry"))
                        this.setTextID(components[2], textId);
                    else this.setChannel(components[2], textId);
                    break;
                case "remove":
                    if (components[1].equals("entry"))
                        this.getMap().remove(components[2]);
                    else this.getPlayerMap().remove(components[2]);
                    break;
            }
        };
    }

    private String[] getTooltipFormat() {
        return ArrayUtils.toArray(getTransferRate());
    }

    private void setTransferRate(String amount, String id) {
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

    private void setChannel(String channel, String id) {
        this.channel = channel;
        this.handler = this.getMap().get((K) this.text);
        this.markAsDirty();
    }

    private void setTextID(String text, String id) {
        this.text = text;
        this.handler = this.getMap().get((K) this.text);
        this.markAsDirty();
    }

    private void handlePlayerDisplayClick(String componentData, String textId, Widget.ClickData clickData, EntityPlayer player) {
        String[] component = componentData.split(":");
        UUID uuid = UUID.fromString(component[1]);
        if (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getOwner().equals(uuid))
            return;
        if (component[0].equals("Add"))
            this.getEnderProfile().getAllowedUsers().add(uuid);
        else if (component[0].equals("Remove"))
            this.getEnderProfile().getAllowedUsers().remove(uuid);
    }

    private void addPlayerDisplayText(List<ITextComponent> textList) {
        int count = 0;
        List<EntityPlayerMP> playerList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        for (EntityPlayer player : playerList) {
            boolean contains = this.getEnderProfile().getAllowedUsers().contains(player.getUniqueID());
            textList.add(new TextComponentString("[§e" + (++count) + "§r] " + player.getDisplayNameString()).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(player.getDisplayNameString())))).appendText("\n")
                    .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.add").setStyle(new Style().setColor(contains ? GRAY : YELLOW)), "Add:" + player.getUniqueID()))
                    .appendText(" ")
                    .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.remove").setStyle(new Style().setColor(contains ? YELLOW : GRAY)), "Remove:" + player.getUniqueID())));
        }
    }

    private Consumer<List<ITextComponent>> addChannelDisplayText(List<Integer> searchResults, List<Integer> patternFlags, List<String> search) {
        return (textList) -> {
            int count = 0, results = 0;
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("machine.universal.channels") + "§r(§e" + searchResults.get(1) + "§r/§e" + this.getPlayerMap().size() + "§r)"));
            for (Map.Entry<String, Pair<CoverEnderProfile, Map<K, V>>> entry : this.getPlayerMap().entrySet()) {
                String text =  entry.getKey() != null ? entry.getKey() : "PUBLIC";
                if (!text.isEmpty() && !Pattern.compile(text, patternFlags.get(1)).matcher(search.get(1)).find())
                    continue;

                textList.add(new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                        .appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.select").setStyle(new Style().setColor(text.equals(this.channel) ? GRAY : YELLOW)), "select:channel:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:channel:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "@Popup:" + text))
                        .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.owner", entry.getValue().getLeft().getOwner())))));
            }
            searchResults.set(1, results);
        };
    }

    private Consumer<List<ITextComponent>> addEntryDisplayText(List<Integer> searchResults, List<Integer> patternFlags, List<String> search) {
        return (textList) -> {
            int count = 0, results = 0;
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("machine.universal.entries") + "§r(§e" + searchResults.get(0) + "§r/§e" + this.getMap().size() + "§r)"));
            for (Map.Entry<K, V> entry : this.getMap().entrySet()) {
                String text = (String) entry.getKey();
                if (!text.isEmpty() && !Pattern.compile(text, patternFlags.get(0)).matcher(search.get(0)).find())
                    continue;

                ITextComponent keyEntry = new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                        .appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.select").setStyle(new Style().setColor(text.equals(this.text) ? GRAY : YELLOW)), "select:entry:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:entry:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "@Popup:" + text));
                textList.add(keyEntry);
                this.addEntryText(keyEntry, entry.getKey(), entry.getValue());
                results++;
            }
            searchResults.set(0, results);
        };
    }

    protected abstract void addEntryText(ITextComponent keyEntry, K key, V value);

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
}
