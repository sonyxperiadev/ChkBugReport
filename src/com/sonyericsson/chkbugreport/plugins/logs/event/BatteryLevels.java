package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.plugins.logs.LogLine;

import java.util.Vector;

public class BatteryLevels {

    private static final long HOUR = 60 * 60 * 1000;

    private long mLastTs = -1;
    private int mLastLevel = -1;
    private int mPrevLevel = -1;
    private Vector<BatteryLevel> mData = new Vector<BatteryLevel>();
    private long mMaxMsPerPerc = 0;
    private long mMinMsPerPerc = 0;
    private long mMaxPercPerHour = 0;
    private long mMinPercPerHour = 0;
    private boolean mMinMaxSet = false;

    public BatteryLevels(EventLogPlugin plugin) {
    }

    public void addData(LogLine sl) {
        int level = Integer.parseInt(sl.fields[0]);
        long ts = sl.ts;
        long msPerPerc = 0;
        long percPerHour = 0;
        if (mLastLevel != -1) {
            if (mLastLevel == level) {
                if (mPrevLevel != -1) {
                    msPerPerc = (ts - mLastTs) / (mPrevLevel - level);
                }
            } else {
                msPerPerc = (ts - mLastTs) / (mLastLevel - level);
            }
            percPerHour = (mLastLevel - level) * HOUR / (ts - mLastTs);
            if (mMinMaxSet) {
                mMinMsPerPerc = Math.min(mMinMsPerPerc, msPerPerc);
                mMaxMsPerPerc = Math.max(mMaxMsPerPerc, msPerPerc);
                mMinPercPerHour = Math.min(mMinPercPerHour, percPerHour);
                mMaxPercPerHour = Math.max(mMaxPercPerHour, percPerHour);
            } else {
                mMinMsPerPerc = msPerPerc;
                mMaxMsPerPerc = msPerPerc;
                mMinPercPerHour = percPerHour;
                mMaxPercPerHour = percPerHour;
                mMinMaxSet = true;
            }
        }
        if (mLastLevel != level) {
            mPrevLevel = mLastLevel;
            mLastLevel = level;
            mLastTs = ts;
        }
        mData.add(new BatteryLevel(level, ts, msPerPerc, percPerHour));
    }

    public int getCount() {
        return mData.size();
    }

    public BatteryLevel get(int idx) {
        return mData.get(idx);
    }

    public long getMaxMsPerPerc() {
        return mMaxMsPerPerc;
    }

    public long getMinMsPerPerc() {
        return mMinMsPerPerc;
    }

    public long getMaxPercPerHour() {
        return mMaxPercPerHour;
    }

    public long getMinPercPerHour() {
        return mMinPercPerHour;
    }

}
