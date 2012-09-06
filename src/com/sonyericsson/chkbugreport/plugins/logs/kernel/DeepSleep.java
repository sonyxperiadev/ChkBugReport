package com.sonyericsson.chkbugreport.plugins.logs.kernel;

public class DeepSleep {

    private long mLastRealTs;
    private long mLastTs;
    private long mRealTs;
    private long mTs;

    public DeepSleep(long lastRealTs, long lastTs, long realTs, long ts) {
        mLastRealTs = lastRealTs;
        mLastTs = lastTs;
        mRealTs = realTs;
        mTs = ts;
    }

    public long getLastRealTs() {
        return mLastRealTs;
    }

    public long getLastTs() {
        return mLastTs;
    }

    public long getRealTs() {
        return mRealTs;
    }

    public long getTs() {
        return mTs;
    }

}
