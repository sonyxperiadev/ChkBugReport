/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport.plugins.logs.event;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * Direct database access statistics
 */
/* package */ class DBStat {
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
