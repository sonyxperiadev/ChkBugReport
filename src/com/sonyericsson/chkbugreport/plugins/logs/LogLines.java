package com.sonyericsson.chkbugreport.plugins.logs;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

@SuppressWarnings("serial")
public class LogLines extends Vector<LogLine> implements Comparator<LogLine> {

    public void sort() {
        Collections.sort(this, this);
    }

    @Override
    public int compare(LogLine l0, LogLine l1) {
        if (l0.ts < l1.ts) return -1;
        if (l0.ts > l1.ts) return +1;
        return 0;
    }

}
