package com.my.ddfiledelete;

public class TimesCounter {
    private int mcount;
    private int maxCount;
    private Runnable runnable;

    public TimesCounter(int count) {
        this(count, null);
    }

    public TimesCounter(int count, Runnable timesAction) {
        maxCount = count;
        mcount = 0;
        this.runnable = timesAction;
    }

    public boolean check() {
        mcount++;
        if (mcount >= maxCount) {
            if (runnable != null) {
                runnable.run();
            }
            return true;
        }
        return false;
    }
}
