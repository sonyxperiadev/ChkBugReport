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
