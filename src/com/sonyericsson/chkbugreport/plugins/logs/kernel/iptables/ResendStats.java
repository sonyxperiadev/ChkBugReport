package com.sonyericsson.chkbugreport.plugins.logs.kernel.iptables;

import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ShadedValue;

import java.util.HashMap;
import java.util.Vector;

public class ResendStats {

    private IPTableLogAnalyzer mParent;

    public ResendStats(IPTableLogAnalyzer parent) {
        mParent = parent;
    }

    public void run() {
        final Vector<Packet> packets = mParent.getPackets();

        // NOTE: this can be done in a much more memory efficient way, knowing that if a packet
        // is resent, then it's resent within 5 minutes
        HashMap<String, Packet> seen = new HashMap<String, Packet>();
        int resentCount[] = { 0, 0 };
        int resentBytes[] = { 0, 0 };

        // Collect data
        for (Packet p : packets) {
            // Build unique id of packet
            if (p.hasFlag("SYN")) continue; // skip connection initiation
            String seq = p.getAttr("SEQ");
            String ack = p.getAttr("ACK");
            if (seq == null || ack == null) continue;
            StringBuilder id = new StringBuilder();
            id.append(p.src);
            id.append(':');
            id.append(p.getAttr("SPT"));
            id.append('-');
            id.append(p.dst);
            id.append(':');
            id.append(p.getAttr("DPT"));
            id.append('/');
            id.append(seq);
            id.append('/');
            id.append(ack);
            String ids = id.toString();
            Packet prev = seen.get(ids);
            if (prev != null) {
                // Need to check id as well, and size
                if (prev.len == p.len && prev.id == p.id - 1) {
                    // Exception: FIN packet after non-FIN packet should be ignored
                    if (!(!prev.hasFlag("FIN") && p.hasFlag("FIN"))) {
                        int idx = p.isInput() ? 0 : 1;
                        resentCount[idx] += 1;
                        resentBytes[idx] += p.len;
                        p.log.css += " packet-resent";
                        p.addFlag("resent");
                    }
                }
            }
            seen.put(ids, p);
        }
        if (resentBytes[0] == 0 && resentBytes[1] == 0) {
            return;
        }
        Chapter ch = mParent.createChapter("Resent packats stats");
        Block b = new Block(ch);
        b.add("Resent incoming packets: ");
        b.add(new ShadedValue(resentCount[0]));
        b.add(" (");
        b.add(new ShadedValue(resentBytes[0]));
        b.add(" bytes)");
        b = new Block(ch);
        b.add("Resent outgoing packets: ");
        b.add(new ShadedValue(resentCount[1]));
        b.add(" (");
        b.add(new ShadedValue(resentBytes[1]));
        b.add(" bytes)");

        // Create chart combined with battery level (if available)
        new ResentPacketGraph().run(ch, packets, "Resent packets", "iptables_resent_chart");
    }

    class ResentPacketGraph extends PacketGraph {

        @Override
        protected boolean filter(Packet p) {
            return p.hasFlag("resent");
        }

    }

}
