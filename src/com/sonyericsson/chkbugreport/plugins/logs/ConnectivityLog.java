package com.sonyericsson.chkbugreport.plugins.logs;

public class ConnectivityLog {

    private String mInterface;
    private String mState;
    private long mTs;

    public ConnectivityLog(long ts, String interf, String state) {
        mTs = ts;
        mInterface = interf;
        mState = state;
    }

    public String getInterface() {
        return mInterface;
    }

    public String getState() {
        return mState;
    }

    public long getTs() {
        return mTs;
    }
}
