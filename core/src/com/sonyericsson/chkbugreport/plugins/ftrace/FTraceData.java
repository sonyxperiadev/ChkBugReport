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
package com.sonyericsson.chkbugreport.plugins.ftrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.ps.PSRecord;

import java.util.Collections;
import java.util.Vector;

/* package */ class FTraceData {

    private static final int MAX_PID = 65535;

    private FTraceProcessRecord mPids[] = new FTraceProcessRecord[MAX_PID];

    private int mLastProcId = 0;

    private TraceRecord mHead = null;
    private TraceRecord mTail = null;

    public FTraceData(BugReportModule br) {
        getProc(0, br).name = "SLEEP";
        mHead = mTail = new TraceRecord(0, 0, 0, 'S', 'S', 0);
    }

    public void setProcName(int pid, String s, BugReportModule br) {
        FTraceProcessRecord pr = getProc(pid, br);
        if (pr.name == null) {
            pr.name = "" + pid + "-" + s;
        }
    }

    public FTraceProcessRecord getProc(int pid) {
        return mPids[pid];
    }

    public FTraceProcessRecord getProc(int pid, BugReportModule br) {
        if (mPids[pid] == null) {
            String name = findNameOf(pid, br);
            mPids[pid] = new FTraceProcessRecord(pid, name);
        }
        return mPids[pid];
    }

    private String findNameOf(int pid, BugReportModule br) {
        String name = null;
        PSRecord psr = br.getPSRecord(pid);
        int ppid = (psr == null) ? -1 : psr.getParentPid();
        String base = (psr == null) ? null : psr.getName();
        ProcessRecord pr = br.getProcessRecord(pid, false, false);
        if (pr != null) {
            base = pr.getProcName();
        }
        if (base != null) {
            name = makeName(base, pid, ppid, br);
        }
        return name;
    }

    private String makeName(String base, int pid, int ppid, BugReportModule br) {
        if (ppid <= 1) {
            return "" + pid + "-" + base;
        } else {
            String ret = getProcName(ppid, br);
            int idx = ret.indexOf('\\');
            if (idx >= 0) {
                ret = ret.substring(idx + 1);
            }
            return ret + "\\" + pid + "-" + base;
        }
    }

    private String getProcName(int pid, BugReportModule br) {
        return getProc(pid, br).getName();
    }

    public String genId() {
        StringBuffer sb = new StringBuffer();
        int tmp = mLastProcId;
        do {
            sb.append((char)('A' + (tmp % 26)));
            tmp /= 26;
        } while (tmp > 0);

        mLastProcId++;
        return sb.toString();
    }

    public int updateNr(FTraceProcessRecord proc, int newState, boolean newPid, char newCState, boolean guessInitState) {
        int ret = 0;
        int oldState = proc.state;
        if (oldState != newState) {
            if (oldState == Const.STATE_SLEEP || oldState == Const.STATE_DISK) ret = +1;
            if (newState == Const.STATE_SLEEP || newState == Const.STATE_DISK) ret = -1;
        }
        if (guessInitState && !proc.initStateSet) {
            if (newPid) {
                if (oldState == Const.STATE_SLEEP && newState == Const.STATE_RUN) {
                    // if no wakeup, then it was already waiting
                    proc.initState = Const.STATE_WAIT;
                    incNrRunWait(ret); // Need to fix history as well
                } else if (oldState == Const.STATE_SLEEP && newState == Const.STATE_WAIT) {
                    if (newCState == 'D') {
                        proc.initState = Const.STATE_DISK;
                    }
                }
            } else {
                if (ret == +1) {
                    proc.initState = Const.STATE_RUN; // The previous state couldn't be sleep
                    incNrRunWait(ret); // Need to fix history as well
                }
            }
        }
        proc.initStateSet = true; // This was the only and last chance to guess the init state
        proc.state = newState;
        return ret;
    }

    public void incNrRunWait(int delta) {
        TraceRecord cur = mHead.next;
        while (cur != null) {
            cur.nrRunWait += delta;
            cur = cur.next;
        }
    }

    public void append(TraceRecord data) {
        mTail.next = data;
        mTail = data;
    }

    public boolean isEmpty() {
        return mTail == null || mHead == null || mHead.next == null;
    }

    public long getDuration() {
        return (mTail.time - mHead.next.time);
    }

    public TraceRecord getFirstTraceRecord() {
        return mHead.next;
    }

    public Vector<FTraceProcessRecord> sort() {
        // Collect process statistics
        Vector<FTraceProcessRecord> list = new Vector<FTraceProcessRecord>();
        for (int i = 0; i < 65535; i++) {
            if (mPids[i] != null && mPids[i].used > 0) {
                list.add(mPids[i]);
            }
        }
        Collections.sort(list, new FTraceProcessRecordComparator());
        return list;
    }

}
