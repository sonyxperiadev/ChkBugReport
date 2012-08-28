package com.sonyericsson.chkbugreport.ps;

import java.util.HashMap;
import java.util.Iterator;

public class PSRecords implements Iterable<PSRecord> {

    private HashMap<Integer, PSRecord> mPSRecords = new HashMap<Integer, PSRecord>();
    private PSRecord mPSTree = new PSRecord(0, 0, 0, 0, null);

    public boolean isEmpty() {
        return mPSRecords.size() == 0;
    }

    public PSRecord getPSRecord(int pid) {
        return mPSRecords.get(pid);
    }

    public PSRecord getPSTree() {
        return mPSTree;
    }

    public void put(int pid, PSRecord psRecord) {
        mPSRecords.put(pid, psRecord);
    }

    @Override
    public Iterator<PSRecord> iterator() {
        return mPSRecords.values().iterator();
    }


}
