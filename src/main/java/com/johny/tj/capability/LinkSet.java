package com.johny.tj.capability;

import gregtech.api.metatileentity.MetaTileEntity;

import java.util.function.Supplier;

public interface LinkSet {

    void setLink(Supplier<MetaTileEntity> entitySupplier);

    MetaTileEntity getLink();

}
