package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.plugins.logs.LogLine;

import java.util.Vector;

public class BatteryLevels {

    private static final long HOUR = 60 * 60 * 1000;

    public static final String INFO_ID = "eventlog_battery_levels";

    private long mFirstTs;
    private long mLastTs;
    private long mLastVoltTS = -1;
    private int mLastVolt = -1;
    private Vector<BatteryLevel> mData = new Vector<BatteryLevel>();
    private long mMaxMsPerMV = 0;
    private long mMinMsPerMV = 0;
    private long mMaxMVPerHour = 0;
    private long mMinMVPerHour = 0;
    private int mMaxVolt;
    private int mMinVolt;
    private int mMaxTemp;
    private int mMinTemp;
    private boolean mMinMaxSet = false;

    public BatteryLevels(EventLogPlugin plugin) {
    }

    public void addData(LogLine sl) {
        try {
            int level = Integer.parseInt(sl.fields[0]);
            int volt = Integer.parseInt(sl.fields[1]);
            int temp = Integer.parseInt(sl.fields[2]);
            long ts = sl.ts;
            long msPerMV = 0;
            long mVPerHour = 0;
            if (mLastVolt != -1) {
                if (mLastVolt == volt || mLastVoltTS == ts) {
                    return;
                }
                msPerMV = (ts - mLastVoltTS) / (mLastVolt - volt);
                mVPerHour = (mLastVolt - volt) * HOUR / (ts - mLastVoltTS);
                if (mMinMaxSet) {
                    mMinMsPerMV = Math.min(mMinMsPerMV, msPerMV);
                    mMaxMsPerMV = Math.max(mMaxMsPerMV, msPerMV);
                    mMinMVPerHour = Math.min(mMinMVPerHour, mVPerHour);
                    mMaxMVPerHour = Math.max(mMaxMVPerHour, mVPerHour);
                    mMinVolt = Math.min(mMinVolt, volt);
                    mMaxVolt = Math.max(mMaxVolt, volt);
                    mMinTemp = Math.min(mMinTemp, temp);
                    mMaxTemp = Math.max(mMaxTemp, temp);
                } else {
                    mMinMsPerMV = msPerMV;
                    mMaxMsPerMV = msPerMV;
                    mMinMVPerHour = mVPerHour;
                    mMaxMVPerHour = mVPerHour;
                    mMinVolt = mMaxVolt = volt;
                    mMinTemp = mMaxTemp = temp;
                    mMinMaxSet = true;
                }
            }
            mLastVolt = volt;
            mLastVoltTS = ts;
            mData.add(new BatteryLevel(level, volt, temp, ts, msPerMV, mVPerHour));
        } catch (NumberFormatException e) {
            // Something went wrong
            // TODO: log this properly! For now just print the stacktrace
            e.printStackTrace();
        }
    }

    public int getCount() {
        return mData.size();
    }

    public BatteryLevel get(int idx) {
        return mData.get(idx);
    }

    public long getMaxMsPerMV() {
        return mMaxMsPerMV;
    }

    public long getMinMsPerMV() {
        return mMinMsPerMV;
    }

    public long getMaxMVPerHour() {
        return mMaxMVPerHour;
    }

    public long getMinMVPerHour() {
        return mMinMVPerHour;
    }

    public int getMaxVolt() {
        return mMaxVolt;
    }

    public int getMinVolt() {
        return mMinVolt;
    }

    public int getMaxTemp() {
        return mMaxTemp;
    }

    public int getMinTemp() {
        return mMinTemp;
    }

    public long getFirstTs() {
        return mFirstTs;
    }

    public long getLastTs() {
        return mLastTs;
    }

    /* package */ void setRange(long firstTs, long lastTs) {
        mFirstTs = firstTs;
        mLastTs = lastTs;

    }

}
