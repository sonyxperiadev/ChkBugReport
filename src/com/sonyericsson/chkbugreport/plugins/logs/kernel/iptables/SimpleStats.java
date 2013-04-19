package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.chart.Data;
import com.sonyericsson.chkbugreport.chart.DataSet;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.battery.BatteryLevelChart;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

public class SimpleStats {

    private IPTableLogAnalyzer mParent;

    public SimpleStats(IPTableLogAnalyzer parent) {
        mParent = parent;
    }

    public void run() {
        final Vector<Packet> packets = mParent.getPackets();

        PacketStatGroup combined = new PacketStatGroup();
        HashMap<String, PacketStatGroup> perCategory = new HashMap<String, PacketStatGroup>();
        HashMap<String, Vector<Packet>> packetsPerCategory = new HashMap<String, Vector<Packet>>();

        // Collect data
        for (Packet p : packets) {
            // Combined
            combined.add(p);
            // Per category
            String cat = p.getCategory();
            PacketStatGroup psg = perCategory.get(cat);
            if (psg == null) {
                psg = new PacketStatGroup();
                perCategory.put(cat, psg);
            }
            psg.add(p);
            Vector<Packet> pcat = packetsPerCategory.get(cat);
            if (pcat == null) {
                pcat = new Vector<Packet>();
                packetsPerCategory.put(cat, pcat);
            }
            pcat.add(p);
        }

        // Create stats tables
        if (combined.isEmpty()) {
            return;
        }
        Chapter ch = mParent.createChapter("Packet statistics");
        final Module mod = ch.getModule();
        combined.generate(ch);
        for (Entry<String, PacketStatGroup> kv : perCategory.entrySet()) {
            String cat = kv.getKey();
            PacketStatGroup psg = kv.getValue();
            Chapter chChild = new Chapter(mod, cat);
            ch.addChapter(chChild);
            psg.generate(chChild);
        }

        // Create chart combined with battery level (if available)
        // (but only if real timestamps are available)
        if (packets.get(0).realTs != 0) {
            ChartGenerator chart = new ChartGenerator("Network usage");
            BatteryLevels bl = (BatteryLevels) mod.getInfo(BatteryLevels.INFO_ID);
            if (bl != null) {
                chart.addPlugin(new BatteryLevelChart(bl));
            }
            chart.addPlugin(new PacketChart("All", packets));
            for (Entry<String, Vector<Packet>> item : packetsPerCategory.entrySet()) {
                chart.addPlugin(new PacketChart(item.getKey(), item.getValue()));
            }
            ch.add(chart.generate(mod, "iptables_packets_chart"));
        }
    }

    private void createStatsTable(Chapter ch, HashMap<String, PacketStat> stats, String title) {
        // Create tables
        Table tbl = new Table(Table.FLAG_SORT, ch);
        tbl.addColumn("Type", Table.FLAG_NONE, "type varchar");
        tbl.addColumn("Prefix", Table.FLAG_NONE, "prefix varchar");
        tbl.addColumn("Count", Table.FLAG_ALIGN_RIGHT, "count int");
        tbl.addColumn("Bytes", Table.FLAG_ALIGN_RIGHT, "bytes int");
        tbl.begin();
        for (PacketStat stat : stats.values()) {
            tbl.addData(stat.type);
            tbl.addData(stat.prefix);
            tbl.addData(new ShadedValue(stat.count));
            tbl.addData(new ShadedValue(stat.bytes));
        }
        tbl.end();
    }

    class PacketStat {
        String type;
        String prefix;
        int count;
        int bytes;
    }

    class PacketStatGroup {
        HashMap<String, PacketStat> inStats = new HashMap<String, PacketStat>();
        HashMap<String, PacketStat> outStats = new HashMap<String, PacketStat>();

        public void add(Packet p) {
            String prefix = p.prefix == null ? "(null)" : p.prefix;
            HashMap<String, PacketStat> stats = p.isInput() ? inStats : outStats;
            PacketStat stat = stats.get(prefix);
            if (stat == null) {
                stat = new PacketStat();
                stat.prefix = prefix;
                stat.type = p.isInput() ? "IN" : "OUT";
                stats.put(prefix, stat);
            }
            stat.count++;
            stat.bytes += p.len;
        }

        public void generate(Chapter ch) {
            if (!inStats.isEmpty()) {
                createStatsTable(ch, inStats, "Incomming packets");
            }
            if (!outStats.isEmpty()) {
                createStatsTable(ch, outStats, "Outgoing packets");
            }
        }

        public boolean isEmpty() {
            return (inStats.isEmpty() && outStats.isEmpty());
        }

    }

    class PacketChart extends ChartPlugin {

        private String mName;
        private Vector<Packet> mPackets;

        public PacketChart(String name, Vector<Packet> packets) {
            mName = name;
            mPackets = packets;
        }

        @Override
        public boolean init(Module mod, ChartGenerator chart) {
            if (mPackets.isEmpty()) {
                return false;
            }
            init(chart, true, "IN  ");
            init(chart, false, "OUT ");
            return true;
        }

        private void init(ChartGenerator chart, boolean in, String pref) {
            DataSet ds = new DataSet(DataSet.Type.STATE, pref + mName);
            ds.addColor(0x40ffffff);
            ds.addColor(0x40000000);
            long last = -1, pw = 1000;
            for (Packet p : mPackets) {
                if (p.isInput() ^ !in) continue; // no, I'm not cursing here :-)
                long ts = p.realTs;
                if (last != -1 && last + pw < ts) {
                    ds.addData(new Data(last + pw, 0));
                }
                ds.addData(new Data(ts, 1));
                last = ts;
            }
            if (last != -1) {
                ds.addData(new Data(last + pw, 0));
            }
            chart.add(ds);
        }

        @Override
        public long getFirstTs() {
            return mPackets.get(0).realTs;
        }

        @Override
        public long getLastTs() {
            return mPackets.get(mPackets.size() - 1).realTs;
        }

    }


}
