package com.johny.tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.TJValues;
import com.johny.tj.builder.handlers.BasicEnergyHandler;
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
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.johny.tj.gui.TJGuiTextures.POWER_BUTTON;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public abstract class AbstractCoverEnder<K, V> extends CoverBehavior implements CoverWithUI, ITickable, IControllable {

    protected String text = "default";
    protected boolean isWorkingEnabled;
    protected CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;
    protected int maxTransferRate;
    protected int transferRate;

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
        listWidget.addWidget(new AdvancedTextWidget(2, 3, this::addDisplayText, 0xFFFFFF)
                .setClickHandler(this::handleDisplayClick)
                .setMaxWidthLimit(124));
        widgetGroup.addWidget(new ImageWidget(30, 15, 115, 18, DISPLAY));
        widgetGroup.addWidget(new ImageWidget(30, 38, 115, 18, DISPLAY));
        widgetGroup.addWidget(new ImageWidget(30, 61, 115, 80, DISPLAY));
        widgetGroup.addWidget(new TextFieldWidget(32, 43, 112, 18, false, this::getTextID, this::setTextID)
                .setValidator(str -> Pattern.compile("\\*?[a-zA-Z0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new TextFieldWidget(32, 20, 110, 18, false, this::getTransferRate, this::setTransferRate)
                .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()));
        widgetGroup.addWidget(new ClickButtonWidget(151, 38, 18, 18, "O", this::onAddEntry));
        widgetGroup.addWidget(new ClickButtonWidget(151, 15, 18, 18, "+", this::onIncrement));
        widgetGroup.addWidget(new ClickButtonWidget(7, 15, 18, 18, "-", this::onDecrement));
        widgetGroup.addWidget(new ClickButtonWidget(7, 61, 18, 18, "", this::onClear)
                .setButtonTexture(BUTTON_CLEAR_GRID));
        widgetGroup.addWidget(new CycleButtonWidget(30, 145, 115, 18, CoverPump.PumpMode.class, this::getPumpMode, this::setPumpMode));
        widgetGroup.addWidget(new ToggleButtonWidget(7, 145, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled));
        addWidgets(addWidgetGroup::addWidget);
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 246)
                .bindPlayerInventory(player.inventory, 165)
                .widget(widgetGroup)
                .widget(listWidget)
                .widget(addWidgetGroup)
                .build(this, player);
    }

    private void setTransferRate(String amount) {
        this.transferRate = Math.min(Integer.parseInt(amount), maxTransferRate);
        markAsDirty();
    }

    public String getTransferRate() {
        return String.valueOf(transferRate);
    }

    private void onIncrement(Widget.ClickData clickData) {
        transferRate = MathHelper.clamp(transferRate * 2, 1, maxTransferRate);
    }

    private void onDecrement(Widget.ClickData clickData) {
        transferRate = MathHelper.clamp(transferRate / 2, 1, maxTransferRate);
    }

    private void setPumpMode(CoverPump.PumpMode pumpMode) {
        this.pumpMode = pumpMode;
        markAsDirty();
    }

    private CoverPump.PumpMode getPumpMode() {
        return pumpMode;
    }

    private void setTextID(String text) {
        this.text = text;
        markAsDirty();
    }

    private String getTextID() {
        return text;
    }

    private void addDisplayText(List<ITextComponent> textList) {
        for (Map.Entry<K, V> entry : getMap().entrySet()) {
            String text = (String) entry.getKey();
            ITextComponent textComponent = withButton(new TextComponentString("§e[§r" + text + "§e]"), "O" + text)
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentString("[X]"), "X" + text));
            textList.add(textComponent);

            if (entry.getValue() instanceof FluidTank) {
                FluidStack fluid = ((FluidTank) entry.getValue()).getFluid();
                int capacity = ((FluidTank) entry.getValue()).getCapacity();
                if (fluid != null)
                    textComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(fluid.getUnlocalizedName())
                            .appendText(" ")
                            .appendText(fluid.amount + "L")
                            .appendText(" / ")
                            .appendText(capacity + "L")));
            }
            if (entry.getValue() instanceof LargeItemStackHandler) {
                ItemStack item = ((LargeItemStackHandler) entry.getValue()).getStackInSlot(0);
                int capacity = ((LargeItemStackHandler) entry.getValue()).getCapacity();
                if (!item.isEmpty())
                    textComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(item.getTranslationKey() + ".name")
                            .appendText(" ")
                            .appendText(String.valueOf(item.getCount()))
                            .appendText(" / ")
                            .appendText(String.valueOf(capacity))));
            }
            if (entry.getValue() instanceof BasicEnergyHandler) {
                BasicEnergyHandler container = (BasicEnergyHandler) entry.getValue();
                textComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("machine.universal.energy.stored", container.getStored(), container.getCapacity())));
            }
        }
    }

    private void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        for (Map.Entry<K, V> entry : getMap().entrySet()) {
            String key = (String) entry.getKey();
            if (componentData.equals("O" + key)) {
                setTextID(key);
                break;
            }
            if (componentData.equals("X" + key) && !key.equals("default")) {
                getMap().remove(key);
                break;
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("Text", text);
        data.setInteger("PumpMode", pumpMode.ordinal());
        data.setBoolean("IsWorking", isWorkingEnabled);
        data.setInteger("TransferRate", transferRate);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        text = data.hasKey("Text") ? data.getString("Text") : "default";
        pumpMode = CoverPump.PumpMode.values()[data.getInteger("PumpMode")];
        isWorkingEnabled = data.getBoolean("IsWorking");
        transferRate = data.getInteger("TransferRate");
    }

    @Override
    public boolean isWorkingEnabled() {
        return isWorkingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingEnabled) {
        this.isWorkingEnabled = isWorkingEnabled;
    }
}
