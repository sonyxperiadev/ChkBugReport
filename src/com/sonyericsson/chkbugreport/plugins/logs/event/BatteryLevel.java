package com.sonyericsson.chkbugreport.plugins.logs.event;

public class BatteryLevel {

    private int mLevel;
    private long mTs;
    private long mMsPerPerc;
    private long mPercPerHour;

    public BatteryLevel(int level, long ts, long msPerPerc, long percPerHour) {
        mLevel = level;
        mTs = ts;
        mMsPerPerc = msPerPerc;
        mPercPerHour = percPerHour;
    }

    public int getLevel() {
        return mLevel;
    }

    public long getTs() {
        return mTs;
    }

    public long getMsPerPerc() {
        return mMsPerPerc;
    }

    public long getPercPerHour() {
        return mPercPerHour;
    }



}
