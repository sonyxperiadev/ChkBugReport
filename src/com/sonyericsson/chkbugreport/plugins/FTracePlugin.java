/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.PSRecord;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Vector;

import javax.imageio.ImageIO;

public class FTracePlugin extends Plugin {

    private static final String TAG = "[FTracePlugin]";

    private static final int UNKNOWN = 0;
    private static final int WAKEUP = 1;
    private static final int SWITCH = 2;

    private static final int STATE_SLEEP = 0;
    private static final int STATE_DISK = 1;
    private static final int STATE_WAIT = 2;
    private static final int STATE_RUN = 3;

    private static final char STATE_SIGNALS[] = {'0', 'Z', 'W', 'X' };
    private static final char STATE_SIGNALS_IDLE[] = {'0', '0', '0', 'X' };

    private static final int TRACE_W = 600;
    private static final int TRACE_H = 24;

    private static final int MAX_PID = 65535;

    private static final String NO_PROC_NAME = "<...>";

    private FTraceProcessRecord mPids[] = new FTraceProcessRecord[MAX_PID];

    private int mLastProcId = 0;

    private TraceRecord mHead = null;
    private TraceRecord mTail = null;

    private String mTimeBarName;

    static class TraceRecord {
        long time;
        int prevPid, nextPid;
        char prevState, nextState;
        int event;
        int nrRunWait;
        TraceRecord next;

        TraceRecord(long time, int prev, int next, char prevState, char nextState, int event) {
            this.time = time;
            this.prevPid = prev;
            this.nextPid = next;
            this.prevState = prevState;
            this.nextState = nextState;
            this.event = event;
            this.next = null;
            this.nrRunWait = 0;
        }
    }

    static class FTraceProcessRecord {
        int pid;
        String name;
        int used;
        String id;
        int state = STATE_SLEEP;
        long lastTime;
        long runTime;
        long waitTime;
        int waitTimeCnt;
        int waitTimeMax;
        long diskTime;
        int diskTimeCnt;
        int diskTimeMax;
        int initState = STATE_SLEEP;
        boolean initStateSet = false;
        ProcessRecord procRec;

        public FTraceProcessRecord(int pid, String name) {
            this.pid = pid;
            this.name = name;
        }

        public String getName() {
            if (name != null) return name;
            return Integer.toString(pid);
        }

        public String getVCDName() {
            if (name != null) {
                return Util.fixVCDName(name);
            }
            return Integer.toString(pid);
        }

    }

    static class FTraceProcessRecordComparator implements Comparator<FTraceProcessRecord> {
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

    private FTraceProcessRecord getProc(int pid, BugReport br) {
        if (mPids[pid] == null) {
            String name = findNameOf(pid, br);
            mPids[pid] = new FTraceProcessRecord(pid, name);
        }
        return mPids[pid];
    }

