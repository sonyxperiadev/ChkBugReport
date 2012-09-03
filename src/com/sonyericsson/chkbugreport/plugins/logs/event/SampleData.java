package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.plugins.logs.LogLine;

public class SampleData {

    long ts;
    int pid;
    String name;
    int duration;
    int perc;
    LogLine logLine;

    public SampleData(long ts, int pid, String name, int duration, int perc, LogLine sl) {
        this.ts = ts;
        this.pid = pid;
        this.name = name;
        this.duration = duration;
        this.perc = perc;
        this.logLine = sl;
    }

}