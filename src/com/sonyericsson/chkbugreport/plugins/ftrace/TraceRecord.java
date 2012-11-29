package com.sonyericsson.chkbugreport.plugins.ftrace;

/* package */ class TraceRecord {
    long time;
    int prevPid, nextPid;
    char prevState, nextState;
    int event;
    int nrRunWait;
    TraceRecord next;

    TraceRecord(long time, int prev, int next, char prevState, char nextState, int event) {
        this.time = time;
        this.prevPid = prev;
        this.nextPid = next;
        this.prevState = prevState;
        this.nextState = nextState;
        this.event = event;
        this.next = null;
        this.nrRunWait = 0;
    }
}
