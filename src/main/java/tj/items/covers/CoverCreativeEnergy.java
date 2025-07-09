package tj.items.covers;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import tj.gui.widgets.TJTextFieldWidget;
import tj.textures.TJTextures;

import java.util.regex.Pattern;

import static gregtech.api.gui.GuiTextures.BORDERED_BACKGROUND;
import static gregtech.api.gui.GuiTextures.DISPLAY;

public class CoverCreativeEnergy extends CoverBehavior implements ITickable, CoverWithUI {

    private final IEnergyContainer energyContainer;
    private boolean simulateVoltage;
    private long energyRate = Long.MAX_VALUE;

    public CoverCreativeEnergy(ICoverable coverHolder, EnumFacing attachedSide) {
        super(coverHolder, attachedSide);
        this.energyContainer = coverHolder.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);
    }

    @Override
    public boolean canAttach() {
        return this.coverHolder.getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, this.attachedSide) != null;
    }

    @Override
    public void renderCover(CCRenderState ccRenderState, Matrix4 matrix4, IVertexOperation[] iVertexOperations, Cuboid6 cuboid6, BlockRenderLayer blockRenderLayer) {
        TJTextures.COVER_CREATIVE_ENERGY.renderSided(attachedSide, cuboid6, ccRenderState, iVertexOperations, matrix4);
    }

    @Override
    public void update() {
        this.energyContainer.addEnergy(this.energyRate);
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (!playerIn.world.isRemote)
            this.openUI((EntityPlayerMP) playerIn);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        return ModularUI.builder(BORDERED_BACKGROUND, 176, 160)
                .bindPlayerInventory(player.inventory, 80)
                .label(4, 4, "metaitem.creative.item.cover.name")
                .widget(new ImageWidget(7, 40, 162, 18, DISPLAY)
                        .setPredicate(this::getSimulateVoltage))
                .widget(new TJTextFieldWidget(12, 45, 157, 18, false, this::getEnergyRate, this::setEnergyRate)
                        .setValidator(str -> Pattern.compile("\\*?[0-9_]*\\*?").matcher(str).matches()))
                .widget(new ToggleButtonWidget(7, 22, 162, 18, this::getSimulateVoltage, this::setSimulateVoltage))
                .build(this, player);
    }

    private void setSimulateVoltage(boolean simulateVoltage) {
        this.simulateVoltage = simulateVoltage;
    }

    private boolean getSimulateVoltage() {
        return this.simulateVoltage;
    }

    private void setEnergyRate(String energyRate) {
        this.energyRate = Long.parseLong(energyRate);
    }

    private String getEnergyRate() {
        return String.valueOf(this.energyRate);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("simulateVoltage", this.simulateVoltage);
        data.setLong("energyRate", this.energyRate);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.simulateVoltage = data.getBoolean("simulateVoltage");
        if (data.hasKey("energyRate"))
            this.energyRate = data.getLong("energyRate");
    }
}
