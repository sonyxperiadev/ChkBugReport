/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
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
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.List;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.PreText;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.PackageInfoPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;
import com.sonyericsson.chkbugreport.plugins.logs.event.NetstatSamples;
import com.sonyericsson.chkbugreport.util.DumpTree;
import com.sonyericsson.chkbugreport.util.DumpTree.Node;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class BatteryInfoPlugin extends Plugin {

    private static final String TAG = "[BatteryInfoPlugin]";

    private static final Signal SIGNALS[] = new Signal[]{
        new Signal("charging", Signal.TYPE_BIN, null, null),
        new Signal("wake_lock", Signal.TYPE_BIN, null, null),
        new Signal("screen", Signal.TYPE_BIN, null, null),
        new Signal("edge", Signal.TYPE_BIN, null, null),
        new Signal("umts", Signal.TYPE_BIN, null, null),
        new Signal("hsdpa", Signal.TYPE_BIN, null, null),
        new Signal("hspa", Signal.TYPE_BIN, null, null),
    };

    private static final int GRAPH_W = 800;
    private static final int GRAPH_H = 300;
    private static final int GRAPH_PX = 700;
    private static final int GRAPH_PY = 250;
    private static final int GRAPH_PW = 600;
    private static final int GRAPH_PH = 200;
    private static final int GRAPH_SH = 25;
    private static final int GRAPH_BG = 10;

    private static final int MAX_LEVEL = 110;

    private static final Color COL_SIGNAL = Color.BLACK;
    private static final Color COL_SIGNAL_PART = Color.GRAY;

    private static final int MS = 1;
    private static final int SEC = 1000 * MS;
    private static final int MIN = 60 * SEC;
    private static final int HOUR = 60 * MIN;
    private static final int DAY = 24 * HOUR;

    private static final long WAKE_LOG_BUG_THRESHHOLD = 1 * HOUR;

    private long mMaxTs;

    private Graphics2D mG;

    private Vector<ChartPlugin> mBLChartPlugins = new Vector<ChartPlugin>();

    static class Signal {
        public static final int TYPE_BIN = 0;
        public static final int TYPE_INT = 1;
        public static final int TYPE_PRC = 2;

        private String mName;
        private int mType;
        private String[] mValueNames;
        private int[] mValues;
        private long mTs;
        private int mValue;

        public Signal(String name, int type, String valueNames[], int values[]) {
            mName = name;
            mType = type;
            mValueNames = valueNames;
            mValues = values;
            mTs = -1;
            mValue = -1;
        }

        public String getName() {
            return mName;
        }

        public int getType() {
            return mType;
        }

        public String[] getValueNames() {
            return mValueNames;
        }

        public int[] getValues() {
            return mValues;
        }

        public int getValue() {
            return mValue;
        }

        public long getTs() {
            return mTs;
        }

        public void setValue(long ts, int value) {
            mTs = ts;
            mValue = value;
        }
    }

    static class CpuPerUid {
        long usr;
        long krn;
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
        addBatteryLevelChartPlugin(new ConnectivityChangePlugin("mobile", "mobile conn"));
        addBatteryLevelChartPlugin(new ConnectivityChangePlugin("WIFI", "wifi conn"));
        addBatteryLevelChartPlugin(new NetstatSamplePlugin(NetstatSamples.INFO_ID_MOBILE, "mobile traffic"));
        addBatteryLevelChartPlugin(new NetstatSamplePlugin(NetstatSamples.INFO_ID_WIFI, "wifi traffic"));
    }

    public void addBatteryLevelChartPlugin(ChartPlugin plugin) {
        mBLChartPlugins.add(plugin);
    }

    @Override
    public int getPrio() {
        return 90;
    }

    @Override
    public void reset() {
        // NOP
    }

    @Override
    public void load(Module br) {
        // NOP
    }

    @Override
    public void generate(Module rep) {
        BugReportModule br = (BugReportModule) rep;

        // Create the main chapter
        Chapter ch = new Chapter(br, "Battery info");

        genBatteryInfo(br, ch);
        genBatteryInfoFromLog(br, ch);

        if (!ch.isEmpty()) {
            br.addChapter(ch);
        }
    }

    private void genBatteryInfo(BugReportModule br, Chapter ch) {
        Section sec = br.findSection(Section.DUMP_OF_SERVICE_BATTERYINFO);
        if (sec == null) {
            br.printErr(3, TAG + "Section not found: " + Section.DUMP_OF_SERVICE_BATTERYINFO + " (ignoring it)");
            return;
        }

        // Find the battery history
        int idx = 0;
        int cnt = sec.getLineCount();
        boolean foundBatteryHistory = false;
        while (idx < cnt) {
            String buff = sec.getLine(idx++);
            if (buff.equals("Battery History:")) {
                foundBatteryHistory = true;
                break;
            }
        }

        if (!foundBatteryHistory) {
            br.printErr(3, TAG + "Battery history not found in section " + Section.DUMP_OF_SERVICE_BATTERYINFO);
            idx = 0;
        } else {
            // Create the image
            int totalH = GRAPH_H + GRAPH_SH * SIGNALS.length + GRAPH_BG;
            BufferedImage img = new BufferedImage(GRAPH_W, totalH, BufferedImage.TYPE_INT_RGB);
            mG = (Graphics2D)img.getGraphics();
            mG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            mG.setColor(Color.WHITE);
            mG.fillRect(0, 0, GRAPH_W, totalH);
            mG.setColor(Color.BLACK);
            mG.drawRect(0, 0, GRAPH_W - 1, totalH - 1);

            // Draw the axis
            int as = 5;
            mG.setColor(Color.BLACK);
            mG.drawLine(GRAPH_PX, GRAPH_PY, GRAPH_PX, GRAPH_PY - GRAPH_PH);
            mG.drawLine(GRAPH_PX, GRAPH_PY, GRAPH_PX - GRAPH_PW, GRAPH_PY);
            mG.drawLine(GRAPH_PX - as, GRAPH_PY - GRAPH_PH + as, GRAPH_PX, GRAPH_PY - GRAPH_PH);
            mG.drawLine(GRAPH_PX + as, GRAPH_PY - GRAPH_PH + as, GRAPH_PX, GRAPH_PY - GRAPH_PH);
            mG.drawLine(GRAPH_PX - GRAPH_PW + as, GRAPH_PY - as, GRAPH_PX - GRAPH_PW, GRAPH_PY);
            mG.drawLine(GRAPH_PX - GRAPH_PW + as, GRAPH_PY + as, GRAPH_PX - GRAPH_PW, GRAPH_PY);

            // Draw the title
            FontMetrics fm = mG.getFontMetrics();
            mG.drawString("Battery history", 10, 10 + fm.getAscent());

            // Draw some guide lines
            Color colGuide = new Color(0xc0c0ff);
            for (int value = 25; value <= 100; value += 25) {
                int yv = toY(value);
                mG.setColor(colGuide);
                mG.drawLine(GRAPH_PX - 1, yv, GRAPH_PX - GRAPH_PW, yv);
                mG.setColor(Color.BLACK);
                String s = "" + value + "%";
                mG.drawString(s, GRAPH_PX + 1, yv);
            }

            // Read the battery history and plot the chart
            mMaxTs = -1;
            long lastTs = -1;
            int lastLevelX = -1;
            int lastLevelY = -1;
            Color colLevel = new Color(0x000000);
            while (idx < cnt) {
                String buff = sec.getLine(idx++);
                if (buff.length() == 0) {
                    break;
                }

                // Read the timestamp
                long ts = readTs(buff.substring(0, 21));
                lastTs = ts;
                if (mMaxTs == -1) {
                    mMaxTs = ts * 110 / 100;
                }
                // Read the battery level
                String levelS = buff.substring(22, 25);
                if (levelS.charAt(0) == ' ') continue; // there is a disturbance in the force...
                int level = Integer.parseInt(levelS);

                // Plot the level
                int levelX = toX(ts);
                int levelY = toY(level);
                if (lastLevelX != -1 && lastLevelY != -1) {
                    mG.setColor(colLevel);
                    mG.drawLine(lastLevelX, lastLevelY, levelX, levelY);
                }

                // Parse the signal levels
                if (buff.length() > 35) {
                    buff = buff.substring(35);
                    String signals[] = buff.split(" ");
                    for (String s : signals) {
                        char c = s.charAt(0);
                        if (c == '+') {
                            addSignal(ts, s.substring(1), 1);
                        } else if (c == '-') {
                            addSignal(ts, s.substring(1), 0);
                        } else {
                            int eq = s.indexOf('=');
                            if (eq > 0) {
                                String value = s.substring(eq + 1);
                                s = s.substring(0, eq);
                                addSignal(ts, s, value);
                            }
                        }
                    }
                }

                lastLevelX = levelX;
                lastLevelY = levelY;
            }

            // Finish off every signal
            for (int i = 0; i < SIGNALS.length; i++) {
                addSignal(lastTs, SIGNALS[i].getName(), -1);
            }

            // Draw labels on time axis
            long step = 30*60*1000L;
            int count = (int)(mMaxTs / step);
            while (count > 10) {
                step *= 2;
                count = (int)(mMaxTs / step);
            }
            for (long ts = step; ts <= mMaxTs; ts += step) {
                int xv = toX(ts);
                int hour = (int)(ts / (60*60*1000L));
                int min = (int)((ts / (60*1000L)) % 60);
                mG.setColor(Color.BLACK);
                mG.drawLine(xv, GRAPH_PY, xv, GRAPH_PY + 5);
                String s = String.format("%d:%02d", hour, min);
                mG.drawString(s, xv, GRAPH_PY + fm.getAscent());
            }

            // Finish and save the graph
            String fn = "batteryhistory.png";
            try {
                ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Add the graph
            Chapter cch = new Chapter(br, "Battery History");
            ch.addChapter(cch);
            cch.add(new Img(fn));
        }

        // Parse the rest as indented dump tree
        DumpTree dump = new DumpTree(sec, idx);

        // Extract the "Per-PID Stats"
        DumpTree.Node node = dump.find("Per-PID Stats:");
        if (node != null) {
            ch.addChapter(genPerPidStats(br, node));
        }

        // Extract the total statistics (eclair)
        node = dump.find("Total Statistics (Current and Historic):");
        if (node != null) {
            Chapter child = new Chapter(br, "Total Statistics (Current and Historic)");
            ch.addChapter(child);
            genStats(br, child, node, false, "total");
        }

        // Extract the last run statistics (eclair)
        node = dump.find("Last Run Statistics (Previous run of system):");
        if (node != null) {
            Chapter child = new Chapter(br, "Last Run Statistics (Previous run of system)");
            ch.addChapter(child);
            genStats(br, child, node, true, "lastrun");
        }

        // Extract the statistics since last charge
        node = dump.find("Statistics since last charge:");
        if (node != null) {
            Chapter child = new Chapter(br, "Statistics since last charge");
            ch.addChapter(child);
            genStats(br, child, node, true, "sincecharge");
        }

        // Extract the statistics since last unplugged
        node = dump.find("Statistics since last unplugged:");
        if (node != null) {
            Chapter child = new Chapter(br, "Statistics since last unplugged");
            ch.addChapter(child);
            genStats(br, child, node, true, "sinceunplugged");
        }

    }

    private void genBatteryInfoFromLog(BugReportModule br, Chapter ch) {
        BatteryLevels bl = (BatteryLevels) br.getInfo(BatteryLevels.INFO_ID);
        if (bl == null) return;

        BatteryLevelGenerator mBLGen = new BatteryLevelGenerator(bl);
        mBLGen.addPlugins(mBLChartPlugins);
        mBLGen.generate(br, ch);
    }

    private void genStats(BugReportModule br, Chapter ch, Node node, boolean detectBugs, String csvPrefix) {
        PackageInfoPlugin pkgInfo = (PackageInfoPlugin) br.getPlugin("PackageInfoPlugin");
        BugState bug = null;
        PreText pre = new PreText(ch);

        // Prepare the kernelWakeLock table
        Chapter kernelWakeLock = new Chapter(br, "Kernel Wake locks");
        Pattern pKWL = Pattern.compile(".*?\"(.*?)\": (.*?) \\((.*?) times\\)");
        Table tgKWL = new Table(Table.FLAG_SORT, kernelWakeLock);
        tgKWL.setCSVOutput(br, "battery_" + csvPrefix + "_kernel_wakelocks");
        tgKWL.setTableName(br, "battery_" + csvPrefix + "_kernel_wakelocks");
        tgKWL.addColumn("Kernel Wakelock", null, Table.FLAG_NONE, "kernel_wakelock varchar");
        tgKWL.addColumn("Count", null, Table.FLAG_ALIGN_RIGHT, "count int");
        tgKWL.addColumn("Time", null, Table.FLAG_ALIGN_RIGHT, "time varchar");
        tgKWL.addColumn("Time(ms)", null, Table.FLAG_ALIGN_RIGHT, "time_ms int");
        tgKWL.begin();

        // Prepare the wake lock table
        Chapter wakeLock = new Chapter(br, "Wake locks");
        new Hint(wakeLock).add("Hint: hover over the UID to see it's name.");
        Pattern pWL = Pattern.compile("Wake lock (.*?): (.*?) ([a-z]+) \\((.*?) times\\)");
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
        Chapter cpuPerUid = new Chapter(br, "CPU usage per UID");
        new Hint(cpuPerUid).add("Hint: hover over the UID to see it's name.");
        Pattern pProc = Pattern.compile("Proc (.*?):");
        Pattern pCPU = Pattern.compile("CPU: (.*?) usr \\+ (.*?) krn");
        Table tgCU = new Table(Table.FLAG_SORT, cpuPerUid);
        tgCU.setCSVOutput(br, "battery_" + csvPrefix + "_cpu_per_uid");
        tgCU.setTableName(br, "battery_" + csvPrefix + "_cpu_per_uid");
        tgCU.addColumn("UID", null, Table.FLAG_ALIGN_RIGHT, "uid int");
        tgCU.addColumn("Usr (ms)", null, Table.FLAG_ALIGN_RIGHT, "usr_ms int");
        tgCU.addColumn("Krn (ms)", null, Table.FLAG_ALIGN_RIGHT, "krn_ms int");
        tgCU.addColumn("Total (ms)", null, Table.FLAG_ALIGN_RIGHT, "total_ms int");
        tgCU.addColumn("Usr (min)", null, Table.FLAG_ALIGN_RIGHT, "usr_min int");
        tgCU.addColumn("Krn (min)", null, Table.FLAG_ALIGN_RIGHT, "krn_min int");
        tgCU.addColumn("Total (min)", null, Table.FLAG_ALIGN_RIGHT, "total_min int");
        tgCU.begin();

        // Prepare the CPU per Proc table
        Chapter cpuPerProc = new Chapter(br, "CPU usage per Proc");
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
        tgCP.addColumn("Total (ms)", null, Table.FLAG_ALIGN_RIGHT, "total_ms int");
        tgCP.begin();

        // Prepare the network traffic table
        Chapter net = new Chapter(br, "Network traffic");
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
        HashMap<String, CpuPerUid> cpuPerUidStats = new HashMap<String, CpuPerUid>();
        for (Node item : node) {
            String line = item.getLine();
            if (line.startsWith("#")) {
                String sUID = line.substring(1, line.length() - 1);
                PackageInfoPlugin.UID uid = null;
                String uidName = sUID;
                Anchor uidLink = null;
                if (pkgInfo != null) {
                    int uidInt = Integer.parseInt(sUID);
                    uid = pkgInfo.getUID(uidInt);
                    if (uid != null) {
                        uidName = uid.getFullName();
                        uidLink = pkgInfo.getAnchorToUid(uid);
                    }
                }

                // Collect wake lock and network traffic data
                for (Node subNode : item) {
                    String s = subNode.getLine();
                    if (s.startsWith("Wake lock") && !s.contains("(nothing executed)")) {
                        Matcher m = pWL.matcher(s);
                        if (m.find()) {
                            String name = m.group(1);
                            String sTime = m.group(2);
                            String type = m.group(3);
                            String sCount = m.group(4);
                            long ts = readTs(sTime.replace(" ", ""));
                            tgWL.setNextRowStyle(colorizeTime(ts));
                            tgWL.addData(uidName, new Link(uidLink, sUID));
                            tgWL.addData(name);
                            tgWL.addData(type);
                            tgWL.addData(sCount);
                            tgWL.addData(sTime);
                            tgWL.addData(new ShadedValue(ts));
                            if (ts > WAKE_LOG_BUG_THRESHHOLD) {
                                bug = createBug(br, bug);
                                bug.list.add("Wake lock: " + name);
                            }
                        } else {
                            System.err.println("Could not parse line: " + s);
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
                            System.err.println("Could not parse line: " + s);
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
                                    long usr = readTs(sUsr.replace(" ", ""));
                                    String sKrn = m.group(2);
                                    long krn = readTs(sKrn.replace(" ", ""));

                                    CpuPerUid cpu = cpuPerUidStats.get(sUID);
                                    if (cpu == null) {
                                        cpu = new CpuPerUid();
                                        cpu.uidLink = uidLink;
                                        cpu.uidName = uidName;
                                        cpuPerUidStats.put(sUID, cpu);
                                    }
                                    cpu.usr += usr;
                                    cpu.krn += krn;

                                    tgCP.addData(uidName, new Link(uidLink, sUID));
                                    tgCP.addData(procName);
                                    tgCP.addData(sUsr);
                                    tgCP.addData(new ShadedValue(usr));
                                    tgCP.addData(sKrn);
                                    tgCP.addData(new ShadedValue(krn));
                                    tgCP.addData(new ShadedValue(usr + krn));
                                } else {
                                    System.err.println("Could not parse line: " + cpuItem.getLine());
                                }
                            }
                        } else {
                            System.err.println("Could not parse line: " + s);
                        }
                    }
                }
            } else if (line.startsWith("Kernel Wake lock")) {
                if (!line.contains("(nothing executed)")) {
                    // Collect into table
                    Matcher m = pKWL.matcher(line);
                    if (m.find()) {
                        String name = m.group(1);
                        String sTime = m.group(2);
                        String sCount = m.group(3);
                        long ts = readTs(sTime.replace(" ", ""));
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
                        System.err.println("Could not parse line: " + line);
                    }
                }
            } else {
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
            bug.bug = new Bug(Bug.PRIO_POWER_CONSUMPTION, 0, "Suspicious power consumption");
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
        Chapter ch = new Chapter(br, "Per-PID Stats");
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

            long ts = readTs(sTime);
            tg.addData(new ShadedValue(ts));
        }
        tg.end();
        return ch;
    }

    private int toX(long ts) {
        return (int)(GRAPH_PX - (ts * GRAPH_PW / mMaxTs));
    }

    private int toY(int level) {
        return GRAPH_PY - level * GRAPH_PH / MAX_LEVEL;
    }

    private void addSignal(long ts, String name, String value) {
        if (name.equals("status")) {
            if (value.equals("charging")) {
                addSignal(ts, "charging", 1);
            } else {
                addSignal(ts, "charging", 0);
            }
        } else if (name.equals("data_conn")) {
            if (value.equals("umts")) {
                addSignal(ts, "edge", 0);
                addSignal(ts, "umts", 1);
                addSignal(ts, "hdspa", 0);
                addSignal(ts, "hspa", 0);
            } else if (value.equals("hspa")) {
                addSignal(ts, "edge", 0);
                addSignal(ts, "umts", 0);
                addSignal(ts, "hdspa", 0);
                addSignal(ts, "hspa", 1);
            } else if (value.equals("hsdpa")) {
                addSignal(ts, "edge", 0);
                addSignal(ts, "umts", 0);
                addSignal(ts, "hdspa", 1);
                addSignal(ts, "hspa", 0);
            } else if (value.equals("edge")) {
                addSignal(ts, "edge", 1);
                addSignal(ts, "umts", 0);
                addSignal(ts, "hdspa", 0);
                addSignal(ts, "hspa", 0);
            } else {
                addSignal(ts, "edge", 0);
                addSignal(ts, "umts", 0);
                addSignal(ts, "hdspa", 0);
                addSignal(ts, "hspa", 0);
            }
        }
    }

    private void addSignal(long ts, String name, int value) {
        int idx = findSignal(name);
        if (idx < 0) return;

        Signal sig = SIGNALS[idx];
        int offY = GRAPH_H + idx * GRAPH_SH;
        int baseY = offY + GRAPH_SH - 2;
        int sigValue = sig.getValue();
        if (value == sigValue) {
            return;
        }
        if (sigValue != -1) {
            int lastX = toX(sig.getTs());
            int x = toX(ts);
            switch (sig.getType()) {
                case Signal.TYPE_BIN:
                    if (lastX == x) {
                        // Signal is rendered too often, draw a gray area instead
                        mG.setColor(COL_SIGNAL_PART);
                        mG.fillRect(x, offY, 1, baseY - offY + 1);
                    } else {
                        mG.setColor(COL_SIGNAL);
                        if (sigValue == 0) {
                            mG.drawLine(lastX + 1, baseY, x, baseY);
                        } else {
                            mG.fillRect(lastX + 1, offY, x - lastX, baseY - offY + 1);
                        }
                    }
                    break;
                case Signal.TYPE_INT:
                    // TODO
                    break;
                case Signal.TYPE_PRC:
                    mG.setColor(COL_SIGNAL);
                    int h = (baseY - offY) * sigValue /100;
                    mG.fillRect(lastX, baseY - h, x - lastX + 1, baseY + 1);
                    break;
            }
        } else {
            // We are setting the first value, let's render the signal name here
            mG.setColor(Color.BLACK);
            mG.drawString(sig.getName(), GRAPH_PX, baseY);
        }
        sig.setValue(ts, value);
    }

    private int findSignal(String s) {
        for (int i = 0; i < SIGNALS.length; i++) {
            if (SIGNALS[i].getName().equals(s)) {
                return i;
            }
        }
        return -1;
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

}
