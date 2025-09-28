package tj.machines.singleblock;

import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregicadditions.client.ClientHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.SortingButtonWidget;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.recipes.ModHandler;
import gregtech.api.unification.material.type.DustMaterial;
import gregtech.api.util.GTUtility;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import tj.gui.widgets.SlotScrollableWidget;

import javax.annotation.Nullable;
import java.util.List;

import static gregtech.api.unification.material.Materials.Obsidian;


public class MetaTileEntityCompressedCrate extends MetaTileEntity {

    private static final int ROW_SIZE = 27;
    private static final int AMOUNT_OF_ROWS = 27;
    private static final DustMaterial OBSIDIAN = Obsidian;

    public MetaTileEntityCompressedCrate(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.initializeInventory();
        this.itemInventory = this.importItems;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityCompressedCrate(this.metaTileEntityId);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("tj.machine.compressed_chest.description"));
        tooltip.add(I18n.format("machine.universal.stack", 64));
        tooltip.add(I18n.format("machine.universal.slots", ROW_SIZE * AMOUNT_OF_ROWS));
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new ItemStackHandler(ROW_SIZE * AMOUNT_OF_ROWS);
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {}

    @Override
    public void writeItemStackData(NBTTagCompound itemStack) {
        super.writeItemStackData(itemStack);
        NBTTagCompound compound = new NBTTagCompound();
        GTUtility.writeItems(this.importItems, "Inventory", compound);
        itemStack.setTag("Data", compound);
    }

    @Override
    public void initFromItemStackData(NBTTagCompound itemStack) {
        super.initFromItemStackData(itemStack);
        GTUtility.readItems(this.importItems, "Inventory", itemStack.getCompoundTag("Data"));
    }

    @Override
    public boolean hasFrontFacing() {
        return false;
    }

    @Override
    public int getLightOpacity() {
        return 1;
    }

    @Override
    public String getHarvestTool() {
        return OBSIDIAN.toString().contains("wood") ? "axe" : "pickaxe";
    }

    @Override
    public int getActualComparatorValue() {
        return ItemHandlerHelper.calcRedstoneFromInventory(this.importItems);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        if (ModHandler.isMaterialWood(OBSIDIAN)) {
            return Pair.of(ClientHandler.WOODEN_CRATE.getParticleTexture(), getPaintingColor());
        } else {
            int color = ColourRGBA.multiply(
                    GTUtility.convertRGBtoOpaqueRGBA_CL(OBSIDIAN.materialRGB),
                    GTUtility.convertRGBtoOpaqueRGBA_CL(getPaintingColor()));
            color = GTUtility.convertOpaqueRGBA_CLtoRGB(color);
            return Pair.of(ClientHandler.METAL_CRATE.getParticleTexture(), color);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        if (OBSIDIAN.toString().contains("wood")) {
            ClientHandler.WOODEN_CRATE.render(renderState, translation, GTUtility.convertRGBtoOpaqueRGBA_CL(getPaintingColorForRendering()), pipeline);
        } else {
            int baseColor = ColourRGBA.multiply(GTUtility.convertRGBtoOpaqueRGBA_CL(OBSIDIAN.materialRGB), GTUtility.convertRGBtoOpaqueRGBA_CL(getPaintingColorForRendering()));
            ClientHandler.METAL_CRATE.render(renderState, translation, baseColor, pipeline);
        }
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND,
                        Math.max(176, 14 + Math.min(27, ROW_SIZE) * 18),
                        18 + 18 * Math.min(12, AMOUNT_OF_ROWS) + 94)
                .label(5, 5, this.getMetaFullName());
        SlotScrollableWidget scrollableListWidget = new SlotScrollableWidget(7, 18, 18 + ROW_SIZE * 18,  18 * Math.min(7, AMOUNT_OF_ROWS) + 94, ROW_SIZE);
        builder.widget(new SortingButtonWidget(111, 4, 60, 10, "gregtech.gui.sort",
                (info) -> MetaTileEntityCompressedChest.sortInventorySlotContents(this.importItems)));

        for (int i = 0; i < this.importItems.getSlots(); i++) {
            scrollableListWidget.addWidget(new SlotWidget(this.importItems, i, 18 * (i % ROW_SIZE), 18 * (i / ROW_SIZE), true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        int startX = (Math.max(176, 14 + ROW_SIZE * 18) - 162) / 2;
        builder.widget(scrollableListWidget);
        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, startX, 18 + 18 * Math.min(12, AMOUNT_OF_ROWS) + 12);
        return builder.build(this.getHolder(), entityPlayer);
    }
}