    private String findNameOf(int pid, BugReport br) {
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

    private String getProcName(int pid, BugReport br) {
        return getProc(pid, br).getName();
    }

    private String makeName(String base, int pid, int ppid, BugReport br) {
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

    private void setProcName(int pid, String s, BugReport br) {
        FTraceProcessRecord pr = getProc(pid, br);
        if (pr.name == null) {
            pr.name = "" + pid + "-" + s;
        }
    }

    private String genId() {
        StringBuffer sb = new StringBuffer();
        int tmp = mLastProcId;
        do {
            sb.append((char)('A' + (tmp % 26)));
            tmp /= 26;
        } while (tmp > 0);

        mLastProcId++;
        return sb.toString();
    }

    @Override
    public int getPrio() {
        return 50;
    }

    @Override
    public void load(Report br) {
        // NOP
    }

    @Override
    public void generate(Report rep) {
        BugReport br = (BugReport)rep;
        getProc(0, br).name = "SLEEP";
        mHead = mTail = new TraceRecord(0, 0, 0, 'S', 'S', 0);

        // Locate the ftrace section
        Section ftrace = br.findSection(Section.FTRACE);
        if (ftrace == null) {
            br.printErr(TAG + "Cannot find section: " + Section.FTRACE);
            return;
        }

        int nrRunWait = 0;
        int cnt = ftrace.getLineCount();
        String buff = null;
        // Check that the correct tracer is selected
        buff = ftrace.getLine(0);
        if (!buff.equals("# tracer: sched_switch")) {
            br.printErr(TAG + "The context switch tracer is not selected!");
            return;
        }

        // We must have some data in the buffer
        if (cnt <= 4) {
            br.printErr(TAG + "The trace buffer is empty!");
            return;
        }

        // Skip the first 3 lines
        int adjNoIdle = 1;
        for (int i = 3; i < cnt; i++) {
            buff = ftrace.getLine(i);
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
            int event = UNKNOWN;
            s++;
            if (buff.substring(s, s + 3).equals("  +")) {
                event = WAKEUP;
            } else if (buff.substring(s, s + 3).equals("==>")) {
                event = SWITCH;
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
                setProcName(srcPid, srcProc, br);
            }
            if (!dstProc.equals(NO_PROC_NAME)) {
                setProcName(dstPid, dstProc, br);
            }

            // System.out.println(String.format("src_proc='%s' src_pid='%d' ts='%d' src_state='%c' event='%d' dst_pid='%d' dst_state='%c' dst_proc='%s'", src_proc, src_pid, time_us, src_state, event, dst_pid, dst_state, dst_proc));

            // Calculate the number of processes running
            int newNr = nrRunWait;
            FTraceProcessRecord proc = getProc(srcPid, br);
            if (event == SWITCH) {
                int prevState = calcPrevState(srcState);
                newNr += updateNr(proc, prevState, false, srcState);
            }
            int nextState = event == WAKEUP ? STATE_WAIT : STATE_RUN;
            proc = getProc(dstPid, br);
            newNr += updateNr(proc, nextState, true, dstState);
            if (newNr <= 0) {
                // This shouldn't happen!
                // incNrRunWait(1 - newNr); // This could be used as a workaround (but again, this should never happen)
                br.printErr(TAG + "Needs adjusting! newNr=" + newNr + " @" + timeUS);
                newNr = 1;
            }
            nrRunWait = newNr;

            TraceRecord data = new TraceRecord(timeUS, srcPid, dstPid, srcState, dstState, event);
            data.nrRunWait = nrRunWait - 1; // -1 due to not counting the idle process (which is either running or waiting)
            mTail.next = data;
            mTail = data;
            getProc(srcPid, br).used++;
            getProc(dstPid, br).used++;

            if (srcPid == 0 || dstPid == 0) {
                adjNoIdle = 0; // No need to adjust due to idle not "running"
            }
        }
        if (adjNoIdle == 1) {
            incNrRunWait(adjNoIdle);
        }
        long duration = mTail.time - mHead.next.time;

        // Save the VCD file
        String fn = br.getRelRawDir() + "ftrace.vcd";
        try {
            int runWaitBits = 8;
            FileOutputStream fos = new FileOutputStream(br.getBaseDir() + fn);
            PrintStream fo = new PrintStream(fos);

            // write header
            fo.println("$timescale 1us $end");
            fo.println("$scope mytrace $end");

            fo.println("$var wire " + runWaitBits + " RUNWAIT Processes.Running.And.Waiting $end");

            for (int i = 0; i < 65535; i++) {
                if (mPids[i] != null && mPids[i].used > 0) {
                    String id = genId();
                    String name = getProc(i, br).getVCDName();
                    mPids[i].id = id;
                    fo.println("$var wire 1 " + id + " " + name + " $end");
                }
            }

            fo.println("$upscope $end");
            fo.println("$enddefinitions $end");

            TraceRecord cur = mHead.next;

            fo.println("#" + cur.time);
            fo.println("b" + Util.toBinary(0, runWaitBits) + " RUNWAIT");
            for (int i = 0; i < 65535; i++) {
                if (mPids[i] != null && mPids[i].used > 0) {
                    fo.println("b" + getSignal(i, mPids[i].initState) + " " + mPids[i].id);
                }
            }

            long lastTime = 0;
            int lastNrRunWait = 0;
            while (cur != null) {
                if (lastTime != cur.time) {
                    lastTime = cur.time;
                    fo.println("#" + cur.time);
                }

                // Update the number of processes running
                if (cur.nrRunWait != lastNrRunWait) {
                    lastNrRunWait = cur.nrRunWait;
                    fo.println("b" + Util.toBinary(lastNrRunWait, runWaitBits) + " RUNWAIT");
                }

                // Now check what happens with the prev task
                // In case of wakeup, nothing happens with the previous task, so we are
                // interested only in context switches
                if (cur.event == SWITCH) {
                    FTraceProcessRecord prev = getProc(cur.prevPid, br);
                    int prevState = calcPrevState(cur.prevState);
                    if (prevState != prev.state) {
                        // Change in state
                        if (prev.lastTime != 0) {
                            long elapsed = lastTime - prev.lastTime;
                            if (prev.state == STATE_RUN) {
                                prev.runTime += elapsed;
                            } else if (prev.state == STATE_WAIT) {
                                prev.waitTime += elapsed;
                                prev.waitTimeCnt++;
                                prev.waitTimeMax = Math.max(prev.waitTimeMax, (int)elapsed);
                            } else if (prev.state == STATE_DISK) {
                                prev.diskTime += elapsed;
                                prev.diskTimeCnt++;
                                prev.diskTimeMax = Math.max(prev.diskTimeMax, (int)elapsed);
                            }
                        }
                        prev.state = prevState;
                        prev.lastTime = lastTime;
                        fo.println("b" + getSignal(prev.pid, prevState) + " " + prev.id);
                    }
                }

                // And let's see what happens with the new task
                FTraceProcessRecord next = getProc(cur.nextPid, br);
                int nextState = STATE_RUN;
                if (cur.event == WAKEUP) {
                    // Not running yet, so it must be waiting
                    nextState = STATE_WAIT;
                }
                if (nextState != next.state) {
                    // Change in state
                    if (next.lastTime != 0) {
                        long elapsed = lastTime - next.lastTime;
                        if (next.state == STATE_RUN) {
                            next.runTime += elapsed;
                        } else if (next.state == STATE_WAIT) {
                            next.waitTime += elapsed;
                            next.waitTimeCnt++;
                            next.waitTimeMax = Math.max(next.waitTimeMax, (int)elapsed);
                        } else if (next.state == STATE_DISK) {
                            next.diskTime += elapsed;
                            next.diskTimeCnt++;
                            next.diskTimeMax = Math.max(next.diskTimeMax, (int)elapsed);
                        }
                    }
                    next.state = nextState;
                    next.lastTime = lastTime;
                    fo.println("b" + getSignal(next.pid, nextState) + " " + next.id);
                }
                cur = cur.next;
            }
            fo.close();
            fos.close();
        } catch (IOException e) {
            br.printErr(TAG + "Error saving vcd file: " + e);
        }

        // Collect process statistics
        Vector<FTraceProcessRecord> list = new Vector<FTraceProcessRecord>();
        for (int i = 0; i < 65535; i++) {
            if (mPids[i] != null && mPids[i].used > 0) {
                list.add(mPids[i]);
            }
        }
        Collections.sort(list, new FTraceProcessRecordComparator());

        // Map ftrace process records to bugreport process records
        for (FTraceProcessRecord pr : list) {
            boolean useParent = false;
            ProcessRecord procRec = br.getProcessRecord(pr.pid, true, false);
            ProcessRecord pProcRec = null;
            PSRecord psr = br.getPSRecord(pr.pid);
            if (psr != null) {
                pProcRec = br.getProcessRecord(psr.getParentPid(), false, false);
                if (pProcRec != null && !"zygote".equals(pProcRec.getProcName())) {
                    useParent = true;
                }
            }
            if (procRec == null || (useParent && pProcRec != null)) {
                procRec = pProcRec;
            }
            if (procRec != null) {
                pr.procRec = procRec;
            }
        }

        // Create report
        Chapter ch, main = new Chapter(br, "FTrace");

        main.addLine("<p>VCD file saved as (you can use GTKWave to open it): <a href=\"" + fn + "\">" + fn + "</a></p>");

        // Create statistics
        ch = new Chapter(br, "Statistics");
        main.addChapter(ch);
        beginStatTbl(ch, br, duration, true, true);
        for (FTraceProcessRecord pr : list) {
            addStatTblRow(br, ch, pr, duration, true);
        }
        endStatTbl(ch);

        // Create Trace chapter
        ch = new Chapter(br, "Trace");
        main.addChapter(ch);
        beginTraceTbl(ch, br, duration, true, true, true);
        for (FTraceProcessRecord pr : list) {
            // Create the trace image
            String png = br.getRelDataDir() + "ftrace_" + pr.pid + ".png";
            createTracePng(br.getBaseDir() + png, pr, mHead.next, duration);
            // Add the table row
            addTraceTblRow(br, ch, pr, true);
        }
        endTraceTbl(ch);

        // Add some stats and traces to the individual process records
        HashSet<ProcessRecord> usedPR = new HashSet<ProcessRecord>();
        for (FTraceProcessRecord fpr_dummy : list) {
            ProcessRecord pr = fpr_dummy.procRec;
            if (pr != null && !usedPR.contains(pr)) {
                usedPR.add(pr);

                // Add the statistics
                beginStatTbl(pr, br, duration, true, false);
                for (FTraceProcessRecord fpr : list) {
                    if (fpr.procRec != pr) continue;
                    addStatTblRow(br, pr, fpr, duration, false);
                }
                endStatTbl(pr);

                // Add the trace
                beginTraceTbl(pr, br, duration, true, false, false);
                for (FTraceProcessRecord fpr : list) {
                    if (fpr.procRec != pr) continue;
                    addTraceTblRow(br, pr, fpr, false);
                }
                endTraceTbl(pr);
            }
        }

        // Create the parallel-histogrram
        ch = new Chapter(br, "Parallel process histogram");
        main.addChapter(ch);
        createParallelHist(ch, br, mHead.next, duration, TRACE_W);

        br.addChapter(main);
    }

    private String makeProcName(BugReport br, FTraceProcessRecord pr, boolean addLink) {
        // Add priority info
        String name = pr.getName();
        PSRecord ps = br.getPSRecord(pr.pid);
        if (ps != null) {
            name += " " + Util.getNiceImg(ps.getNice()) + " " + Util.getSchedImg(ps.getPolicy()) + " ";
        }

        // Convert to link if needed
        int linkPid = -1;
        if (addLink) {
            ProcessRecord procRec = pr.procRec;
            if (procRec != null) {
                linkPid = procRec.getPid();
            }
        }
        if (linkPid != -1) {
            name = "<a href=\"" + br.createLinkToProcessRecord(linkPid) + "\">" + name + "</a>";
        }

        return name;
    }

    private void beginStatTbl(Chapter ch, Report br, long duration, boolean addTotal, boolean addExplanation) {
        ch.addLine("<p>Process runtime statistics (total trace duration: " + shadeTimeUS(duration) + "us):</p>");

        if (addExplanation) {
            ch.addLine("<div class=\"hint\">(Hint: click on the headers to sort the data)</div>");
        }

        ch.addLine("<table class=\"ftrace-stat tablesorter\"><!-- I know, tables are evil, but I still have to learn to use floats -->");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"ftrace-stat-header\">");
        ch.addLine("    <th>Name</td>");
        ch.addLine("    <th>Run time (us)</td>");
        ch.addLine("    <th>Run time (%)</td>");
        ch.addLine("    <th>Wait time (us)</td>");
        ch.addLine("    <th>Wait time (%)</td>");
        ch.addLine("    <th>Avg. (us)</td>");
        ch.addLine("    <th>Max. (us)</td>");
        ch.addLine("    <th>Wait/Run</td>");
        ch.addLine("    <th>IO wait time (us)</td>");
        ch.addLine("    <th>IO wait time (%)</td>");
        ch.addLine("    <th>Avg. (us)</td>");
        ch.addLine("    <th>Max. (us)</td>");
        ch.addLine("    <th>IO wait/Run</td>");
        ch.addLine("  </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");
    }

    private void addStatTblRow(BugReport br, Chapter ch, FTraceProcessRecord pr, long duration, boolean addLink) {
        float waitOverRun = 0.0f;
        float diskOverRun = 0.0f;
        int avgWaitTime = 0;
        int avgDiskTime = 0;
        if (pr.runTime > 0) {
            waitOverRun = (float)pr.waitTime / pr.runTime;
            diskOverRun = (float)pr.diskTime / pr.runTime;
        }
        if (pr.waitTimeCnt > 0) {
            avgWaitTime = (int)(pr.waitTime / pr.waitTimeCnt);
        }
        if (pr.diskTimeCnt > 0) {
            avgDiskTime = (int)(pr.diskTime / pr.diskTimeCnt);
        }
        ch.addLine("  <tr>");
        String name = makeProcName(br, pr, addLink);
        ch.addLine("    <td>" + name + "</td>");
        ch.addLine("    <td>" + shadeTimeUS(pr.runTime) + "</td>");
        ch.addLine("    <td>" + String.format("%3.1f", (pr.runTime * 100.0 / duration)) + "%</td>");
        ch.addLine("    <td>" + shadeTimeUS(pr.waitTime) + "</td>");
        ch.addLine("    <td>" + String.format("%3.1f", (pr.waitTime * 100.0 / duration)) + "%</td>");
        ch.addLine("    <td>" + shadeTimeUS(avgWaitTime) + "</td>");
        ch.addLine("    <td>" + shadeTimeUS(pr.waitTimeMax) + "</td>");
        ch.addLine("    <td>" + String.format("%3.2f", waitOverRun) + "</td>");
        ch.addLine("    <td>" + shadeTimeUS(pr.diskTime) + "</td>");
        ch.addLine("    <td>" + String.format("%3.1f", (pr.diskTime * 100.0 / duration)) + "%</td>");
        ch.addLine("    <td>" + shadeTimeUS(avgDiskTime) + "</td>");
        ch.addLine("    <td>" + shadeTimeUS(pr.diskTimeMax) + "</td>");
        ch.addLine("    <td>" + String.format("%3.2f", diskOverRun) + "</td>");
        ch.addLine("  </tr>");
    }

    private void endStatTbl(Chapter ch) {
        ch.addLine("  </tbody>");
        ch.addLine("</table>");
    }

    private void beginTraceTbl(Chapter ch, Report br, long duration, boolean addTimeBar, boolean addParallelChart, boolean addExplanation) {
        ch.addLine("<p>Process trace overview:</p>");

        if (addExplanation) {
            ch.addLine("<div><img src=\"" + br.getRelDataDir() + "ftrace-legend-dred.png\"/> Partially running</div>");
            ch.addLine("<div><img src=\"" + br.getRelDataDir() + "ftrace-legend-red.png\"/> Running</div>");
            ch.addLine("<div><img src=\"" + br.getRelDataDir() + "ftrace-legend-dcyan.png\"/> Partially waiting</div>");
            ch.addLine("<div><img src=\"" + br.getRelDataDir() + "ftrace-legend-cyan.png\"/> Waiting</div>");
            ch.addLine("<div><img src=\"" + br.getRelDataDir() + "ftrace-legend-yellow.png\"/> Waiting for IO</div>");
            ch.addLine("<div><img src=\"" + br.getRelDataDir() + "ftrace-legend-black.png\"/> Sleeping</div>");

            ch.addLine("<p>NOTE: you can drag and move table rows to reorder them!</p>");
        }

        ch.addLine("<table class=\"ftrace-trace tablednd\"><!-- I know, tables are evil, but I still have to learn to use floats -->");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"ftrace-trace-header\">");
        ch.addLine("    <th>Name</td>");
        ch.addLine("    <th>Trace</td>");
        ch.addLine("  </tr>");

        if (addTimeBar) {
            String fnTimeBar = getTimeBarName(br, duration);
            if (fnTimeBar != null) {
                ch.addLine("  <tr>");
                ch.addLine("    <th>Relative time</td>");
                ch.addLine("    <th><img src=\"" + br.getRelDataDir() + fnTimeBar + "\"/></td>");
                ch.addLine("  </tr>");
            }
        }

        if (addParallelChart) {
            ch.addLine("  <tr>");
            ch.addLine("    <th>Number of processes wanting to run in parallel</td>");
            ch.addLine("    <th><img src=\"" + br.getRelDataDir() + getParallelChartName() + "\"/></td>");
            ch.addLine("  </tr>");
        }

        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");
    }

