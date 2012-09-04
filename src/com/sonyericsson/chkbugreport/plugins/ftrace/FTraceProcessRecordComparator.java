package com.sonyericsson.chkbugreport.plugins.ftrace;

import java.util.Comparator;

public class FTraceProcessRecordComparator implements Comparator<FTraceProcessRecord> {

    @Override
    public int compare(FTraceProcessRecord o1, FTraceProcessRecord o2) {
        if (o1.runTime < o2.runTime) return 1;
        if (o1.runTime > o2.runTime) return -1;
        if (o1.waitTime < o2.waitTime) return 1;
        if (o1.waitTime > o2.waitTime) return -1;
        if (o1.diskTime < o2.diskTime) return 1;
        if (o1.diskTime > o2.diskTime) return -1;
        return 0;
    }

}

