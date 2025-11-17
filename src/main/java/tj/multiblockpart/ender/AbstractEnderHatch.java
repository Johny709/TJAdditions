package tj.multiblockpart.ender;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.machines.multi.multiblockpart.GAMetaTileEntityMultiblockPart;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.gui.widgets.tab.HorizontalTabListRenderer;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.common.covers.CoverPump;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import tj.TJValues;
import tj.builder.WidgetTabBuilder;
import tj.capability.IEnderNotifiable;
import tj.gui.widgets.NewTextFieldWidget;
import tj.gui.widgets.PopUpWidget;
import tj.gui.widgets.TJAdvancedTextWidget;
import tj.gui.widgets.TJClickButtonWidget;
import tj.gui.widgets.impl.ClickPopUpWidget;
import tj.gui.widgets.impl.ScrollableTextWidget;
import tj.gui.widgets.impl.TJToggleButtonWidget;
import tj.items.covers.EnderCoverProfile;
import tj.textures.TJSimpleOverlayRenderer;
import tj.textures.TJTextures;
import tj.util.consumers.QuadConsumer;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.GuiTextures.TOGGLE_BUTTON_BACK;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.HorizontalStartCorner.LEFT;
import static gregtech.api.gui.widgets.tab.HorizontalTabListRenderer.VerticalLocation.TOP;
import static net.minecraft.util.text.TextFormatting.GRAY;
import static net.minecraft.util.text.TextFormatting.YELLOW;
import static tj.gui.TJGuiTextures.*;
import static tj.gui.TJGuiTextures.LIST_OVERLAY;

public abstract class AbstractEnderHatch<T, V> extends GAMetaTileEntityMultiblockPart implements IControllable, IMultiblockAbilityPart<T>, IEnderNotifiable<V> {

    protected String channel;
    protected String lastEntry;
    protected String displayName;
    protected UUID ownerId;
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    private MultiblockControllerBase controller;
    protected int maxTransferRate;
    protected int transferRate;
    protected V handler;

