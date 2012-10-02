package com.sonyericsson.chkbugreport.plugins.logs.event;

public class BatteryLevel {

    private int mLevel;
    private int mVolt;
    private int mTemp;
    private long mTs;
    private long mMsPerMV;
    private long mMVPerHour;

    public BatteryLevel(int level, int volt, int temp, long ts, long msPerMV, long mVPerHour) {
        mLevel = level;
        mVolt = volt;
        mTemp = temp;
        mTs = ts;
        mMsPerMV = msPerMV;
        mMVPerHour = mVPerHour;
    }

    public int getLevel() {
        return mLevel;
    }

    public int getVolt() {
        return mVolt;
    }

    public int getTemp() {
        return mTemp;
    }

    public long getTs() {
        return mTs;
    }

    public long getMsPerMV() {
        return mMsPerMV;
    }

    public long getMVPerHour() {
        return mMVPerHour;
    }

}
