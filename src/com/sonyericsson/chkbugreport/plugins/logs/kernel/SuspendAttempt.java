package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.Lines;

import java.util.Vector;

/* package */ class SuspendAttempt {

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_FAILED = 0;
    public static final int STATE_SUCCEEDED = 1;

    public int state = STATE_UNKNOWN;
    public Vector<String> wakelocks = new Vector<String>();
    public Lines log = new Lines(null);

    public void addWakelock(String name) {
        wakelocks.add(name);
    }

}
