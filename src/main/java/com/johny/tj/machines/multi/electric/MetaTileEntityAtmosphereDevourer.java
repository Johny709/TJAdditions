package com.johny.tj.machines.multi.electric;

import com.johny.tj.builder.multicontrollers.TJMultiblockDisplayBase;
import gregicadditions.client.ClientHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.multiblock.BlockPattern;
import gregtech.api.render.ICubeRenderer;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityRotorHolder;
import net.minecraft.util.ResourceLocation;

//TODO WIP
public class MetaTileEntityAtmosphereDevourer extends TJMultiblockDisplayBase {

    private MultiblockAbility<MetaTileEntityRotorHolder> ABILITY_ROTOR_HOLDER;
    private static final int CYCLE_LENGTH = 230;
    private static final int BASE_ROTOR_DAMAGE = 20;
    private static final long BASE_AIR_OUTPUT = 512;
    private int rotorCycleLength;

    public MetaTileEntityAtmosphereDevourer(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        ABILITY_ROTOR_HOLDER = new MultiblockAbility<>();
    }

    public MetaTileEntityRotorHolder getRotorHolder() {
        return this.getAbilities(ABILITY_ROTOR_HOLDER).get(0);
    }

    @Override
    protected void updateFormedValid() {
        if (getRotorHolder().isHasRotor()) {
            if (++rotorCycleLength >= CYCLE_LENGTH) {
                int damageToBeApplied = (int) Math.round(BASE_ROTOR_DAMAGE * getRotorHolder().getRelativeRotorSpeed()) + 1;
                if (getRotorHolder().applyDamageToRotor(damageToBeApplied, false)) {
                    rotorCycleLength = 0;
                }
            }
        }
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityAtmosphereDevourer(metaTileEntityId);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return null;
    }

    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
        return ClientHandler.INCOLOY_MA956_CASING;
    }

}
