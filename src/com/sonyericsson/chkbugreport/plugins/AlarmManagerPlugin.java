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

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.util.DumpTree;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;
import com.sonyericsson.chkbugreport.util.TableGen;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmManagerPlugin extends Plugin {

    private static final String TAG = "[AlarmManagerPlugin]";

    private boolean mLoaded;
    private Section mSection;
    private Vector<Alarm> mAlarms = new Vector<Alarm>();
    private Vector<AlarmStat> mStats = new Vector<AlarmStat>();

    @Override
    public int getPrio() {
        return 91;
    }

    @Override
    public void load(Module br) {
        // Reset
        mLoaded = false;
        mSection = null;
        mAlarms.clear();

        // Load data
        mSection = br.findSection(Section.DUMP_OF_SERVICE_ALARM);
        if (mSection == null) {
            br.printErr(3, TAG + "Section not found: " + Section.DUMP_OF_SERVICE_ALARM + " (aborting plugin)");
            return;
        }

        // Parse the data
        DumpTree dump = new DumpTree(mSection, 0);
        final String nodeKey = "Current Alarm Manager state:";
        DumpTree.Node root = dump.find(nodeKey);
        if (root == null) {
            br.printErr(3, "Cannot find node '" + nodeKey + "'");
            return;
        }
        boolean stats = false;
        for (DumpTree.Node item : root) {
            String line = item.getLine();
            if (stats) {
                addPackageStat(br, item);
            } else if (line.startsWith("Alarm Stats:")) {
                stats = true;
            } else if (line.startsWith("ELAPSED ")) {
                addAlarm(br, item);
            } else if (line.startsWith("ELAPSED_WAKEUP ")) {
                addAlarm(br, item);
            } else if (line.startsWith("RTC ")) {
                addAlarm(br, item);
            } else if (line.startsWith("RTC_WAKEUP ")) {
                addAlarm(br, item);
            }
        }

        // Done
        mLoaded = true;
    }

    private void addPackageStat(Module br, Node item) {
        AlarmStat stat = new AlarmStat();
        stat.pkg = item.getLine();
        for (int i = 0; i < item.getChildCount(); i++) {
            Node child = item.getChild(i);
            if (i == 0) {
                Pattern p = Pattern.compile("(.*)ms running, (.*) wakeups");
                Matcher m = p.matcher(child.getLine());
                if (!m.matches()) {
                    br.printErr(4, "Cannot parse alarm stat: " + child.getLine());
                    return;
                }
                stat.runtime = Long.parseLong(m.group(1));
                stat.wakeups = Long.parseLong(m.group(2));
            } else {
                Pattern p = Pattern.compile("(.*) alarms: (.*)");
                Matcher m = p.matcher(child.getLine());
                if (!m.matches()) {
                    br.printErr(4, "Cannot parse alarm stat: " + child.getLine());
                    return;
                }
                AlarmAction action = new AlarmAction();
                action.count = Long.parseLong(m.group(1));
                action.action = m.group(2);
                stat.actions.add(action);
                stat.alarms += action.count;
            }
        }
        mStats.add(stat);
    }

    private void addAlarm(Module br, Node item) {
        Alarm alarm = new Alarm();
        Pattern p = Pattern.compile("([A-Z_]+) #[0-9]+: Alarm\\{[a-f0-9]+ type [0-3] (.*)\\}");
        Matcher m = p.matcher(item.getLine());
        if (!m.matches()) {
            br.printErr(4, "Cannot parse alarm: " + item.getLine());
            return;
        }

        alarm.type = m.group(1);
        alarm.pkg = m.group(2);

        String props = item.getChild(0).getLine();
        p = Pattern.compile("type=(.?) when=(.*) repeatInterval=(.*) count=(.*)");
        m = p.matcher(props);
        if (!m.matches()) {
            br.printErr(4, "Cannot parse alarm properties: " + props);
            return;
        }
        alarm.whenS = m.group(2);
        alarm.when = readTs(alarm.whenS);
        alarm.repeat = Integer.parseInt(m.group(3));
        alarm.count = Integer.parseInt(m.group(4));

        String op = item.getChild(1).getLine();
        p = Pattern.compile("operation=PendingIntent\\{[0-9a-f]+: PendingIntentRecord\\{[0-9a-f]+ (.*) ([a-zA-Z]*)\\}\\}");
        m = p.matcher(op);
        if (!m.matches()) {
            br.printErr(4, "Cannot parse alarm operation: " + op);
            return;
        }
        alarm.opPkg = m.group(1);
        alarm.opMet = m.group(2);

        mAlarms.add(alarm);
    }

    private long readTs(String s) {
        s = Util.strip(s);
        long ret = 0;
        int idx;

        // skip over the negative and positive signs
        if (s.charAt(0) == '-') {
            s = s.substring(1);
        }
        if (s.charAt(0) == '+') {
            s = s.substring(1);
        }

        // Remove the "ms" from the end... it screws up our parsing
        if (s.endsWith("ms")) {
            s = s.substring(0, s.length() - 2);
        }

        // Ignore everything before the first digit
        while (s.length() > 0 && !Character.isDigit(s.charAt(0))) {
            s = s.substring(1);
        }

        // parse day
        idx = s.indexOf("d");
        if (idx >= 0) {
            int day = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += day * (24 * 3600000L);
        }
        // parse hours
        idx = s.indexOf("h");
        if (idx >= 0) {
            int hour = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += hour * 3600000L;
        }

        // parse minutes
        idx = s.indexOf("m");
        if (idx >= 0) {
            int min = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += min * 60000L;
        }

        // parse seconds
        idx = s.indexOf("s");
        if (idx >= 0) {
            int sec = Integer.parseInt(s.substring(0, idx));
            s = s.substring(idx + 1);
            ret += sec * 1000L;
        }

        // parse millis
        int ms = Integer.parseInt(s);
        ret += ms;

        return ret;
    }

    @Override
    public void generate(Module rep) {
        if (!mLoaded) return;
        BugReportModule br = (BugReportModule) rep;

        // Generate the report
        Chapter mainCh = new Chapter(br, "AlarmManager");
        br.addChapter(mainCh);

        genAlarmList(br, mainCh);
        genAlarmStat(br, mainCh);
        genAlarmStatDetailed(br, mainCh);
        genAlarmStatCombined(br, mainCh);
    }

    private Chapter genAlarmList(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Alarms");
        mainCh.addChapter(ch);

        ch.addLine("List of alarms (" + mAlarms.size() + "):");

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, "alarm_list");
        tg.setTableName(br, "alarm_list");
        tg.addColumn("Type", null, "type", TableGen.FLAG_NONE);
        tg.addColumn("Pkg", null, "pkg", TableGen.FLAG_NONE);
        tg.addColumn("Time", null, "time varchar", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Time(ms)", null, "time_ms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Interv(ms)", null, "interv_ms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Count", null, "count int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("OpPkg", null, "op_pkg varchar", TableGen.FLAG_NONE);
        tg.addColumn("OpMet", null, "op_met varchar", TableGen.FLAG_NONE);
        tg.begin();

        for (Alarm alarm : mAlarms) {
            tg.addData(alarm.type);
            tg.addData(alarm.pkg);
            tg.addData(alarm.whenS);
            tg.addData(Util.shadeValue(alarm.when));
            tg.addData(Util.shadeValue(alarm.repeat));
            tg.addData(alarm.count);
            tg.addData(alarm.opPkg);
            tg.addData(alarm.opMet);
        }
        tg.end();
        return ch;
    }

    private String createLink(AlarmStat stat) {
        return "detail_" + stat.hashCode();
    }

    private Chapter genAlarmStat(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Alarm stats");
        mainCh.addChapter(ch);
        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, "alarm_stat");
        tg.setTableName(br, "alarm_stat");
        tg.addColumn("Pkg", null, "pkg", TableGen.FLAG_NONE);
        tg.addColumn("Runtime(ms)", null, "runtime_ms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Wakeups", null, "wakeups int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Alarms", null, "alarms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.begin();

        for (AlarmStat stat : mStats) {
            tg.addData("#" + createLink(stat), stat.pkg, TableGen.FLAG_NONE);
            tg.addData(Util.shadeValue(stat.runtime));
            tg.addData(Util.shadeValue(stat.wakeups));
            tg.addData(Util.shadeValue(stat.alarms));
        }
        tg.end();
        return ch;
    }

    private Chapter genAlarmStatDetailed(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Alarm detailed stats");
        mainCh.addChapter(ch);

        for (AlarmStat stat : mStats) {
            Chapter childCh = new Chapter(br, stat.pkg);
            ch.addChapter(childCh);

            childCh.addLine("<a name=\"" + createLink(stat) + "\"/>");
            TableGen tg = new TableGen(childCh, TableGen.FLAG_SORT);
            tg.setCSVOutput(br, "alarm_stat_detailed_" + stat.pkg);
            tg.addColumn("Alarms", TableGen.FLAG_ALIGN_RIGHT);
            tg.addColumn("Action", TableGen.FLAG_NONE);
            tg.begin();

            for (AlarmAction act : stat.actions) {
                tg.addData(Util.shadeValue(act.count));
                tg.addData(act.action);
            }
            tg.end();
        }
        return ch;
    }

    private Chapter genAlarmStatCombined(BugReportModule br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Alarm combined stats");
        mainCh.addChapter(ch);

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, "alarm_stat_combined");
        tg.setTableName(br, "alarm_stat_combined");
        tg.addColumn("Pkg", null, "pkg varchar", TableGen.FLAG_NONE);
        tg.addColumn("Alarms", null, "alarms int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Action", null, "action varchar", TableGen.FLAG_NONE);
        tg.begin();

        for (AlarmStat stat : mStats) {
            for (AlarmAction act : stat.actions) {
                tg.addData(stat.pkg);
                tg.addData(Util.shadeValue(act.count));
                tg.addData(act.action);
            }
        }

        tg.end();
        return ch;
    }

    class Alarm {
        public String type;
        public String pkg;
        public String whenS;
        public long when;
        public long repeat;
        public int count;
        public String opPkg;
        public String opMet;
    }

    class AlarmStat {
        public long runtime;
        public String pkg;
        public long wakeups;
        public long alarms;
        public Vector<AlarmAction> actions = new Vector<AlarmAction>();
    }

    class AlarmAction {
        public String action;
        public long count;
    }
}
