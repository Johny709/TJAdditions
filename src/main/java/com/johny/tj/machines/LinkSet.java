package com.johny.tj.machines;

import gregtech.api.metatileentity.MetaTileEntity;

import java.util.function.Supplier;

public interface LinkSet {

    void setLink(Supplier<MetaTileEntity> entitySupplier);

    MetaTileEntity getLink();

}
