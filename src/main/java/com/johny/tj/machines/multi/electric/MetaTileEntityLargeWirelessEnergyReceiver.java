package com.johny.tj.machines.multi.electric;

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

    private IEnergyContainer energyOutputContainer;

    public MetaTileEntityLargeWirelessEnergyReceiver(ResourceLocation metaTileEntityId, TransferType transferType) {
        super(metaTileEntityId, transferType);
        this.transferType = transferType;
        reinitializeStructurePattern();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityLargeWirelessEnergyReceiver(metaTileEntityId, transferType);
    }

    @Override
    protected boolean hasEnoughEnergy(long amount) {
        return true;
    }

    @Override
    protected void transferRF(int energyToAdd, IEnergyStorage RFContainer) {
        if (RFContainer != null && energyOutputContainer.getEnergyCanBeInserted() >= energyToAdd) {
            int energyExtracted = RFContainer.extractEnergy(Math.min(Integer.MAX_VALUE, energyToAdd * 4), false);
            energyOutputContainer.addEnergy(energyExtracted / 4);
        }
    }

    @Override
    protected void transferEU(long energyToAdd, IEnergyContainer EUContainer) {
        if (EUContainer != null && energyOutputContainer.getEnergyCanBeInserted() >= energyToAdd) {
            long energyExtracted = EUContainer.removeEnergy(energyToAdd);
            energyOutputContainer.addEnergy(Math.abs(energyExtracted));
        }
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        energyOutputContainer = new EnergyContainerList(getAbilities(MultiblockAbility.OUTPUT_ENERGY));
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.RED_STEEL_CASING;
    }
}