    private void addTraceTblRow(BugReport br, Chapter ch, FTraceProcessRecord pr, boolean addLink) {
        String png = br.getRelDataDir() + "ftrace_" + pr.pid + ".png";
        ch.addLine("  <tr>");
        String name = makeProcName(br, pr, addLink);
        ch.addLine("    <td>" + name + "</td>");
        ch.addLine("    <td><img src=\"" + png + "\"/></td>");
        ch.addLine("  </tr>");
    }

    private void endTraceTbl(Chapter ch) {
        ch.addLine("  </tbody>");
        ch.addLine("</table>");
    }

    private String getParallelChartName() {
        return "ftrace_nr_parallel.png";
    }

    private String getTimeBarName(Report br, long duration) {
        if (mTimeBarName == null) {
            String fnTimeBar = "ftrace_time.png";
            if (Util.createTimeBar(br, fnTimeBar, TRACE_W, 0, duration / 1000)) { // us -> ms
                mTimeBarName = fnTimeBar;
            }
        }
        return mTimeBarName;
    }

    private String shadeTimeUS(long time) {
        return Util.shadeValue((int)time, "ftrace-us");
    }

    private char getSignal(int pid, int state) {
        if (pid == 0) return STATE_SIGNALS_IDLE[state];
        return STATE_SIGNALS[state];
    }

