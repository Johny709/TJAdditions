package tj.machines.singleblock;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import gregicadditions.GAValues;
import gregicadditions.machines.overrides.GATieredMetaTileEntity;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IActiveOutputSide;
import gregtech.api.capability.IElectricItem;
import gregtech.api.metatileentity.MTETrait;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Serves as a basis to implement your own recipe or workable handler for single blocks.
 */
public abstract class TJTieredWorkableMetaTileEntity extends GATieredMetaTileEntity implements IActiveOutputSide {

    protected final ItemStackHandler chargerInventory;
    private EnumFacing itemOutputFacing = this.frontFacing.getOpposite();
    private EnumFacing fluidOutputFacing = this.frontFacing.getOpposite();
    private boolean allowInputFromOutputSide;
    private boolean isItemAutoOutput;
    private boolean isFluidAutoOutput;

    public TJTieredWorkableMetaTileEntity(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        this.chargerInventory = new ItemStackHandler(1) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    @Override
    protected boolean shouldUpdate(MTETrait trait) {
        return false;
    }

    @Override
    public void update() {
        super.update();
        if (!this.getWorld().isRemote) {
            IElectricItem chargerContainer = this.chargerInventory.getStackInSlot(0).getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
            if (chargerContainer != null) {
                chargerContainer.charge(this.getMaxVoltage(), 1, true, false);
            }
            if (this.getOffsetTimer() % 5 == 0) {
                if (this.isItemAutoOutput)
                    this.pushItemsIntoNearbyHandlers(this.itemOutputFacing);
                if (this.isFluidAutoOutput)
                    this.pushFluidsIntoNearbyHandlers(this.fluidOutputFacing);
            }
        }
    }

    @Override
    public boolean onWrenchClick(EntityPlayer playerIn, EnumHand hand, EnumFacing facing, CuboidRayTraceResult hitResult) {
        EnumFacing currentOutputSide = playerIn.isSneaking() ? this.fluidOutputFacing : this.itemOutputFacing;
        if (currentOutputSide == facing || this.getFrontFacing() == facing) {
            return false;
        }
        if (!getWorld().isRemote) {
            if (playerIn.isSneaking())
                this.setFluidOutputFacing(facing);
            else this.setItemOutputFacing(facing);
        }
        return true;
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        switch (dataId) {
            case 100: this.itemOutputFacing = EnumFacing.VALUES[buf.readByte()]; break;
            case 101: this.isItemAutoOutput = buf.readBoolean(); break;
            case 102: this.fluidOutputFacing = EnumFacing.VALUES[buf.readByte()]; break;
            case 103: this.isFluidAutoOutput = buf.readBoolean(); break;
        }
        this.scheduleRenderUpdate();
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeBoolean(this.isItemAutoOutput);
        buf.writeBoolean(this.isFluidAutoOutput);
        buf.writeByte(this.itemOutputFacing.getIndex());
        buf.writeByte(this.fluidOutputFacing.getIndex());
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.isItemAutoOutput = buf.readBoolean();
        this.isFluidAutoOutput = buf.readBoolean();
        this.itemOutputFacing = EnumFacing.VALUES[buf.readByte()];
        this.fluidOutputFacing = EnumFacing.VALUES[buf.readByte()];
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("isItemAutoOutput", this.isItemAutoOutput);
        data.setBoolean("isFluidAutoOutput", this.isFluidAutoOutput);
        data.setBoolean("allowInputFromOutputSide", this.allowInputFromOutputSide);
        data.setInteger("itemOutputFacing", this.itemOutputFacing.getIndex());
        data.setInteger("fluidOutputFacing", this.fluidOutputFacing.getIndex());
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.isItemAutoOutput = data.getBoolean("isItemAutoOutput");
        this.isFluidAutoOutput = data.getBoolean("isFluidAutoOutput");
        this.allowInputFromOutputSide = data.getBoolean("allowInputFromOutputSide");
        if (data.hasKey("itemOutputFacing"))
            this.itemOutputFacing = EnumFacing.VALUES[data.getInteger("itemOutputFacing")];
        if (data.hasKey("fluidOutputFacing"))
            this.fluidOutputFacing = EnumFacing.VALUES[data.getInteger("fluidOutputFacing")];
    }

    protected void setItemOutputFacing(EnumFacing itemOutputFacing) {
        this.itemOutputFacing = itemOutputFacing;
        if (!this.getWorld().isRemote) {
            this.getHolder().notifyBlockUpdate();
            this.writeCustomData(100, buf -> buf.writeByte(itemOutputFacing.getIndex()));
            this.markDirty();
        }
    }

    protected void setItemAutoOutput(boolean isItemAutoOutput) {
        this.isItemAutoOutput = isItemAutoOutput;
        if (!this.getWorld().isRemote) {
            this.writeCustomData(101, buffer -> buffer.writeBoolean(isItemAutoOutput));
            this.markDirty();
        }
    }

    protected void setFluidOutputFacing(EnumFacing fluidOutputFacing) {
        this.fluidOutputFacing = fluidOutputFacing;
        if (!this.getWorld().isRemote) {
            this.getHolder().notifyBlockUpdate();
            this.writeCustomData(102, buffer -> buffer.writeByte(fluidOutputFacing.getIndex()));
            this.markDirty();
        }
    }

    protected void setFluidAutoOutput(boolean isFluidAutoOutput) {
        this.isFluidAutoOutput = isFluidAutoOutput;
        if (!this.getWorld().isRemote) {
            this.writeCustomData(103, buffer -> buffer.writeBoolean(isFluidAutoOutput));
            this.markDirty();
        }
    }

    public EnumFacing getItemOutputFacing() {
        return this.itemOutputFacing;
    }

    public EnumFacing getFluidOutputFacing() {
        return this.fluidOutputFacing;
    }

    @Override
    public boolean isAutoOutputItems() {
        return this.isItemAutoOutput;
    }

    @Override
    public boolean isAutoOutputFluids() {
        return isFluidAutoOutput;
    }

    @Override
    public boolean isAllowInputFromOutputSide() {
        return this.allowInputFromOutputSide;
    }

    public long getMaxVoltage() {
        return GAValues.V[this.getTier()];
    }
}
