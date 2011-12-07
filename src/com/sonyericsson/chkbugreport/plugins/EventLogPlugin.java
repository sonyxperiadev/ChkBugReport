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

import com.sonyericsson.chkbugreport.Bug;
import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
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
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;

public class EventLogPlugin extends LogPlugin {

    private static final int[] MLL_LEVELS_MAX = { 100, 500 };
    private static final int[] MLL_LEVELS_AVG = { 10, 33 };
    private static final int[] ALT_LEVELS = { 250, 1000 };

    private static final int TAG_DVM_LOCK_SAMPLE = 20003;
    private static final int TAG_DB_SAMPLE = 52000;
    private static final int TAG_CONTENT_QUERY_SAMPLE = 52002;
    private static final int TAG_CONTENT_UPDATE_SAMPLE = 52003;
    private static final int TAG_BINDER_SAMPLE = 52004;

    private HashMap<String, MLLStat> mMLL = new HashMap<String, EventLogPlugin.MLLStat>();
    private Vector<ALTStat> mALT = new Vector<ALTStat>();
    /* db_sample stats */
    private HashMap<String, DBStat> mDBStats = new HashMap<String, EventLogPlugin.DBStat>();
    /* content_query_sample stats */
    private HashMap<String, DBStat> mCQStats = new HashMap<String, EventLogPlugin.DBStat>();
    /* content_update_sample stats */
    private HashMap<String, DBStat> mCUStats = new HashMap<String, EventLogPlugin.DBStat>();
    /* content_query_sample + content_update_stats */
    private HashMap<String, DBStat> mCTStats = new HashMap<String, EventLogPlugin.DBStat>();
    /* Activity manager events */
    private Vector<AMData> mAMDatas = new Vector<AMData>();
    /* The next fake pid which can be allocated */
    private int mNextFakePid = 100000;
    /* The collection of *_sample data */
    private HashMap<String, Vector<SampleData>> mSDs = new HashMap<String, Vector<SampleData>>();

    public EventLogPlugin() {
        super("Event", "event", Section.EVENT_LOG);
    }

    @Override
    public int getPrio() {
        return 32;
    }

    @Override
    public void load(Report br) {
        mMLL.clear();
        mALT.clear();
        mDBStats.clear();
        mCQStats.clear();
        mCUStats.clear();
        mCTStats.clear();
        mAMDatas.clear();
        mNextFakePid = 100000;
        mSDs.clear();
        super.load(br);
    }

    @Override
    protected void generateExtra(BugReport rep, Chapter ch) {
        BugReport br = (BugReport)rep;

        // Collect *_sample data in statistics
        collectSampleStats();

        // Finish sub-chapters
        finishMainLoopLatency(br, ch);
        finishActivityLaunchTime(br, ch);
        finishDBStats(br, ch);
        generateAMGraphs(br, ch);
        generateSampleDataGraphs(br, ch);
    }

    @Override
    protected void analyze(LogLine sl, int i, BugReport br, Section s) {
        String eventType = Util.strip(sl.tag);
        if (sl.fmt == LogLine.FMT_CRASH) {
            // Crash is too smart, it also parses the logs, so we need different method for analyzes
            if (sl.tagId == TAG_DVM_LOCK_SAMPLE) {
                addDvmLockSampleDataC(br, "dvm_lock_sample", sl);
            } else if (sl.tagId == TAG_DB_SAMPLE) {
                addGenericSampleDataC(br, "db_sample", sl);
            } else if (sl.tagId == TAG_CONTENT_QUERY_SAMPLE) {
                addGenericSampleDataC(br, "content_query_sample", sl);
            } else if (sl.tagId == TAG_CONTENT_UPDATE_SAMPLE) {
                addGenericSampleDataC(br, "content_update_sample", sl);
            } else if (sl.tagId == TAG_BINDER_SAMPLE) {
                addGenericSampleDataC(br, "binder_sample", sl);
            }
        } else {
            if (eventType.endsWith("_sample")) {
                addSampleData(br, eventType, sl);
                // Fall through: some of the sample data is handled more then once
            }
            if ("am_anr".equals(eventType)) {
                analyzeCrashOrANR(sl, i, br, "anr");
                // Fall through: am_ logs are processed again
            }
            if ("am_crash".equals(eventType)) {
                analyzeCrashOrANR(sl, i, br, "crash");
                // Fall through: am_ logs are processed again
            }
            if ("main_loop_latency".equals(eventType)) {
                addMainLoopLatencyData(sl.fields, i);
            } else if ("activity_launch_time".equals(eventType)) {
                addActivityLaunchTimeData(sl.fields, i);
                addActivityLaunchMarker(sl);
            } else if (eventType.startsWith("am_")) {
                addAMData(eventType, br, sl, i);
            } else if ("dvm_gc_info".equals(eventType)) {
                addDvmGCInfoData(sl);
            } else if ("configuration_changed".equals(eventType)) {
                handleConfigChanged(sl);
            }
        }
    }

    private void addActivityLaunchMarker(LogLine sl) {
        if (sl.fields.length == 4) {
            addActivityLaunchMarker(sl, sl.fields[1]);
        }
    }

    private void handleConfigChanged(LogLine sl) {
        // Collect the info for GC graph
        ConfigChange cc = new ConfigChange(sl.ts);
        addConfigChange(cc);

        // Create a marker in the log
        try {
            int changed = Integer.parseInt(sl.msg);
            StringBuffer sb = new StringBuffer();
            if (0 != (changed & 0x0001)) {
                sb.append("MCC<br/>");
            }
            if (0 != (changed & 0x0002)) {
                sb.append("MNC<br/>");
            }
            if (0 != (changed & 0x0004)) {
                sb.append("Locale<br/>");
            }
            if (0 != (changed & 0x0008)) {
                sb.append("Touch<br/>");
            }
            if (0 != (changed & 0x0010)) {
                sb.append("Keyb.<br/>");
            }
            if (0 != (changed & 0x0020)) {
                sb.append("HW Kb<br/>");
            }
            if (0 != (changed & 0x0040)) {
                sb.append("Navig.<br/>");
            }
            if (0 != (changed & 0x0080)) {
                sb.append("Orient.<br/>");
            }
            if (0 != (changed & 0x0100)) {
                sb.append("Layout<br/>");
            }
            if (0 != (changed & 0x0200)) {
                sb.append("UI Mode<br/>");
            }
            if (0 != (changed & 0x0400)) {
                sb.append("Theme<br/>");
            }
            if (0 != (changed & 0x40000000)) {
                sb.append("Font<br/>");
            }
            sl.addMarker("log-float-icon", "style=\"font-size: 75%\"", sb.toString(), null);
        } catch (NumberFormatException nfe) { /* NOP */ }
    }