    private int calcPrevState(char srcState) {
        if (srcState == 'R') return STATE_WAIT;
        if (srcState == 'D') return STATE_DISK;
        return STATE_SLEEP;
    }

    private int updateNr(FTraceProcessRecord proc, int newState, boolean newPid, char newCState) {
        int ret = 0;
        int oldState = proc.state;
        if (oldState != newState) {
            if (oldState == STATE_SLEEP || oldState == STATE_DISK) ret = +1;
            if (newState == STATE_SLEEP || newState == STATE_DISK) ret = -1;
        }
        if (!proc.initStateSet) {
            if (newPid) {
                if (oldState == STATE_SLEEP && newState == STATE_RUN) {
                    // if no wakeup, then it was already waiting
                    proc.initState = STATE_WAIT;
                    incNrRunWait(ret); // Need to fix history as well
                } else if (oldState == STATE_SLEEP && newState == STATE_WAIT) {
                    if (newCState == 'D') {
                        proc.initState = STATE_DISK;
                    }
                }
            } else {
                if (ret == +1) {
                    proc.initState = STATE_RUN; // The previous state couldn't be sleep
                    incNrRunWait(ret); // Need to fix history as well
                }
            }
        }
        proc.initStateSet = true; // This was the only and last chance to guess the init state
        proc.state = newState;
        return ret;
    }

