package com.sonyericsson.chkbugreport.plugins.logs.event;

public class SampleData {

    long ts;
    int pid;
    String name;
    int duration;
    int perc;

    public SampleData(long ts, int pid, String name, int duration, int perc) {
        this.ts = ts;
        this.pid = pid;
        this.name = name;
        this.duration = duration;
        this.perc = perc;
    }

}