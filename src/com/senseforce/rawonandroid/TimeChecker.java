package com.senseforce.rawonandroid;

import android.util.Log;

public class TimeChecker {
    private static TimeChecker ourInstance = new TimeChecker();

    private long timeBegin = 0l;
    private long timeEnd = 0l;

    public static TimeChecker getInstance() {
        return ourInstance;
    }

    public static TimeChecker newInstance() {
        return new TimeChecker();
    }

    private  TimeChecker() {

    }

    public void prepare() {
        timeBegin = System.nanoTime();
        timeEnd = timeBegin;
    }

    public long check(String tag) {
        timeEnd = System.nanoTime();
        long interval = (timeEnd - timeBegin) / 1000000;
        Log.d("TimeChecker", tag + " 耗时 " + interval + " 毫秒");
        timeBegin = System.nanoTime();
        return interval;
    }
}
