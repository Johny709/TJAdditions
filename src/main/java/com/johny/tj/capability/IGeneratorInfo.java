package com.johny.tj.capability;

public interface IGeneratorInfo {

    long getProduction();

    /**
     * First index is prefix, otherwise is suffixed
     */
    String[] productionInfo();

}
