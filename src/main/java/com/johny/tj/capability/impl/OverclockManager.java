package com.johny.tj.capability.impl;

public class OverclockManager {

    private int EUt;
    private int duration;
    private int parallel;

    public void setEUt(int EUt) {
        this.EUt = EUt;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
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
