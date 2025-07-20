package tj.machines.singleblock;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.GAValues;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.DischargerSlotWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.builder.handlers.ChiselWorkbenchWorkableHandler;
import tj.textures.TJTextures;
import tj.util.EnumFacingHelper;

import javax.annotation.Nullable;
import java.util.List;

import static gregtech.api.gui.GuiTextures.BUTTON_FLUID_OUTPUT;
import static gregtech.api.gui.GuiTextures.BUTTON_ITEM_OUTPUT;
import static tj.TJRecipeMaps.ARCHITECT_RECIPES;
import static tj.gui.TJGuiTextures.POWER_BUTTON;

public class MetaTileEntityChiselWorkbench extends TJTieredWorkableMetaTileEntity {

    private final ChiselWorkbenchWorkableHandler chiselWorkbenchWorkableHandler = new ChiselWorkbenchWorkableHandler(this, () -> this.importItems, () -> this.exportItems, () -> this.energyContainer, null, this::getMaxVoltage, () -> 1);

    public MetaTileEntityChiselWorkbench(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.initializeInventory();
        this.chiselWorkbenchWorkableHandler.initialize(1);
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
            this.chiselWorkbenchWorkableHandler.update();
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
        return ARCHITECT_RECIPES.createUITemplate(this.chiselWorkbenchWorkableHandler::getProgressPercent, this.importItems, this.exportItems, this.importFluids, this.exportFluids)
                .widget(new LabelWidget(7, 5, getMetaFullName()))
                .widget(new DischargerSlotWidget(this.chargerInventory, 0, 79, 62)
                        .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY))
                .widget(new ToggleButtonWidget(151, 62, 18, 18, POWER_BUTTON, this.chiselWorkbenchWorkableHandler::isWorkingEnabled, this.chiselWorkbenchWorkableHandler::setWorkingEnabled)
                        .setTooltipText("machine.universal.toggle.run.mode"))
                .widget(new ToggleButtonWidget(7, 62, 18, 18, BUTTON_ITEM_OUTPUT, this::isAutoOutputItems, this::setItemAutoOutput)
                        .setTooltipText("gregtech.gui.item_auto_output.tooltip"))
                .widget(new ToggleButtonWidget(25, 62, 18, 18, BUTTON_FLUID_OUTPUT, this::isAutoOutputFluids, this::setFluidAutoOutput))
                .widget(new ImageWidget(79, 42, 18, 18, GuiTextures.INDICATOR_NO_ENERGY)
                        .setPredicate(this.chiselWorkbenchWorkableHandler::hasNotEnoughEnergy))
                .bindPlayerInventory(player.inventory)
                .build(this.getHolder(), player);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        TJTextures.TJ_ASSEMBLER_OVERLAY.render(renderState, translation, pipeline, this.frontFacing, this.chiselWorkbenchWorkableHandler.isActive(), this.chiselWorkbenchWorkableHandler.hasProblem(), this.chiselWorkbenchWorkableHandler.isWorkingEnabled());
        TJTextures.CHISEL.renderSided(EnumFacingHelper.getLeftFacingFrom(this.frontFacing), renderState, translation, pipeline);
        TJTextures.CHISEL.renderSided(EnumFacingHelper.getRightFacingFrom(this.frontFacing), renderState, translation, pipeline);
    }
}