    public AbstractEnderHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
    }

    protected int getPortalColor() {
        return 0xffffff;
    }

    @Override
    public boolean onRightClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        if (this.ownerId == null) {
            this.ownerId = playerIn.getUniqueID();
            this.displayName = playerIn.getDisplayNameString();
        }
        return super.onRightClick(playerIn, hand, facing, hitResult);
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
        String[] playerName = {""};
        WidgetTabBuilder tabBuilder = new WidgetTabBuilder()
                .setTabListRenderer(() -> new HorizontalTabListRenderer(LEFT, TOP))
                .addWidget(new LabelWidget(30, 4, this.getMetaFullName()))
                .addTab(this.getMetaFullName(), this.getStackForm(), tab -> {
                    NewTextFieldWidget<?> textFieldWidgetRename = new NewTextFieldWidget<>(12, 20, 159, 13)
                            .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                            .setBackgroundText("machine.universal.toggle.rename.entry")
                            .setTooltipText("machine.universal.toggle.rename.entry")
                            .setTextResponder(this.getEnderProfile()::editEntry)
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
                                widgetGroup.addWidget(new TJToggleButtonWidget(-20, 38, 18, 18)
                                        .setTooltipText("machine.universal.toggle.clear")
                                        .setButtonId(player.getUniqueID().toString())
                                        .setBackgroundTextures(BUTTON_CLEAR_GRID)
                                        .setToggleButtonResponder(this::onClear)
                                        .setToggleTexture(TOGGLE_BUTTON_BACK)
                                        .useToggleTexture(true));
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
                                widgetGroup.addWidget(new LabelWidget(3, 170, "machine.universal.owner", this.displayName));
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
                                        .addPopup(widgetGroup1 -> {
                                            TJAdvancedTextWidget playerTextWidget = new TJAdvancedTextWidget(2, 3, this.addPlayerDisplayText(searchResults, patternFlags, search), 0xFFFFFF)
                                                    .addClickHandler(this.handlePlayerDisplayClick(playerName));
                                            widgetGroup1.addWidget(new ClickPopUpWidget(0, 0, 0, 0)
                                                    .addPopup(widgetGroup2 -> {
                                                        widgetGroup2.addWidget(new ImageWidget(0, 0, 182, 130, BORDERED_BACKGROUND));
                                                        widgetGroup2.addWidget(new ImageWidget(3, 25, 176, 80, DISPLAY));
                                                        widgetGroup2.addWidget(new ImageWidget(30, 106, 115, 18, DISPLAY));
                                                        widgetGroup2.addWidget(new ScrollableTextWidget(3, 25, 185, 80)
                                                                .addTextWidget(playerTextWidget));
                                                        widgetGroup2.addWidget(new AdvancedTextWidget(10, 4, textList -> textList.add(new TextComponentString(I18n.translateToLocalFormatted("metaitem.ender_cover.allowed_players", this.channel))), 0x404040));
                                                        widgetGroup2.addWidget(new NewTextFieldWidget<>(32, 110, 112, 13, false)
                                                                .setValidator(str -> Pattern.compile(".*").matcher(str).matches())
                                                                .setTextResponder((result, id) -> search[2] = result)
                                                                .setBackgroundText("machine.universal.search")
                                                                .setTextSupplier(() -> search[2])
                                                                .setMaxStringLength(256)
                                                                .setUpdateOnTyping(true));
                                                      return true;
                                                    }).addPopup(0, 25, 182, 80, playerTextWidget, false, widgetGroup2 -> {
                                                        widgetGroup2.addWidget(new ImageWidget(0, 0, 182, 80, BORDERED_BACKGROUND));
                                                        widgetGroup2.addWidget(new AdvancedTextWidget(10, 4, textList -> textList.add(new TextComponentString(I18n.translateToLocalFormatted("metaitem.ender_cover.edit_permission", playerName[0]))), 0x404040));
                                                        return false;
                                                    }));
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
                .build(this.getHolder(), player);
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
                        this.setEntry(components[2], player.getUniqueID().toString());
                    else this.setChannel(components[2], player.getUniqueID().toString());
                    break;
                case "remove":
                    if (components[1].equals("entry"))
                        this.getEnderProfile().removeEntry(components[2], player.getUniqueID().toString());
                    else this.removeChannel(components[2], player.getUniqueID().toString());
                    break;
                case "@Popup": textFieldWidget.setTextId(components[1] + ":" + player.getUniqueID());
                    break;
            }
        };
    }

    private QuadConsumer<String, String, Widget.ClickData, EntityPlayer> handlePlayerDisplayClick(String[] playerName) {
        return (componentData, textId, clickData, player) -> {
            String[] component = componentData.split(":");
            UUID uuid = UUID.fromString(component[1]);
            if (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getOwner().equals(uuid))
                return;
            switch (component[0]) {
                case "Add": this.getEnderProfile().getAllowedUsers().put(uuid, new long[]{0, 0, 0, 0, 0, 0});
                    break;
                case "Remove": this.getEnderProfile().getAllowedUsers().remove(uuid);
                    break;
                case "@Popup": playerName[0] = component[2];
                    break;
            }
        };
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
            int results = 0;
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.entries") + "§r(§e" + searchResults[0] + "§r/§e" + this.getEnderProfile().getEntries().size() + "§r)"));
            for (Map.Entry<String, V> entry : this.getEnderProfile().getEntries().entrySet()) {
                String text = entry.getKey();
                if (!search[0].isEmpty() && !Pattern.compile(search[0], this.getFlags(patternFlags[0])).matcher(text).find())
                    continue;

                ITextComponent keyEntry = new TextComponentString(": [§a" + (++results) + "§r] " + text + (text.equals(this.lastEntry) ? " §a<<<" : ""))
                        .appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.select").setStyle(new Style().setColor(text.equals(this.lastEntry) ? GRAY : YELLOW)), "select:entry:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:entry:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "@Popup:" + text));
                textList.add(keyEntry);
                this.addEntryText(keyEntry, entry.getKey(), entry.getValue());
            }
            searchResults[0] = results;
        };
    }

    private Consumer<List<ITextComponent>> addChannelDisplayText(int[] searchResults, int[][] patternFlags, String[] search) {
        return (textList) -> {
            int results = 0;
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.channels") + "§r(§e" + searchResults[1] + "§r/§e" + this.getPlayerMap().size() + "§r)"));
            for (Map.Entry<String, EnderCoverProfile<V>> entry : this.getPlayerMap().entrySet()) {
                String text =  entry.getKey() != null ? entry.getKey() : "PUBLIC";
                if (!search[1].isEmpty() && !Pattern.compile(search[1], this.getFlags(patternFlags[1])).matcher(text).find())
                    continue;

                textList.add(new TextComponentString(": [§a" + (++results) + "§r] " + text + (text.equals(this.channel) ? " §a<<<" : ""))
                        .appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.select").setStyle(new Style().setColor(text.equals(this.channel) ? GRAY : YELLOW)), "select:channel:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove:channel:" + text))
                        .appendText(" ")
                        .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.rename"), "@Popup:" + text))
                        .setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.owner", entry.getValue().getOwner())))));
            }
            searchResults[1] = results;
        };
    }

    private Consumer<List<ITextComponent>> addPlayerDisplayText(int[] searchResults, int[][] patternFlags, String[] search) {
        return (textList) -> {
            int results = 0;
            List<EntityPlayerMP> playerList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
            textList.add(new TextComponentString("§l" + I18n.translateToLocal("tj.multiblock.tab.players") + "§r(§e" + searchResults[2] + "§r/§e" + playerList.size() + "§r)"));
            for (EntityPlayer player : playerList) {
                String text = player.getDisplayNameString();
                if (!search[2].isEmpty() && !Pattern.compile(search[2], this.getFlags(patternFlags[2])).matcher(text).find())
                    continue;
                boolean contains = this.getEnderProfile().getAllowedUsers().containsKey(player.getUniqueID());
                textList.add(new TextComponentString(": [§a" + (++results) + "§r] " + text).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(text)))).appendText("\n")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.add").setStyle(new Style().setColor(contains ? GRAY : YELLOW)), "Add:" + player.getUniqueID()))
                        .appendText(" ")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.remove").setStyle(new Style().setColor(contains ? YELLOW : GRAY)), "Remove:" + player.getUniqueID()))
                        .appendText(" ")
                        .appendSibling(TJAdvancedTextWidget.withButton(new TextComponentTranslation("machine.universal.linked.edit"), "@Popup:" + player.getUniqueID() + ":" + text)));
            }
            searchResults[2] = results;
        };
    }

    protected abstract void addEntryText(ITextComponent keyEntry, String key, V value);

    private String[] getTooltipFormat() {
        return ArrayUtils.toArray(getTransferRate());
    }

    private void setTransferRate(String amount, String id) {
        this.transferRate = Math.min(Integer.parseInt(amount), this.maxTransferRate);
        this.markDirty();
    }

    public String getTransferRate() {
        return String.valueOf(this.transferRate);
    }

    private void onIncrement(Widget.ClickData clickData) {
        this.transferRate = MathHelper.clamp(this.transferRate * 2, 1, this.maxTransferRate);
        this.markDirty();
    }

    private void onDecrement(Widget.ClickData clickData) {
        this.transferRate = MathHelper.clamp(this.transferRate / 2, 1, this.maxTransferRate);
        this.markDirty();
    }

    private void setPumpMode(CoverPump.PumpMode pumpMode) {
        this.pumpMode = pumpMode;
        this.markDirty();
    }

    private void setPublic(boolean isPublic, String uuid) {
        if (this.getEnderProfile().getOwner() != null && this.getEnderProfile().getOwner().equals(UUID.fromString(uuid))) {
            this.getEnderProfile().setPublic(isPublic);
            this.markDirty();
        }
    }

    private void addChannel(String key, String uuid) {
        if (this.getEnderProfile().getOwner() == null || this.getEnderProfile().getAllowedUsers().containsKey(UUID.fromString(uuid))) {
            this.getPlayerMap().putIfAbsent(key, new EnderCoverProfile<>(this.ownerId, new Object2ObjectOpenHashMap<>()));
            this.markDirty();
        }
    }

    private void renameChannel(String key, String id) {
        int index = id.lastIndexOf(":");
        String uuid = id.substring(index + 1);
        String oldKey = id.substring(0, index);
        EnderCoverProfile<V> profile = this.getPlayerMap().get(oldKey);
        if (profile != null && profile.editChannel(key, UUID.fromString(uuid))) {
            this.getPlayerMap().put(key, this.getPlayerMap().remove(oldKey));
            this.markDirty();
        }
    }

    private void removeChannel(String key, String uuid) {
        if (this.getPlayerMap().get(key).removeChannel(uuid)) {
            this.getPlayerMap().remove(key);
            this.markDirty();
        }
    }

    private void setChannel(String key, String id) {
        EnderCoverProfile<?> profile = this.getPlayerMap().getOrDefault(key, this.getPlayerMap().get(null));
        UUID uuid = UUID.fromString(id);
        if (!key.equals(this.channel) && (profile.isPublic() || profile.getAllowedUsers().get(uuid) != null && profile.getAllowedUsers().get(uuid)[3] == 1)) {
            this.getEnderProfile().removeFromNotifiable(this.lastEntry, this);
            this.setChannel(key);
            this.getEnderProfile().addToNotifiable(this.lastEntry, this);
        }
    }

    @Override
    public void setChannel(String channel) {
        this.channel = channel;
        this.markDirty();
    }

    private void setEntry(String key, String uuid) {
        if (this.getEnderProfile().setEntry(key, this.lastEntry, uuid, this)) {
            this.handler = this.getEnderProfile().getEntries().get(key);
            this.setEntry(key);
            if (this.controller != null)
                this.controller.invalidateStructure();
        }
    }

    private void addEntry(String key, String uuid) {
        this.getEnderProfile().addEntry(key, uuid, this.createHandler());
        this.markDirty();
    }

    private void onClear(boolean toggle, String uuid) {
        this.getEnderProfile().editEntry(this.lastEntry, uuid, this.createHandler());
        this.markDirty();
    }

    @Override
    public void setHandler(V handler) {
        this.handler = handler;
    }

    @Override
    public void setEntry(String lastEntry) {
        this.lastEntry = lastEntry;
        this.markDirty();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        int oldBaseColor = renderState.baseColour;
        int oldAlphaOverride = renderState.alphaOverride;

        renderState.baseColour = getPortalColor() << 8;
        renderState.alphaOverride = 0xFF;
        this.getOverlay().renderSided(this.frontFacing, renderState, translation, pipeline);

        renderState.baseColour = TJValues.VC[this.getTier()] << 8;
        TJTextures.INSIDE_OVERLAY_BASE.renderSided(this.frontFacing, renderState, translation, pipeline);

        renderState.baseColour = oldBaseColor;
        renderState.alphaOverride = oldAlphaOverride;
        TJTextures.OUTSIDE_OVERLAY_BASE.renderSided(this.frontFacing, renderState, translation, pipeline);
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 2) {
            this.ownerId = buf.readUniqueId();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer packetBuffer) {
        super.writeInitialSyncData(packetBuffer);
        packetBuffer.writeBoolean(this.ownerId != null);
        if (this.ownerId != null)
            packetBuffer.writeUniqueId(this.ownerId);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        if (buf.readBoolean())
            this.ownerId = buf.readUniqueId();
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
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
        return data;
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
            this.getEnderProfile().addToNotifiable(this.lastEntry, this);
        }
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing side) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE)
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingEnabled) {
        this.isWorkingEnabled = isWorkingEnabled;
        this.markDirty();
    }

    @Override
    public void markToDirty() {
        this.markDirty();
    }

    @Override
    public boolean isAttachedToMultiBlock() {
        return false;
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase controller) {
        this.controller = controller;
    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase controller) {
        this.controller = null;
    }
}
