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
package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.SysPropsPlugin;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;

public abstract class LogPlugin extends Plugin {

    public static final String TAG = "[LogPlugin]";

    private static final long DAY = 24 * 60 * 60 * 1000;

    private HashMap<Integer,ProcessLog> mLogs = new HashMap<Integer, ProcessLog>();

    private long mTsFirst = -1;
    private long mTsLast = -1;

    private String mWhich;
    private String mId;
    private String mSectionName;

    private HashMap<Integer,GCRecords> mGCs = new HashMap<Integer, GCRecords>();
    private Vector<LogLine> mParsedLog = new Vector<LogLine>();
    private Vector<ConfigChange> mConfigChanges = new Vector<ConfigChange>();

    private boolean mLoaded = false;

    private Section mSection;
    private Chapter mCh;

    public LogPlugin(String which, String id, String sectionName) {
        mWhich = which;
        mId = id;
        mSectionName = sectionName;
    }

    public Chapter getChapter() {
        return mCh;
    }

    public Vector<LogLine> getLogs() {
        return mParsedLog;
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
        mCh = new Chapter(br, mWhich + " log");
        int cnt = mSection.getLineCount();
        int fmt = LogLine.FMT_UNKNOWN;
        LogLine prev = null;
        int skippedDueToTimeJump = 0;
        for (int i = 0; i < cnt; i++) {
            String line = mSection.getLine(i);
            LogLine sl = new LogLine(br, line, fmt, prev);

            if (sl.ok) {
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
        cnt = mParsedLog.size();
        if (cnt > 0) {
            mTsFirst = mParsedLog.get(0).ts;
            mTsLast = mParsedLog.get(cnt - 1).ts;
        }

        // Check for timestamp order
        int orderErrors = 0;
        Vector<LogLine> errLines = new Vector<LogLine>();
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
            Bug bug = new Bug(Bug.PRIO_INCORRECT_LOG_ORDER, 0, "Incorrect timestamp order in " + mSectionName);
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
                log.add(ll.copy());
                if (!first) {
                    log.add("...");
                }
            }
            br.addBug(bug);
        }
        if (orderErrors > 0) {
            Bug bug = new Bug(Bug.PRIO_LOG_TIMEJUMP, 0, "Huge time gap in " + mSectionName);
            bug.add(new Block()
                .add("There was at least one huge time gap (at least one day) in the log. The lines before the last time gap ("
                    + skippedDueToTimeJump + " lines) have been skipped in the ")
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
        Chapter chGC = new Chapter(br, "GC graphs");
        if (generateGCGraphs(br, chGC) > 0) {
            mCh.addChapter(chGC);
        }

        // Generate other extra sections
        generateExtra(br, mCh);

        br.addChapter(mCh);
    }

    private Chapter generateLog(BugReportModule br) {
        Chapter ch = new Chapter(br, "Log");
        DocNode log = new Block().addStyle("log");
        ch.add(log);

        int cnt = mParsedLog.size();
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mParsedLog.get(i);
            if (sl.ok) {
                ProcessLog pl = getLogOf(br, sl.pid);
                pl.add(sl.copy());
            }
            log.add(sl);
        }
        return ch;
    }

    private void generateSpamTopList(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Spam top list");
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
            int pid = pl.mPid;
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
        // NOP
    }

    protected ProcessLog getLogOf(BugReportModule br, int pid) {
        ProcessLog log = mLogs.get(pid);
        if (log == null) {
            log = new ProcessLog(br, pid);
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
        int w = 800;
        int h = 300;
        int cx = 100;
        int cy = 250;
        int gw = 600;
        int gh = 200;

        int pid = gcs.get(0).pid;
        long firstTs = getFirstTs();
        long duration = (getLastTs() - firstTs);
        int heapLimit = 32;
        if (duration <= 0) return false;

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

        // Create an empty image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, w - 1, h - 1);

        // Draw the axis
        int as = 5;
        g.setColor(Color.BLACK);
        g.drawLine(cx, cy, cx, cy - gh);
        g.drawLine(cx, cy, cx + gw, cy);
        g.drawLine(cx - as, cy - gh + as, cx, cy - gh);
        g.drawLine(cx + as, cy - gh + as, cx, cy - gh);
        g.drawLine(cx + gw - as, cy - as, cx + gw, cy);
        g.drawLine(cx + gw - as, cy + as, cx + gw, cy);

        // Draw the title
        FontMetrics fm = g.getFontMetrics();
        String procName = "";
        ProcessRecord pr = br.getProcessRecord(pid, false, false);
        if (pr != null) {
            procName = pr.getName();
        } else {
            procName = Integer.toString(pid);
        }
        g.drawString("Memory after GC in process " + procName, 10, 10 + fm.getAscent());

        // Draw the duration
        String dur;
        if (duration < 5*1000) {
            dur = String.format("%dms", duration);
        } else if (duration < 5*60*1000) {
            dur = String.format("%.1fs", duration / 1000.0f);
        } else if (duration < 5*60*60*1000) {
            dur = String.format("%.1fmin", duration / 60000.0f);
        } else {
            dur = String.format("%.1fh", duration / 3600000.0f);
        }
        dur = "Log length: " + dur;
        g.drawString(dur, w - 10 - fm.stringWidth(dur), 10 + fm.getAscent());

        // Collect the maximum value
        int max = 0;
        for (GCRecord gc : gcs) {
            int total = gc.memExtSize + gc.memFreeSize;
            if (total > max) {
                max = total;
            }
        }
        max = max * 110 / 100; // add 10% for better visibility

        // Draw some guide lines
        int count = 5;
        int step = max / count;
        step = (step + 249) / 250 * 250;
        Color colGuide = new Color(0xc0c0ff);
        for (int i = 1; i <= count; i++) {
            int value = i * step;
            if (value > max) break;
            int yv = cy - value * gh / max;
            g.setColor(colGuide);
            g.drawLine(cx + 1, yv, cx + gw, yv);
            g.setColor(Color.BLACK);
            String s = "" + value + "K";
            g.drawString(s, cx - fm.stringWidth(s) - 1, yv);
        }

        // Draw the config changes (useful to see the correlation between config changes and memory usage)
        Color colConfigChange = Color.LIGHT_GRAY;
        g.setColor(colConfigChange);
        int ccCnt = 0;
        for (ConfigChange cc : mConfigChanges) {
            long ts = cc.ts;
            if (ts < firstTs) continue; // skip one, this doesn't count
            int x = cx + (int)((ts - firstTs) * (gw - 1) / (duration));
            g.drawLine(x, cy - 1, x, cy - gh);
            ccCnt++;
        }

        // Draw the heap limit line (if visible)
        int ylimit = (heapLimit*1024) * gh / max;
        if (ylimit < gh) {
            int yv = cy - ylimit;
            g.setColor(Color.BLACK);
            g.drawLine(cx + 1, yv, cx + gw, yv);
            g.drawString("" + heapLimit + "MB", cx + gw + 5, yv);
        }

        // Plot the values (size)
        Color colFreeSize = new Color(0xc0c080);
        Color colTotalSize = new Color(0x8080d7);
        int lastX = -1, lastYF = -1, lastYT = -1;
        int r = 3;
        for (GCRecord gc : gcs) {
            int yf = cy - gc.memFreeSize * (gh - 1) / max;
            int x = cx + (int)((gc.ts - getFirstTs()) * (gw - 1) / duration);
            g.setColor(colFreeSize);
            if (lastX != -1) {
                g.drawLine(lastX, lastYF, x, yf);
            }
            g.fillArc(x - r, yf - r, 2*r+1, 2*r+1, 0, 360);
            lastYF = yf;
            if (hasExternal) {
                int yt = cy - (gc.memFreeSize + gc.memExtSize) * (gh - 1) / max;
                g.setColor(colTotalSize);
                if (lastX != -1) {
                    g.drawLine(lastX, lastYT, x, yt);
                }
                g.fillArc(x - r, yt - r, 2*r+1, 2*r+1, 0, 360);
                lastYT = yt;
            }
            lastX = x;
        }

        // Plot the values (alloc)
        Color colFreeAlloc = new Color(0x808000);
        Color colTotalAlloc = new Color(0x0000c0);
        lastX = -1; lastYF = -1; lastYT = -1;
        for (GCRecord gc : gcs) {
            int yf = cy - gc.memFreeAlloc * (gh - 1) / max;
            int x = cx + (int)((gc.ts - firstTs) * (gw - 1) / (duration));
            g.setColor(colFreeAlloc);
            if (lastX != -1) {
                g.drawLine(lastX, lastYF, x, yf);
            }
            g.fillArc(x - r, yf - r, 2*r+1, 2*r+1, 0, 360);
            lastYF = yf;
            if (hasExternal) {
                int yt = cy - (gc.memFreeAlloc + gc.memExtAlloc) * (gh - 1) / max;
                g.setColor(colTotalAlloc);
                if (lastX != -1) {
                    g.drawLine(lastX, lastYT, x, yt);
                }
                g.fillArc(x - r, yt - r, 2*r+1, 2*r+1, 0, 360);
                lastYT = yt;
            }
            lastX = x;
        }

        // Plot the values (alloc)
        Color colTotalAllocO = new Color(0xff4040);
        if (hasExternal) {
            lastX = -1; lastYF = -1; lastYT = -1;
            for (GCRecord gc : gcs) {
                int yt = cy - (gc.memFreeSize + gc.memExtAlloc) * (gh - 1) / max;
                int x = cx + (int)((gc.ts - firstTs) * (gw - 1) / (duration));
                g.setColor(colTotalAllocO);
                if (lastX != -1) {
                    g.drawLine(lastX, lastYT, x, yt);
                }
                g.fillArc(x - r, yt - r, 2*r+1, 2*r+1, 0, 360);
                lastX = x;
                lastYT = yt;
            }
        }

        // Draw the legend
        int yl = h - 10 - fm.getDescent();
        String s = "VM Heap (alloc)";
        g.setColor(colFreeAlloc);
        g.drawString(s, w * 1 / 4 - fm.stringWidth(s)/2, yl);
        if (hasExternal) {
            s = "VM Heap + External (alloc)";
            g.setColor(colTotalAlloc);
            g.drawString(s, w * 2 / 4 - fm.stringWidth(s)/2, yl);
            s = "Mem footprint";
            g.setColor(colTotalAllocO);
            g.drawString(s, w * 3 / 4 - fm.stringWidth(s)/2, yl);
        }
        yl -= fm.getHeight();
        s = "VM Heap (size)";
        g.setColor(colFreeSize);
        g.drawString(s, w * 1 / 4 - fm.stringWidth(s)/2, yl);
        if (hasExternal) {
            s = "VM Heap + External (size)";
            g.setColor(colTotalSize);
            g.drawString(s, w * 2 / 4 - fm.stringWidth(s)/2, yl);
        }
        if (ccCnt > 0) {
            // Draw legend for config changes
            s = "| Config changes";
            g.setColor(colConfigChange);
            g.drawString(s, w * 3 / 4 - fm.stringWidth(s)/2, yl);
        }

        // Save the image
        String fn = "gc_" + mId + "_" + pid + ".png";
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Append a link at the end of the system log
        ch.add(new Img(fn));

        // Also insert a link at the beginning of the per-process log
        ProcessLog pl = getLogOf(br, pid);
        pl.add(new Img(fn));

        // And also add it to the process record
        if (pr != null) {
            new Para(pr)
                .add("Memory usage from GC " + mId + " logs:")
                .add(new Img(fn));
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

    public static class GCRecord {

        public long ts;
        public int pid;
        public int memFreeAlloc;
        public int memExtAlloc;
        public int memFreeSize;
        public int memExtSize;

        public GCRecord(long ts, int pid, int memFreeAlloc, int memFreeSize, int memExtAlloc, int memExtSize) {
            this.ts = ts;
            this.pid = pid;
            this.memFreeAlloc = memFreeAlloc;
            this.memExtAlloc = memExtAlloc;
            this.memFreeSize = memFreeSize;
            this.memExtSize = memExtSize;
        }
    }

    public static class GCRecords extends Vector<GCRecord> {
        private static final long serialVersionUID = 1L;
    }

    class ProcessLog extends Chapter {

        private int mPid;
        private int mLines;

        public ProcessLog(Module mod, int pid) {
            super(mod, String.format(mId + "log_%05d.html", pid));
            mPid = pid;
        }

        public int getPid() {
            return mPid;
        }

        public void add(LogLineBase ll) {
            // LogLines should never be added directly here
            // or else the anchors will be mixed up!
            Util.assertTrue(false);
        }

        public void add(LogLineBase.LogLineProxy ll) {
            super.add(ll);
            mLines++;
        }

        public int getLineCount() {
            return mLines;
        }

    }

    public static class ConfigChange {
        public long ts;

        public ConfigChange(long ts) {
            this.ts = ts;
        }
    }

}
