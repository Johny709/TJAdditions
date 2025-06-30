package tj.capability;

/**
 * For Machines which have multiple workable instances
 */
public interface IMultipleWorkable extends IMultiControllable {

    /*
     * @return Recipe EU/t of this instance
     */
    int getRecipeEUt(int i);

    /*
     * @return Parallels performed of this instance
     */
    int getParallel(int i);

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

    /*
     * @return total amount of instances in this recipe workable
     */
    int getSize();
}
