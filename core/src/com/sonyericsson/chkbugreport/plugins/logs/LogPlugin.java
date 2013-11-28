/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.GuessedValue;
import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.TimeWindowMarker;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.Data;
import com.sonyericsson.chkbugreport.chart.DataSet;
import com.sonyericsson.chkbugreport.chart.Marker;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.doc.WebOnlyChapter;
import com.sonyericsson.chkbugreport.plugins.SysPropsPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.webapp.LogWebApp;
import com.sonyericsson.chkbugreport.util.LineReader;
import com.sonyericsson.chkbugreport.util.XMLNode;
import com.sonyericsson.chkbugreport.webserver.ChkBugReportWebServer;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

public abstract class LogPlugin extends Plugin implements LogData {

    public static final String TAG = "[LogPlugin]";

    private static final long DAY = 24 * 60 * 60 * 1000;

    private HashMap<Integer,ProcessLog> mLogs = new HashMap<Integer, ProcessLog>();

    private long mTsFirst = -1;
    private long mTsLast = -1;

    private String mWhich;
    private String mId;
    private String mSectionName;

    private HashMap<Integer,GCRecords> mGCs = new HashMap<Integer, GCRecords>();
    private LogLines mParsedLog = new LogLines();
    private Vector<ConfigChange> mConfigChanges = new Vector<ConfigChange>();

    private boolean mLoaded = false;

    private Section mSection;
    private Chapter mCh;

    private Hooks mHooks = new Hooks(this);

    public LogPlugin(String which, String id, String sectionName) {
        mWhich = which;
        mId = id;
        mSectionName = sectionName;
    }

    public Chapter getChapter() {
        return mCh;
    }

    @Override
    abstract public String getInfoId();

    public LogLines getLogs() {
        return mParsedLog;
    }

    @Override
    public int size() {
        return mParsedLog.size();
    }

    @Override
    public LogLine get(int i) {
        return mParsedLog.get(i);
    }

    @Override
    public void reset() {
        mTsFirst = -1;
        mTsLast = -1;
        mGCs.clear();
        mParsedLog.clear();
        mLogs.clear();
        mLoaded = false;
        mSection = null;
        mCh = null;
        mConfigChanges.clear();
        mHooks.reset();
    }

    @Override
    public void onHook(Module mod, XMLNode hook) {
        mHooks.add(hook);
    }

