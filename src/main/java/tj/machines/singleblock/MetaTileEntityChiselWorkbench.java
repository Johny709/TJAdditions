package tj.machines.singleblock;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.builder.handlers.ChiselWorkbenchWorkableHandler;
import tj.textures.TJTextures;
import tj.util.EnumFacingHelper;

import javax.annotation.Nullable;
import java.util.List;

import static gregtech.api.gui.GuiTextures.*;
import static gregtech.api.gui.GuiTextures.SLOT;
import static tj.gui.TJGuiTextures.POWER_BUTTON;

public class MetaTileEntityChiselWorkbench extends TJTieredWorkableMetaTileEntity {

    private final ChiselWorkbenchWorkableHandler chiselWorkableHandler = new ChiselWorkbenchWorkableHandler(this);

    public MetaTileEntityChiselWorkbench(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.initializeInventory();
        this.chiselWorkableHandler.initialize(1);
        this.chiselWorkableHandler.setImportItems(() -> this.importItems);
        this.chiselWorkableHandler.setExportItems(() -> this.exportItems);
        this.chiselWorkableHandler.setImportEnergy(() -> this.energyContainer);
        this.chiselWorkableHandler.setMaxVoltage(this::getMaxVoltage);
        this.chiselWorkableHandler.setParallel(() -> 1);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityChiselWorkbench(this.metaTileEntityId, this.getTier());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.universal.tooltip.voltage_in", this.energyContainer.getInputVoltage(), GAValues.VN[this.getTier()]));
        tooltip.add(I18n.format("gregtech.universal.tooltip.energy_storage_capacity", this.energyContainer.getEnergyCapacity()));
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote)
            this.chiselWorkableHandler.update();
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new ItemStackHandler(3);
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new ItemStackHandler(1);
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        return ModularUI.defaultBuilder()
                .widget(new ProgressWidget(this.chiselWorkableHandler::getProgressPercent, 77, 22, 21, 20, PROGRESS_BAR_ARROW, ProgressWidget.MoveType.HORIZONTAL))
                .widget(new SlotWidget(this.importItems, 0, 16, 22, true, true)
                        .setBackgroundTexture(SLOT, INT_CIRCUIT_OVERLAY))
                .widget(new SlotWidget(this.importItems, 1, 34, 22, true, true)
                        .setBackgroundTexture(SLOT, INT_CIRCUIT_OVERLAY))
                .widget(new SlotWidget(this.importItems, 2, 52, 22, true, true)
                        .setBackgroundTexture(SLOT, MOLD_OVERLAY))
                .widget(new SlotWidget(this.exportItems, 0, 105, 22, true, false)
                        .setBackgroundTexture(SLOT))
                .widget(new LabelWidget(7, 5, getMetaFullName()))
                .widget(new DischargerSlotWidget(this.chargerInventory, 0, 79, 62)
                        .setBackgroundTexture(SLOT, CHARGER_OVERLAY))
                .widget(new ToggleButtonWidget(151, 62, 18, 18, POWER_BUTTON, this.chiselWorkableHandler::isWorkingEnabled, this.chiselWorkableHandler::setWorkingEnabled)
                        .setTooltipText("machine.universal.toggle.run.mode"))
                .widget(new ToggleButtonWidget(7, 62, 18, 18, BUTTON_ITEM_OUTPUT, this::isAutoOutputItems, this::setItemAutoOutput)
                        .setTooltipText("gregtech.gui.item_auto_output.tooltip"))
                .widget(new ToggleButtonWidget(25, 62, 18, 18, BUTTON_FLUID_OUTPUT, this::isAutoOutputFluids, this::setFluidAutoOutput))
                .widget(new ImageWidget(79, 42, 18, 18, INDICATOR_NO_ENERGY)
                        .setPredicate(this.chiselWorkableHandler::hasNotEnoughEnergy))
                .bindPlayerInventory(player.inventory)
                .build(this.getHolder(), player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_ASSEMBLER_OVERLAY.render(renderState, translation, pipeline, this.frontFacing, this.chiselWorkableHandler.isActive(), this.chiselWorkableHandler.hasProblem(), this.chiselWorkableHandler.isWorkingEnabled());
        TJTextures.CHISEL.renderSided(this.frontFacing.getOpposite(), renderState, translation, pipeline);
        TJTextures.CHISEL.renderSided(EnumFacingHelper.getLeftFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.CHISEL.renderSided(EnumFacingHelper.getRightFacingFrom(this.frontFacing), renderState, translation, pipeline);
    }
}
