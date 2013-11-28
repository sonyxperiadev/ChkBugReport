/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.Module;
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

    public void run(Module module) {
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
        final Context ctx = ch.getContext();
        combined.generate(ch);
        for (Entry<String, PacketStatGroup> kv : perCategory.entrySet()) {
            String cat = kv.getKey();
            PacketStatGroup psg = kv.getValue();
            Chapter chChild = new Chapter(ctx, cat);
            ch.addChapter(chChild);
            psg.generate(chChild);
        }

        // Create chart combined with battery level (if available)
        new PacketGraph().run(module, ch, packets, "Network usage", "iptables_packets_chart");
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
