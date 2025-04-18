package com.johny.tj.capability;

import java.util.function.IntSupplier;

public interface LinkPosInterDim<T> extends LinkPos<T> {

    int dimensionID();

    void setDimension(IntSupplier dimensionID, int index);

    int getDimension(int index);
}
