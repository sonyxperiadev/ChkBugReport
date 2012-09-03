package com.sonyericsson.chkbugreport.plugins.logs.event;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class SampleDatas {

    private HashMap<String, Vector<SampleData>> mSDs = new HashMap<String, Vector<SampleData>>();

    public void addData(String eventType, SampleData sd) {
        Vector<SampleData> sds = mSDs.get(eventType);
        if (sds == null) {
            sds = new Vector<SampleData>();
            mSDs.put(eventType, sds);
        }
        sds.add(sd);
    }

    public Vector<SampleData> getSamplesByType(String eventType) {
        return mSDs.get(eventType);
    }

    public Set<Entry<String,Vector<SampleData>>> entrySet() {
        return mSDs.entrySet();
    }

}
