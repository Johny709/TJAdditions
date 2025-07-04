package tj.capability.impl;

public class OverclockManager<T> {

    private int EUt;
    private int duration;
    private int parallel;
    private T recipeProperty;

    public void setEUtAndDuration(int EUt, int duration) {
        this.EUt = EUt;
        this.duration = duration;
    }

    public void setRecipeProperty(T recipeProperty) {
        this.recipeProperty = recipeProperty;
    }

    public void setEUt(int EUt) {
        this.EUt = EUt;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }

    public T getRecipeProperty() {
        return recipeProperty;
    }

    public int getEUt() {
        return EUt;
    }

    public int getDuration() {
        return duration;
    }

    public int getParallel() {
        return parallel;
    }
}