    private void analyzeCrashOrANR(LogLine sl, int i, BugReport br, String type) {
        // Put a marker box
        String cType = type.toUpperCase();
        String anchor = getId() + "elog_" + type + "_" + i;
        sl.addMarker("log-float-err", null, "<a name=\"" + anchor + "\">" + cType + "</a>", cType);

        // Create a bug and store the relevant log lines
        String msg = null;
        int pid = -1;
        if (sl.fields.length < 4) {
            // Strange... let's use the log message completely
            msg = cType + ": " + sl.msg;
        } else {
            msg = cType + " in '" + sl.fields[1] + "' (" + sl.fields[0] + ")";
            try {
                pid = Integer.parseInt(sl.fields[0]);
            } catch (NumberFormatException nfe) { /* NOP */ }
        }
        int prio = type.equals("anr") ? Bug.PRIO_ANR_EVENT_LOG : Bug.PRIO_JAVA_CRASH_EVENT_LOG;
        Bug bug = new Bug(prio, sl.ts, msg);
        bug.setAttr("firstLine", i);
        bug.addLine("<div><a href=\"" + br.createLinkTo(getChapter(), anchor) + "\">(link to event log)</a></div>");
        if (pid != -1) {
            bug.addLine("<div><a href=\"" + br.createLinkToProcessRecord(pid) + "\">(link to process record)</a></div>");
        }
        bug.addLine("<div class=\"log\">");
        bug.addLine(sl.html);
        bug.addLine("</div>");
        bug.addLine("<pre>");
        if (sl.fields.length >= 4) {
            bug.setAttr("pid", sl.fields[0]);
            bug.setAttr("package", sl.fields[1]);
            bug.setAttr("reason", sl.fields[3]);

            // Print some additional info
            int flags = -1;
            try {
                flags = Integer.parseInt(sl.fields[2]);
            } catch (NumberFormatException nfe) { /* NOP */ }
            bug.addLine("PID:            " + sl.fields[0]);
            bug.addLine("Package:        " + sl.fields[1]);
            bug.addLine("Reason:         " + sl.fields[3]);
            bug.addLine("Flags:          0x" + Integer.toHexString(flags) + ":");
            bug.addLine("  - SYSTEM:                    " + (0 != (flags & (1 << 0))));
            bug.addLine("  - DEBUGGABLE:                " + (0 != (flags & (1 << 1))));
            bug.addLine("  - HAS_CODE:                  " + (0 != (flags & (1 << 2))));
            bug.addLine("  - PERSISTENT:                " + (0 != (flags & (1 << 3))));
            bug.addLine("  - FACTORY TEST:              " + (0 != (flags & (1 << 4))));
            bug.addLine("  - ALLOW TASK REPARENTING:    " + (0 != (flags & (1 << 5))));
            bug.addLine("  - ALLOW CLEAR USERDATA:      " + (0 != (flags & (1 << 6))));
            bug.addLine("  - UPDATED SYSTEM APP:        " + (0 != (flags & (1 << 7))));
            bug.addLine("  - TEST ONLY:                 " + (0 != (flags & (1 << 8))));
            bug.addLine("  - SUPPORTS SMALL SCREENS:    " + (0 != (flags & (1 << 9))));
            bug.addLine("  - SUPPORTS NORMAL SCREENS:   " + (0 != (flags & (1 << 10))));
            bug.addLine("  - SUPPORTS LARGE SCREENS:    " + (0 != (flags & (1 << 11))));
            bug.addLine("  - SUPPORTS XLARGE SCREENS:   " + (0 != (flags & (1 << 19))));
            bug.addLine("  - RESIZEABLE FOR SCREENS:    " + (0 != (flags & (1 << 12))));
            bug.addLine("  - SUPPORTS SCREEN DENSITIES: " + (0 != (flags & (1 << 13))));
            bug.addLine("  - VM SAFE MODE:              " + (0 != (flags & (1 << 14))));
            bug.addLine("  - ALLOW BACKUP:              " + (0 != (flags & (1 << 15))));
            bug.addLine("  - KILL AFTER RESTORE:        " + (0 != (flags & (1 << 16))));
            bug.addLine("  - RESTORE ANY VERSION:       " + (0 != (flags & (1 << 17))));
            bug.addLine("  - EXTERNAL STORAGE:          " + (0 != (flags & (1 << 18))));
            bug.addLine("  - CANT SAVE STATE:           " + (0 != (flags & (1 << 27))));
            bug.addLine("  - FORWARD LOCK:              " + (0 != (flags & (1 << 29))));
            bug.addLine("  - NEVER ENCRYPT:             " + (0 != (flags & (1 << 30))));
        }
        bug.addLine("</pre>");
        br.addBug(bug);
    }

