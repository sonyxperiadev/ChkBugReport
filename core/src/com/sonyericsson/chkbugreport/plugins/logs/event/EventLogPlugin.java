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
package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.PreText;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.logs.ConfigChange;
import com.sonyericsson.chkbugreport.plugins.logs.GCRecord;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogPlugin;
import com.sonyericsson.chkbugreport.util.Util;

import java.util.HashMap;
import java.util.Vector;

public class EventLogPlugin extends LogPlugin {

    public static final String INFO_ID_LOG = "eventlog_log";

    private Vector<ALTStat> mALT = new Vector<ALTStat>();
    /* db_sample stats */
    private HashMap<String, DBStat> mDBStats = new HashMap<String, DBStat>();
    /* content_query_sample stats */
    private HashMap<String, DBStat> mCQStats = new HashMap<String, DBStat>();
    /* content_update_sample stats */
    private HashMap<String, DBStat> mCUStats = new HashMap<String, DBStat>();
    /* content_query_sample + content_update_stats */
    private HashMap<String, DBStat> mCTStats = new HashMap<String, DBStat>();
    /* The collection of *_sample data */
    private SampleDatas mSDs;

    private ActivityManagerTrace mAM;
    private SampleDatasGenerator mSamples;
    private ActivityManagerGraphGenerator mAMGraph;
    private ActivityManagerStatsGenerator mAMStats;
    private ActivityManagerProcStatsGenerator mAMProcStats;
    private BatteryLevels mBatteryLevels;

    private NetstatSamples mNetstatMobile;
    private NetstatSamples mNetstatWifi;

    public EventLogPlugin() {
        super("Event", "event", Section.EVENT_LOG);
    }

    @Override
    public int getPrio() {
        return 32;
    }

    @Override
    public void reset() {
        super.reset();
        mALT.clear();
        mDBStats.clear();
        mCQStats.clear();
        mCUStats.clear();
        mCTStats.clear();
        mSDs = new SampleDatas();
        mBatteryLevels = new BatteryLevels(this);
        mAM = new ActivityManagerTrace(this);
        mNetstatMobile = new NetstatSamples();
        mNetstatWifi = new NetstatSamples();
    }

    @Override
    public void load(Module rep) {
        super.load(rep);
        mAM.finishLoad();
        mBatteryLevels.setRange(getFirstTs(), getLastTs());
        mSamples = new SampleDatasGenerator(this, mSDs);
        mAMGraph = new ActivityManagerGraphGenerator(this, mAM);
        mAMStats = new ActivityManagerStatsGenerator(this, mAM);
        mAMProcStats = new ActivityManagerProcStatsGenerator(this, mAM);
        rep.addInfo(ActivityManagerTrace.INFO_ID, mAM);
        rep.addInfo(BatteryLevels.INFO_ID, mBatteryLevels);
        rep.addInfo(NetstatSamples.INFO_ID_MOBILE, mNetstatMobile);
        rep.addInfo(NetstatSamples.INFO_ID_WIFI, mNetstatWifi);
        postLoad(rep);
    }

    @Override
    public String getInfoId() {
        return INFO_ID_LOG;
    }

    @Override
    protected void generateExtra(BugReportModule rep, Chapter ch) {
        BugReportModule br = (BugReportModule)rep;

        // Collect *_sample data in statistics
        collectSampleStats();

        // Finish sub-chapters
        finishActivityLaunchTime(br, ch);
        finishDBStats(br, ch);
        mSamples.generate(br, ch);
        mAMGraph.generate(br, ch);
        mAMStats.generate(br, ch);
        mAMProcStats.generate(br, ch);
    }

    @Override
    protected void analyze(LogLine sl, int i, BugReportModule br, Section s) {
        super.analyze(sl, i, br, s);

        String eventType = Util.strip(sl.tag);
        if ("netstats_mobile_sample".equals(eventType)) {
            if (sl.fields.length == 14) {
                mNetstatMobile.add(new NetstatSample("mobile", sl.ts, sl.fields));
            }
            return;
        }
        if ("netstats_wifi_sample".equals(eventType)) {
            if (sl.fields.length == 14) {
                mNetstatWifi.add(new NetstatSample("mobile", sl.ts, sl.fields));
            }
            return;
        }
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
        if ("activity_launch_time".equals(eventType)) {
            addActivityLaunchTimeData(sl);
            addActivityLaunchMarker(sl);
        } else if (eventType.startsWith("am_")) {
            mAM.addAMData(eventType, br, sl, i);
        } else if ("dvm_gc_info".equals(eventType)) {
            addDvmGCInfoData(sl);
        } else if ("configuration_changed".equals(eventType)) {
            handleConfigChanged(sl);
        } else if ("battery_level".equals(eventType)) {
            mBatteryLevels.addData(sl, br);
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
            sl.addMarker("log-float-icon", sb.toString(), null);
        } catch (NumberFormatException nfe) { /* NOP */ }
    }

