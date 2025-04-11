package com.johny.tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.johny.tj.gui.widgets.TJTankWidget;
import com.johny.tj.textures.TJTextures;
import com.johny.tj.util.EnderWorldData;
import gregicadditions.GAValues;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.johny.tj.gui.TJGuiTextures.POWER_BUTTON;
import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.widgets.AdvancedTextWidget.withButton;

public class CoverEnderFluid extends CoverBehavior implements CoverWithUI, ITickable, IControllable {

    private final IFluidHandler fluidTank;
    private String text = "default";
    private final int maxTransferRate;
    private int transferRate;
    private final int tier;
    private boolean isWorkingEnabled;
    private CoverPump.PumpMode pumpMode = CoverPump.PumpMode.IMPORT;

    public CoverEnderFluid(ICoverable coverHolder, EnumFacing attachedSide, int tier) {
        super(coverHolder, attachedSide);
        this.tier = tier;
        this.fluidTank = this.coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        this.maxTransferRate = (int) Math.min(Math.pow(4, tier) * 16, Integer.MAX_VALUE);
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.attachedSide) != null;
    }

    @Override
    public void renderCover(CCRenderState ccRenderState, Matrix4 matrix4, IVertexOperation[] iVertexOperations, Cuboid6 cuboid6, BlockRenderLayer blockRenderLayer) {
        TJTextures.COVER_CREATIVE_FLUID.renderSided(attachedSide, cuboid6, ccRenderState, iVertexOperations, matrix4);
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (!playerIn.world.isRemote) {
            this.openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }


    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup widgetGroup = new WidgetGroup();
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
        widgetGroup.addWidget(new ClickButtonWidget(151, 38, 18, 18, "O", this::onPressed));
        widgetGroup.addWidget(new ClickButtonWidget(151, 15, 18, 18, "+", this::onIncrement));
        widgetGroup.addWidget(new ClickButtonWidget(7, 15, 18, 18, "-", this::onDecrement));
        widgetGroup.addWidget(new ClickButtonWidget(7, 61, 18, 18, "", this::onClear)
                .setButtonTexture(BUTTON_CLEAR_GRID));
        widgetGroup.addWidget(new TJTankWidget(this::getFluidTank, 7, 38, 18, 18)
                .setBackgroundTexture(FLUID_SLOT)
                .setContainerClicking(true, true));
        widgetGroup.addWidget(new CycleButtonWidget(30, 145, 115, 18, CoverPump.PumpMode.class, this::getPumpMode, this::setPumpMode));
        widgetGroup.addWidget(new ToggleButtonWidget(7, 145, 18, 18, POWER_BUTTON, this::isWorkingEnabled, this::setWorkingEnabled));
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 246)
                .bindPlayerInventory(player.inventory, 165)
                .label(30, 4, "metaitem.ender_fluid_cover_" + GAValues.VN[tier].toLowerCase() + ".name")
                .widget(widgetGroup)
                .widget(listWidget)
                .build(this, player);
    }

    private void setPumpMode(CoverPump.PumpMode pumpMode) {
        this.pumpMode = pumpMode;
        markAsDirty();
    }

    private CoverPump.PumpMode getPumpMode() {
        return pumpMode;
    }

    private void setTransferRate(String amount) {
        this.transferRate = Math.min(Integer.parseInt(amount), maxTransferRate);
        markAsDirty();
    }

    private String getTransferRate() {
        return String.valueOf(transferRate);
    }

    private void onClear(Widget.ClickData clickData) {
        EnderWorldData.getFluidTankMap().get(text).setFluid(null);
    }

    private void onPressed(Widget.ClickData clickData) {
        EnderWorldData.getFluidTankMap().putIfAbsent(text, new FluidTank(Integer.MAX_VALUE));
    }

    private void setTextID(String text) {
        this.text = text;
        markAsDirty();
    }

    private String getTextID() {
        return text;
    }

    private void onIncrement(Widget.ClickData clickData) {
        transferRate = MathHelper.clamp(transferRate * 2, 1, maxTransferRate);
    }

    private void onDecrement(Widget.ClickData clickData) {
        transferRate = MathHelper.clamp(transferRate / 2, 1, maxTransferRate);
    }

    private IFluidTank getFluidTank() {
        IFluidTank tank = EnderWorldData.getFluidTankMap().get(text);
        return tank != null ? tank : EnderWorldData.getFluidTankMap().get("default");
    }

    private void addDisplayText(List<ITextComponent> textList) {
        for (Map.Entry<String, FluidTank> tank : EnderWorldData.getFluidTankMap().entrySet()) {
            ITextComponent textComponent = withButton(new TextComponentString("§e[§r" + tank.getKey() + "§e]"), "O" + tank.getKey())
                    .appendText(" ")
                    .appendSibling(withButton(new TextComponentString("[X]"), "X" + tank.getKey()));

            FluidStack fluid = tank.getValue().getFluid();
            if (fluid != null)
                textComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation(fluid.getUnlocalizedName())
                        .appendText(" ")
                        .appendText(fluid.amount + "L")));
            textList.add(textComponent);
        }
    }

    private void handleDisplayClick(String componentData, Widget.ClickData clickData) {
        for (Map.Entry<String, FluidTank> tank : EnderWorldData.getFluidTankMap().entrySet()) {
            if (componentData.equals("O" + tank.getKey())) {
                setTextID(tank.getKey());
                break;
            }
            if (componentData.equals("X" + tank.getKey()) && !tank.getKey().equals("default")) {
                EnderWorldData.getFluidTankMap().remove(tank.getKey());
                break;
            }
        }
    }

    @Override
    public void update() {
        if (isWorkingEnabled) {
            if (pumpMode == CoverPump.PumpMode.EXPORT) {
                FluidStack enderStack = getFluidTank().drain(transferRate, false);
                    if (fluidTank.fill(enderStack, false) > 0)
                        getFluidTank().drain(fluidTank.fill(enderStack, true), true);
            } else {
                FluidStack fluidStack = fluidTank.drain(transferRate, false);
                    if (getFluidTank().fill(fluidStack, false) > 0)
                        fluidTank.drain(getFluidTank().fill(fluidStack, true), true);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("Text", text);
        data.setInteger("TransferRate", transferRate);
        data.setInteger("PumpMode", pumpMode.ordinal());
        data.setBoolean("IsWorking", isWorkingEnabled);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        text = data.hasKey("Text") ? data.getString("Text") : "default";
        pumpMode = CoverPump.PumpMode.values()[data.getInteger("PumpMode")];
        transferRate = data.getInteger("TransferRate");
        isWorkingEnabled = data.getBoolean("IsWorking");
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
