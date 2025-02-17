package com.johny.tj.capability;

/**
 * For Machines which have multiple workable instances
 */
public interface IMultipleWorkable extends IMultiControllable {

    /*
     * @return current progress of this instance
     */
    int getProgress(int i);

    /*
     * @return gets the amount to complete operation of this instance
     */
    int getMaxProgress(int i);

    /*
     * @return check is this instance active
     */
    boolean isInstanceActive(int i);
}
