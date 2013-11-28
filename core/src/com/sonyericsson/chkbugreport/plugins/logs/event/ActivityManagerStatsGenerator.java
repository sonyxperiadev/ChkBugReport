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
import java.util.Vector;

// TODO: hide this
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
        Chapter ch = new Chapter(br.getContext(), "AM Stats");
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
        Chapter ch = new Chapter(br.getContext(), title);

        new Block(ch).addStyle("note-box")
            .add("Color coding:")
            .add(new Block().addStyle("level75").add("Component is in created state more then 75% of the time"))
            .add(new Block().addStyle("level50").add("Component is in created state more then 50% of the time"))
            .add(new Block().addStyle("level25").add("Component is in created state more then 25% of the time"));

        new Hint(ch).add("Duration " + duration + "ms = " + Util.formatTS(duration));

        // Check for errors
        int errors = 0;
        for (ComponentStat stat: set.values()) {
            errors += stat.errors;
        }
        if (errors > 0) {
            new Block(ch).addStyle("err")
                .add("NOTE: " + errors + " errors/inconsistencies found in the log, " +
                        "statistics might not be correct! The affected components have been highlighted below.</div>");
        }

        Table t = new Table(Table.FLAG_SORT, ch);
        t.setCSVOutput(br, csv);
        t.setTableName(br, csv);
        t.addColumn("Pkg", null, Table.FLAG_NONE, "pkg varchar");
        t.addColumn("Cls", null, Table.FLAG_NONE, "cls varchar");
        t.addColumn("Created count", null, Table.FLAG_ALIGN_RIGHT, "created_count int");
        t.addColumn("Total created time", null, Table.FLAG_ALIGN_RIGHT, "created_time varchar");
        t.addColumn("Total created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_ms int");
        t.addColumn("Total created time(%)", null, Table.FLAG_ALIGN_RIGHT, "created_time_p int");
        t.addColumn("Max created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_max int");
        t.addColumn("Avg created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_avg int");
        if (type == AMData.ACTIVITY) {
            t.addColumn("Resumed count", null, Table.FLAG_ALIGN_RIGHT, "resumed_count int");
            t.addColumn("Total resumed time", null, Table.FLAG_ALIGN_RIGHT, "resumed_time varchar");
            t.addColumn("Total resumed time(ms)", null, Table.FLAG_ALIGN_RIGHT, "resumed_time_ms int");
            t.addColumn("Total resumed time(%)", null, Table.FLAG_ALIGN_RIGHT, "resumed_time_p int");
            t.addColumn("Max resumed time(ms)", null, Table.FLAG_ALIGN_RIGHT, "resumed_time_max int");
            t.addColumn("Avg resumed time(ms)", null, Table.FLAG_ALIGN_RIGHT, "resumed_time_avg int");
        }
        t.begin();

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
            t.setNextRowStyle(style);

            // Dump data
            t.addData(stat.pkg);
            t.addData(stat.cls);
            t.addData(new ShadedValue(stat.createCount));
            t.addData(Util.formatTS(stat.totalCreatedTime));
            t.addData(new ShadedValue(stat.totalCreatedTime));
            t.addData(createdTimePerc + "%");
            t.addData(new ShadedValue(stat.maxCreatedTime));
            if (stat.createCount == 0) {
                t.addData("");
            } else {
                t.addData(new ShadedValue(stat.totalCreatedTime / stat.createCount));
            }
            if (type == AMData.ACTIVITY) {
                t.addData(new ShadedValue(stat.resumeCount));
                t.addData(Util.formatTS(stat.totalResumedTime));
                t.addData(new ShadedValue(stat.totalResumedTime));
                t.addData(stat.totalResumedTime * 100 / duration + "%");
                t.addData(new ShadedValue(stat.maxResumedTime));
                if (stat.resumeCount == 0) {
                    t.addData("");
                } else {
                    t.addData(new ShadedValue(stat.totalResumedTime / stat.resumeCount));
                }
            }
        }
        t.end();
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
