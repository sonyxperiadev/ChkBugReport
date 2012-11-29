package com.sonyericsson.chkbugreport.plugins.logs;

public class GCRecord {

    public long ts;
    public int pid;
    public int memFreeAlloc;
    public int memExtAlloc;
    public int memFreeSize;
    public int memExtSize;

    public GCRecord(long ts, int pid, int memFreeAlloc, int memFreeSize, int memExtAlloc, int memExtSize) {
        this.ts = ts;
        this.pid = pid;
        this.memFreeAlloc = memFreeAlloc;
        this.memExtAlloc = memExtAlloc;
        this.memFreeSize = memFreeSize;
        this.memExtSize = memExtSize;
    }
}
