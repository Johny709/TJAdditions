package tj.mixin.gregtech;

import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tj.TJConfig;
import tj.builder.WidgetTabBuilder;
import tj.gui.TJGuiTextures;
import tj.gui.TJHorizontoalTabListRenderer;

import java.util.List;

import static tj.gui.TJHorizontoalTabListRenderer.HorizontalStartCorner.LEFT;
import static tj.gui.TJHorizontoalTabListRenderer.VerticalLocation.BOTTOM;

@Mixin(value = MultiblockWithDisplayBase.class, remap = false)
public abstract class MultiblockWithDisplayBaseMixin extends MultiblockControllerBase {

    public MultiblockWithDisplayBaseMixin(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Shadow
    protected abstract void addDisplayText(List<ITextComponent> textList);

    @Shadow
    protected abstract void handleDisplayClick(String componentData, Widget.ClickData clickData);

    @Inject(method = "createUITemplate", at = @At("HEAD"), cancellable = true)
    private void injectCreateUITemplate(EntityPlayer entityPlayer, CallbackInfoReturnable<ModularUI.Builder> cir) {
        if (TJConfig.machines.multiblockUIOverrides) {
            ModularUI.Builder builder = ModularUI.extendedBuilder();
            WidgetTabBuilder tabBuilder = new WidgetTabBuilder()
                    .setTabListRenderer(() -> new TJHorizontoalTabListRenderer(LEFT, BOTTOM))
                    .setPosition(-10, 1);
            this.addNewTabs(tabBuilder);
            builder.image(-10, -20, 195, 237, TJGuiTextures.NEW_MULTIBLOCK_DISPLAY);
            builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT ,-3, 134);
            builder.widget(new LabelWidget(0, -13, this.getMetaFullName(), 0xFFFFFF));
            builder.widget(tabBuilder.build());
            cir.setReturnValue(builder);
        }
    }

    @Unique
    private void addNewTabs(WidgetTabBuilder tabBuilder) {
        tabBuilder.addTab("tj.multiblock.tab.display", this.getStackForm(), this::addMainDisplayTab);
    }

    @Unique
    private void addMainDisplayTab(WidgetGroup widgetGroup) {
        widgetGroup.addWidget(new AdvancedTextWidget(10, -2, this::addDisplayText, 0xFFFFFF)
                .setMaxWidthLimit(180)
                .setClickHandler(this::handleDisplayClick));
    }
}
