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