    private void analyzeCrashOrANR(LogLine sl, int i, BugReportModule br, String type) {
        // Put a marker box
        String cType = type.toUpperCase();
        sl.addMarker("log-float-err", cType, null);

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
        Bug bug = new Bug(Bug.Type.PHONE_ERR, prio, sl.ts, msg);
        bug.setAttr(Bug.ATTR_FIRST_LINE, i);
        new Block(bug).add(new Link(sl.getAnchor(), "(link to log)"));
        if (pid != -1) {
            new Block(bug)
                .add("Link to process record: ")
                .add(new ProcessLink(br, pid));
        }
        new Block(bug).addStyle("log").add(sl.symlink());
        PreText log = new PreText(bug);
        if (sl.fields.length >= 4) {
            bug.setAttr(Bug.ATTR_PID, sl.fields[0]);
            bug.setAttr(Bug.ATTR_PACKAGE, sl.fields[1]);
            bug.setAttr(Bug.ATTR_REASON, sl.fields[3]);

            // Print some additional info
            int flags = -1;
            try {
                flags = Integer.parseInt(sl.fields[2]);
            } catch (NumberFormatException nfe) { /* NOP */ }
            log.addln("PID:            " + sl.fields[0]);
            log.addln("Package:        " + sl.fields[1]);
            log.addln("Reason:         " + sl.fields[3]);
            log.addln("Flags:          0x" + Integer.toHexString(flags) + ":");
            log.addln("  - SYSTEM:                    " + (0 != (flags & (1 << 0))));
            log.addln("  - DEBUGGABLE:                " + (0 != (flags & (1 << 1))));
            log.addln("  - HAS_CODE:                  " + (0 != (flags & (1 << 2))));
            log.addln("  - PERSISTENT:                " + (0 != (flags & (1 << 3))));
            log.addln("  - FACTORY TEST:              " + (0 != (flags & (1 << 4))));
            log.addln("  - ALLOW TASK REPARENTING:    " + (0 != (flags & (1 << 5))));
            log.addln("  - ALLOW CLEAR USERDATA:      " + (0 != (flags & (1 << 6))));
            log.addln("  - UPDATED SYSTEM APP:        " + (0 != (flags & (1 << 7))));
            log.addln("  - TEST ONLY:                 " + (0 != (flags & (1 << 8))));
            log.addln("  - SUPPORTS SMALL SCREENS:    " + (0 != (flags & (1 << 9))));
            log.addln("  - SUPPORTS NORMAL SCREENS:   " + (0 != (flags & (1 << 10))));
            log.addln("  - SUPPORTS LARGE SCREENS:    " + (0 != (flags & (1 << 11))));
            log.addln("  - SUPPORTS XLARGE SCREENS:   " + (0 != (flags & (1 << 19))));
            log.addln("  - RESIZEABLE FOR SCREENS:    " + (0 != (flags & (1 << 12))));
            log.addln("  - SUPPORTS SCREEN DENSITIES: " + (0 != (flags & (1 << 13))));
            log.addln("  - VM SAFE MODE:              " + (0 != (flags & (1 << 14))));
            log.addln("  - ALLOW BACKUP:              " + (0 != (flags & (1 << 15))));
            log.addln("  - KILL AFTER RESTORE:        " + (0 != (flags & (1 << 16))));
            log.addln("  - RESTORE ANY VERSION:       " + (0 != (flags & (1 << 17))));
            log.addln("  - EXTERNAL STORAGE:          " + (0 != (flags & (1 << 18))));
            log.addln("  - CANT SAVE STATE:           " + (0 != (flags & (1 << 27))));
            log.addln("  - FORWARD LOCK:              " + (0 != (flags & (1 << 29))));
            log.addln("  - NEVER ENCRYPT:             " + (0 != (flags & (1 << 30))));
        }
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

    private void addSampleData(Module br, String eventType, LogLine sl) {
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
            SampleData sd = new SampleData(sl.ts, sl.pid, name, duration, perc, sl);
            addSampleData(eventType, sd);
            if (eventType.equals("binder_sample")) {
                // These are interesting from the process point of view as well
                name = sl.fields[3];
                sd = new SampleData(sl.ts, sl.pid, name, duration, perc, sl);
                addSampleData(eventType + "_alt", sd);
            }
        } catch (NumberFormatException e) {
            br.printErr(4, TAG + "addSampleData(eventType=" + eventType + "):" + e);
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
        mSDs.addData(eventType, sd);
    }

    private void addActivityLaunchTimeData(LogLine sl) {
        String activity = sl.fields[1];
        int time = Integer.parseInt(sl.fields[2]);
        int total = Integer.parseInt(sl.fields[3]);
        ALTStat alt = new ALTStat();
        alt.activity = activity;
        alt.time = time;
        alt.total = total;
        alt.line = sl;
        mALT.add(alt);
    }

    private void finishActivityLaunchTime(BugReportModule br, Chapter ch) {
        if (mALT.size() == 0) return;

        Chapter chALT = new Chapter(br.getContext(), "Stats - Activity launch time");
        ch.addChapter(chALT);

        Table tg = new Table(Table.FLAG_SORT, chALT);
        tg.setCSVOutput(br, "eventlog_alt");
        tg.setTableName(br, "eventlog_alt");
        tg.addColumn("Activity", null, Table.FLAG_NONE, "activity varchar");
        tg.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tg.addColumn("Total(ms)", null, Table.FLAG_ALIGN_RIGHT, "total_ms int");
        tg.begin();
        for (ALTStat alt : mALT) {
            tg.addData(new Link(alt.line.getAnchor(), alt.activity));
            tg.addData(new ShadedValue(alt.time));
            tg.addData(new ShadedValue(alt.total));
        }
        tg.end();
    }

    private void collectSampleStats() {
        collectSampleStats("db_sample", mDBStats);
        collectSampleStats("content_query_sample", mCQStats);
        collectSampleStats("content_query_sample", mCTStats);
        collectSampleStats("content_update_sample", mCUStats);
        collectSampleStats("content_update_sample", mCTStats);
    }

    private void collectSampleStats(String eventType, HashMap<String, DBStat> stats) {
        Vector<SampleData> datas = mSDs.getSamplesByType(eventType);
        if (datas == null) return;
        for (SampleData sd : datas) {
            addDBData(sd.pid, sd.name, sd.duration, stats, sd);
        }
    }

    private void addDBData(int pid, String db, int time, HashMap<String, DBStat> stats, SampleData sd) {
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
        stat.data.add(sd);
    }

    private void finishDBStats(BugReportModule br, Chapter ch) {
        writeDBStats(mDBStats, "Stats - Direct DB access", "eventlog_db_stat", br, ch);
        writeDBStats(mCQStats, "Stats - Content query", "eventlog_content_query", br, ch);
        writeDBStats(mCUStats, "Stats - Content update", "eventlog_content_update", br, ch);
        writeDBStats(mCTStats, "Stats - Content query + update", "eventlog_content_total", br, ch);
    }

    private void writeDBStats(HashMap<String,DBStat> stats, String title, String id, BugReportModule br, Chapter ch) {
        if (stats.size() == 0) return;

        Chapter chDB = new Chapter(br.getContext(), title);
        ch.addChapter(chDB);

        Table tg = new Table(Table.FLAG_SORT, chDB);
        tg.setCSVOutput(br, id);
        tg.setTableName(br, id);
        tg.addColumn("Databse", null, Table.FLAG_NONE, "database varchar");
        tg.addColumn("Total(ms)", null, Table.FLAG_ALIGN_RIGHT, "total_ms int");
        tg.addColumn("Max(ms)", null, Table.FLAG_ALIGN_RIGHT, "max_ms int");
        tg.addColumn("Avg(ms)", null, Table.FLAG_ALIGN_RIGHT, "avg_ms int");
        tg.addColumn("Samples", null, Table.FLAG_ALIGN_RIGHT, "samples int");
        tg.addColumn("Pids", null, Table.FLAG_ALIGN_RIGHT, "pids varchar");
        tg.begin();
        int dbId = 0;
        for (DBStat db : stats.values()) {
            db.finish();
            DocNode pids = new DocNode();
            for (int pid : db.pids) {
                pids.add(new ProcessLink(br, pid, ProcessLink.SHOW_PID));
                pids.add(" ");
            }

            // Save filtered logs
            String fn = id + "_" + dbId + ".html";
            Chapter ext = saveDBFilteredLogs(br, fn, db.data);
            br.addExtraFile(ext);

            tg.addData(new Link(ext.getAnchor(), db.db));
            tg.addData(new ShadedValue(db.totalTime));
            tg.addData(new ShadedValue(db.maxTime));
            tg.addData(new ShadedValue(db.totalTime / db.count));
            tg.addData(db.count);
            tg.addData(pids);

            dbId++;
        }
        tg.end();
    }

    private Chapter saveDBFilteredLogs(BugReportModule br, String fn, Vector<SampleData> data) {
        Chapter ret = new Chapter(br.getContext(), fn);
        for (SampleData sd : data) {
            ret.add(sd.logLine.symlink());
        }
        return ret;
    }

    public ActivityManagerStatsGenerator getActivityMStats() {
        return mAMStats;
    }

}