    @Override
    public void load(Module rep) {
        BugReportModule br = (BugReportModule)rep;

        mSection = br.findSection(mSectionName);
        if (mSection == null) {
            br.printErr(3, TAG + "Cannot find section " + mSectionName + " (aborting plugin)");
            return;
        }

        // Load and parse the lines
        mCh = new Chapter(br.getContext(), mWhich + " log");
        int cnt = mSection.getLineCount();
        int fmt = LogLine.FMT_UNKNOWN;
        LogLine prev = null;
        int skippedDueToTimeJump = 0;
        int skippedDueToTimeWindow = 0;
        TimeWindowMarker twStart = br.getContext().getTimeWindowStart();
        TimeWindowMarker twEnd = br.getContext().getTimeWindowEnd();
        for (int i = 0; i < cnt; i++) {
            String line = mSection.getLine(i);
            LogLine sl = new LogLine(br, line, fmt, prev);

            if (sl.ok) {
                // Check for timewidow matching
                boolean skip = false;
                if (!twStart.isAfterOrNoFilter(sl.ts)) {
                    skip = true;
                }
                if (!twEnd.isBeforeOrNoFilter(sl.ts)) {
                    skip = true;
                }
                if (skip) {
                    skippedDueToTimeWindow++;
                    continue;
                }

                // Check for timejumps
                if (prev != null) {
                    if (prev.ts + DAY < sl.ts) {
                        // We got a huge timejump, ignore everything before this
                        skippedDueToTimeJump += mParsedLog.size();
                        mParsedLog.clear();
                        prev = null;
                    }
                }

                mParsedLog.add(sl);
                fmt = sl.fmt;
                prev = sl;
            }
        }

        // Execute the hooks
        mHooks.execute(br);
        // Delete hidden log lines
        cnt = mParsedLog.size();
        while (--cnt >= 0) {
            if (mParsedLog.get(cnt).isHidden()) {
                mParsedLog.remove(cnt);
            }
        }

        // Fetch boundary timestamps
        cnt = mParsedLog.size();
        if (cnt > 0) {
            mTsFirst = mParsedLog.get(0).ts;
            mTsLast = mParsedLog.get(cnt - 1).ts;
        }

        // Check for timestamp order
        int orderErrors = 0;
        LogLines errLines = new LogLines();
        LogLine lastLine = null;
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mParsedLog.get(i);
            if (sl.ok) {
                if (lastLine != null) {
                    if (lastLine.ts > sl.ts) {
                        orderErrors++;
                        errLines.add(lastLine);
                        errLines.add(sl);
                    }
                }
                lastLine = sl;
            }
        }
        if (orderErrors > 0) {
            Bug bug = new Bug(Bug.Type.TOOL_WARN, Bug.PRIO_INCORRECT_LOG_ORDER, 0, "Incorrect timestamp order in " + mSectionName);
            bug.add(new Block()
                    .add("Timestamps are not in correct order in the ")
                    .add(new Link(getChapter().getAnchor(), mSectionName)));
            bug.add(new Block()
                    .add("This could effect some plugins, which might produce wrong data! The following lines seems to be out of order:"));
            DocNode log = new Block(bug).addStyle("log");
            boolean first = false;
            log.add("...");
            for (LogLine ll : errLines) {
                first = !first;
                log.add(ll.symlink());
                if (!first) {
                    log.add("...");
                }
            }
            br.addBug(bug);
        }
        if (skippedDueToTimeJump > 0) {
            Bug bug = new Bug(Bug.Type.TOOL_WARN, Bug.PRIO_LOG_TIMEJUMP, 0, "Huge time gap in " + mSectionName);
            bug.add(new Block()
                .add("There was at least one huge time gap (at least one day) in the log. The lines before the last time gap ("
                    + skippedDueToTimeJump + " lines) have been skipped in the ")
                .add(new Link(getChapter().getAnchor(), mSectionName)));
            br.addBug(bug);
        }
        if (skippedDueToTimeWindow > 0) {
            Bug bug = new Bug(Bug.Type.TOOL_WARN, Bug.PRIO_LOG_TIMEWINDOW, 0, "Lines ignored due to time window in " + mSectionName);
            bug.add(new Block()
            .add("There were " + skippedDueToTimeWindow + " lines ignored due to the time window you specified ("
                    + twStart.format() + ".." + twEnd.format() + ") in the ")
                    .add(new Link(getChapter().getAnchor(), mSectionName)));
            br.addBug(bug);
        }

