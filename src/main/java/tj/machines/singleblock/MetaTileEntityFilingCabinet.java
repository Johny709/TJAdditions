package tj.machines.singleblock;

import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.metatileentity.IFastRenderMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.MetaTileEntityUIFactory;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.gui.widgets.SlotScrollableWidget;
import tj.gui.widgets.TJSlotWidget;
import tj.items.handlers.CabinetItemStackHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetaTileEntityFilingCabinet extends MetaTileEntity implements IFastRenderMetaTileEntity {

    private static final IndexedCuboid6 COLLISION_BOX = new IndexedCuboid6(null, new Cuboid6(3 / 16.0, 0 / 16.0, 3 / 16.0, 13 / 16.0, 14 / 16.0, 13 / 16.0));
    private final Set<EntityPlayer> guiUsers = new HashSet<>();
    private float doorAngle = 0.0f;
    private float prevDoorAngle = 0.0f;

    public MetaTileEntityFilingCabinet(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.initializeInventory();
        this.itemInventory = this.importItems;
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityFilingCabinet(this.metaTileEntityId);
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new CabinetItemStackHandler(27, 64)
                .setSizeChangeListener(this::resizeInventory);
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {}

    @Override
    public void writeItemStackData(NBTTagCompound itemStack) {
        super.writeItemStackData(itemStack);
        itemStack.setTag("Inventory", ((ItemStackHandler) this.importItems).serializeNBT());
    }

    @Override
    public void initFromItemStackData(NBTTagCompound itemStack) {
        super.initFromItemStackData(itemStack);
        ((ItemStackHandler) this.importItems).deserializeNBT(itemStack.getCompoundTag("Inventory"));
    }

    private void resizeInventory(int size) {
        NonNullList<ItemStack> transferStackList = NonNullList.create();
        String name = ((CabinetItemStackHandler) this.importItems).getAllowedItemName();
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            transferStackList.add(this.importItems.getStackInSlot(i));
        }
        this.itemInventory = this.importItems = new CabinetItemStackHandler(size, 64)
                .setSizeChangeListener(this::resizeInventory)
                .setAllowedItemByName(name);
        int minSize = Math.min(size, transferStackList.size());
        for (int i = 0; i < minSize ; i++) {
            this.importItems.setStackInSlot(i, transferStackList.get(i));
        }
        if (!this.getWorld().isRemote) {
            this.writeCustomData(10, buf -> buf.writeInt(size));
            this.markDirty();
            this.guiUsers.forEach(player -> MetaTileEntityUIFactory.INSTANCE.openUI(this.getHolder(), (EntityPlayerMP) player));
        }
    }

    @Override
    protected ModularUI createUI(EntityPlayer player) {
        SlotScrollableWidget slotScrollableWidget = new SlotScrollableWidget(7, 14, 180, 72, 9);
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            slotScrollableWidget.addWidget(new TJSlotWidget(this.importItems, i, 18 * (i % 9), 18 * (i / 9), true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        return ModularUI.builder(GuiTextures.BACKGROUND, 176, 176)
                .bindOpenListener(() -> this.guiUsers.add(player))
                .bindCloseListener(() -> this.guiUsers.remove(player))
                .widget(new LabelWidget(7, 5, getMetaFullName()))
                .widget(slotScrollableWidget)
                .bindPlayerInventory(player.inventory, 94)
                .build(this.getHolder(), player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {}

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntityFast(CCRenderState renderState, Matrix4 translation, float partialTicks) {
        ColourMultiplier colourMultiplier = new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(GTValues.VC[1]));
        float angle = this.prevDoorAngle + (this.doorAngle - this.prevDoorAngle) * partialTicks;
        angle = 1.0f - (1.0f - angle) * (1.0f - angle) * (1.0f - angle);
        float resultDoorAngle = angle * 120.0f;
        Textures.SAFE.render(renderState, translation, new IVertexOperation[] { colourMultiplier }, getFrontFacing(), resultDoorAngle);
    }

    @Override
    public void addCollisionBoundingBox(List<IndexedCuboid6> collisionList) {
        collisionList.add(COLLISION_BOX);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getLightOpacity() {
        return 1;
    }

    @Override
    protected boolean shouldSerializeInventories() {
        return false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.getPos().add(-1, 0, -1), this.getPos().add(2, 1, 2));
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeCompoundTag(((ItemStackHandler) this.importItems).serializeNBT());
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        try {
            ((ItemStackHandler) this.importItems).deserializeNBT(buf.readCompoundTag());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == 10)
            this.resizeInventory(buf.readInt());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("Inventory", ((ItemStackHandler) this.importItems).serializeNBT());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("Inventory"))
            ((ItemStackHandler) this.importItems).deserializeNBT(data.getCompoundTag("Inventory"));
    }
}
