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
import com.sonyericsson.chkbugreport.Section;

/* package */ class OldParser extends Parser {

    private static final String NO_PROC_NAME = "<...>";

    private BugReportModule mBr;

    public OldParser(BugReportModule br, FTracePlugin plugin) {
        mBr = br;
    }

    public FTraceData parse(Section section) {
        int cnt = section.getLineCount();
        String buff = null;
        // Check that the correct tracer is selected
        buff = section.getLine(0);
        if (!buff.equals("# tracer: sched_switch")) {
            mBr.printErr(3, FTracePlugin.TAG + "The context switch tracer is not selected!");
            return null;
        }

        // We must have some data in the buffer
        if (cnt <= 4) {
            mBr.printErr(3, FTracePlugin.TAG + "The trace buffer is empty!");
            return null;
        }

        FTraceData ret = new FTraceData(mBr);
        int adjNoIdle = 1;
        int nrRunWait = 0;
        for (int i = 0; i < cnt; i++) {
            buff = section.getLine(i);
            long timeUS;

            if (buff.length() == 0) continue; // skip comments
            if (buff.charAt(0) == '#') continue; // skip comments
            if (buff.charAt(0) < ' ') continue; // skip empty lines

            // Parse the data
            int p = 0, s = 0;

            // Parse SRC_PROC
            while (buff.charAt(p) == ' ') p++;
            s = 16;
            String srcProc = buff.substring(p, s);
            p = ++s;

            // Parse SRC PID
            while (buff.charAt(s) != ' ') s++;
            int srcPid = Integer.parseInt(buff.substring(p, s));
            p = ++s;

            // Skip CPU (not used)
            while (buff.charAt(s) == ' ') s++;
            while (buff.charAt(s) != ' ') s++;
            while (buff.charAt(s) == ' ') s++;

            // Parse timestamp
            double timestamp;
            p = s;
            while (buff.charAt(s) != ':') s++;
            timestamp = Double.parseDouble(buff.substring(p, s));
            s++;
            timeUS = (long)(timestamp * 1000 * 1000);

            // Skip SRC PID (we already now)
            while (buff.charAt(s) != ':') s++;
            s++;

            // Skip SRC PRIO (not used)
            while (buff.charAt(s) != ':') s++;
            s++;

            // Read SRC STATE
            char srcState = buff.charAt(s);
            s++;

            // Parse event: wakeup or switch
            int event = Const.UNKNOWN;
            s++;
            if (buff.substring(s, s + 3).equals("  +")) {
                event = Const.WAKEUP;
            } else if (buff.substring(s, s + 3).equals("==>")) {
                event = Const.SWITCH;
            }
            s += 3;

            // Skip CPU (not used)
            while (buff.charAt(s) == ' ') s++;
            while (buff.charAt(s) != ' ') s++;
            while (buff.charAt(s) == ' ') s++;

            // Parse DST PID
            p = s;
            while (buff.charAt(s) != ':') s++;
            int dstPid = Integer.parseInt(buff.substring(p, s));
            s++;

            // Skip DST PRIO (not used)
            while (buff.charAt(s) != ':') s++;
            s++;

            // Read DST STATE
            char dstState = buff.charAt(s);
            s++;

            // Read DST PROC
            p = ++s;
            String dstProc = buff.substring(p);

            if (!srcProc.equals(NO_PROC_NAME)) {
                ret.setProcName(srcPid, srcProc, mBr);
            }
            if (!dstProc.equals(NO_PROC_NAME)) {
                ret.setProcName(dstPid, dstProc, mBr);
            }

            // System.out.println(String.format("src_proc='%s' src_pid='%d' ts='%d' src_state='%c' event='%d' dst_pid='%d' dst_state='%c' dst_proc='%s'", src_proc, src_pid, time_us, src_state, event, dst_pid, dst_state, dst_proc));

            // Calculate the number of processes running
            int newNr = nrRunWait;
            FTraceProcessRecord proc = ret.getProc(srcPid, mBr);
            if (event == Const.SWITCH) {
                int prevState = Const.calcPrevState(srcState);
                newNr += ret.updateNr(proc, prevState, false, srcState, true);
            }
            int nextState = event == Const.WAKEUP ? Const.STATE_WAIT : Const.STATE_RUN;
            proc = ret.getProc(dstPid, mBr);
            newNr += ret.updateNr(proc, nextState, true, dstState, true);
            if (newNr <= 0) {
                // This shouldn't happen!
                // incNrRunWait(1 - newNr); // This could be used as a workaround (but again, this should never happen)
                mBr.printErr(4, FTracePlugin.TAG + "Needs adjusting! newNr=" + newNr + " @" + timeUS);
                newNr = 1;
            }
            nrRunWait = newNr;

            TraceRecord data = new TraceRecord(timeUS, srcPid, dstPid, srcState, dstState, event);
            data.nrRunWait = nrRunWait - 1; // -1 due to not counting the idle process (which is either running or waiting)
            ret.append(data);
            ret.getProc(srcPid, mBr).used++;
            ret.getProc(dstPid, mBr).used++;

            if (srcPid == 0 || dstPid == 0) {
                adjNoIdle = 0; // No need to adjust due to idle not "running"
            }
        }
        if (adjNoIdle == 1) {
            ret.incNrRunWait(adjNoIdle);
        }

        return ret;
    }

}
