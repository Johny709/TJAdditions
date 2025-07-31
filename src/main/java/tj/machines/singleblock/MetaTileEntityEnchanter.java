package tj.machines.singleblock;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.builder.handlers.EnchanterWorkableHandler;
import tj.textures.TJTextures;
import tj.util.EnumFacingHelper;

import javax.annotation.Nullable;
import java.util.List;

import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.GuiTextures.INDICATOR_NO_ENERGY;
import static tj.gui.TJGuiTextures.POWER_BUTTON;

public class MetaTileEntityEnchanter extends TJTieredWorkableMetaTileEntity {

    private final EnchanterWorkableHandler enchanterWorkableHandler = new EnchanterWorkableHandler(this);
    private final IFluidTank tank;

    public MetaTileEntityEnchanter(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.tank = new FluidTank(16000);
        this.enchanterWorkableHandler.initialize(1);
        this.enchanterWorkableHandler.setImportItems(this::getImportItems);
        this.enchanterWorkableHandler.setExportItems(this::getExportItems);
        this.enchanterWorkableHandler.setImportFluids(this::getImportFluids);
        this.enchanterWorkableHandler.setImportEnergy(() -> this.energyContainer);
        this.enchanterWorkableHandler.setMaxVoltage(this::getMaxVoltage);
        this.enchanterWorkableHandler.setParallel(() -> 1);
        this.initializeInventory();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityEnchanter(this.metaTileEntityId, this.getTier());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.universal.tooltip.voltage_in", this.energyContainer.getInputVoltage(), GAValues.VN[this.getTier()]));
        tooltip.add(I18n.format("gregtech.universal.tooltip.energy_storage_capacity", this.energyContainer.getEnergyCapacity()));
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote)
            this.enchanterWorkableHandler.update();
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new ItemStackHandler(2);
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new ItemStackHandler(2);
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return new FluidTankList(true, this.tank);
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        return ModularUI.defaultBuilder()
                .widget(new ProgressWidget(this.enchanterWorkableHandler::getProgressPercent, 77, 22, 21, 20, PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL))
                .widget(new SlotWidget(this.importItems, 0, 34, 22, true, true)
                        .setBackgroundTexture(SLOT, BOXED_OVERLAY))
                .widget(new SlotWidget(this.importItems, 1, 52, 22, true, true)
                        .setBackgroundTexture(SLOT, MOLD_OVERLAY))
                .widget(new SlotWidget(this.exportItems, 0, 105, 22, true, false)
                        .setBackgroundTexture(SLOT))
                .widget(new SlotWidget(this.exportItems, 1, 123, 22, true, false)
                        .setBackgroundTexture(SLOT))
                .widget(new TankWidget(this.tank, 16, 22, 18, 18)
                        .setBackgroundTexture(FLUID_SLOT))
                .widget(new LabelWidget(7, 5, getMetaFullName()))
                .widget(new DischargerSlotWidget(this.chargerInventory, 0, 79, 62)
                        .setBackgroundTexture(SLOT, CHARGER_OVERLAY))
                .widget(new ToggleButtonWidget(151, 62, 18, 18, POWER_BUTTON, this.enchanterWorkableHandler::isWorkingEnabled, this.enchanterWorkableHandler::setWorkingEnabled)
                        .setTooltipText("machine.universal.toggle.run.mode"))
                .widget(new ToggleButtonWidget(7, 62, 18, 18, BUTTON_ITEM_OUTPUT, this::isAutoOutputItems, this::setItemAutoOutput)
                        .setTooltipText("gregtech.gui.item_auto_output.tooltip"))
                .widget(new ToggleButtonWidget(25, 62, 18, 18, BUTTON_FLUID_OUTPUT, this::isAutoOutputFluids, this::setFluidAutoOutput))
                .widget(new ImageWidget(79, 42, 18, 18, INDICATOR_NO_ENERGY)
                        .setPredicate(this.enchanterWorkableHandler::hasNotEnoughEnergy))
                .bindPlayerInventory(player.inventory)
                .build(this.getHolder(), player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_MULTIBLOCK_WORKABLE_OVERLAY.render(renderState, translation, pipeline, this.frontFacing, this.enchanterWorkableHandler.isActive(), this.enchanterWorkableHandler.hasProblem(), this.enchanterWorkableHandler.isWorkingEnabled());
        TJTextures.ENCHANTED_BOOK.renderSided(this.frontFacing.getOpposite(), renderState, translation, pipeline);
        TJTextures.ENCHANTED_BOOK.renderSided(EnumFacingHelper.getLeftFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.ENCHANTED_BOOK.renderSided(EnumFacingHelper.getRightFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.ENCHANTING_TABLE.renderSided(EnumFacingHelper.getTopFacingFrom(this.frontFacing), renderState, translation, pipeline);
    }
}
