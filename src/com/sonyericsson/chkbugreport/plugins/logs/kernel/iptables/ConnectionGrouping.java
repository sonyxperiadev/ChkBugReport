package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.plugins.logs.LogToolbar;

import java.util.HashMap;
import java.util.Vector;

public class ConnectionGrouping {

    private IPTableLogAnalyzer mParent;

    public ConnectionGrouping(IPTableLogAnalyzer parent) {
        mParent = parent;
    }

    public void run() {
        final Vector<Packet> packets = mParent.getPackets();
        Vector<Connection> conns = new Vector<Connection>();
        HashMap<String, Connection> connMap = new HashMap<String, Connection>();
        HashMap<String, DocNode> categories = new HashMap<String, DocNode>();

        // Group packets by connection
        for (Packet p : packets) {
            String connId = calcConnId(p);
            Connection conn = connMap.get(connId);
            if (conn == null) {
                conn = new Connection(connId);
                conns.add(conn);
                connMap.put(connId, conn);
            }
            conn.add(p);
        }

        if (conns.isEmpty()) {
            return; // No data to show
        }

        // Collect categories as well

        // Dump connection logs
        Chapter chCat = mParent.createChapter("Grouped packets (categories)");
        Chapter ch = mParent.createChapter("Grouped packets");
        new LogToolbar(ch);
        DocNode log = new Block(ch).addStyle("log");

        for (Connection conn : conns) {
            // Dump to main chapter
            dumpConn(conn, log);
            // Dump to category chapter
            DocNode catLog = categories.get(conn.category);
            if (catLog == null) {
                Chapter chTmp = new Chapter(ch.getModule(), conn.category);
                chCat.addChapter(chTmp);
                new LogToolbar(chTmp);
                catLog = new Block(chTmp).addStyle("log");
                categories.put(conn.category, catLog);
            }
            dumpConn(conn, catLog);
        }
    }

    private void dumpConn(Connection conn, DocNode log) {
        new Block(log).add("================================================================================");
        new Block(log).add(conn.connId);
        new Block(log).add("================================================================================");
        for (Packet p : conn.packets) {
            log.add(p.log.copy());
        }
        new Block(log);
    }

    private String calcConnId(Packet p) {
        // Extract source and destination ip/port, handling ICMP packets as well
        String src = p.src;
        String dst = p.dst;
        String srcp = p.getAttr("SPT");
        String dstp = p.getAttr("DPT");
        if ("ICMP".equals(p.proto)) {
            if (p.ref != null) {
                src = p.ref.src;
                dst = p.ref.dst;
                srcp = p.ref.getAttr("SPT");
                dstp = p.ref.getAttr("DPT");
            }
        }

        // Swap ports to keep it consistent (i.e. to match packets for the same connection)
        int srcIp = IPUtils.compileIp(src);
        int dstIp = IPUtils.compileIp(dst);
        if (srcIp > dstIp) {
            String tmp = src; src = dst; dst = tmp;
            tmp = srcp; srcp = dstp; dstp = tmp;
        }

        return src + ":" + srcp + "-" + dst + ":" + dstp;
    }

    class Connection {

        static final String CAT_SEP = " --- ";

        public String connId;
        public Vector<Packet> packets = new Vector<Packet>();
        public String category;

        public Connection(String connId) {
            this.connId = connId;
        }

        public void add(Packet p) {
            if (category == null) {
                String cat1 = IPUtils.getIpRangeName(p.src);
                String cat2 = IPUtils.getIpRangeName(p.dst);
                if (cat1.compareTo(cat2) < 0) {
                    category = cat1 + CAT_SEP + cat2;
                } else {
                    category = cat2 + CAT_SEP + cat1;
                }
            }
            packets.add(p);
        }
    }

}
