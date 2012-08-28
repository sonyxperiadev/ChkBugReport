package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.util.TableGen;

import java.util.HashMap;

public class ActivityManagerProcStatsGenerator {

    private EventLogPlugin mPlugin;
    private ActivityManagerTrace mAmTrace;

    public ActivityManagerProcStatsGenerator(EventLogPlugin plugin, ActivityManagerTrace amTrace) {
        mPlugin = plugin;
        mAmTrace = amTrace;
    }

    /**
     * Generate statistics based on AM proc logs
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
        Chapter ch = new Chapter(br, "AM Proc Stats");
        mainCh.addChapter(ch);

        ch.addLine("<div class=\"note-box\">Color coding:");
        ch.addLine("<div class=\"level75\">Process is alive more then 75% of the time</div>");
        ch.addLine("<div class=\"level50\">Process is alive more then 50% of the time</div>");
        ch.addLine("<div class=\"level25\">Process is alive more then 25% of the time</div>");
        ch.addLine("</div>");

        // Process each sample and measure the runtimes
        HashMap<String, ProcStat> stats = new HashMap<String, ProcStat>();
        for (int i = 0; i < cnt; i++) {
            AMData am = mAmTrace.get(i);
            if (am.getType() != AMData.PROC) {
                continue;
            }

            String component = am.getComponent();
            if (component == null) continue;

            ProcStat stat = stats.get(component);
            if (stat == null) {
                stat = new ProcStat(br, component, firstTs, lastTs);
                stats.put(component, stat);
            }

            stat.addData(am);
        }

        // Generate statistics table
        ch.addLine("<div class=\"hint\">(Duration " + duration + "ms = " + Util.formatTS(duration) + ")</div>");

        // Check for errors
        int errors = 0;
        for (ProcStat stat: stats.values()) {
            errors += stat.errors;
        }
        if (errors > 0) {
            ch.addLine("<div class=\"err\">NOTE: " + errors + " errors/inconsistencies found in the log, " +
                    "statistics might not be correct! The affected components have been highlighted below.</div>");
        }

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, "eventlog_amdata_proc");
        tg.setTableName(br, "eventlog_amdata_proc");
        tg.addColumn("Proc", null, "proc varchar", TableGen.FLAG_NONE);

        tg.addColumn("Created count", null, "created_count int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time", null, "created_time varchar", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time(ms)", null, "created_time_ms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total created time(%)", null, "created_time_p int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Max created time(ms)", null, "created_time_max int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Avg created time(ms)", null, "created_time_avg int", TableGen.FLAG_ALIGN_RIGHT);

        tg.addColumn("Restart count", null, "restart_count int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Min restart time(ms)", null, "restart_time_min int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Avg restart time(ms)", null, "restart_time_avg int", TableGen.FLAG_ALIGN_RIGHT);

        tg.addColumn("Restart after kill count", null, "restart_after_kill_count int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Min restart after kill time(ms)", null, "restart_after_kill_time_min int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Avg restart after kill time(ms)", null, "restart_after_kill_time_avg int", TableGen.FLAG_ALIGN_RIGHT);

        tg.begin();

        for (ProcStat stat: stats.values()) {
            // Make sure the component is finished
            stat.finish();

            // Decide on coloring
            long totalTimePerc = stat.totalTime * 100 / duration;
            String style = "";
            if (totalTimePerc > 75) {
                style = "level75";
            } else if (totalTimePerc > 50) {
                style = "level50";
            } else if (totalTimePerc > 25) {
                style = "level25";
            }
            if (stat.errors != 0) {
                style += " err-row";
            }
            tg.setNextRowStyle(style);

            // Dump the data
            tg.addData(stat.proc);

            tg.addData(Util.shadeValue(stat.count));
            tg.addData(Util.formatTS(stat.totalTime));
            tg.addData(Util.shadeValue(stat.totalTime));
            tg.addData(totalTimePerc + "%");
            tg.addData(Util.shadeValue(stat.maxTime));
            tg.addData(stat.count == 0 ? "" : Util.shadeValue(stat.totalTime / stat.count));

            tg.addData(Util.shadeValue(stat.restartCount));
            tg.addData(Util.shadeValue(stat.minRestartTime));
            tg.addData(stat.restartCount == 0 ? "" : Util.shadeValue(stat.totalRestartTime / stat.restartCount));

            tg.addData(Util.shadeValue(stat.bgKillRestartCount));
            tg.addData(Util.shadeValue(stat.minBgKillRestartTime));
            tg.addData(stat.bgKillRestartCount == 0 ? "" : Util.shadeValue(stat.totalBgKillRestartTime / stat.bgKillRestartCount));
        }
        tg.end();
    }


}
