package com.johny.tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.TJValues;
import com.johny.tj.builder.handlers.BasicEnergyHandler;
import com.johny.tj.gui.widgets.TJClickButtonWidget;
import com.johny.tj.gui.widgets.TJTextFieldWidget;
import com.johny.tj.items.handlers.LargeItemStackHandler;
import com.johny.tj.textures.TJSimpleOverlayRenderer;
import com.johny.tj.textures.TJTextures;
import gregtech.api.capability.IControllable;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.common.covers.CoverPump;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.johny.tj.gui.TJGuiTextures.*;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public abstract class AbstractCoverEnder<K, V> extends CoverBehavior implements CoverWithUI, ITickable, IControllable {

    protected String text = "default";
    protected String searchPrompt = "";
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    protected int maxTransferRate;
    protected int transferRate = maxTransferRate;
    protected boolean isFilterPopUp;
    protected boolean isCaseSensitive;
    protected boolean hasSpaces;
    private int searchResults;

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

    protected abstract TJSimpleOverlayRenderer getOverlay();

    protected abstract Map<K, V> getMap();

    protected abstract void addWidgets(Consumer<Widget> widget);

    protected abstract void onAddEntry(Widget.ClickData clickData);

    protected abstract void onClear(Widget.ClickData clickData);

    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup widgetGroup = new WidgetGroup(), addWidgetGroup = new WidgetGroup();
        ScrollableListWidget listWidget = new ScrollableListWidget(30, 61, 127, 80) {
            @Override
            public boolean isWidgetClickable(Widget widget) {
                return true; // this ScrollWidget will only add one widget so checks are unnecessary if position changes.
            }
        };
        listWidget.addWidget(new AdvancedTextWidget(2, 3, this::addDisplayText, 0xFFFFFF) {
            @Override
            public boolean mouseClicked(int mouseX, int mouseY, int button) {
                if (!isFilterPopUp())
                    return super.mouseClicked(mouseX, mouseY, button);
                return false;
            }
        }.setClickHandler(this::handleDisplayClick)
                .setMaxWidthLimit(124));
        widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
        widgetGroup.addWidget(new ImageWidget(30, 38, 115, 18, DISPLAY));
        widgetGroup.addWidget(new ImageWidget(30, 61, 115, 80, DISPLAY));
        widgetGroup.addWidget(new ImageWidget(30, 142, 115, 18, DISPLAY));
        widgetGroup.addWidget(new TJTextFieldWidget(32, 43, 112, 18, false, this::getTextID, this::setTextID)
                .setTooltipText("machine.universal.toggle.current.entry")
                .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
        widgetGroup.addWidget(new TJTextFieldWidget(32, 20, 112, 18, false, this::getTransferRate, this::setTransferRate)
                .setTooltipText("metaitem.ender_cover.transfer")
                .setTooltipFormat(this::getTooltipFormat)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new TJTextFieldWidget(32, 147, 112, 18, false, this::getSearchPrompt, this::setSearchPrompt)
                .setBackgroundText("machine.universal.search")
                .setValidator(str -> Pattern.compile(".*").matcher(str).matches()));
        widgetGroup.addWidget(new TJClickButtonWidget(151, 38, 18, 18, "O", this::onAddEntry)
                .setTooltipText("machine.universal.toggle.add.entry"));
        widgetGroup.addWidget(new TJClickButtonWidget(151, 15, 18, 18, "+", this::onIncrement)
                .setTooltipText("machine.universal.toggle.increment.disabled"));
        widgetGroup.addWidget(new TJClickButtonWidget(7, 15, 18, 18, "-", this::onDecrement)
                .setTooltipText("machine.universal.toggle.decrement.disabled"));
        widgetGroup.addWidget(new TJClickButtonWidget(7, 61, 18, 18, "", this::onClear)
                .setTooltipText("machine.universal.toggle.clear")
                .setButtonTexture(BUTTON_CLEAR_GRID));
        widgetGroup.addWidget(new ToggleButtonWidget(7, 142, 18, 18, CASE_SENSITIVE_BUTTON, this::isCaseSensitive, this::setCaseSensitive)
                .setTooltipText("machine.universal.case_sensitive"));
        widgetGroup.addWidget(new ToggleButtonWidget(151, 142, 18, 18, SPACES_BUTTON, this::hasSpaces, this::setSpaces)
                .setTooltipText("machine.universal.spaces"));
        widgetGroup.addWidget(new CycleButtonWidget(30, 161, 115, 18, CoverPump.PumpMode.class, this::getPumpMode, this::setPumpMode));
        widgetGroup.addWidget(new ToggleButtonWidget(7, 161, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled)
                .setTooltipText("machine.universal.toggle.run.mode"));
        addWidgets(addWidgetGroup::addWidget);
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 262)
                .bindPlayerInventory(player.inventory, 181)
                .widget(widgetGroup)
                .widget(listWidget)
                .widget(addWidgetGroup)
                .build(this, player);
    }

    private void setSearchPrompt(String searchPrompt) {
        this.searchPrompt = searchPrompt;
        markAsDirty();
    }

    private String getSearchPrompt() {
        return this.searchPrompt;
    }

    private String[] getTooltipFormat() {
        return ArrayUtils.toArray(getTransferRate());
    }

    private void setTransferRate(String amount) {
        this.transferRate = Math.min(Integer.parseInt(amount), this.maxTransferRate);
        markAsDirty();
    }

    public String getTransferRate() {
        return String.valueOf(this.transferRate);
    }

    private void onIncrement(Widget.ClickData clickData) {
        this.transferRate = MathHelper.clamp(this.transferRate * 2, 1, this.maxTransferRate);
        markAsDirty();
    }

    private void onDecrement(Widget.ClickData clickData) {
        this.transferRate = MathHelper.clamp(this.transferRate / 2, 1, this.maxTransferRate);
        markAsDirty();
    }

    private void setPumpMode(CoverPump.PumpMode pumpMode) {
        this.pumpMode = pumpMode;
        markAsDirty();
    }

    private CoverPump.PumpMode getPumpMode() {
        return this.pumpMode;
    }

    private void setTextID(String text) {
        this.text = text;
        markAsDirty();
    }

    private String getTextID() {
        return this.text;
    }

    private void addDisplayText(List<ITextComponent> textList) {
        int count = 0, searchResults = 0;
        textList.add(new TextComponentString("§l" + I18n.translateToLocal("machine.universal.entries") + "§r(§e" + this.searchResults + "§r/§e" + this.getMap().size() + "§r)"));
        for (Map.Entry<K, V> entry : getMap().entrySet()) {
            String text = (String) entry.getKey();
            String result = text, result2 = text;

            if (!this.isCaseSensitive) {
                result = result.toLowerCase();
                result2 = result2.toUpperCase();
            }

            if (!this.hasSpaces)
                result = result.replace(" ", "");

            if (!result.isEmpty() && !result.contains(this.searchPrompt) && !result2.contains(this.searchPrompt))
                continue;

            ITextComponent keyEntry = new TextComponentString("[§e" + (++count) + "§r] " + text + "§r")
                    .appendText("\n")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.select"), "select" + text))
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentTranslation("machine.universal.linked.remove"), "remove" + text));
            textList.add(keyEntry);

            if (entry.getValue() instanceof FluidTank) {
                FluidStack fluid = ((FluidTank) entry.getValue()).getFluid();
                boolean empty = fluid == null;
                String name = !empty ? fluid.getUnlocalizedName() : I18n.translateToLocal("metaitem.fluid_cell.empty");
                int capacity = !empty ? ((FluidTank) entry.getValue()).getCapacity() : 0;
                int amount = !empty ? fluid.amount : 0;
                keyEntry.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(name)
                        .appendText("§b ")
                        .appendText(amount + "L")
                        .appendText(" §r/§b ")
                        .appendText(capacity + "L")));
            }
            if (entry.getValue() instanceof LargeItemStackHandler) {
                ItemStack item = ((LargeItemStackHandler) entry.getValue()).getStackInSlot(0);
                boolean empty = item.isEmpty();
                String name = !empty ? item.getTranslationKey() + ".name" : I18n.translateToLocal("metaitem.fluid_cell.empty");
                int capacity = !empty ? ((LargeItemStackHandler) entry.getValue()).getCapacity() : 0;
                int amount = !empty ? item.getCount() : 0;
                keyEntry.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(name)
                        .appendText("§b ")
                        .appendText(String.valueOf(amount))
                        .appendText(" §r/§b ")
                        .appendText(String.valueOf(capacity))));
            }
            if (entry.getValue() instanceof BasicEnergyHandler) {
                BasicEnergyHandler container = (BasicEnergyHandler) entry.getValue();
                keyEntry.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.energy.stored", container.getStored(), container.getCapacity())));
            }
            searchResults++;
        }
        this.searchResults = searchResults;
    }

    private void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        for (Map.Entry<K, V> entry : getMap().entrySet()) {
            String key = (String) entry.getKey();
            if (componentData.equals("select" + key)) {
                this.setTextID(key);
                break;
            }
            if (componentData.equals("remove" + key) && !key.equals("default")) {
                this.getMap().remove(key);
                break;
            }
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
        data.setString("Text", this.text);
        data.setInteger("PumpMode", this.pumpMode.ordinal());
        data.setBoolean("IsWorking", this.isWorkingEnabled);
        data.setBoolean("CaseSensitive", this.isCaseSensitive);
        data.setBoolean("HasSpaces", this.hasSpaces);
        data.setInteger("TransferRate", this.transferRate);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.text = data.hasKey("Text") ? data.getString("Text") : "default";
        this.pumpMode = CoverPump.PumpMode.values()[data.getInteger("PumpMode")];
        this.isWorkingEnabled = data.getBoolean("IsWorking");
        this.isCaseSensitive = data.getBoolean("CaseSensitive");
        this.hasSpaces = data.getBoolean("HasSpaces");
        this.transferRate = data.getInteger("TransferRate");
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingEnabled) {
        this.isWorkingEnabled = isWorkingEnabled;
        markAsDirty();
    }

    private boolean isCaseSensitive() {
        return this.isCaseSensitive;
    }

    private void setCaseSensitive(Boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
        markAsDirty();
    }

    private boolean hasSpaces() {
        return this.hasSpaces;
    }

    private void setSpaces(Boolean hasSpaces) {
        this.hasSpaces = hasSpaces;
        markAsDirty();
    }
}
