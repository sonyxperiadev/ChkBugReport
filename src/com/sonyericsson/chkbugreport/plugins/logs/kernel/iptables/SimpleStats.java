package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;

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
        }

        // Create stats tables
        if (combined.isEmpty()) {
            return;
        }
        Chapter ch = mParent.createChapter("Packet statistics");
        combined.generate(ch);
        for (Entry<String, PacketStatGroup> kv : perCategory.entrySet()) {
            String cat = kv.getKey();
            PacketStatGroup psg = kv.getValue();
            Chapter chChild = new Chapter(ch.getModule(), cat);
            ch.addChapter(chChild);
            psg.generate(chChild);
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


}
