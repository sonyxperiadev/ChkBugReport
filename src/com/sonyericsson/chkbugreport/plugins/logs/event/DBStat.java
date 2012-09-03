package com.sonyericsson.chkbugreport.plugins.logs.event;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * Direct database access statistics
 */
public class DBStat {
    public String db;
    public int totalTime;
    public int maxTime;
    public int count;
    public Vector<Integer> pids = new Vector<Integer>();
    public Vector<SampleData> data = new Vector<SampleData>();

    public void finish() {
        Collections.sort(data, new Comparator<SampleData>() {
            @Override
            public int compare(SampleData o1, SampleData o2) {
                if (o1.ts < o2.ts) return -1;
                if (o1.ts > o2.ts) return +1;
                return 0;
            }
        });
    }
}
