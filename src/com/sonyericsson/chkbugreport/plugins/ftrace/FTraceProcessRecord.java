package com.sonyericsson.chkbugreport.plugins.ftrace;

import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.util.Util;

public class FTraceProcessRecord {
    int pid;
    String name;
    int used;
    String id;
    int state = Const.STATE_SLEEP;
    long lastTime;
    long runTime;
    long waitTime;
    int waitTimeCnt;
    int waitTimeMax;
    long diskTime;
    int diskTimeCnt;
    int diskTimeMax;
    int initState = Const.STATE_SLEEP;
    boolean initStateSet = false;
    ProcessRecord procRec;

    public FTraceProcessRecord(int pid, String name) {
        this.pid = pid;
        this.name = name;
    }

    public String getName() {
        if (name != null) return name;
        return Integer.toString(pid);
    }

    public String getVCDName() {
        if (name != null) {
            return Util.fixVCDName(name);
        }
        return Integer.toString(pid);
    }

}

