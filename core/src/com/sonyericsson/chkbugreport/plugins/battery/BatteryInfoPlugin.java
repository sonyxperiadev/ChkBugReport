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
package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.GuessedValue;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.chart.ChartPluginInfo;
import com.sonyericsson.chkbugreport.chart.ChartPluginRepo;
import com.sonyericsson.chkbugreport.chart.Data;
import com.sonyericsson.chkbugreport.chart.DataSet;
import com.sonyericsson.chkbugreport.chart.DataSet.Type;
import com.sonyericsson.chkbugreport.chart.DataSetInfo;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.List;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.PreText;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.PackageInfoPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.SystemLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.DumpTree;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;
import com.sonyericsson.chkbugreport.util.LineReader;
import com.sonyericsson.chkbugreport.util.Util;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BatteryInfoPlugin extends Plugin {

    private static final String TAG = "[BatteryInfoPlugin]";

    private static final Pattern pBATTERY_LEVEL_LOG = Pattern.compile("\\s+(\\S+)\\s\\((\\d+)\\)\\s+(\\d+).*");

    private static final int MS = 1;
    private static final int SEC = 1000 * MS;
    private static final int MIN = 60 * SEC;
    private static final int HOUR = 60 * MIN;
    private static final int DAY = 24 * HOUR;

    private static final long WAKE_LOG_BUG_THRESHHOLD = 1 * HOUR;

    private Vector<ChartPlugin> mBLChartPlugins = new Vector<ChartPlugin>();

    private HashMap<String, DataSet> mDatas;
    private HashSet<String> mConn;

    private Hooks mHooks = new Hooks(this);

    static class CpuPerUid {
        long usr;
        long krn;
        long fg;
        Anchor uidLink;
        String uidName;
    }

    static class BugState {
        Bug bug;
        DocNode list;
    }

    public BatteryInfoPlugin() {
        addBatteryLevelChartPlugin(new ScreenOnPlugin());
        addBatteryLevelChartPlugin(new DeepSleepPlugin());
        addBatteryLevelChartPlugin(new ConnectivityChangePlugin());
        addBatteryLevelChartPlugin(new NetstatSamplePlugin());
    }

    public void addBatteryLevelChartPlugin(ChartPlugin plugin) {
        mBLChartPlugins.add(plugin);
    }

    @Override
    public int getPrio() {
        return 90;
    }

    @Override
    public void autodetect(Module module, byte[] buff, int offs, int len, GuessedValue<String> type) {
        LineReader lr = new LineReader(buff, offs, len);
        String line = lr.readLine();
        if ("Battery History:".equals(line)) {
            type.set(Section.DUMP_OF_SERVICE_BATTERYINFO, 99);
        }
    }

    @Override
    public void reset() {
        mDatas = null;
        mConn = null;
    }

    @Override
    public void onHook(Module mod, XMLNode hook) {
        mHooks.add(mod, hook);
    }

    @Override
    public void load(Module mod) {
        // NOP
    }

    @Override
    public void generate(Module rep) {
        BugReportModule br = (BugReportModule) rep;
        Chapter ch = br.findOrCreateChapter("Battery info");
        genBatteryInfo(br, ch);
        genBatteryInfoFromLog(br, ch);

        final ChartPluginRepo repo = br.getChartPluginRepo();
        repo.add(new ChartPluginInfo() {
            @Override
            public String getName() {
                return "Battery/From log/Screen on";
            }
            @Override
            public ChartPlugin createInstance() {
                return new ScreenOnPlugin();
            }
        });
        repo.add(new ChartPluginInfo() {
            @Override
            public String getName() {
                return "Battery/From log/Deep sleeps";
            }
            @Override
            public ChartPlugin createInstance() {
                return new DeepSleepPlugin();
            }
        });
        repo.add(new ChartPluginInfo() {
            @Override
            public String getName() {
                return "Battery/From log/Connectivity";
            }
            @Override
            public ChartPlugin createInstance() {
                return new ConnectivityChangePlugin();
            }
        });
        repo.add(new ChartPluginInfo() {
            @Override
            public String getName() {
                return "Battery/From log/Net stat";
            }
            @Override
            public ChartPlugin createInstance() {
                return new NetstatSamplePlugin();
            }
        });
    }

    private void genBatteryInfo(BugReportModule br, Chapter ch) {
        Section sec = br.findSection(Section.DUMP_OF_SERVICE_BATTERYINFO);
        if (sec == null) {
            sec = br.findSection(Section.DUMP_OF_SERVICE_BATTERYSTATS);
        }
        if (sec == null) {
            br.printErr(3, TAG + "Section not found: " + Section.DUMP_OF_SERVICE_BATTERYINFO +
                    " or " + Section.DUMP_OF_SERVICE_BATTERYSTATS + " (ignoring it)");
            return;
        }

        // Find the battery history
        int idx = 0;
        int cnt = sec.getLineCount();
        boolean foundBatteryHistory = false;
        while (idx < cnt) {
            String buff = sec.getLine(idx++);
            if (buff.startsWith("Battery History (")) {
                foundBatteryHistory = true;
                break;
            }
        }

        if (!foundBatteryHistory) {
            br.printErr(3, TAG + "Battery history not found in section " + Section.DUMP_OF_SERVICE_BATTERYINFO);
            idx = 0;
        } else {
            // Find the timestamp
            ChartGenerator chart = new ChartGenerator("Battery history (Beginning of time -> now)");
            mDatas = new HashMap<String, DataSet>();
            mConn = new HashSet<String>();
            DataSet levelDs = new DataSet(Type.PLOT, "Battery level");
            levelDs.setMin(0);
            levelDs.setMax(100);

            // Read the battery history and plot the chart
            while (idx < cnt) {
                String buff = sec.getLine(idx++);
                if (buff.length() == 0) {
                    break;
                }

                Matcher m = pBATTERY_LEVEL_LOG.matcher(buff);
                //Fixme: Handle other types of lines in battery log, for now we just pull levels.
                if(m.matches()) {

                    // Read the timestamp
                    long ts = Util.parseRelativeTimestamp(m.group(1));

                    // Read the battery level
                    String levelS = m.group(3);
                    if (levelS.charAt(0) == ' ') continue; // there is a disturbance in the force...
                    int level = Integer.parseInt(levelS);

                    levelDs.addData(new Data(ts, level));

                    //Fixme: Dig through and parse signals again.
//                    // Parse the signal levels
//                    if (buff.length() > 35) {
//                        buff = buff.substring(35);
//                        String signals[] = buff.split(" ");
//                        for (String s : signals) {
//                            char c = s.charAt(0);
//                            if (c == '+') {
//                                addSignal(ts, s.substring(1), 1);
//                            } else if (c == '-') {
//                                addSignal(ts, s.substring(1), 0);
//                            } else {
//                                int eq = s.indexOf('=');
//                                if (eq > 0) {
//                                    String value = s.substring(eq + 1);
//                                    s = s.substring(0, eq);
//                                    addSignal(ts, s, value);
//                                }
//                            }
//                        }
//                    }
                }
            }

            // Add the datas to the chart
            chart.add(levelDs);
            for (DataSet ds : mDatas.values()) {
                chart.add(ds);
            }
            chart.addPreface(new Hint().add("NOTE: the timestamps are guessed and might not be correct!"));

            // Publish the datasets to the repo
            ChartPluginRepo repo = br.getChartPluginRepo();
            repo.add(new DataSetInfo(levelDs, "Battery/Info"));
            for (DataSet ds : mDatas.values()) {
                repo.add(new DataSetInfo(ds, "Battery/Info"));
            }

            // Add the graph
            chart.setOutput("batteryhistory.png");
            DocNode plot = chart.generate(br);
            if (plot != null) {
                Chapter cch = new Chapter(br.getContext(), "Battery History");
                ch.addChapter(cch);
                cch.add(plot);
            }
        }

        // Parse the rest as indented dump tree
        DumpTree dump = new DumpTree(sec, idx);

        // Extract the "Per-PID Stats"
        DumpTree.Node node = dump.find("Per-PID Stats:", false);
        if (node != null) {
            ch.addChapter(genPerPidStats(br, node));
        }

        // Extract the total statistics (eclair)
        node = dump.find("Total Statistics (Current and Historic):", false);
        if (node != null) {
            Chapter child = new Chapter(br.getContext(), "Total Statistics (Current and Historic)");
            ch.addChapter(child);
            genStats(br, child, node, false, "total");
        }

        // Extract the last run statistics (eclair)
        node = dump.find("Last Run Statistics (Previous run of system):", false);
        if (node != null) {
            Chapter child = new Chapter(br.getContext(), "Last Run Statistics (Previous run of system)");
            ch.addChapter(child);
            genStats(br, child, node, true, "lastrun");
        }

        // Extract the statistics since last charge
        node = dump.find("Statistics since last charge:", false);
        if (node != null) {
            Chapter child = new Chapter(br.getContext(), "Statistics since last charge");
            ch.addChapter(child);
            genStats(br, child, node, true, "sincecharge");
        }

        // Extract the statistics since last unplugged
        node = dump.find("Statistics since last unplugged:", false);
        if (node != null) {
            Chapter child = new Chapter(br.getContext(), "Statistics since last unplugged");
            ch.addChapter(child);
            genStats(br, child, node, true, "sinceunplugged");
        }

    }

    private long getTimestamp(BugReportModule br) {
        // First use the bugreport timestamp
        Calendar cal = br.getTimestamp();
        if (cal != null) {
            // Don't forget the GMT adjustment
            return cal.getTimeInMillis() + br.getContext().getGmtOffset() * HOUR;
        }

        // If not available, use the maximum timestamp from the logs
        long ret = 0;
        ret = adjustBasedOnLog(ret, br, MainLogPlugin.INFO_ID_MAINLOG);
        ret = adjustBasedOnLog(ret, br, SystemLogPlugin.INFO_ID_SYSTEMLOG);
        ret = adjustBasedOnLog(ret, br, EventLogPlugin.INFO_ID_LOG);

        // If still not available, use the current time
        if (ret == 0) {
            ret = System.currentTimeMillis();
        }

        return ret;
    }

    private long adjustBasedOnLog(long ret, BugReportModule mod, String infoId) {
        LogLines logs = (LogLines) mod.getInfo(MainLogPlugin.INFO_ID_SYSTEMLOG);
        if (logs != null && logs.size() > 0) {
            ret = Math.max(ret, logs.get(logs.size() - 1).ts);
        }
        return ret;
    }

    private void genBatteryInfoFromLog(BugReportModule br, Chapter ch) {
        final BatteryLevels bl = (BatteryLevels) br.getInfo(BatteryLevels.INFO_ID);
        if (bl == null) return;

        BatteryLevelGenerator mBLGen = new BatteryLevelGenerator(bl);
        mBLGen.addPlugins(mBLChartPlugins);
        mBLGen.generate(br, ch);

        br.getChartPluginRepo().add(new ChartPluginInfo() {
            @Override
            public String getName() {
                return "Battery/From log/Levels";
            }
            @Override
            public ChartPlugin createInstance() {
                return new BatteryLevelChart(bl, BatteryLevelChart.LEVEL_ONLY);
            }
        });
        br.getChartPluginRepo().add(new ChartPluginInfo() {
            @Override
            public String getName() {
                return "Battery/From log/Levels (+V,+T)";
            }
            @Override
            public ChartPlugin createInstance() {
                return new BatteryLevelChart(bl);
            }
        });
    }

    private void genStats(BugReportModule br, Chapter ch, Node node, boolean detectBugs, String csvPrefix) {
        PackageInfoPlugin pkgInfo = (PackageInfoPlugin) br.getPlugin("PackageInfoPlugin");
        BugState bug = null;
        PreText pre = new PreText(ch);

        Pattern pUID = Pattern.compile("(\\d+|(u\\d+(a|i|ai|s)\\d+)):");

        // Prepare the kernelWakeLock table
        Chapter kernelWakeLock = new Chapter(br.getContext(), "Kernel Wake locks");
        Pattern pKWL = Pattern.compile("Kernel Wake lock (.*?): (.*) \\((.*?) times\\).*");
        Table tgKWL = new Table(Table.FLAG_SORT, kernelWakeLock);
        tgKWL.setCSVOutput(br, "battery_" + csvPrefix + "_kernel_wakelocks");
        tgKWL.setTableName(br, "battery_" + csvPrefix + "_kernel_wakelocks");
        tgKWL.addColumn("Kernel Wakelock", null, Table.FLAG_NONE, "kernel_wakelock varchar");
        tgKWL.addColumn("Count", null, Table.FLAG_ALIGN_RIGHT, "count int");
        tgKWL.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time varchar");
        tgKWL.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tgKWL.begin();

        // Prepare the partial wake lock table
        Chapter partialWakeLock = new Chapter(br.getContext(), "Partial Wake locks");
        new Hint(partialWakeLock).add("Hint: hover over the UID to see it's name.");
        Table tgPWL = new Table(Table.FLAG_SORT, partialWakeLock);
        tgPWL.setCSVOutput(br, "battery_" + csvPrefix + "_partial_wakelocks");
        tgPWL.setTableName(br, "battery_" + csvPrefix + "_partial_wakelocks");
        tgPWL.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tgPWL.addColumn("Wake lock", null, Table.FLAG_NONE, "wakelock varchar");
        tgPWL.addColumn("Count", null, Table.FLAG_ALIGN_RIGHT, "count int");
        tgPWL.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time varchar");
        tgPWL.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tgPWL.begin();

        // Prepare the wake lock table
        Chapter wakeLock = new Chapter(br.getContext(), "Wake locks");
        new Hint(wakeLock).add("Hint: hover over the UID to see it's name.");
        Table tgWL = new Table(Table.FLAG_SORT, wakeLock);
        tgWL.setCSVOutput(br, "battery_" + csvPrefix + "_wakelocks");
        tgWL.setTableName(br, "battery_" + csvPrefix + "_wakelocks");
        tgWL.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tgWL.addColumn("Wake lock", null, Table.FLAG_NONE, "wakelock varchar");
        tgWL.addColumn("Type", null, Table.FLAG_NONE, "type varchar");
        tgWL.addColumn("Count", null, Table.FLAG_ALIGN_RIGHT, "count int");
        tgWL.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time varchar");
        tgWL.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tgWL.begin();

        // Prepare the CPU per UID table
        Chapter cpuPerUid = new Chapter(br.getContext(), "CPU usage per UID");
        new Hint(cpuPerUid).add("Hint: hover over the UID to see it's name.");
        Pattern pProc = Pattern.compile("Proc (.*?):");
        Pattern pCPU = Pattern.compile("CPU: (.*?) usr \\+ (.*?) krn \\; (.*?) fg");
        Table tgCU = new Table(Table.FLAG_SORT, cpuPerUid);
        tgCU.setCSVOutput(br, "battery_" + csvPrefix + "_cpu_per_uid");
        tgCU.setTableName(br, "battery_" + csvPrefix + "_cpu_per_uid");
        tgCU.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tgCU.addColumn("Usr (ms)", null, Table.FLAG_ALIGN_RIGHT, "usr_ms int");
        tgCU.addColumn("Krn (ms)", null, Table.FLAG_ALIGN_RIGHT, "krn_ms int");
        tgCU.addColumn("fg (ms)", null, Table.FLAG_ALIGN_RIGHT, "fg_ms int");
        tgCU.addColumn("Total (ms)", null, Table.FLAG_ALIGN_RIGHT, "total_ms int");
        tgCU.addColumn("Usr (min)", null, Table.FLAG_ALIGN_RIGHT, "usr_min int");
        tgCU.addColumn("Krn (min)", null, Table.FLAG_ALIGN_RIGHT, "krn_min int");
        tgCU.addColumn("Total (min)", null, Table.FLAG_ALIGN_RIGHT, "total_min int");
        tgCU.begin();

        // Prepare the CPU per Proc table
        Chapter cpuPerProc = new Chapter(br.getContext(), "CPU usage per Proc");
        new Hint(cpuPerUid).add("Hint: hover over the UID to see it's name.");
        Table tgCP = new Table(Table.FLAG_SORT, cpuPerProc);
        tgCP.setCSVOutput(br, "battery_" + csvPrefix + "_cpu_per_proc");
        tgCP.setTableName(br, "battery_" + csvPrefix + "_cpu_per_proc");
        tgCP.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tgCP.addColumn("Proc", null, Table.FLAG_NONE, "proc varchar");
        tgCP.addColumn("Usr", null, Table.FLAG_ALIGN_RIGHT, "usr int");
        tgCP.addColumn("Usr (ms)", null, Table.FLAG_ALIGN_RIGHT, "usr_ms int");
        tgCP.addColumn("Krn", null, Table.FLAG_ALIGN_RIGHT, "krn int");
        tgCP.addColumn("Krn (ms)", null, Table.FLAG_ALIGN_RIGHT, "krn_ms int");
        tgCP.addColumn("Fg", null, Table.FLAG_ALIGN_RIGHT, "fg int");
        tgCP.addColumn("Fg (ms)", null, Table.FLAG_ALIGN_RIGHT, "fg_ms int");
        tgCP.addColumn("Total (ms)", null, Table.FLAG_ALIGN_RIGHT, "total_ms int");
        tgCP.begin();

        // Prepare the network traffic table
        Chapter net = new Chapter(br.getContext(), "Network traffic");
        new Hint(cpuPerUid).add("Hint: hover over the UID to see it's name.");
        Pattern pNet = Pattern.compile("Network: (.*?) received, (.*?) sent");
        Table tgNet = new Table(Table.FLAG_SORT, net);
        tgNet.setCSVOutput(br, "battery_" + csvPrefix + "_net");
        tgNet.setTableName(br, "battery_" + csvPrefix + "_net");
        tgNet.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tgNet.addColumn("Received (B)", null, Table.FLAG_ALIGN_RIGHT, "received_b int");
        tgNet.addColumn("Sent (B)", null, Table.FLAG_ALIGN_RIGHT, "sent_b int");
        tgNet.addColumn("Total (B)", null, Table.FLAG_ALIGN_RIGHT, "total_b int");
        tgNet.begin();

        // Process the data
        long sumRecv = 0, sumSent = 0;
        //Kernel Wake Lock and PartialWakeLocks are not indented correctly.
        boolean parsingKWL = false;
        boolean parsingPWL = false;
        HashMap<String, CpuPerUid> cpuPerUidStats = new HashMap<String, CpuPerUid>();

        for (Node item : node) {
            String line = item.getLine();
            Matcher mUID = pUID.matcher(line);
            if (mUID.matches()) {
                String sUID = mUID.group(1);
                PackageInfoPlugin.UID uid = null;
                String uidName = sUID;
                Anchor uidLink = null;
                if (pkgInfo != null) {
                    int uidInt = Util.parseUid(sUID);
                    uid = pkgInfo.getUID(uidInt);
                    if (uid != null) {
                        uidName = uid.getFullName();
                        uidLink = pkgInfo.getAnchorToUid(uid);
                    }
                }

                // Collect wake lock and network traffic data
                for (Node subNode : item) {
                    String s = subNode.getLine();
                    if (s.startsWith("Wake lock")) {
                        WakeLock foundLock = null;
                        try {
                            foundLock = new WakeLock(sUID, s);
                        } catch (NumberFormatException e) {
                            System.err.println("WL: Could not parse line: " + s);
                        }
                        if(foundLock != null) {
                            tgWL.setNextRowStyle(colorizeTime(foundLock.getDurationMs()));
                            if(uidName != null && uidLink != null) {
                                tgWL.addData(uidName, new Link(uidLink, uidName));
                            } else {
                                tgWL.addData(foundLock.getUIDString());
                            }
                            tgWL.addData(foundLock.getName());
                            tgWL.addData(foundLock.getType());
                            tgWL.addData(foundLock.getCount());
                            tgWL.addData(Util.formatTS(foundLock.getDurationMs()));
                            tgWL.addData(new ShadedValue(foundLock.getDurationMs()));
                            if (foundLock.getDurationMs() > WAKE_LOG_BUG_THRESHHOLD) {
                                bug = createBug(br, bug);
                                bug.list.add("Wake lock: " + foundLock.getName());
                            }
                        }
                    } else if (s.startsWith("Network: ")) {
                        Matcher m = pNet.matcher(s);
                        if (m.find()) {
                            long recv = parseBytes(m.group(1));
                            long sent = parseBytes(m.group(2));
                            sumRecv += recv;
                            sumSent += sent;
                            tgNet.addData(uidName, new Link(uidLink, sUID));
                            tgNet.addData(new ShadedValue(recv));
                            tgNet.addData(new ShadedValue(sent));
                            tgNet.addData(new ShadedValue(recv + sent));
                        } else {
                            System.err.println("NW: Could not parse line: " + s);
                        }
                    } else if (s.startsWith("Proc ")) {
                        Matcher mProc = pProc.matcher(s);
                        if (mProc.find()) {
                            String procName = mProc.group(1);
                            Node cpuItem = subNode.findChildStartsWith("CPU:");
                            if (cpuItem != null) {
                                Matcher m = pCPU.matcher(cpuItem.getLine());
                                if (m.find()) {
                                    String sUsr = m.group(1);
                                    long usr = Util.parseRelativeTimestamp(sUsr.replace(" ", ""));
                                    String sKrn = m.group(2);
                                    long krn = Util.parseRelativeTimestamp(sKrn.replace(" ", ""));
                                    String sFg = m.group(3);
                                    long fg = Util.parseRelativeTimestamp(sFg.replace(" ", ""));
                                    CpuPerUid cpu = cpuPerUidStats.get(sUID);
                                    if (cpu == null) {
                                        cpu = new CpuPerUid();
                                        cpu.uidLink = uidLink;
                                        cpu.uidName = uidName;
                                        cpuPerUidStats.put(sUID, cpu);
                                    }
                                    cpu.usr += usr;
                                    cpu.krn += krn;
                                    cpu.fg += fg;

                                    tgCP.addData(uidName, new Link(uidLink, sUID));
                                    tgCP.addData(procName);
                                    tgCP.addData(sUsr);
                                    tgCP.addData(new ShadedValue(usr));
                                    tgCP.addData(sKrn);
                                    tgCP.addData(new ShadedValue(krn));
                                    tgCP.addData(sFg);
                                    tgCP.addData(new ShadedValue(fg));
                                    tgCP.addData(new ShadedValue(usr + krn));
                                } else {
                                    System.err.println("CPU: Could not parse line: " + cpuItem.getLine());
                                }
                            }
                        } else {
                            System.err.println("PROC: Could not parse line: " + s);
                        }
                    }
                }
            } else if (line.startsWith("All kernel wake locks:")){
                parsingKWL = true;
            } else if (line.startsWith("Kernel Wake lock") && parsingKWL) {
                if (!line.contains("(nothing executed)")) {
                    // Collect into table
                    Matcher m = pKWL.matcher(line);
                    if (m.find()) {
                        String name = m.group(1);
                        String sTime = m.group(2);
                        String sCount = m.group(3);
                        long ts = Util.parseRelativeTimestamp(sTime.replace(" ", ""));
                        tgKWL.setNextRowStyle(colorizeTime(ts));
                        tgKWL.addData(name);
                        tgKWL.addData(sCount);
                        tgKWL.addData(sTime);
                        tgKWL.addData(new ShadedValue(ts));
                        if (ts > WAKE_LOG_BUG_THRESHHOLD) {
                            bug = createBug(br, bug);
                            bug.list.add("Kernel Wake lock: " + name);
                        }
                    } else {
                        System.err.println("KWL: Could not parse line: " + line);
                    }
                }
            } else if(line.startsWith("All partial wake locks:")) {
                parsingPWL = true;
            } else if (line.startsWith("Wake lock") && parsingPWL) {
                WakeLock foundLock = null;
                try {
                    foundLock = new WakeLock(line);
                } catch (NumberFormatException e) {
                    System.err.println("WL: Could not parse line: " + line);
                }
                if(foundLock != null) {
                    String uidName = null;
                    Anchor uidLink = null;
                    if (pkgInfo != null) {
                        int uidInt = foundLock.getUID();

                        PackageInfoPlugin.UID uid = pkgInfo.getUID(uidInt);
                        if (uid != null) {
                            uidName = uid.getFullName();
                            uidLink = pkgInfo.getAnchorToUid(uid);
                        }
                    }
                    tgPWL.setNextRowStyle(colorizeTime(foundLock.getDurationMs()));
                    if(uidName != null && uidLink != null) {
                        tgPWL.addData(uidName, new Link(uidLink, uidName));
                    } else {
                        tgPWL.addData(foundLock.getUIDString());
                    }

                    tgPWL.addData(foundLock.getName());
                    tgPWL.addData(foundLock.getCount());
                    tgPWL.addData(Util.formatTS(foundLock.getDurationMs()));
                    tgPWL.addData(new ShadedValue(foundLock.getDurationMs()));
                    if (foundLock.getDurationMs() > WAKE_LOG_BUG_THRESHHOLD) {
                        bug = createBug(br, bug);
                        bug.list.add("Wake lock: " + foundLock.getName());
                    }
                }
            } else {
                parsingKWL = false;
                parsingPWL = false;
                if (item.getChildCount() == 0) {
                    pre.addln(line);
                }
            }
        }

        // Build chapter content

        if (!tgKWL.isEmpty()) {
            tgKWL.end();
            ch.addChapter(kernelWakeLock);
        }

        if (!tgPWL.isEmpty()) {
            tgPWL.end();
            ch.addChapter(partialWakeLock);
        }

        if (!tgWL.isEmpty()) {
            tgWL.end();
            ch.addChapter(wakeLock);
        }

        if (!cpuPerUidStats.isEmpty()) {
            for (String sUid : cpuPerUidStats.keySet()) {
                CpuPerUid cpu = cpuPerUidStats.get(sUid);
                tgCU.addData(cpu.uidName, new Link(cpu.uidLink, sUid));
                tgCU.addData(new ShadedValue(cpu.usr));
                tgCU.addData(new ShadedValue(cpu.krn));
                tgCU.addData(new ShadedValue(cpu.fg));
                tgCU.addData(new ShadedValue(cpu.usr + cpu.krn));
                tgCU.addData(Long.toString(cpu.usr / MIN));
                tgCU.addData(Long.toString(cpu.krn / MIN));
                tgCU.addData(Long.toString((cpu.usr + cpu.krn) / MIN));
            }
            tgCU.end();
            ch.addChapter(cpuPerUid);
        }

        if (!tgCP.isEmpty()) {
            tgCP.end();
            ch.addChapter(cpuPerProc);
        }

        if (!tgNet.isEmpty()) {
            tgNet.addSeparator();
            tgNet.addData("TOTAL:");
            tgNet.addData(new ShadedValue(sumRecv));
            tgNet.addData(new ShadedValue(sumSent));
            tgNet.addData(new ShadedValue(sumRecv + sumSent));
            tgNet.end();
            ch.addChapter(net);
        }

        // Finish and add the bug if created
        if (detectBugs && bug != null) {
            bug.bug.add(new Link(ch.getAnchor(), "Click here for more information"));
            br.addBug(bug.bug);
        }
    }

    private BugState createBug(BugReportModule br, BugState bug) {
        if (bug == null) {
            bug = new BugState();
            bug.bug = new Bug(Bug.Type.PHONE_WARN, Bug.PRIO_POWER_CONSUMPTION, 0, "Suspicious power consumption");
            bug.bug.add(new Para().add("Some wakelocks are taken for too long:"));
            bug.list = new List(List.TYPE_UNORDERED, bug.bug);
        }
        return bug;
    }

    private long parseBytes(String s) {
        long mul = 1;

        if (s.endsWith("MB") || s.endsWith("mb")) {
            s = s.substring(0, s.length() - 2);
            mul = 1024L * 1024L;
        } else if (s.endsWith("KB") || s.endsWith("kb")) {
            s = s.substring(0, s.length() - 2);
            mul = 1024L;
        } else if (s.endsWith("B") || s.endsWith("b")) {
            s = s.substring(0, s.length() - 1);
        }

        s = s.replace(',', '.'); // in some cases ',' might be used as a decimal sign
        if (s.indexOf('.') >= 0) {
            return (long) (mul * Double.parseDouble(s));
        } else {
            return mul * Long.parseLong(s);
        }
    }

    private String colorizeTime(long ts) {
        if (ts >= 1*DAY) {
            return "level100";
        }
        if (ts >= 1*HOUR) {
            return "level75";
        }
        if (ts >= 10*MIN) {
            return "level50";
        }
        if (ts >= 1*MIN) {
            return "level25";
        }
        return null;
    }

    private Chapter genPerPidStats(BugReportModule br, Node node) {
        Chapter ch = new Chapter(br.getContext(), "Per-PID Stats");
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, "battery_per_pid_stats");
        tg.setTableName(br, "battery_per_pid_stats");
        tg.addColumn("PID", null, Table.FLAG_NONE, "pid int");
        tg.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time varchar");
        tg.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tg.begin();

        for (Node item : node) {
            // item.getLine() has the following format:
            // "PID 147 wake time: +2m37s777ms"
            String f[] = item.getLine().split(" ");
            int pid = Integer.parseInt(f[1]);
            tg.addData(new ProcessLink(br, pid));

            String sTime = f[4];
            tg.addData(sTime);

            long ts = Util.parseRelativeTimestamp(sTime);
            tg.addData(new ShadedValue(ts));
        }
        tg.end();
        return ch;
    }

    private void addSignal(long ts, String name, String value) {
        if (name.equals("status")) {
            if (value.equals("charging")) {
                addSignal(ts, "charging", 1);
            } else {
                addSignal(ts, "charging", 0);
            }
        } else if (name.equals("data_conn")) {
            mConn.add(value);
            for (String c : mConn) {
                addSignal(ts, c, value.equals(c) ? 1 : 0);
            }
        } else {
            // TODO: what to do with other signals?
        }
    }

    private void addSignal(long ts, String name, int value) {
        DataSet ds = mDatas.get(name);
        if (ds == null) {
            ds = new DataSet(DataSet.Type.STATE, name);
            ds.addColor(0x80ffffff);
            ds.addColor(0x80000000);
            mDatas.put(name, ds);
        }
        ds.addData(new Data(ts, value));
    }


}
