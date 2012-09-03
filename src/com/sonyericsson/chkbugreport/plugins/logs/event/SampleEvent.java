package com.sonyericsson.chkbugreport.plugins.logs.event;

public class SampleEvent {
    boolean start;
    long ts;
    int id;
    public SampleEvent(boolean start, long ts, int id) {
        this.start = start;
        this.ts = ts;
        this.id = id;
    }
}
