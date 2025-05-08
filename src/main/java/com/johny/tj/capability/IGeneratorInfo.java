package com.johny.tj.capability;

public interface IGeneratorInfo {

    long getProduction();

    /**
     * Everything is prefix until "suffix" String is passed in and will be part of suffix afterward.
     */
    String[] productionInfo();

}