    private void addDvmGCInfoData(LogLine sl) {
        if (sl.fields.length != 4) return;
        try {
//            long l0 = Long.parseLong(sl.fields[0]);
            long l1 = Long.parseLong(sl.fields[1]);
//            long l2 = Long.parseLong(sl.fields[2]);
            long l3 = Long.parseLong(sl.fields[3]);

//            int gcTime = unFloat12(Util.bits(l0, 23, 12));
//            int bytesFreed = unFloat12(Util.bits(l0, 11, 0));

//            int objsFreed = unFloat12(Util.bits(l1, 59, 48));
            int agrActSize = unFloat12(Util.bits(l1, 47, 36));
//            int agrAwdSize = unFloat12(Util.bits(l1, 35, 24));
//            int agrObjsAlloc = unFloat12(Util.bits(l1, 23, 12));
            int agrBytesAlloc = unFloat12(Util.bits(l1, 11, 0));

//            int softLimit = unFloat12(Util.bits(l2, 59, 48));
//            int zygActSize = unFloat12(Util.bits(l2, 47, 36));
//            int zygAwdSize = unFloat12(Util.bits(l2, 35, 24));
//            int zygObjsAlloc = unFloat12(Util.bits(l2, 23, 12));
//            int zygBytesAlloc = unFloat12(Util.bits(l2, 11, 0));

            // int footprint = unFloat12(Util.bits(l3, 47, 36)); // Disabled
            // int mallinfo = unFloat12(Util.bits(l3, 35, 24)); // Disabled
            int extLim = unFloat12(Util.bits(l3, 23, 12));
            int extAlloc = unFloat12(Util.bits(l3, 11, 0));

            GCRecord gc = new GCRecord(sl.ts, sl.pid, agrBytesAlloc / 1024, agrActSize / 1024, extAlloc / 1024, extLim / 1024);
            addGCRecord(sl.pid, gc);
        } catch (NumberFormatException nfe) {
            // Just ignore it, it's not the end of the world
        }
    }

    private int unFloat12(long value) {
        return (int)((value & 0x1ff) << ((value >> 9) * 4));
    }

    private void addSampleData(Report br, String eventType, LogLine sl) {
        int fieldCount = sl.fields.length;
        if (fieldCount < 4) return; // cannot handle these
        try {
            int duration = 0, perc = 0;
            String name = null;
            if (eventType.equals("dvm_lock_sample")) {
                // This is a bit different
                duration = Integer.parseInt(sl.fields[fieldCount-2]);
                perc = Integer.parseInt(sl.fields[fieldCount-1]);
                name = sl.fields[0];
            } else {
                duration = Integer.parseInt(sl.fields[fieldCount-3]);
                perc = Integer.parseInt(sl.fields[fieldCount-1]);
                name = sl.fields[0];
                name = fixSampleDataName(name);
            }
            SampleData sd = new SampleData(sl.ts, sl.pid, name, duration, perc);
            addSampleData(eventType, sd);
        } catch (NumberFormatException e) {
            br.printErr(TAG + "addSampleData(eventType=" + eventType + "):" + e);
        }
    }

    private String fixSampleDataName(String name) {
        if (name.startsWith("content://")) {
            name = name.substring(10);

            // Need to optimize this, otherwise there will be too many values
            int idx = name.indexOf('/', 10);
            if (idx > 0) {
                name = name.substring(0, idx);
            }
        }
        return name;
    }

    private void addSampleData(String eventType, SampleData sd) {
        Vector<SampleData> sds = mSDs.get(eventType);
        if (sds == null) {
            sds = new Vector<SampleData>();
            mSDs.put(eventType, sds);
        }
        sds.add(sd);
    }

    private void addDvmLockSampleDataC(Report br, String eventType, LogLine sl) {
        int fieldCount = sl.fields.length;
        if (fieldCount < 4) return; // cannot handle these
        try {
            int duration = extractIntValueFromCrashLogField(sl.fields[fieldCount-2]);
            int perc = extractIntValueFromCrashLogField(sl.fields[fieldCount-1]);
            String name = extractValueFromCrashLogField(sl.fields[0]);
            SampleData sd = new SampleData(sl.ts, sl.pid, name, duration, perc);
            addSampleData(eventType, sd);
        } catch (NumberFormatException e) {
            br.printErr(TAG + "addSampleData(eventType=" + eventType + "):" + e);
        }
    }

    private void addGenericSampleDataC(Report br, String eventType, LogLine sl) {
        int fieldCount = sl.fields.length;
        if (fieldCount < 4) return; // cannot handle these
        try {
            int duration = extractIntValueFromCrashLogField(sl.fields[fieldCount-3]);
            int perc = extractIntValueFromCrashLogField(sl.fields[fieldCount-1]);
            String name = extractValueFromCrashLogField(sl.fields[0]);
            name = fixSampleDataName(name);
            SampleData sd = new SampleData(sl.ts, sl.pid, name, duration, perc);
            addSampleData(eventType, sd);
        } catch (NumberFormatException e) {
            br.printErr(TAG + "addSampleData(eventType=" + eventType + "):" + e);
        }
    }

