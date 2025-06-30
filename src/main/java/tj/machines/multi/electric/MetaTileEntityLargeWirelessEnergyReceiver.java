package tj.machines.multi.electric;

import gregicadditions.client.ClientHandler;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.api.render.ICubeRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.energy.IEnergyStorage;

public class MetaTileEntityLargeWirelessEnergyReceiver extends MetaTileEntityLargeWirelessEnergyEmitter {

    private IEnergyContainer outputEnergyContainer;

    public MetaTileEntityLargeWirelessEnergyReceiver(ResourceLocation metaTileEntityId, TransferType transferType) {
        super(metaTileEntityId, transferType);
        this.transferType = transferType;
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWirelessEnergyReceiver(this.metaTileEntityId, this.transferType);
    }

    @Override
    protected boolean hasEnoughEnergy(long amount) {
        return true;
    }

    @Override
    protected void transferRF(int energyToAdd, IEnergyStorage RFContainer) {
        if (RFContainer == null)
            return;
        long energyRemainingToFill = (this.outputEnergyContainer.getEnergyCapacity() - this.outputEnergyContainer.getEnergyStored());
        if (this.outputEnergyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            int energyExtracted = RFContainer.extractEnergy((int) Math.min(Integer.MAX_VALUE, Math.min(energyToAdd * 4L, energyRemainingToFill)), false);
            this.outputEnergyContainer.addEnergy(energyExtracted / 4);
        }
    }

    @Override
    protected void transferEU(long energyToAdd, IEnergyContainer EUContainer) {
        if (EUContainer == null)
            return;
        long energyRemainingToFill = this.outputEnergyContainer.getEnergyCapacity() - this.outputEnergyContainer.getEnergyStored();
        if (this.outputEnergyContainer.getEnergyStored() < 1 || energyRemainingToFill != 0) {
            long energyExtracted = EUContainer.removeEnergy(energyToAdd);
            this.outputEnergyContainer.addEnergy(Math.abs(energyExtracted));
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        this.outputEnergyContainer = new EnergyContainerList(getAbilities(MultiblockAbility.OUTPUT_ENERGY));
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.RED_STEEL_CASING;
    }

    @Override
    public long getEnergyStored() {
        return this.outputEnergyContainer.getEnergyStored();
    }

    @Override
    public long getEnergyCapacity() {
        return this.outputEnergyContainer.getEnergyCapacity();
    }
}
