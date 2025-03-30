package com.johny.tj.capability;

import java.util.function.IntSupplier;

public interface LinkPosInterDim extends LinkPos {

    int dimensionID();

    void setDimension(IntSupplier dimensionID, int index);

    int getDimension(int index);
}