    private String extractValueFromCrashLogField(String s) {
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
        }
        int idx = s.indexOf('=');
        if (idx > 0) {
            if (canBeKeyword(s.substring(0, idx))) {
                s = s.substring(idx + 1);
            }
        }
        return s;
    }

    private int extractIntValueFromCrashLogField(String s) {
        s = extractValueFromCrashLogField(s).toLowerCase();
        int idx = s.indexOf(' ');
        if (idx > 0) {
            s = s.substring(0, idx);
        }
        if (s.startsWith("0x")) {
            return Integer.parseInt(s.substring(2), 16);
        } else {
            return Integer.parseInt(s);
        }
    }

    private boolean canBeKeyword(String s) {
        int l = s.length();
        for (int i = 0; i < l; i++) {
            char c = s.charAt(i);
            boolean ok = false;
            if (c >= 'a' && c <= 'z') {
                ok = true;
            } else if (c >= 'A' && c <= 'Z') {
                ok = true;
            } else if (c >= '0' && c <= '9') {
                ok = true;
            } else if (c == '_') {
                ok = true;
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private void addAMData(String eventType, BugReport br, LogLine sl, int i) {
        try {
            addAMDataUnsafe(eventType, br, sl, i);
        } catch (Exception e) {
            br.printErr("Error parsing AM data from event log at line " + (i + 1));
        }
    }

    private void addAMDataUnsafe(String eventType, BugReport br, LogLine sl, int i) {
        if ("am_on_paused_called".equals(eventType)) {
            addAMData(new AMData(AMData.PAUSED, sl.pid, sl.getFields(0), sl.ts));
            suggestName(br, sl, 0, 15);
        } else if ("am_on_resume_called".equals(eventType)) {
            addAMData(new AMData(AMData.RESUMED, sl.pid, sl.getFields(0), sl.ts));
            suggestName(br, sl, 0, 15);
        } else if ("am_create_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_CREATE, -1, sl.getFields(2), sl.ts));
        } else if ("am_restart_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_RESTART, -1, sl.getFields(2), sl.ts));
        } else if ("am_destroy_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_DESTROY, -1, sl.getFields(2), sl.ts));
        } else if ("am_pause_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_PAUSE, -1, sl.getFields(1), sl.ts));
        } else if ("am_resume_activity".equals(eventType)) {
            addAMData(new AMData(AMData.ON_RESUME, -1, sl.getFields(2), sl.ts));
        } else if ("am_proc_start".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[0]);
            addAMData(new AMData(AMData.BORN, pid, sl.getFields(4), sl.ts));
            suggestName(br, sl, 0, 2, 20);
        } else if ("am_proc_bound".equals(eventType)) {
            suggestName(br, sl, 0, 1, 20);
        } else if ("am_proc_died".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[0]);
            addAMData(new AMData(AMData.DIE, pid, null, sl.ts));
            suggestName(br, sl, 0, 1, 20);
        } else if ("am_create_service".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[3]);
            addAMData(new AMData(AMData.ON_CREATE, pid, sl.getFields(1), sl.ts));
            suggestName(br, sl, 3, 1, 18);
        } else if ("am_destroy_service".equals(eventType)) {
            int pid = Integer.parseInt(sl.fields[2]);
            addAMData(new AMData(AMData.ON_DESTROY, pid, sl.getFields(1), sl.ts));
            suggestName(br, sl, 2, 1, 18);
        } else {
            // ignore
        }
    }

    private void suggestName(BugReport br, LogLine sl, int idxPid, int idxPkg, int prio) {
        if (Math.max(idxPid, idxPkg) >= sl.fields.length) return; // not enough fields
        int pid = -1;
        try {
            pid = Integer.parseInt(sl.fields[idxPid]);
        } catch (Exception e) {
            return; // strange pid
        }
        suggestNameImpl(br, sl, pid, idxPkg, prio);
    }

    private void suggestName(BugReport br, LogLine sl, int idxPkg, int prio) {
        suggestNameImpl(br, sl, sl.pid, idxPkg, prio);
    }

    private void suggestNameImpl(BugReport br, LogLine sl, int pid, int idxPkg, int prio) {
        if (idxPkg >= sl.fields.length) return; // not enough fields
        String procName = sl.fields[idxPkg];
        if (procName.length() == 0) {
            return; // missing package name
        }
        int idx = procName.indexOf('/');
        if (idx > 0) {
            procName = procName.substring(0, idx);
        }
        ProcessRecord pr = br.getProcessRecord(pid, true, false);
        if (pr != null) {
            pr.suggestName(procName, prio);
        }
    }

    private void addAMData(AMData amData) {
        mAMDatas.add(amData);
    }

    private void addMainLoopLatencyData(String[] fields, int line) {
        String activity = fields[0];
        String action = fields[1];
        String id = activity + "-" + action;
        int max = Integer.parseInt(fields[2]);
        int avg = Integer.parseInt(fields[3]);
        MLLStat mll = mMLL.get(id);
        if (mll == null) {
            mll = new MLLStat();
            mll.activity = activity;
            mll.action = action;
            mMLL.put(id, mll);
        }
        mll.max = max;
        mll.avg = avg;
        mll.line = line;
    }

    private void finishMainLoopLatency(BugReport br, Chapter ch) {
        if (mMLL.size() == 0) return;

        Chapter chMLL = new Chapter(br, "Stats - Main loop latency");
        ch.addChapter(chMLL);
        chMLL.addLine("<div class=\"hint\">(Hint: click on the headers to sort the data. Shift+click to sort on multiple columns.)</div>");
        chMLL.addLine("<table class=\"mll tablesorter\">");
        chMLL.addLine("<thead>");
        chMLL.addLine("<tr>");
        chMLL.addLine("<th>Activity/Component</td>");
        chMLL.addLine("<th>Action</td>");
        chMLL.addLine("<th>Max(ms)</td>");
        chMLL.addLine("<th>Avg(ms)</td>");
        chMLL.addLine("</tr>");
        chMLL.addLine("</thead>");
        chMLL.addLine("<tbody>");

        boolean odd = false;
        for (MLLStat mll : mMLL.values()) {
            odd = !odd;
            int levelMax = findLevel(mll.max, MLL_LEVELS_MAX);
            int levelAvg = findLevel(mll.avg, MLL_LEVELS_AVG);
            chMLL.addLine("<tr class=\"mll-" + (odd ? "odd" : "even") + "\">");
            chMLL.addLine("<td><a href=\"#" + getAnchorToLine(mll.line) + "\">" + mll.activity + "</a></td>");
            chMLL.addLine("<td>" + mll.action + "</td>");
            chMLL.addLine("<td class=\"mll-level" + levelMax + "\">" + mll.max + "</td>");
            chMLL.addLine("<td class=\"mll-level" + levelAvg + "\">" + mll.avg + "</td>");
            chMLL.addLine("</tr>");
        }

        chMLL.addLine("</tbody>");
        chMLL.addLine("<table>");
    }

    private void addActivityLaunchTimeData(String[] fields, int line) {
        String activity = fields[1];
        int time = Integer.parseInt(fields[2]);
        int total = Integer.parseInt(fields[3]);
        ALTStat alt = new ALTStat();
        alt.activity = activity;
        alt.time = time;
        alt.total = total;
        alt.line = line;
        mALT.add(alt);
    }

    private void finishActivityLaunchTime(BugReport br, Chapter ch) {
        if (mALT.size() == 0) return;

        Chapter chALT = new Chapter(br, "Stats - Activity launch time");
        ch.addChapter(chALT);
        chALT.addLine("<div class=\"hint\">(Hint: click on the headers to sort the data. Shift+click to sort on multiple columns.)</div>");
        chALT.addLine("<table class=\"mll tablesorter\">");
        chALT.addLine("<thead>");
        chALT.addLine("<tr>");
        chALT.addLine("<th>Activity</td>");
        chALT.addLine("<th>Time(ms)</td>");
        chALT.addLine("<th>Total(ms)</td>");
        chALT.addLine("</tr>");
        chALT.addLine("</thead>");
        chALT.addLine("<tbody>");

        boolean odd = false;
        for (ALTStat alt : mALT) {
            odd = !odd;
            int levelTime = findLevel(alt.time, ALT_LEVELS);
            int levelTotal = findLevel(alt.total, ALT_LEVELS);
            chALT.addLine("<tr class=\"mll-" + (odd ? "odd" : "even") + "\">");
            chALT.addLine("<td><a href=\"#" + getAnchorToLine(alt.line) + "\">" + alt.activity + "</a></td>");
            chALT.addLine("<td class=\"mll-level" + levelTime + "\">" + alt.time + "</td>");
            chALT.addLine("<td class=\"mll-level" + levelTotal + "\">" + alt.total + "</td>");
            chALT.addLine("</tr>");
        }

        chALT.addLine("</tbody>");
        chALT.addLine("<table>");
    }

    private void collectSampleStats() {
        collectSampleStats("db_sample", mDBStats);
        collectSampleStats("content_query_sample", mCQStats);
        collectSampleStats("content_query_sample", mCTStats);
        collectSampleStats("content_update_sample", mCUStats);
        collectSampleStats("content_update_sample", mCTStats);
    }


    private void collectSampleStats(String eventType, HashMap<String, DBStat> stats) {
        Vector<SampleData> datas = mSDs.get(eventType);
        if (datas == null) return;
        for (SampleData sd : datas) {
            addDBData(sd.pid, sd.name, sd.duration, stats);
        }
    }

    private void addDBData(int pid, String db, int time, HashMap<String, DBStat> stats) {
        DBStat stat = stats.get(db);
        if (stat == null) {
            stat = new DBStat();
            stat.db = db;
            stats.put(db, stat);
        }
        stat.count++;
        stat.totalTime += time;
        if (time > stat.maxTime) {
            stat.maxTime = time;
        }
        if (!stat.pids.contains(pid)) {
            stat.pids.add(pid);
        }
    }

    private void finishDBStats(BugReport br, Chapter ch) {
        writeDBStats(mDBStats, "Stats - Direct DB access", br, ch);
        writeDBStats(mCQStats, "Stats - Content query", br, ch);
        writeDBStats(mCUStats, "Stats - Content update", br, ch);
        writeDBStats(mCTStats, "Stats - Content query + update", br, ch);
    }

    private void writeDBStats(HashMap<String,DBStat> stats, String title, BugReport br, Chapter ch) {
        if (stats.size() == 0) return;

        Chapter chDB = new Chapter(br, title);
        ch.addChapter(chDB);
        chDB.addLine("<div class=\"hint\">(Hint: click on the headers to sort the data. Shift+click to sort on multiple columns.)</div>");
        chDB.addLine("<table class=\"mll tablesorter\">");
        chDB.addLine("<thead>");
        chDB.addLine("<tr>");
        chDB.addLine("<th>Database</td>");
        chDB.addLine("<th>Total(ms)</td>");
        chDB.addLine("<th>Max(ms)</td>");
        chDB.addLine("<th>Avg(ms)</td>");
        chDB.addLine("<th>Samples</td>");
        chDB.addLine("<th>Pids</td>");
        chDB.addLine("</tr>");
        chDB.addLine("</thead>");
        chDB.addLine("<tbody>");

        boolean odd = false;
        for (DBStat db : stats.values()) {
            odd = !odd;
            chDB.addLine("<tr class=\"mll-" + (odd ? "odd" : "even") + "\">");
            chDB.addLine("<td>" + db.db + "</td>");
            chDB.addLine("<td>" + db.totalTime + "</td>");
            chDB.addLine("<td>" + db.maxTime + "</td>");
            chDB.addLine("<td>" + (db.totalTime / db.count) + "</td>");
            chDB.addLine("<td>" + db.count + "</td>");
            chDB.addLine("<td>");
            for (int pid : db.pids) {
                chDB.addLine("<a href=\"" + br.createLinkToProcessRecord(pid) + "\">" + pid + "</a> ");
            }
            chDB.addLine("</td>");
            chDB.addLine("</tr>");
        }

        chDB.addLine("</tbody>");
        chDB.addLine("<table>");
    }

    private int findLevel(int value, int[] levels) {
        int cnt = levels.length;
        for (int i = 0; i < cnt; i++) {
            if (value <= levels[i]) {
                return i;
            }
        }
        return cnt;
    }

    /**
     * Generate report based on AM logs
     * @param br The bugreport
     * @param mainCh The main chapter
     */
    private void generateAMGraphs(Report br, Chapter mainCh) {
        // Sanity check
        int cnt = mAMDatas.size();
        if (cnt == 0) {
            return;
        }
        if (getFirstTs() >= getLastTs()) {
            br.printErr("Event log too short!");
            return;
        }

        // Create the chapter
        Chapter ch = new Chapter(br, "AM Graphs");
        mainCh.addChapter(ch);

        // First, we must make sure that all data has a pid associated to it
        for (int i = 0; i < cnt; i++) {
            AMData am = mAMDatas.get(i);
            String component = am.getComponent();
            if (am.getPid() >= 0) continue;
            if (component == null) continue;
            int pid = findPid(i, component);
            if (pid >= 0) {
                am.setPid(pid);
            }
        }

        // Also, we must make sure that all data has a component associated to it
        for (int i = 0; i < cnt; i++) {
            AMData am = mAMDatas.get(i);
            String component = am.getComponent();
            int pid = am.getPid();
            if (pid < 0) continue;
            if (component != null) continue;
            component = findComponent(i, pid);
            if (component != null) {
                am.setComponent(component);
            }
        }

        // Then we process each log record and add it to the chart
        // and to the VCD file
        int vcdId = 1;
        HashMap<String, Integer> vcdIds = new HashMap<String, Integer>();
        HashMap<String, AMChart> charts = new HashMap<String, AMChart>();
        for (int i = 0; i < cnt; i++) {
            AMData am = mAMDatas.get(i);
            int pid = am.getPid();
            String component = am.getComponent();
            if (pid < 0 || component == null) continue;
            if (null == vcdIds.get(component)) {
                vcdIds.put(component, vcdId++);
            }
            AMChart chart = charts.get(component);
            if (chart == null) {
                chart = new AMChart(pid, component, getFirstTs(), getLastTs());
                charts.put(component, chart);
            }
            chart.addData(am);
        }

        // Write the VCD file
        String fn = br.getRelRawDir() + "am_logs.vcd";
        try {
            FileOutputStream fos = new FileOutputStream(br.getBaseDir() + fn);
            PrintStream fo = new PrintStream(fos);

            // write header
            fo.println("$timescale 1ms $end");
            fo.println("$scope am_logs $end");
            for (Entry<String, Integer> item : vcdIds.entrySet()) {
                fo.println("$var wire 1 a" + item.getValue() + " " + item.getKey() + " $end");
            }
            fo.println("$upscope $end");
            fo.println("$enddefinitions $end");

            // Write initial values
            fo.println("#" + getFirstTs());
            for (Entry<String, Integer> item : vcdIds.entrySet()) {
                int id = item.getValue();
                String component = item.getKey();
                AMChart chart = charts.get(component);
                int initState = AMChart.STATE_UNKNOWN;
                if (chart != null) {
                    initState = chart.getInitState();
                }
                char state = getVCDState(initState);
                fo.println("b" + state + " a" + id);
            }

            // Write events
            for (int i = 0; i < cnt; i++) {
                AMData am = mAMDatas.get(i);
                int pid = am.getPid();
                String component = am.getComponent();
                if (pid < 0 || component == null) continue;
                int id = vcdIds.get(component);
                int state = AMChart.actionToState(am.getAction());
                if (state != AMChart.STATE_UNKNOWN) {
                    fo.println("#" + am.getTS());
                    fo.println("b" + getVCDState(state) + " a" + id);
                }
            }


            ch.addLine("<p>AM logs converted to VCD file (you can use GTKWave to open it): <a href=\"" + fn + "\">" + fn + "</a></p>");
        } catch (IOException e) {
            br.printErr("Error saving vcd file: " + e);
        }

        // We need to finish the charts (fill in the end, save the image, etc)
        ch.addLine("<p>NOTE: you can drag and move table rows to reorder them!</p>");

        ch.addLine("<table class=\"am-graph tablednd\">");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"am-graph-header\">");
        ch.addLine("    <th>Component</td>");
        ch.addLine("    <th>Graph</td>");
        ch.addLine("  </tr>");

        fn = "amchart_time.png";
        if (Util.createTimeBar(br, fn, AMChart.W, getFirstTs(), getLastTs())) {
            ch.addLine("  <tr>");
            ch.addLine("    <th></td>");
            ch.addLine("    <th><img src=\"" + fn + "\"/></td>");
            ch.addLine("  </tr>");
        }
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");
        for (AMChart chart : charts.values()) {
            fn = chart.finish(br);
            if (fn == null) continue;
            ch.addLine("  <tr>");
            ch.addLine("    <td>" + chart.getComponent() + "</td>");
            ch.addLine("    <td><img src=\"" + fn + "\"/></td>");
            ch.addLine("  </tr>");
        }
        ch.addLine("  </tbody>");
        ch.addLine("</table>");

    }

    private char getVCDState(int initState) {
        switch (initState) {
            case AMChart.STATE_ALIVE:
                return 'Z';
            case AMChart.STATE_CREATED:
                return '-';
            case AMChart.STATE_RESUMED:
                return 'X';
            default:
                return '0';
        }
    }

    /**
     * Try to guess the pid of a given component
     * @param i The index where to start searching
     * @param component The component
     * @return The found pid, or -1 if not found
     */
    private int findPid(int i, String component) {
        int pid = -1;
        int cnt = mAMDatas.size();

        // Search backwards
        if (pid == -1) {
            for (int j = i - 1; j >= 0; j--) {
                AMData am = mAMDatas.get(j);
                if (component.equals(am.getComponent())) {
                    pid = am.getPid();
                    if (pid >= 0) {
                        break;
                    }
                }
            }
        }

        // Search forward
        if (pid == -1) {
            for (int j = i + 1; j < cnt; j++) {
                AMData am = mAMDatas.get(j);
                if (component.equals(am.getComponent())) {
                    pid = am.getPid();
                    if (pid >= 0) {
                        break;
                    }
                }
            }
        }

        // TODO: maybe we should verify it: there shouldn't be any am_proc_died or similar logs
        // between the found index and the current index

        // If not found, allocate a dummy/fake pid
        if (pid == -1) {
            pid = allocFakePid();
        }

        return pid;
    }

    /**
     * Try to guess the component of a given pid
     * @param i The index where to start searching
     * @param pid The component
     * @return The found component, or null if not found
     */
    private String findComponent(int i, int pid) {
        String component = null;

        // Search backwards only (since this method is used only for proc_died)
        if (component == null) {
            for (int j = i - 1; j >= 0; j--) {
                AMData am = mAMDatas.get(j);
                if (pid == am.getPid()) {
                    component = am.getComponent();
                    if (component != null) {
                        break;
                    }
                }
            }
        }

        return component;
    }

    private int allocFakePid() {
        return mNextFakePid++;
    }

    private void generateSampleDataGraphs(Report br, Chapter mainCh) {
        Chapter ch = null;

        for (Entry<String, Vector<SampleData>> entry : mSDs.entrySet()) {
            String eventType = entry.getKey();
            Vector<SampleData> sds = entry.getValue();
            if (sds.size() <= 1) continue;

            // Create the chapter if not created yet
            if (ch == null) {
                ch = new Chapter(br, "*_sample graphs");
                mainCh.addChapter(ch);
            }

            // create the graph
            ch.addLine("<p>Graph built from " + eventType + " logs:</p>");
            // save the data as vcd file as well
            String fnVcd = br.getRelRawDir() + "sample_graph_" + eventType + ".vcd";
            if (generateSampleDataVCD(br, fnVcd, sds, eventType)) {
                // TODO
            }
            ch.addLine("<div class=\"hint\">(VCD file also generated: <a href=\"" + fnVcd + "\">" + fnVcd + "</a>)</div>");
            String fn = br.getRelDataDir() + "sample_graph_" + eventType + ".png";
            if (generateSampleDataGraph(br, fn, sds, eventType)) {
                // TODO
            }
            ch.addLine("<div><img src=\"" + fn + "\"/></div>");

        }
    }

    private boolean generateSampleDataGraph(Report br, String fn, Vector<SampleData> sds, String eventType) {
        int w = 800;
        int h = 350;
        int cx = 100;
        int cy = 250;
        int gw = 600;
        int gh = 200;
        int nx = 750;
        int ny = 10;
        int tx = cx;
        int ty = cy;
        int th = 75;

        int maxNameW = 0;

        // Need a font metrics before the actual image is created :-(
        FontMetrics fm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics();

        // Allocate colors for each value
        // Also count the number of distinct values
        HashMap<String, Color> mColors = new HashMap<String, Color>();
        int idx = 0, lh = fm.getHeight();
        if (lh < 18) {
            lh = 18;
        }
        for (SampleData sd : sds) {
            String name = sd.name;
            Color col = mColors.get(name);
            if (col == null) {
                int rgba = Util.getColor(idx++) | 0x40000000;
                col = new Color(rgba, true);
                mColors.put(name, col);
            }
            maxNameW = Math.max(maxNameW, fm.stringWidth(name));
        }
        w += maxNameW + 32;
        int maxNameH = ny * 2 + idx * lh;
        h = Math.max(h, maxNameH);

        // Create an empty image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, w - 1, h - 1);

        // Draw the legend
        for (Entry<String, Color> entry : mColors.entrySet()) {
            String name = entry.getKey();
            Color color = entry.getValue();
            g.setColor(color);
            g.fillRect(nx, ny + (lh - 16) / 2, 16, 16);
            g.drawRect(nx, ny + (lh - 16) / 2, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString(name, nx + 32, ny + fm.getAscent());
            ny += lh;
        }

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
        g.drawString(eventType, 10, 10 + fm.getAscent());

        // Draw some guide lines
        int max = 110;
        int count = 5;
        int step = 20;
        Color colGuide = new Color(0xc0c0ff);
        for (int i = 1; i <= count; i++) {
            int value = i * step;
            if (value > max) break;
            int yv = cy - value * gh / max;
            g.setColor(colGuide);
            g.drawLine(cx + 1, yv, cx + gw, yv);
            g.setColor(Color.BLACK);
            String s = "" + value + "%  ";
            g.drawString(s, cx - fm.stringWidth(s) - 1, yv);
        }

        // Plot the values (size)
        long duration = (getLastTs() - getFirstTs());
        if (duration <= 0) return false;
        for (SampleData sd : sds) {
            int bh = sd.perc * (gh - 1) / max;
            int bx = (int)((sd.ts - getFirstTs()) * (gw - 1) / duration);
            int bw = (int)((sd.duration) * (gw - 1) / (getLastTs() - getFirstTs()));
            if (bw < 3) {
                bw = 3;
            }
            g.setColor(mColors.get(sd.name));
            g.fillRect(cx + bx - bw, cy - bh, bw, bh);
            g.drawRect(cx + bx - bw, cy - bh, bw - 1, bh - 1);
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, g, tx, ty, gw, th, getFirstTs(), getLastTs(), true)) {
            return false;
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean generateSampleDataVCD(Report br, String fn, Vector<SampleData> sds, String eventType) {
        if (sds.size() == 0) return false;

        // In the first pass we need to find the unique ids, and also generate a sorted
        // list of events
        HashMap<String, Integer> ids = new HashMap<String, Integer>();
        Vector<SampleEvent> events = new Vector<SampleEvent>();
        int idx = 0;
        for (SampleData sd : sds) {
            String name = sd.name;

            // Associate an id to the name
            Integer idObj = ids.get(name);
            if (idObj == null) {
                idObj = new Integer(idx++);
                ids.put(name, idObj);
            }

            // Add the start and stop events
            events.add(new SampleEvent(true, sd.ts - sd.duration, idObj));
            events.add(new SampleEvent(false, sd.ts, idObj));
        }

        // Sort the events by timestamp
        Collections.sort(events, new Comparator<SampleEvent>() {
            @Override
            public int compare(SampleEvent o1, SampleEvent o2) {
                if (o1.ts < o2.ts) return -1;
                if (o1.ts > o2.ts) return +1;
                return 0;
            }
        });

        // Save the file
        try {
            PrintStream ps = new PrintStream(new File(br.getBaseDir() + fn));
            int bits = 8;

            // Write header
            ps.println("$timescale 1ms $end");
            ps.println("$scope am_logs $end");
            for (Entry<String, Integer> entry : ids.entrySet()) {
                String name = entry.getKey();
                int id = entry.getValue();
                name = Util.fixVCDName(name);
                ps.println("$var wire " + bits + " n" + id + " " + name + " $end");
            }
            ps.println("$upscope $end");
            ps.println("$enddefinitions $end");

            // Write initial values
            long lastTs = getFirstTs();
            ps.println("#" + lastTs);
            for (int id = 0; id < idx; id++) {
                ps.println("bZ n" + id);
            }

            // Write events
            int count[] = new int[idx];
            for (SampleEvent event : events) {
                if (event.ts != lastTs) {
                    ps.println("#" + event.ts);
                    lastTs = event.ts;
                }
                int id = event.id;
                if (event.start) {
                    count[id]++;
                } else {
                    count[id]--;
                }
                ps.println("b" + (count[id] == 0 ? "Z" : Util.toBinary(count[id], bits)) + " n" + id);
            }

            // Write final values
            lastTs = getLastTs();
            ps.println("#" + lastTs);
            for (int id = 0; id < idx; id++) {
                ps.println("bZ n" + id);
            }

            // Finish
            ps.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    static class SampleEvent {
        boolean start;
        long ts;
        int id;
        public SampleEvent(boolean start, long ts, int id) {
            this.start = start;
            this.ts = ts;
            this.id = id;
        }
    }

    /**
     * Main loop latency statistics
     */
    static class MLLStat {
        public String activity;
        public String action;
        public int max;
        public int avg;
        public int line;
    }

    /**
     * Activity launch time statistics
     */
    static class ALTStat {
        public String activity;
        public int time;
        public int total;
        public int line;
    }

    /**
     * Direct database access statistics
     */
    static class DBStat {
        public String db;
        public int totalTime;
        public int maxTime;
        public int count;
        public Vector<Integer> pids = new Vector<Integer>();
    }

    /**
     * A data representing an AM (ActivityManager) event
     */
    static class AMData {

        public static final int BORN = 1;
        public static final int DIE = 2;
        public static final int PAUSED = 3;
        public static final int RESUMED = 4;
        public static final int ON_CREATE = 5;
        public static final int ON_DESTROY = 6;
        public static final int ON_PAUSE = 7;
        public static final int ON_RESTART = 8;
        public static final int ON_RESUME = 9;

        private int mAction;
        private int mPid;
        private String mComponent;
        private long mTS;

        public AMData(int action, int pid, String component, long ts) {
            mAction = action;
            mPid = pid;
            mComponent = component;
            mTS = ts;
            if (mComponent != null) {
                int idx = mComponent.indexOf('/');
                if (idx >= 0) {
                    mComponent = mComponent.substring(0, idx) + mComponent.substring(idx + 1);
                }
            }
        }

        public int getPid() {
            return mPid;
        }

        public int getAction() {
            return mAction;
        }

        public String getComponent() {
            return mComponent;
        }

        public long getTS() {
            return mTS;
        }

        public void setPid(int pid) {
            mPid = pid;
        }

        public void setComponent(String component) {
            mComponent = component;
        }

    }

    /**
     * Graph/chart generated from the activity managers life cycle logs
     */
    static class AMChart {
        public static final int W = 800;
        public static final int H = 25;

        public static final int STATE_UNKNOWN = -1;
        public static final int STATE_NONE = 0;
        public static final int STATE_ALIVE = 1;
        public static final int STATE_CREATED = 2;
        public static final int STATE_RESUMED = 3;

        private static final Color STATE_COLORS[] = {
            new Color(0xffffff),
            new Color(0xffc0c0),
            new Color(0xff8080),
            new Color(0x00ff00),
        };

        private String mComponent;
        private BufferedImage mImg;
        private Graphics2D mG;
        private long mTSStart;
        private long mTSEnd;
        private int mLastX = 0;
        private int mLastState = STATE_UNKNOWN;
        private int mInitState = STATE_UNKNOWN;
        private int mUsed = 0;

        public AMChart(int pid, String component, long tsStart, long tsEnd) {
            mComponent = component;
            mTSStart = tsStart;
            mTSEnd = tsEnd;
            mImg = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            mG = (Graphics2D)mImg.getGraphics();
            mG.setColor(Color.WHITE);
            mG.fillRect(0, 0, W, H);
        }

        public static int actionToState(int action) {
            int state = STATE_UNKNOWN;
            switch (action) {
                case AMData.ON_CREATE: state = STATE_CREATED; break;
                case AMData.ON_DESTROY: state = STATE_ALIVE; break;
                case AMData.ON_PAUSE: state = STATE_CREATED; break;
                case AMData.ON_RESUME: state = STATE_RESUMED; break;
                case AMData.ON_RESTART: state = STATE_RESUMED; break;
                case AMData.BORN: state = STATE_ALIVE; break;
                case AMData.DIE: state = STATE_NONE; break;
            }
            return state;
        }

        public String getComponent() {
            return mComponent;
        }

        public void addData(AMData am) {
            // Calculate the new state
            int state = actionToState(am.getAction());
            if (state == STATE_UNKNOWN) {
                // cannot handle this event
                return;
            }

            // First, try to guess the previous state
            if (mLastState == STATE_UNKNOWN) {
                switch (am.getAction()) {
                    case AMData.ON_CREATE: mLastState = STATE_ALIVE; break;
                    case AMData.ON_DESTROY: mLastState = STATE_CREATED; break;
                    case AMData.ON_PAUSE: mLastState = STATE_RESUMED; break;
                    case AMData.ON_RESUME: mLastState = STATE_CREATED; break;
                    case AMData.ON_RESTART: mLastState = STATE_ALIVE; break;
                    case AMData.BORN: mLastState = STATE_NONE; break;
                    case AMData.DIE: mLastState = STATE_ALIVE; break;
                }
                mInitState = mLastState;
            }

            // Calculate the new state
            int x = (int)(W * (am.getTS() - mTSStart) / (mTSEnd - mTSStart));

            if (mLastState != STATE_UNKNOWN) {
                // Render the state
                drawState(x);
            }

            mLastX = x;
            mLastState = state;
        }

        private void drawState(int x) {
            if (mLastX >= x) {
                mG.setColor(Color.YELLOW);
                mG.fillRect(mLastX, 0, 1, H);
            } else {
                mG.setColor(STATE_COLORS[mLastState]);
                mG.fillRect(mLastX + 1, 0, x - mLastX + 1, H);
            }
            mUsed++;
        }

        public String finish(Report br) {
            // Finish the rendering (render the last state)
            if (mLastState != STATE_UNKNOWN) {
                drawState(W);
            }

            if (mUsed == 0) {
                // Noting was rendered, so don't save the empty image
                return null;
            }

            // Save the image
            String fn = br.getRelDataDir() + "amchart_" + hashCode() + ".png";
            try {
                ImageIO.write(mImg, "png", new File(br.getBaseDir() + fn));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fn;
        }

        public int getInitState() {
            return mInitState;
        }

    }

    static class SampleData {

        long ts;
        int pid;
        String name;
        int duration;
        int perc;

        public SampleData(long ts, int pid, String name, int duration, int perc) {
            this.ts = ts;
            this.pid = pid;
            this.name = name;
            this.duration = duration;
            this.perc = perc;
        }

    }

}
