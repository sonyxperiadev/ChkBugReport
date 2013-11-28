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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.util.Util;

import java.util.HashMap;

/* package */ class ActivityManagerProcStatsGenerator {

    private EventLogPlugin mPlugin;
    private ActivityManagerTrace mAmTrace;
    private HashMap<String, ProcStat> mStats = new HashMap<String, ProcStat>();

    public ActivityManagerProcStatsGenerator(EventLogPlugin plugin, ActivityManagerTrace amTrace) {
        mPlugin = plugin;
        mAmTrace = amTrace;
    }

    /**
     * Generate statistics based on AM proc logs
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
        Chapter ch = new Chapter(br.getContext(), "AM Proc Stats");
        mainCh.addChapter(ch);

        new Block(ch).addStyle("note-box")
            .add("Color coding:")
            .add(new Block().addStyle("level75").add("Process is alive more then 75% of the time"))
            .add(new Block().addStyle("level50").add("Process is alive more then 50% of the time"))
            .add(new Block().addStyle("level25").add("Process is alive more then 25% of the time"));

        // Process each sample and measure the runtimes
        for (int i = 0; i < cnt; i++) {
            AMData am = mAmTrace.get(i);
            if (am.getType() != AMData.PROC) {
                continue;
            }

            String component = am.getComponent();
            if (component == null) continue;

            ProcStat stat = mStats.get(component);
            if (stat == null) {
                stat = new ProcStat(br, component, firstTs, lastTs);
                mStats.put(component, stat);
            }

            stat.addData(am);
        }

        // Generate statistics table
        new Hint(ch).add("Duration " + duration + "ms = " + Util.formatTS(duration));

        // Check for errors
        int errors = 0;
        for (ProcStat stat: mStats.values()) {
            errors += stat.errors;
        }
        if (errors > 0) {
            new Block(ch).addStyle("err")
                .add("NOTE: " + errors + " errors/inconsistencies found in the log, " +
                    "statistics might not be correct! The affected components have been highlighted below.</div>");
        }

        Table t = new Table(Table.FLAG_SORT, ch);
        t.setCSVOutput(br, "eventlog_amdata_proc");
        t.setTableName(br, "eventlog_amdata_proc");
        t.addColumn("Proc", null, Table.FLAG_NONE, "proc varchar");

        t.addColumn("Created count", null, Table.FLAG_ALIGN_RIGHT, "created_count int");
        t.addColumn("Total created time", null, Table.FLAG_ALIGN_RIGHT, "created_time varchar");
        t.addColumn("Total created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_ms int");
        t.addColumn("Total created time(%)", null, Table.FLAG_ALIGN_RIGHT, "created_time_p int");
        t.addColumn("Max created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_max int");
        t.addColumn("Avg created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_avg int");

        t.addColumn("Restart count", null, Table.FLAG_ALIGN_RIGHT, "restart_count int");
        t.addColumn("Min restart time(ms)", null, Table.FLAG_ALIGN_RIGHT, "restart_time_min int");
        t.addColumn("Avg restart time(ms)", null, Table.FLAG_ALIGN_RIGHT, "restart_time_avg int");

        t.addColumn("Restart after kill count", null, Table.FLAG_ALIGN_RIGHT, "restart_after_kill_count int");
        t.addColumn("Min restart after kill time(ms)", null, Table.FLAG_ALIGN_RIGHT, "restart_after_kill_time_min int");
        t.addColumn("Avg restart after kill time(ms)", null, Table.FLAG_ALIGN_RIGHT, "restart_after_kill_time_avg int");

        t.begin();

        for (ProcStat stat: mStats.values()) {
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
            t.setNextRowStyle(style);

            // Dump the data
            t.addData(stat.proc);

            t.addData(new ShadedValue(stat.count));
            t.addData(Util.formatTS(stat.totalTime));
            t.addData(new ShadedValue(stat.totalTime));
            t.addData(totalTimePerc + "%");
            t.addData(new ShadedValue(stat.maxTime));
            if (stat.count == 0) {
                t.addData("");
            } else {
                t.addData(new ShadedValue(stat.totalTime / stat.count));
            }

            t.addData(new ShadedValue(stat.restartCount));
            t.addData(new ShadedValue(stat.minRestartTime));
            if (stat.restartCount == 0) {
                t.addData("");
            } else {
                t.addData(new ShadedValue(stat.totalRestartTime / stat.restartCount));
            }

            t.addData(new ShadedValue(stat.bgKillRestartCount));
            t.addData(new ShadedValue(stat.minBgKillRestartTime));
            if (stat.bgKillRestartCount == 0) {
                t.addData("");
            } else {
                t.addData(new ShadedValue(stat.totalBgKillRestartTime / stat.bgKillRestartCount));
            }
        }
        t.end();
    }

}