        // Analyze the log
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mParsedLog.get(i);
            if (sl.ok) {
                // Analyze the log line
                analyze(sl, i, br, mSection);
            }
        }

        onLoaded(br);

        // Load successful
        mLoaded = true;
    }

    protected void postLoad(Module mod) {
        mod.addInfo(getInfoId(), getLogs());
        if (null != getChapter()) {
            getChapter().addChapter(new WebOnlyChapter(mod.getContext(), "Log (editable)", getInfoId() + "$log"));
        }
    }

    @Override
    public void setWebServer(ChkBugReportWebServer ws) {
        ws.addModule(getInfoId(), new LogWebApp(this, ws));
    }

    protected void onLoaded(BugReportModule br) {
        // NOP
    }

    @Override
    public void generate(Module rep) {
        BugReportModule br = (BugReportModule)rep;
        if (!mLoaded) {
            return;
        }

        mCh.addChapter(generateLog(br));

        // Generate the log spammer top-list
        generateSpamTopList(br, mCh);

        // Generate the GC graphs
        Chapter chGC = new Chapter(br.getContext(), "GC graphs");
        if (generateGCGraphs(br, chGC) > 0) {
            mCh.addChapter(chGC);
        }

        // Generate other extra sections
        generateExtra(br, mCh);

        br.addChapter(mCh);
    }

    private Chapter generateLog(BugReportModule br) {
        Chapter ch = new Chapter(br.getContext(), "Log");
        new LogToolbar(ch);
        DocNode log = new Block().addStyle("log");
        ch.add(log);

        int cnt = mParsedLog.size();
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mParsedLog.get(i);
            if (sl.ok) {
                ProcessLog pl = getLogOf(br, sl.pid);
                pl.add(sl.symlink());
            }
            log.add(sl);
        }
        return ch;
    }

    private void generateSpamTopList(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "Spam top list");
        mainCh.addChapter(ch);
        ch.add(new Para().add("Processes which produced most of the log:"));

        // Copy the process logs into a vector, so we can sort it later on
        Vector<ProcessLog> vec = new Vector<ProcessLog>();
        for (ProcessLog pl : mLogs.values()) {
            vec.add(pl);
        }

        // Sort the list
        Collections.sort(vec, new Comparator<ProcessLog>() {
            @Override
            public int compare(ProcessLog o1, ProcessLog o2) {
                return o2.getLineCount() - o1.getLineCount();
            }
        });

        // Render the data
        Table t = new Table(Table.FLAG_NONE);
        ch.add(t);
        t.addColumn("Process", Table.FLAG_NONE);
        t.addColumn("Pid", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Nr. of lines", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("% of all log", Table.FLAG_ALIGN_RIGHT);
        t.begin();
        int cnt = Math.min(10, vec.size());
        int totLines = mParsedLog.size();
        for (int i = 0; i < cnt; i++) {
            ProcessLog pl = vec.get(i);
            int pid = pl.getPid();
            int count = pl.getLineCount();
            t.addData(new ProcessLink(br, pid));
            t.addData(pid);
            t.addData(count);
            t.addData(String.format("%.1f%%", (count * 100.0f / totLines)));
        }
        t.end();
    }

    protected String getAnchorToLine(int i) {
        return mId + "log_" + i;
    }

    protected String getId() {
        return mId;
    }

    public int getParsedLineCount() {
        return mParsedLog.size();
    }

    public LogLine getParsedLine(int i) {
        return mParsedLog.get(i);
    }

    protected void generateExtra(BugReportModule br, Chapter ch) {
        // NOP
    }

    protected void analyze(LogLine sl, int i, BugReportModule br, Section s) {
        if (sl.level == 'F') {
            reportFatalLog(sl, i, br, s);
        }
    }

    protected ProcessLog getLogOf(BugReportModule br, int pid) {
        ProcessLog log = mLogs.get(pid);
        if (log == null) {
            log = new ProcessLog(this, br, pid);
            mLogs.put(pid, log);

            // Add link from global process record
            ProcessRecord pr = br.getProcessRecord(pid, true, true);
            String text = mWhich + " log (filtered by this process) &gt;&gt;&gt;";
            new Block(pr).add(new Link(log.getAnchor(), text));
            br.addExtraFile(log);
        }
        return log;
    }

    public long getFirstTs() {
        return mTsFirst;
    }

    public long getLastTs() {
        return mTsLast;
    }

    private void reportFatalLog(LogLine sl, int i, BugReportModule br, Section s) {
        // Put a marker box
        sl.addMarker("log-float-err", "FATAL", null);

        // Create a bug and store the relevant log lines
        Bug bug = new Bug(Bug.Type.PHONE_ERR, Bug.PRIO_FATAL_LOG, sl.ts, "Fatal: " + sl.msg);
        new Block(bug).add(new Link(sl.getAnchor(), "(link to log)"));
        new Para(bug).add("Log around the fatal log line (+/-10 lines):");
        DocNode log = new Block(bug).addStyle("log");
        int from = Math.max(0, i - 10);
        int to = Math.min(s.getLineCount() - 1, i + 10);
        if (from > 0) {
            log.add("...");
        }
        for (int idx = from; idx <= to; idx++) {
            LogLine sl2 = getParsedLine(idx);
            log.add(sl2.symlink());
        }
        if (to < s.getLineCount() - 1) {
            log.add("...");
        }
        bug.setAttr(Bug.ATTR_FIRST_LINE, i);
        bug.setAttr(Bug.ATTR_LAST_LINE, i);
        bug.setAttr(Bug.ATTR_LOG_INFO_ID, getInfoId());
        br.addBug(bug);
    }

    private int generateGCGraphs(BugReportModule br, Chapter ch) {
        int cnt = 0;
        for (GCRecords gcs : mGCs.values()) {
            if (gcs.size() >= 2) {
                if (generateGCGraph(br, ch, gcs)) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    private boolean generateGCGraph(BugReportModule br, Chapter ch, GCRecords gcs) {
        int pid = gcs.get(0).pid;
        long firstTs = getFirstTs();
        long duration = (getLastTs() - firstTs);
        int heapLimit = 32;
        if (duration <= 0) return false;

        // Create chart generator
        String procName = "";
        ProcessRecord pr = br.getProcessRecord(pid, false, false);
        if (pr != null) {
            procName = pr.getName();
        } else {
            procName = Integer.toString(pid);
        }
        ChartGenerator chart = new ChartGenerator("Memory after GC in process " + procName);

        // Fetch the heap limit from system properties
        SysPropsPlugin props = (SysPropsPlugin)br.getPlugin("SysPropsPlugin");
        if (props != null) {
            String s = props.getProp("dalvik.vm.heapsize");
            if (s != null) {
                if (s.endsWith("m")) {
                    s = s.substring(0, s.length() - 1);
                }
                try {
                    heapLimit = Integer.parseInt(s);
                } catch (NumberFormatException nfe) { }
            }
        }

        // Avoid issue when duration is too long
        long firstGcTs = gcs.get(0).ts;
        long realDuration = getLastTs() - firstGcTs;
        if (realDuration * 3 < duration) {
            // Need to scale it down
            duration = realDuration;
            firstTs = firstGcTs;
        }

        // Check if external memory tracking is enabled
        boolean hasExternal = false;
        for (GCRecord gc : gcs) {
            if (gc.memExtAlloc >= 0 || gc.memExtSize >= 0) {
                hasExternal = true;
                break;
            }
        }

        // Draw the config changes (useful to see the correlation between config changes and memory usage)
        for (ConfigChange cc : mConfigChanges) {
            chart.addMarker(new Marker(Marker.Type.X, cc.ts, ImageCanvas.LIGHT_GRAY));
        }

        // Draw the heap limit
        chart.addMarker(new Marker(Marker.Type.Y, heapLimit * 1024, ImageCanvas.BLACK));

        // Plot the values (size)
        DataSet dsSize = new DataSet(DataSet.Type.PLOT, "VM Heap (size)", 0xffc0c080);
        dsSize.setAxisId(1);
        dsSize.setMin(0);
        DataSet dsSizeTotal = new DataSet(DataSet.Type.PLOT, "VM Heap + External (size)", 0xff8080d7);
        dsSizeTotal.setAxisId(1);
        DataSet dsAlloc = new DataSet(DataSet.Type.PLOT, "VM Heap (alloc)", 0xff808000);
        dsAlloc.setAxisId(1);
        DataSet dsAllocTotal = new DataSet(DataSet.Type.PLOT, "VM Heap + External (alloc)", 0xff0000c0);
        dsAllocTotal.setAxisId(1);
        DataSet dsTotal = new DataSet(DataSet.Type.PLOT, "Mem footprint", 0xffff4040);
        dsTotal.setAxisId(1);

        for (GCRecord gc : gcs) {
            dsSize.addData(new Data(gc.ts, gc.memFreeSize));
            dsAlloc.addData(new Data(gc.ts, gc.memFreeAlloc));
            if (hasExternal) {
                dsSizeTotal.addData(new Data(gc.ts, gc.memFreeSize + gc.memExtSize));
                dsAllocTotal.addData(new Data(gc.ts, gc.memFreeAlloc + gc.memExtAlloc));
                dsTotal.addData(new Data(gc.ts, gc.memFreeSize + gc.memExtAlloc));
            }
        }

        chart.add(dsSize);
        chart.add(dsAlloc);
        if (hasExternal) {
            chart.add(dsSizeTotal);
            chart.add(dsAllocTotal);
            chart.add(dsTotal);
        }

        // Save the image
        chart.setOutput("gc_" + mId + "_" + pid + ".png");
        DocNode node = chart.generate(br);
        if (node != null) {
            // Add chart to chapter
            ch.add(node);

            // Also insert a link at the beginning of the per-process log
            ProcessLog pl = getLogOf(br, pid);
            pl.add(node);

            // And also add it to the process record
            if (pr != null) {
                new Para(pr).add("Memory usage from GC " + mId + " logs:");
                pr.add(node);
            }
        }

        return true;
    }

    protected void addActivityLaunchMarker(LogLine sl, String activity) {
        String cmp[] = activity.split("/");
        if (cmp.length == 2) {
            if (cmp[1].startsWith(cmp[0])) {
                cmp[1] = cmp[1].substring(cmp[0].length());
            }
            sl.addMarker("log-float", cmp[0] + "<br/>" + cmp[1], null);
        }
    }

    protected void addGCRecord(int pid, GCRecord gc) {
        GCRecords gcs = mGCs.get(pid);
        if (gcs == null) {
            gcs = new GCRecords();
            mGCs.put(pid, gcs);
        }
        gcs.add(gc);
    }

    protected void addConfigChange(ConfigChange cc) {
        mConfigChanges.add(cc);
    }

    @Override
    public void autodetect(Module mod, byte[] buff, int offs, int len, GuessedValue<String> type) {
        LineReader lr = new LineReader(buff, offs, len);
        String line = null;
        int fmt = LogLine.FMT_UNKNOWN;
        LogLine prev = null;
        int okCount = 0, count = 0, eventCount = 0, suspiciousEventCount = 0;
        while ((line = lr.readLine()) != null) {
            try {
                LogLine sl = new LogLine((BugReportModule) mod, line, fmt, prev);
                count++;
                if (sl.ok) {
                    okCount++;
                    fmt = sl.fmt;
                    // We need to guess if it's an event log
                    int cat = canBeEventLog(sl.msg);
                    if (cat > 0) {
                        eventCount++;
                        // Note: we might get some false positives, like lines which are long,
                        // and contain no comma. Count these as well
                        if (cat == 1) {
                            suspiciousEventCount++;
                        }
                    }
                }
            } catch (Exception e) {
                // The log line might be trunkated, so expect all kind of weird errors
                // We just stop processing when such error happens
                break;
            }
        }
        if (okCount > 5 && okCount > count * 0.75f) {
            // We got a match, the only thing left is to detect if it's the event log or system log
            // NOTE: one bad line is allowed, since the last line might be cropped
            int cert = okCount * 99 / count;
            if (eventCount >= okCount - 1 && (suspiciousEventCount < count * 0.15f)) {
                type.set(Section.EVENT_LOG, cert);
            } else {
                type.set(Section.SYSTEM_LOG, cert);
            }
        }
    }

    private int canBeEventLog(String msg) {
        // if it starts and end with [ ], then it's probably an event log
        if (msg.startsWith("[") && msg.endsWith("]")) return 2;
        // if not, then it shouldn't contain any comma
        if (msg.indexOf(',') >= 0) return 0;
        // if it has one field, it shouldn't be too long
        if (msg.length() < 32) return 2;
        // we cannot say it for 100%, so it's supicious
        return 1;
    }

}