    private void incNrRunWait(int delta) {
        TraceRecord cur = mHead.next;
        while (cur != null) {
            cur.nrRunWait += delta;
            cur = cur.next;
        }
    }

    private void createTracePng(String fileName, FTraceProcessRecord pr, TraceRecord head, long duration) {
        // Setup initial data
        int w = TRACE_W;
        int h = TRACE_H;
        long startTime = head.time;
        int pid = pr.pid;
        int lastState = 'S';
        if (pr.initState == STATE_RUN) {
            lastState = 'R';
        } else if (pr.initState == STATE_DISK) {
            lastState = 'D';
        }
        int lastX = 0;

        // Create the empty image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        // Render the trace
        Color darkRed = new Color(0x800000);
        Color darkCyan = new Color(0x008080);
        while (head != null) {
            if (head.prevPid == pid) {
                // This process was switched away, render something
                int x = (int)((head.time - startTime) * w / duration);
                if (lastX != -1) {
                    if (lastX == x) {
                        g.setColor(darkRed);
                        g.fillRect(lastX, 0, 1, h);
                    } else {
                        g.setColor(Color.RED);
                        g.fillRect(lastX + 1, 0, x - lastX + 1, h);
                    }
                }
                lastX = x;
                lastState = head.prevState;
            }
            if (head.nextPid == pid) {
                // This process was resumed (or at least woken up),
                int x = (int)((head.time - startTime) * w / duration);
                if (lastX != -1) {
                    if (lastState == 'D') {
                        g.setColor(Color.YELLOW);
                        g.drawLine(lastX, h/2, x, h/2);
                    } else if (lastState == 'R' && pid != 0) {
                        if (lastX == x) {
                            g.setColor(darkCyan);
                            g.fillRect(lastX, 0, 1, h);
                        } else {
                            g.setColor(Color.CYAN);
                            g.fillRect(lastX + 1, 0, x - lastX + 1, h);
                        }
                    }
                }
                lastX = x;
                lastState = head.nextState;
            }
            head = head.next;
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createParallelHist(Chapter ch, BugReport br, TraceRecord head, long duration, int w) {
        // Setup initial data
        int max = 16;
        long durations[] = new long[max];
        int count = 0;
        int maxUsed = 0;
        long startTime = head.time;
        long lastTime = head.time;
        int lastX = 0;

        // Create the empty image
        int stepSize = 8;
        int h = stepSize * max;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int minNr[] = newIntArr(w, Integer.MAX_VALUE);
        int maxNr[] = newIntArr(w, 0);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.RED);

        // Process the trace
        while (head != null) {
            long now = head.time;

            int newCount = head.nrRunWait;
            if (newCount != count) {
                int x = (int)((head.time - startTime) * w / duration);
                fillMinMax(lastX, x, count, minNr, maxNr);
                lastX = x;

                // We need to add the duration with the previous count
                long dur = now - lastTime;
                lastTime = now;
                int idx = count;
                if (idx >= max) {
                    idx = max-1;
                }
                if (idx > maxUsed) {
                    maxUsed = idx;
                }
                durations[idx] += dur;

                // And only now we update the count to the new value
                count = newCount;
            }

            head = head.next;
        }

        // Now do the actuall rendering
        Color cmin = new Color(0xff0000);
        Color cmax = new Color(0x800000);
        for (int i = 0; i < w; i++) {
            if (minNr[i] > maxNr[i]) {
                // Skip -> no data
                continue;
            }
            int ymin = h - 1 - stepSize * minNr[i];
            if (ymin < 0) ymin = 0;
            int ymax = h - 1 - stepSize * maxNr[i];
            if (ymax < 0) ymax = 0;
            g.setColor(cmin);
            g.fillRect(i, ymin, 1, h - ymin);
            g.setColor(cmax);
            g.fillRect(i, ymax, 1, ymin - ymax);
        }

        ch.addLine("<p>The following table shows how many processes were either running or waiting at the same time:</p>");

        String fnHist = br.getRelDataDir() + "par_proc_hist.png";
        ch.addLine("<div style=\"float: right;\"><img src=\"" + fnHist + "\"/></div>");

        ch.addLine("<div class=\"hint\">(Hint: click on the headers to sort the data)</div>");

        ch.addLine("<table class=\"ftrace-stat tablesorter\" style=\"width: auto;\">");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"ftrace-stat-header\">");
        ch.addLine("    <th>Number of parallel processes</td>");
        ch.addLine("    <th>Run time (us)</td>");
        ch.addLine("    <th>Run time (%)</td>");
        ch.addLine("  </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");

        for (int i = 0; i <= maxUsed; i++) {
            ch.addLine("  <tr>");
            ch.addLine("    <td>" + i + (i == max-1 ? " or more" : "") + "</td>");
            ch.addLine("    <td>" + shadeTimeUS(durations[i]) + "</td>");
            ch.addLine("    <td>" + String.format("%3.1f", (durations[i] * 100.0 / duration)) + "%</td>");
            ch.addLine("  </tr>");
        }

        ch.addLine("  </tbody>");
        ch.addLine("</table>");

        // Add some guidelines
        g.setColor(new Color(0x80ffffff, true));
        for (int i = 0; i < max; i++) {
            int yy = h - 1 - i * stepSize;
            g.drawLine(0, yy, w, yy);
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + br.getRelDataDir() + getParallelChartName()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create the histogram image
        int hw = 400, hh = 400, hmax = 350;
        int hml = 64, hmr = 32, hmt = 32, hmb = 64;
        int thw = hml + hw + hmr;
        int thh = hmt + hh + hmb;
        img = new BufferedImage(thw, thh, BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, thw, thh);
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();

        // Draw the axis
        g.drawLine(hml, hmt, hml, hmt + hh);
        g.drawLine(hml - 5, hmt + 5, hml, hmt);
        g.drawLine(hml + 5, hmt + 5, hml, hmt);
        g.drawLine(hml + hw, hmt + hh, hml, hmt + hh);
        g.drawLine(hml + hw - 5, hmt + hh - 5, hml + hw, hmt + hh);
        g.drawLine(hml + hw - 5, hmt + hh + 5, hml + hw, hmt + hh);

        // Draw the labels on the Y axis
        for (int i = 10; i <= 100; i += 10) {
            int yy = hmt + hh - (hmax * i / 100);
            g.setColor(Color.BLACK);
            g.drawLine(hml - 5, yy, hml, yy);
            String s = Integer.toString(i) + "% ";
            g.drawString(s, hml - 5 - fm.stringWidth(s), yy);
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(hml, yy, hml + hw, yy);
        }

        // Draw the bars and the labels on the X axis
        int cnt = durations.length;
        int bd = (hw / cnt);
        int bm = 2, bw = bd - 2 * bm;
        for (int i = 0; i < durations.length; i++) {
            int green = (cnt - i) * 255 / cnt;
            int red = i * 255 / cnt;
            int rgb = (red << 16) | (green << 8);
            g.setColor(new Color(rgb));
            int bh = (int)(hmax * durations[i] / duration);
            int bx = hml + i * bd + bm;
            g.fillRect(bx, hmt + hh - bh, bw, bh);
            g.setColor(Color.BLACK);
            bx += bw / 2;
            g.drawLine(bx, hmt + hh, bx, hmt + hh + 5);
            String s = Integer.toString(i);
            g.drawString(s, bx - fm.stringWidth(s) / 2, hmt + hh + 5 + fm.getAscent());
        }

        // Draw the title
        g.drawString("Parallel process histogram", 10, 10 + fm.getAscent());

        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fnHist));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int[] newIntArr(int size, int defValue) {
        int ret[] = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = defValue;
        }
        return ret;
    }

    private void fillMinMax(int from, int to, int value, int[] minNr, int[] maxNr) {
        from = Math.max(0, from);
        to = Math.min(minNr.length - 1, to);
        for (int i = from; i <= to; i++) {
            if (value < minNr[i]) {
                minNr[i] = value;
            }
            if (value > maxNr[i]) {
                maxNr[i] = value;
            }
        }
    }

}
