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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.PackageInfoPlugin.PackageInfo;
import com.sonyericsson.chkbugreport.plugins.logs.event.ActivityManagerStatsGenerator;
import com.sonyericsson.chkbugreport.plugins.logs.event.ComponentStat;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

public class UsageHistoryPlugin extends Plugin {

    private static final String TAG = "[UsageHistoryPlugin]";
    private XMLNode mData;
    private HashMap<String, PackageStat> mStats;

    @Override
    public int getPrio() {
        return 92;
    }

    @Override
    public void reset() {
        mData = null;
    }

    @Override
    public void load(Module br) {
        Section s = br.findSection(Section.USAGE_HISTORY);
        if (s == null) {
            br.printErr(3, TAG + "Cannot find section: " + Section.USAGE_HISTORY);
            return;
        }
        mData = XMLNode.parse(s.createInputStream());
        HashMap<String, PackageStat> stats = new HashMap<String, UsageHistoryPlugin.PackageStat>();
        if (!mData.getName().equals("usage-history")) {
            br.printErr(4, TAG + "Cannot parse section " + Section.USAGE_HISTORY + ": root tag invalid: " + mData.getName());
            return;
        }
        for (XMLNode pkg : mData) {
            if (pkg.getName() == null) continue; // ignore text
            if (!"pkg".equals(pkg.getName())) {
                br.printErr(4, TAG + "Cannot parse section " + Section.USAGE_HISTORY + ": package tag invalid: " + pkg.getName());
                return;
            }
            PackageStat pkgStat = new PackageStat();
            pkgStat.pkg = pkg.getAttr("name");
            for (XMLNode act : pkg) {
                if (act.getName() == null) continue; // ignore text
                if (!"comp".equals(act.getName())) {
                    br.printErr(4, TAG + "Cannot parse section " + Section.USAGE_HISTORY + ": component tag invalid: " + act.getName());
                    return;
                }
                ActivityStat actStat = new ActivityStat();
                actStat.pkg = pkgStat.pkg;
                actStat.cls = act.getAttr("name");
                actStat.lrt = Long.parseLong(act.getAttr("lrt"));
                pkgStat.lrt = Math.max(pkgStat.lrt, actStat.lrt);
                pkgStat.activities.add(actStat);
            }
            stats.put(pkgStat.pkg, pkgStat);
        }
        mStats = stats;
    }

    @Override
    public void generate(Module br) {
        if (mStats == null) return;

        EventLogPlugin plugin = (EventLogPlugin) br.getPlugin("EventLogPlugin");
        ActivityManagerStatsGenerator amStats = plugin.getActivityMStats();
        if (amStats == null || amStats.isEmpty()) {
            br.printErr(3, TAG + "Cannot find AM statistics");
            return;
        }
        PackageInfoPlugin pkgPlugin = (PackageInfoPlugin) br.getPlugin("PackageInfoPlugin");
        if (pkgPlugin.isEmpty()) {
            br.printErr(3, TAG + "Cannot find package list");
            return;
        }

        Chapter ch = new Chapter(br.getContext(), "Usage history");
        br.addChapter(ch);
        long lastTs = plugin.getLastTs();
        long duration = lastTs - plugin.getFirstTs();
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        new Hint(ch).add("NOTE: The age is calculated using as reference the time when chkbugreport was calculated");
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, "usage_history_vs_log");
        tg.setTableName(br, "usage_history_vs_log");
        tg.addColumn("Package", null, Table.FLAG_NONE, "pkg varchar");
        tg.addColumn("Type", null, Table.FLAG_NONE, "type varchar");
        tg.addColumn("Last used", null, Table.FLAG_NONE, "last_used varchar");
        tg.addColumn("Age", null, Table.FLAG_ALIGN_RIGHT, "age int");
        tg.addColumn("Services started", null, Table.FLAG_ALIGN_RIGHT, "services_started int");
        tg.addColumn("Max created time(ms)", null, Table.FLAG_ALIGN_RIGHT, "created_time_max_ms int");
        tg.addColumn("Max created time(%)", null, Table.FLAG_ALIGN_RIGHT, "created_time_max_p int");

        tg.begin();
        for (PackageInfo pkg : pkgPlugin.getPackages()) {
            tg.addData(pkg.getName());
            tg.addData((pkg.getFlags() & 1) == 1 ? "System" : "Installed");

            PackageStat stat = mStats.get(pkg.getName());
            if (stat == null) {
                tg.addData("");
                tg.addData("");
            } else {
                long age = (now - stat.lrt) / 1000 / 60 / 60 / 24;
                tg.addData(sdf.format(new Date(stat.lrt)));
                tg.addData(Long.toString(age));
            }

            Vector<ComponentStat> srvStats = amStats.getServiceStatsOfPackage(pkg.getName());
            if (srvStats == null || srvStats.isEmpty()) {
                tg.addData("");
                tg.addData("");
                tg.addData("");
            } else {
                long max = 0;
                int count = 0;
                for (ComponentStat cs : srvStats) {
                    max = Math.max(max, cs.maxCreatedTime);
                    count += cs.createCount;
                }
                tg.addData(count);
                tg.addData(new ShadedValue(max));
                tg.addData(max * 100 / duration);
            }
        }
        tg.end();
    }

    @SuppressWarnings("unused")
    private static class ActivityStat {
        public String pkg;
        public String cls;
        public long lrt;
    }

    private static class PackageStat {
        public String pkg;
        public long lrt;
        public Vector<ActivityStat> activities = new Vector<ActivityStat>();
    }

}
