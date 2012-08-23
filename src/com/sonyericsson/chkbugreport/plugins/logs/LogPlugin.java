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

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Lines;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.plugins.SysPropsPlugin;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;

public abstract class LogPlugin extends Plugin {

    public static final String TAG = "[LogPlugin]";

    private static final String PROCESS_LOG_HEADER =
        "<html>\n" +
        "<head>\n" +
        "  <title>{0} log filter by pid {1,number,#####}</title>\n" +
        "  <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/>\n" +
        "</head>\n" +
        "<body>\n" +
        "<h1>{0} log filter by pid {1,number,#####}</h1>\n" +
        "<div class=\"log\">\n";
    private static final String PROCESS_LOG_FOOTER =
        "</div>\n" +
        "</body>\n" +
        "</html>\n";

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

    @Override
    public void load(Report rep) {
        BugReport br = (BugReport)rep;

        // Reset previous data
        mTsFirst = -1;
        mTsLast = -1;
        mGCs.clear();
        mParsedLog.clear();
        mLogs.clear();
        mLoaded = false;
        mSection = null;
        mCh = null;
        mConfigChanges.clear();

        // Load the data
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
        for (int i = 0; i < cnt; i++) {
            String line = mSection.getLine(i);
            LogLine sl = new LogLine(br, line, fmt, prev);
            mParsedLog.add(sl);

            if (sl.ok) {
                fmt = sl.fmt;
                if (mTsFirst == -1) {
                    mTsFirst = sl.ts;
                }
                mTsLast = sl.ts;
                prev = sl;
            }
        }

        // Analyze the log
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mParsedLog.get(i);
            if (sl.ok) {
                // Analyze the log line
                analyze(sl, i, br, mSection);
            }
        }

        // Load successful
        mLoaded = true;
    }

    @Override
    public void generate(Report rep) {
        BugReport br = (BugReport)rep;
        if (!mLoaded) {
            return;
        }

        mCh.addLine("<div class=\"log\">");

        int cnt = mSection.getLineCount();
        for (int i = 0; i < cnt; i++) {
            LogLine sl = mParsedLog.get(i);
            if (sl.ok) {
                ProcessLog pl = getLogOf(br, sl.pid);
                for (String prefix : sl.prefixes) {
                    pl.addLine(prefix);
                }
                pl.addLine(sl.htmlLite);
            }
            mCh.addLine("<a name=\"" + getAnchorToLine(i) + "\"></a>");
            for (String prefix : sl.prefixes) {
                mCh.addLine(prefix);
            }
            mCh.addLine(sl.html);
        }

        mCh.addLine("</div>");

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

        // Save the process logs
        saveLogs(br);
    }

    private void generateSpamTopList(BugReport br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Spam top list");
        mainCh.addChapter(ch);
        ch.addLine("<p>Processes which produced most of the log:</p>");

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
        ch.addLine("<table class=\"logspam-stat\">");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"logspam-header\">");
        ch.addLine("    <th>Process</td>");
        ch.addLine("    <th>Pid</td>");
        ch.addLine("    <th>Nr. of lines</td>");
        ch.addLine("    <th>% of all log</td>");
        ch.addLine("  </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");

        int cnt = Math.min(10, vec.size());
        int totLines = mParsedLog.size();
        for (int i = 0; i < cnt; i++) {
            ProcessLog pl = vec.get(i);
            int pid = pl.mPid;
            String prA0 = "", prA1 = "";
            String procName = "";
            ProcessRecord pr = br.getProcessRecord(pid, false, false);
            if (pr != null) {
                prA0 = "<a href=\"" + br.createLinkToProcessRecord(pid) + "\">";
                prA1 = "</a>";
                procName = pr.getName();
            }
            ch.addLine("  <tr>");
            ch.addLine("    <td>" + prA0 + procName + prA1 + "</td>");
            ch.addLine("    <td>" + prA0 + pid + prA1 + "</td>");
            ch.addLine("    <td>" + pl.getLineCount() + "</td>");
            ch.addLine("    <td>" + String.format("%.1f%%", (pl.getLineCount() * 100.0f / totLines)) + "</td>");
            ch.addLine("  </tr>");
        }

        ch.addLine("  </tbody>");
        ch.addLine("</table>");
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

    protected void generateExtra(BugReport br, Chapter ch) {
        // NOP
    }

    protected void analyze(LogLine sl, int i, BugReport br, Section s) {
        // NOP
    }

    protected ProcessLog getLogOf(BugReport br, int pid) {
        ProcessLog log = mLogs.get(pid);
        if (log == null) {
            log = new ProcessLog(pid);
            mLogs.put(pid, log);

            // Add link from global process record
            ProcessRecord pr = br.getProcessRecord(pid, true, true);
            if (pr != null) {
                pr.beginBlock();
                pr.addLine("<a href=\"" + br.getRelDataDir() + log.getName() + "\">");
                pr.addLine(mWhich + " log (filtered by this process) &gt;&gt;&gt;");
                pr.addLine("</a>");
                pr.endBlock();
            }

        }
        return log;
    }

    private void saveLogs(BugReport br) {
        try {
            for (ProcessLog log : mLogs.values()) {
                FileOutputStream fos = new FileOutputStream(br.getDataDir() + log.getName());
                PrintStream ps = new PrintStream(fos);
                ps.println(MessageFormat.format(PROCESS_LOG_HEADER, mWhich, log.getPid()));
                int cnt = log.getLineCount();
                for (int i = 0; i < cnt; i++) {
                    ps.println(log.getLine(i));
                }
                ps.println(PROCESS_LOG_FOOTER);
                ps.println("<html>");
                ps.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected long getFirstTs() {
        return mTsFirst;
    }

    protected long getLastTs() {
        return mTsLast;
    }

    private int generateGCGraphs(BugReport br, Chapter ch) {
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

    private boolean generateGCGraph(BugReport br, Chapter ch, GCRecords gcs) {
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
            ImageIO.write(img, "png", new File(br.getBaseDir() + br.getRelDataDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Append a link at the end of the system log
        ch.addLine("<div><img src=\"" + br.getRelDataDir() + fn + "\"/></div>");

        // Also insert a link at the beginning of the per-process log
        ProcessLog pl = getLogOf(br, pid);
        pl.addLine("<div><img src=\"" + fn + "\"/></div>", 0);

        // And also add it to the process record
        if (pr != null) {
            pr.addLine("<p>Memory usage from GC " + mId + " logs:");
            pr.addLine("<div><img src=\"" + br.getRelDataDir() + fn + "\"/></div>");
            pr.addLine("</p>");
        }

        return true;
    }

    protected void addActivityLaunchMarker(LogLine sl, String activity) {
        String cmp[] = activity.split("/");
        if (cmp.length == 2) {
            if (cmp[1].startsWith(cmp[0])) {
                cmp[1] = cmp[1].substring(cmp[0].length());
            }
            sl.addMarker("log-float", "style=\"font-size: 75%\"", cmp[0] + "<br/>" + cmp[1], null);
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

    class ProcessLog extends Lines {

        private int mPid;

        public ProcessLog(int pid) {
            super(String.format(mId + "log_%05d.html", pid));
            mPid = pid;
        }

        public int getPid() {
            return mPid;
        }

    }

    public static class ConfigChange {
        public long ts;

        public ConfigChange(long ts) {
            this.ts = ts;
        }
    }

}
