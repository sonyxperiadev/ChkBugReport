package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.util.TableGen;

import java.util.HashMap;

public class ActivityManagerStatsGenerator {

    private EventLogPlugin mPlugin;
    private ActivityManagerTrace mAmTrace;

    public ActivityManagerStatsGenerator(EventLogPlugin plugin, ActivityManagerTrace amTrace) {
        mPlugin = plugin;
        mAmTrace = amTrace;
    }

    /**
     * Generate statistics based on AM logs
     * @param br The bugreport
     * @param mainCh The main chapter
     */
    public void run(Report br, Chapter mainCh) {
        // Sanity check
        int cnt = mAmTrace.size();
        if (cnt == 0) {
            return;
        }
        long firstTs = mPlugin.getFirstTs();
        long lastTs = mPlugin.getLastTs();
        long duration = lastTs - firstTs;
        if (duration <= 0) {
            br.printErr(3, "Event log too short!");
            return;
        }

        // Create the chapter
        Chapter ch = new Chapter(br, "AM Stats");
        mainCh.addChapter(ch);

        // Process each sample and measure the runtimes
        HashMap<String, ComponentStat> activities = new HashMap<String, ComponentStat>();
        HashMap<String, ComponentStat> services = new HashMap<String, ComponentStat>();
        for (int i = 0; i < cnt; i++) {
            AMData am = mAmTrace.get(i);
            int pid = am.getPid();
            String component = am.getComponent();
            if (pid < 0 || component == null) continue;

            HashMap<String, ComponentStat> set = null;
            if (am.getType() == AMData.SERVICE) {
                set = services;
            } else if (am.getType() == AMData.ACTIVITY) {
                set = activities;
            }

            if (set == null) {
                continue;
            }

            ComponentStat stat = set.get(component);
            if (stat == null) {
                stat = new ComponentStat(br, component, firstTs, lastTs);
                set.put(component, stat);
            }

            stat.addData(am);
        }

        // Generate statistics table
        ch.addChapter(createStatTable(br, services, "Services", AMData.SERVICE, duration, "eventlog_amdata_services"));
        ch.addChapter(createStatTable(br, activities, "Activites", AMData.ACTIVITY, duration, "eventlog_amdata_activities"));
    }

    private Chapter createStatTable(Report br, HashMap<String, ComponentStat> set,
            String title, int type, long duration, String csv)
    {
        Chapter ch = new Chapter(br, title);
        ch.addLine("<div class=\"hint\">(Duration " + duration + "ms = " + Util.formatTS(duration) + ")</div>");

        // Check for errors
        int errors = 0;
        for (ComponentStat stat: set.values()) {
            errors += stat.errors;
        }
        if (errors > 0) {
            ch.addLine("<div class=\"err\">NOTE: " + errors + " errors/inconsistencies found in the log, " +
                    "statistics might not be correct! The affected components have been highlighted below.</div>");
        }

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, csv);
        tg.addColumn("Pkg", TableGen.FLAG_NONE);
        tg.addColumn("Cls", TableGen.FLAG_NONE);
        tg.addColumn("Created count", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time(ms)", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time(%)", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Max created time(ms)", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Avg created time(ms)", TableGen.FLAG_ALIGN_RIGHT);
        if (type == AMData.ACTIVITY) {
            tg.addColumn("Resumed count", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Total resumed time", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Total resumed time(ms)", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Total resumed time(%)", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Max resumed time(ms)", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Avg resumed time(ms)", TableGen.FLAG_ALIGN_RIGHT);
        }
        tg.begin();

        for (ComponentStat stat: set.values()) {
            // Make sure the component is finished
            stat.finish();
            tg.setNextRowStyle(stat.errors == 0 ? null : "err-row");
            tg.addData(Util.extractPkgFromComp(stat.component));
            tg.addData(Util.extractClsFromComp(stat.component));
            tg.addData(Util.shadeValue(stat.createCount));
            tg.addData(Util.formatTS(stat.totalCreatedTime));
            tg.addData(Util.shadeValue(stat.totalCreatedTime));
            tg.addData(stat.totalCreatedTime * 100 / duration + "%");
            tg.addData(Util.shadeValue(stat.maxCreatedTime));
            tg.addData(stat.createCount == 0 ? "" : Util.shadeValue(stat.totalCreatedTime / stat.createCount));
            if (type == AMData.ACTIVITY) {
                tg.addData(Util.shadeValue(stat.resumeCount));
                tg.addData(Util.formatTS(stat.totalResumedTime));
                tg.addData(Util.shadeValue(stat.totalResumedTime));
                tg.addData(stat.totalResumedTime * 100 / duration + "%");
                tg.addData(Util.shadeValue(stat.maxResumedTime));
                tg.addData(stat.resumeCount == 0 ? "" : Util.shadeValue(stat.totalResumedTime / stat.resumeCount));
            }
        }
        tg.end();
        return ch;
    }


}
