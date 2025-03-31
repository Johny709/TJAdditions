package com.johny.tj.capability;

import java.util.function.IntSupplier;

public interface LinkEntityInterDim<T> extends LinkEntity<T> {

    int dimensionID();

    void setDimension(IntSupplier dimensionID, int index);

    int getDimension(int index);
}
