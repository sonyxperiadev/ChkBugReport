package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;

import java.util.HashMap;
import java.util.Vector;

public class SimpleStats {

    private IPTableLogAnalyzer mParent;

    public SimpleStats(IPTableLogAnalyzer parent) {
        mParent = parent;
    }

    public void run() {
        final Vector<Packet> packets = mParent.getPackets();

        // Collect data
        HashMap<String, PacketStat> inStats = new HashMap<String, PacketStat>();
        HashMap<String, PacketStat> outStats = new HashMap<String, PacketStat>();
        for (Packet p : packets) {
            String prefix = p.prefix == null ? "<null>" : p.prefix;
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

        // Create stats tables
        if (inStats.isEmpty() && outStats.isEmpty()) {
            return; // No data to show
        }
        Chapter ch = mParent.createChapter("Packet statistics");
        if (!inStats.isEmpty()) {
            createStatsTable(ch, inStats, "Incomming packets");
        }
        if (!outStats.isEmpty()) {
            createStatsTable(ch, outStats, "Outgoing packets");
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


}
