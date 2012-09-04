package com.sonyericsson.chkbugreport.plugins.ftrace;

public class Const {

    // Event types
    public static final int UNKNOWN = 0;
    public static final int WAKEUP = 1;
    public static final int SWITCH = 2;

    // Process states
    public static final int STATE_SLEEP = 0;
    public static final int STATE_DISK = 1;
    public static final int STATE_WAIT = 2;
    public static final int STATE_RUN = 3;

    public static int calcPrevState(char srcState) {
        if (srcState == 'R') return STATE_WAIT;
        if (srcState == 'D') return STATE_DISK;
        return STATE_SLEEP;
    }



}
