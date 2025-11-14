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
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.ArrayUtils;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.gui.widgets.*;
import tj.gui.widgets.impl.ClickPopUpWidget;
import tj.gui.widgets.impl.ScrollableTextWidget;
import tj.gui.widgets.impl.TJToggleButtonWidget;
import tj.textures.TJSimpleOverlayRenderer;
import tj.textures.TJTextures;
import tj.util.consumers.QuadConsumer;

import javax.annotation.Nonnull;
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

public abstract class AbstractEnderCover<V> extends CoverBehavior implements CoverWithUI, ITickable, IControllable {

    protected String channel;
    protected String lastEntry;
    protected UUID ownerId;
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    protected int maxTransferRate;
    protected int transferRate;
    protected boolean isFilterPopUp;
    protected V handler;

    public AbstractEnderCover(ICoverable coverHolder, EnumFacing attachedSide) {
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
        if (this.ownerId == null) {
            this.ownerId = playerIn.getUniqueID();
        }
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

    @Nonnull
    protected EnderCoverProfile<V> getEnderProfile()  {
        return this.getPlayerMap().getOrDefault(this.channel, this.getPlayerMap().get(null));
    }

    protected abstract TJSimpleOverlayRenderer getOverlay();

    protected abstract Map<String, EnderCoverProfile<V>> getPlayerMap();

    protected abstract void addWidgets(Consumer<Widget> widget);

    protected void addToPopUpWidget(PopUpWidget<?> buttonPopUpWidget) {}

    protected abstract V createHandler();

    @Override
    public ModularUI createUI(EntityPlayer player) {
        int[] searchResults = new int[3];
        int[][] patternFlags = new int[3][9];
        String[] search = new String[]{"", "", ""};
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
                            .setTextId(player.getUniqueID().toString())
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
                                        .setTextId(player.getUniqueID().toString())
                                        .setTextSupplier(() -> this.lastEntry)
                                        .setTextResponder(this::setEntry)
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
                                        .setTextResponder((result, id) -> search[0] = result)
                                        .setBackgroundText("machine.universal.search")
                                        .setTextSupplier(() -> search[0])
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
                                    .setItemDisplay(new ItemStack(Item.getByNameOrId("enderio:item_material"), 1, 11))
                                    .setTooltipText("machine.universal.search.settings")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .useToggleTexture(true), widgetGroup -> this.addSearchTextWidgets(widgetGroup, patternFlags, 0)).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetRename::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
                                    .useToggleTexture(true))
                            .addPopup(0, 61, 182, 60, textWidget, false, widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 60, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(10, 15, 162, 18, DISPLAY));
                                widgetGroup.addWidget(new AdvancedTextWidget(45, 4, (textList) -> {
                                    int index = textFieldWidgetRename.getTextId().lastIndexOf(":");
                                    String entry = textFieldWidgetRename.getTextId().substring(0, index);
                                    textList.add(new TextComponentTranslation("machine.universal.renaming", entry));
                                }, 0x404040));
                                widgetGroup.addWidget(textFieldWidgetRename);
                                return false;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetEntry::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
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
                            }).passPopup(this::addToPopUpWidget);
                    tab.addWidget(clickPopUpWidget);
                }).addTab("tj.multiblock.tab.channels", new ItemStack(Item.getByNameOrId("appliedenergistics2:part"), 1, 76), tab -> {
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
                            .setTextId(player.getUniqueID().toString())
                            .setTextResponder(this::addChannel)
                            .setMaxStringLength(256);
                    TJAdvancedTextWidget textWidget = new TJAdvancedTextWidget(2, 3, this.addChannelDisplayText(searchResults, patternFlags, search), 0xFFFFFF)
                            .addClickHandler(this.handleDisplayClick(textFieldWidgetRename));
                    textWidget.setMaxWidthLimit(1000);
                    ClickPopUpWidget clickPopUpWidget = new ClickPopUpWidget(0, 0, 0, 0)
                            .addPopup(widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(3, 38, 170, 103, DISPLAY));
                                widgetGroup.addWidget(new ImageWidget(30, 142, 115, 18, DISPLAY));
                                widgetGroup.addWidget(new ScrollableTextWidget(3, 38, 182, 103)
                                        .addTextWidget(textWidget));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 20, 112, 18)
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                        .setBackgroundText("machine.universal.toggle.current.channel")
                                        .setTooltipText("machine.universal.toggle.current.channel")
                                        .setTextId(player.getUniqueID().toString())
                                        .setTextSupplier(() -> this.channel)
                                        .setTextResponder(this::setChannel)
                                        .setMaxStringLength(256)
                                        .setUpdateOnTyping(true));
                                widgetGroup.addWidget(new TJToggleButtonWidget(7, 15, 18, 18)
                                        .setButtonSupplier(() -> this.getEnderProfile().isPublic())
                                        .setTooltipText("metaitem.ender_cover.private")
                                        .setButtonId(player.getUniqueID().toString())
                                        .setToggleButtonResponder(this::setPublic)
                                        .setToggleTexture(UNLOCK_LOCK)
                                        .useToggleTexture(true));
                                widgetGroup.addWidget(new NewTextFieldWidget<>(32, 147, 112, 13, false)
                                        .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                        .setTextResponder((result, id) -> search[1] = result)
                                        .setBackgroundText("machine.universal.search")
                                        .setTextSupplier(() -> search[1])
                                        .setMaxStringLength(256)
                                        .setUpdateOnTyping(true));
                                widgetGroup.addWidget(new LabelWidget(3, 170, "machine.universal.owner", this.ownerId));
                                return true;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetRename::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
                                    .useToggleTexture(true))
                            .addPopup(0, 61, 182, 60, textWidget, false, widgetGroup -> {
                                widgetGroup.addWidget(new ImageWidget(0, 0, 182, 60, BORDERED_BACKGROUND));
                                widgetGroup.addWidget(new ImageWidget(10, 15, 162, 18, DISPLAY));
                                widgetGroup.addWidget(new AdvancedTextWidget(45, 4, (textList) -> {
                                    int index = textFieldWidgetRename.getTextId().lastIndexOf(":");
                                    String entry = textFieldWidgetRename.getTextId().substring(0, index);
                                    textList.add(new TextComponentTranslation("machine.universal.renaming", entry));
                                }, 0x404040));
                                widgetGroup.addWidget(textFieldWidgetRename);
                                return false;
                            }).addClosingButton(new TJToggleButtonWidget(10, 35, 81, 18)
                                    .setDisplayText("machine.universal.cancel")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
                                    .useToggleTexture(true))
                            .addClosingButton(new TJToggleButtonWidget(91, 35, 81, 18)
                                    .setButtonResponderWithMouse(textFieldWidgetChannel::triggerResponse)
                                    .setDisplayText("machine.universal.ok")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setButtonSupplier(() -> false)
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
                            }).addPopup(0, 38, 182, 130, new TJToggleButtonWidget(7, 142, 18, 18)
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .setBackgroundTextures(LIST_OVERLAY)
                                    .useToggleTexture(true), widgetGroup -> {
                                widgetGroup.addWidget(new ClickPopUpWidget(0, 0, 0, 0)
                                        .addPopup(innerWidgetGroup -> {
                                            innerWidgetGroup.addWidget(new ImageWidget(0, 0, 182, 130, BORDERED_BACKGROUND));
                                            innerWidgetGroup.addWidget(new ImageWidget(3, 25, 176, 80, DISPLAY));
                                            innerWidgetGroup.addWidget(new ImageWidget(30, 106, 115, 18, DISPLAY));
                                            innerWidgetGroup.addWidget(new ScrollableTextWidget(3, 25, 185, 80)
                                                    .addTextWidget(new TJAdvancedTextWidget(2, 3, this.addPlayerDisplayText(searchResults, patternFlags, search), 0xFFFFFF)
                                                            .addClickHandler(this::handlePlayerDisplayClick)));
                                            innerWidgetGroup.addWidget(new AdvancedTextWidget(10, 4, textList -> textList.add(new TextComponentString(I18n.translateToLocalFormatted("metaitem.ender_cover.allowed_players", this.channel))), 0x404040));
                                            innerWidgetGroup.addWidget(new NewTextFieldWidget<>(32, 110, 112, 13, false)
                                                    .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                                    .setTextResponder((result, id) -> search[2] = result)
                                                    .setBackgroundText("machine.universal.search")
                                                    .setTextSupplier(() -> search[2])
                                                    .setMaxStringLength(256)
                                                    .setUpdateOnTyping(true));
                                            return true;
                                        }).addPopup(117, 25, 60, 78, new TJToggleButtonWidget(151, 106, 18, 18)
                                                .setItemDisplay(new ItemStack(Item.getByNameOrId("enderio:item_material"), 1, 11))
                                                .setTooltipText("machine.universal.search.settings")
                                                .setToggleTexture(TOGGLE_BUTTON_BACK)
                                                .useToggleTexture(true), innerWidgetGroup -> this.addSearchTextWidgets(innerWidgetGroup, patternFlags, 2)));
                                return false;
                            }).addPopup(112, 61, 60, 78, new TJToggleButtonWidget(151, 142, 18, 18)
                                    .setItemDisplay(new ItemStack(Item.getByNameOrId("enderio:item_material"), 1, 11))
                                    .setTooltipText("machine.universal.search.settings")
                                    .setToggleTexture(TOGGLE_BUTTON_BACK)
                                    .useToggleTexture(true), widgetGroup -> this.addSearchTextWidgets(widgetGroup, patternFlags, 1));
                    tab.addWidget(clickPopUpWidget);
                });
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 262)
                .bindPlayerInventory(player.inventory, 181)
                .widget(tabBuilder.build())
                .widget(tabBuilder.buildWidgetGroup())
                .build(this, player);
    }

    private boolean addSearchTextWidgets(WidgetGroup widgetGroup, int[][] patternFlags, int i) {
        widgetGroup.addWidget(new ImageWidget(0, 0, 60, 78, BORDERED_BACKGROUND));
        widgetGroup.addWidget(new ImageWidget(3, 57, 54, 18, DISPLAY));
        widgetGroup.addWidget(new AdvancedTextWidget(5, 62, textList -> textList.add(new TextComponentTranslation("string.regex.flag", this.getFlags(patternFlags[i]))), 0x404040));
        widgetGroup.addWidget(new TJToggleButtonWidget(3, 3, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][0] = pressed ? Pattern.UNIX_LINES : 0)
                .setDisplayText("string.regex.pattern.unix_lines.flag")
                .setTooltipText("string.regex.pattern.unix_lines")
                .setButtonSupplier(() -> patternFlags[i][0] != 0)
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(21, 3, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][1] = pressed ? Pattern.CASE_INSENSITIVE : 0)
                .setDisplayText("string.regex.pattern.case_insensitive.flag")
                .setTooltipText("string.regex.pattern.case_insensitive")
                .setButtonSupplier(() -> patternFlags[i][1] != 0)
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(39, 3, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][2] = pressed ? Pattern.COMMENTS : 0)
                .setDisplayText("string.regex.pattern.comments.flag")
                .setButtonSupplier(() -> patternFlags[i][2] != 0)
                .setTooltipText("string.regex.pattern.comments")
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(3, 21, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][3] = pressed ? Pattern.MULTILINE : 0)
                .setDisplayText("string.regex.pattern.multiline.flag")
                .setTooltipText("string.regex.pattern.multiline")
                .setButtonSupplier(() -> patternFlags[i][3] != 0)
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(21, 21, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][4] = pressed ? Pattern.LITERAL : 0)
                .setDisplayText("string.regex.pattern.literal.flag")
                .setButtonSupplier(() -> patternFlags[i][4] != 0)
                .setTooltipText("string.regex.pattern.literal")
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(39, 21, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][5] = pressed ? Pattern.DOTALL : 0)
                .setDisplayText("string.regex.pattern.dotall.flag")
                .setButtonSupplier(() -> patternFlags[i][5] != 0)
                .setTooltipText("string.regex.pattern.dotall")
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(3, 39, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][6] = pressed ? Pattern.UNICODE_CASE : 0)
                .setDisplayText("string.regex.pattern.unicode_case.flag")
                .setTooltipText("string.regex.pattern.unicode_case")
                .setButtonSupplier(() -> patternFlags[i][6] != 0)
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(21, 39, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][7] = pressed ? Pattern.CANON_EQ : 0)
                .setDisplayText("string.regex.pattern.canon_eq.flag")
                .setButtonSupplier(() -> patternFlags[i][7] != 0)
                .setTooltipText("string.regex.pattern.canon_eq")
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        widgetGroup.addWidget(new TJToggleButtonWidget(39, 39, 18, 18)
                .setToggleButtonResponder((pressed, s) -> patternFlags[i][8] = pressed ? Pattern.UNICODE_CHARACTER_CLASS : 0)
                .setDisplayText("string.regex.pattern.unicode_character_class.flag")
                .setTooltipText("string.regex.pattern.unicode_character_class")
                .setButtonSupplier(() -> patternFlags[i][8] != 0)
                .setToggleTexture(TOGGLE_BUTTON_BACK)
                .useToggleTexture(true));
        return false;
    }

    private QuadConsumer<String, String, Widget.ClickData, EntityPlayer> handleDisplayClick(NewTextFieldWidget<?> textFieldWidget) {
        return (componentData, textId, clickData, player) -> {
            String[] components = componentData.split(":");
            switch (components[0]) {
                case "select":
                    if (components[1].equals("entry"))
                        this.setEntry(components[2], textId);
                    else this.setChannel(components[2], player.getUniqueID().toString());
                    break;
                case "remove":
                    if (components[1].equals("entry"))
                        this.removeEntry(components[2], player.getUniqueID().toString());
                    else this.removeChannel(components[2], player.getUniqueID().toString());
                    break;
                case "@Popup":
                    textFieldWidget.setTextId(components[1] + ":" + player.getUniqueID());
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

    private void setPublic(boolean isPublic, String uuid) {
        if (this.getEnderProfile().getOwner() != null && this.getEnderProfile().getOwner().equals(UUID.fromString(uuid))) {
            this.getEnderProfile().setPublic(isPublic);
            this.markAsDirty();
        }
    }

    private void addChannel(String key, String uuid) {
        if (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getAllowedUsers().contains(UUID.fromString(uuid))) {
            this.getPlayerMap().putIfAbsent(key, new EnderCoverProfile<>(this.ownerId, new Object2ObjectOpenHashMap<>()));
            this.markAsDirty();
        }
    }

    private void renameChannel(String key, String id) {
        int index = id.lastIndexOf(":");
        String entry = id.substring(0, index);
        String uuid = id.substring(index + 1);
        EnderCoverProfile<V> profile = this.getPlayerMap().get(entry);
        if (profile != null && profile.getOwner() != null && profile.getOwner().equals(UUID.fromString(uuid))) {
            this.getEnderProfile().editChannel(key);
            this.getPlayerMap().put(key, this.getPlayerMap().remove(entry));
            this.markAsDirty();
        }
    }

    private void removeChannel(String key, String uuid) {
        if (this.getEnderProfile().getOwner() != null && this.getEnderProfile().getOwner().equals(UUID.fromString(uuid))) {
            this.getPlayerMap().remove(key).removeChannel();
            this.markAsDirty();
        }
    }

    private void setChannel(String key, String uuid) {
        EnderCoverProfile<?> profile = this.getPlayerMap().getOrDefault(key, this.getPlayerMap().get(null));
        if (!key.equals(this.channel) && (profile.isPublic() || profile.getOwner().toString().equals(uuid))) {
            this.getEnderProfile().removeCoverFromEntry(this.lastEntry, this);
            this.setChannel(key);
            this.getEnderProfile().addCoverToEntry(this.lastEntry, this);
        }
    }

    public void setChannel(String channel) {
        this.channel = channel;
        this.markAsDirty();
    }

    private void setEntry(String key, String uuid) {
        if (this.getEnderProfile().containsEntry(key) && !key.equals(this.lastEntry) && (this.getEnderProfile().isPublic() || this.getEnderProfile().getAllowedUsers().contains(UUID.fromString(uuid)))) {
            this.getEnderProfile().removeCoverFromEntry(this.lastEntry, this);
            this.getEnderProfile().addCoverToEntry(key, this);
            this.handler = this.getEnderProfile().getEntries().get(key);
            this.setLastEntry(key);
        }
    }

    private void addEntry(String key, String uuid) {
        if (key != null && (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getAllowedUsers().contains(UUID.fromString(uuid)))) {
            this.getEnderProfile().addEntry(key, this.createHandler());
            this.markAsDirty();
        }
    }

    private void onClear(Widget.ClickData clickData) {
        if (this.getEnderProfile().containsEntry(this.lastEntry)) {
            this.getEnderProfile().editEntry(this.lastEntry, this.createHandler());
            this.markAsDirty();
        }
    }

    private void renameEntry(String key, String id) {
        int index = id.lastIndexOf(":");
        String entry = id.substring(0, index);
        String uuid = id.substring(index + 1);
        if (this.getEnderProfile().containsEntry(entry) && (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getAllowedUsers().contains(UUID.fromString(uuid)))) {
            this.getEnderProfile().editEntry(entry, key);
            this.setLastEntry(key);
        }
    }

    private void removeEntry(String key, String uuid) {
        if (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getAllowedUsers().contains(UUID.fromString(uuid))) {
            this.getEnderProfile().removeEntry(key);
            this.markAsDirty();
        }
    }

    public void setHandler(V handler) {
        this.handler = handler;
    }

    public void setLastEntry(String lastEntry) {
        this.lastEntry = lastEntry;
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

    private int getFlags(int[] flags) {
        int flag = 0;
        for (int i : flags) {
            flag |= i;
        }
        return flag;
    }

    private Consumer<List<ITextComponent>> addEntryDisplayText(int[] searchResults, int[][] patternFlags, String[] search) {
        return (textList) -> {
            int count = 0, results = 0;
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.entries") + "§r(§e" + searchResults[0] + "§r/§e" + this.getEnderProfile().getEntries().size() + "§r)"));
            for (Map.Entry<String, V> entry : this.getEnderProfile().getEntries().entrySet()) {
                String text = entry.getKey();
                if (!search[0].isEmpty() && !Pattern.compile(search[0], this.getFlags(patternFlags[0])).matcher(text).find())
                    continue;

                ITextComponent keyEntry = new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                        .appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.select").setStyle(new Style().setColor(text.equals(this.lastEntry) ? GRAY : YELLOW)), "select:entry:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:entry:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "@Popup:" + text));
                textList.add(keyEntry);
                this.addEntryText(keyEntry, entry.getKey(), entry.getValue());
                results++;
            }
            searchResults[0] = results;
        };
    }

    private Consumer<List<ITextComponent>> addChannelDisplayText(int[] searchResults, int[][] patternFlags, String[] search) {
        return (textList) -> {
            int count = 0, results = 0;
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.channels") + "§r(§e" + searchResults[1] + "§r/§e" + this.getPlayerMap().size() + "§r)"));
            for (Map.Entry<String, EnderCoverProfile<V>> entry : this.getPlayerMap().entrySet()) {
                String text =  entry.getKey() != null ? entry.getKey() : "PUBLIC";
                if (!search[1].isEmpty() && !Pattern.compile(search[1], this.getFlags(patternFlags[1])).matcher(text).find())
                    continue;

                textList.add(new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                        .appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.select").setStyle(new Style().setColor(text.equals(this.channel) ? GRAY : YELLOW)), "select:channel:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:channel:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "@Popup:" + text))
                        .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.owner", entry.getValue().getOwner())))));
                results++;
            }
            searchResults[1] = results;
        };
    }

    private Consumer<List<ITextComponent>> addPlayerDisplayText(int[] searchResults, int[][] patternFlags, String[] search) {
        return (textList) -> {
            int count = 0, results = 0;
            List<EntityPlayerMP> playerList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.players") + "§r(§e" + searchResults[2] + "§r/§e" + playerList.size() + "§r)"));
            for (EntityPlayer player : playerList) {
                String text = player.getDisplayNameString();
                if (!search[2].isEmpty() && !Pattern.compile(search[2], this.getFlags(patternFlags[2])).matcher(text).find())
                    continue;
                boolean contains = this.getEnderProfile().getAllowedUsers().contains(player.getUniqueID());
                textList.add(new TextComponentString("[§e" + (++count) + "§r] " + text).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(text)))).appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.add").setStyle(new Style().setColor(contains ? GRAY : YELLOW)), "Add:" + player.getUniqueID()))
                        .appendText(" ")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.remove").setStyle(new Style().setColor(contains ? YELLOW : GRAY)), "Remove:" + player.getUniqueID())));
                results++;
            }
            searchResults[2] = results;
        };
    }

    protected abstract void addEntryText(ITextComponent keyEntry, String key, V value);

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
        data.setInteger("pumpMode", this.pumpMode.ordinal());
        data.setBoolean("isWorking", this.isWorkingEnabled);
        data.setInteger("transferRate", this.transferRate);
        if (this.channel != null)
            data.setString("channel", this.channel);
        if (this.ownerId != null)
            data.setUniqueId("ownerId", this.ownerId);
        if (this.lastEntry != null)
            data.setString("lastEntry", this.lastEntry);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.pumpMode = CoverPump.PumpMode.values()[data.getInteger("pumpMode")];
        this.isWorkingEnabled = data.getBoolean("isWorking");
        this.transferRate = data.getInteger("transferRate");
        if (data.hasKey("channel"))
            this.channel = data.getString("channel");
        if (data.hasKey("ownerId"))
            this.ownerId = data.getUniqueId("ownerId");
        if (data.hasKey("lastEntry")) {
            this.lastEntry = data.getString("lastEntry");
            this.handler = this.getEnderProfile().getEntries().get(this.lastEntry);
            this.getEnderProfile().addCoverToEntry(this.lastEntry, this);
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
