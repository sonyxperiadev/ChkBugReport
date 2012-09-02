package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.util.TableGen;

import java.util.HashMap;
import java.util.Vector;

public class ActivityManagerStatsGenerator {

    private EventLogPlugin mPlugin;
    private ActivityManagerTrace mAmTrace;
    private HashMap<String, ComponentStat> mActivities = new HashMap<String, ComponentStat>();
    private HashMap<String, ComponentStat> mServices = new HashMap<String, ComponentStat>();

    public ActivityManagerStatsGenerator(EventLogPlugin plugin, ActivityManagerTrace amTrace) {
        mPlugin = plugin;
        mAmTrace = amTrace;
    }

    /**
     * Generate statistics based on AM logs
     * @param br The bugreport
     * @param mainCh The main chapter
     */
    public void generate(Module br, Chapter mainCh) {
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
        for (int i = 0; i < cnt; i++) {
            AMData am = mAmTrace.get(i);
            int pid = am.getPid();
            String component = am.getComponent();
            if (pid < 0 || component == null) continue;

            HashMap<String, ComponentStat> set = null;
            if (am.getType() == AMData.SERVICE) {
                set = mServices;
            } else if (am.getType() == AMData.ACTIVITY) {
                set = mActivities;
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
        ch.addChapter(createStatTable(br, mServices, "Services", AMData.SERVICE, duration, "eventlog_amdata_services"));
        ch.addChapter(createStatTable(br, mActivities, "Activites", AMData.ACTIVITY, duration, "eventlog_amdata_activities"));
    }

    private Chapter createStatTable(Module br, HashMap<String, ComponentStat> set,
            String title, int type, long duration, String csv)
    {
        Chapter ch = new Chapter(br, title);

        ch.addLine("<div class=\"note-box\">Color coding:");
        ch.addLine("<div class=\"level75\">Component is in created state more then 75% of the time</div>");
        ch.addLine("<div class=\"level50\">Component is in created state more then 50% of the time</div>");
        ch.addLine("<div class=\"level25\">Component is in created state more then 25% of the time</div>");
        ch.addLine("</div>");

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
        tg.setTableName(br, csv);
        tg.addColumn("Pkg", null, "pkg varchar", TableGen.FLAG_NONE);
        tg.addColumn("Cls", null, "cls varchar", TableGen.FLAG_NONE);
        tg.addColumn("Created count", null, "created_count int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time", null, "created_time varchar", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time(ms)", null, "created_time_ms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time(%)", null, "created_time_p int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Max created time(ms)", null, "created_time_max int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Avg created time(ms)", null, "created_time_avg int", TableGen.FLAG_ALIGN_RIGHT);
        if (type == AMData.ACTIVITY) {
            tg.addColumn("Resumed count", null, "resumed_count int", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Total resumed time", null, "resumed_time varchar", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Total resumed time(ms)", null, "resumed_time_ms int", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Total resumed time(%)", null, "resumed_time_p int", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Max resumed time(ms)", null, "resumed_time_max int", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Avg resumed time(ms)", null, "resumed_time_avg int", TableGen.FLAG_ALIGN_RIGHT);
        }
        tg.begin();

        for (ComponentStat stat: set.values()) {
            // Make sure the component is finished
            stat.finish();

            // Decide on coloring
            long createdTimePerc = stat.totalCreatedTime * 100 / duration;
            String style = "";
            if (createdTimePerc > 75) {
                style = "level75";
            } else if (createdTimePerc > 50) {
                style = "level50";
            } else if (createdTimePerc > 25) {
                style = "level25";
            }
            if (stat.errors != 0) {
                style += " err-row";
            }
            tg.setNextRowStyle(style);

            // Dump data
            tg.addData(stat.pkg);
            tg.addData(stat.cls);
            tg.addData(Util.shadeValue(stat.createCount));
            tg.addData(Util.formatTS(stat.totalCreatedTime));
            tg.addData(Util.shadeValue(stat.totalCreatedTime));
            tg.addData(createdTimePerc + "%");
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

    public boolean isEmpty() {
        return mActivities.isEmpty() && mServices.isEmpty();
    }

    public Vector<ComponentStat> getServiceStatsOfPackage(String pkg) {
        Vector<ComponentStat> ret = new Vector<ComponentStat>();
        for (ComponentStat stat : mServices.values()) {
            if (stat.pkg.equals(pkg)) {
                ret.add(stat);
            }
        }
        return ret;
    }

}
