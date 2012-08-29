package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;

public class SampleDatasGenerator {

    private EventLogPlugin mPlugin;
    private SampleDatas mSDs;
    private HashMap<Integer, Color> mHeatmapColors = new HashMap<Integer, Color>();

    public SampleDatasGenerator(EventLogPlugin plugin, SampleDatas data) {
        mPlugin = plugin;
        mSDs = data;
    }

    public void run(Report br, Chapter mainCh) {
        Chapter ch = null;

        for (Entry<String, Vector<SampleData>> entry : mSDs.entrySet()) {
            String eventType = entry.getKey();
            Vector<SampleData> sds = entry.getValue();
            if (sds.size() <= 1) continue;

            // Create the chapter if not created yet
            if (ch == null) {
                ch = new Chapter(br, "*_sample graphs");
                mainCh.addChapter(ch);
            }

            // create the graph
            ch.addLine("<p>Graph built from " + eventType + " logs:</p>");
            // save the data as vcd file as well
            String fnVcd = br.getRelRawDir() + "sample_graph_" + eventType + ".vcd";
            if (generateSampleDataVCD(br, fnVcd, sds, eventType)) {
                // TODO
            }
            ch.addLine("<div class=\"hint\">(VCD file also generated: <a href=\"" + fnVcd + "\">" + fnVcd + "</a>)</div>");
            String fn = br.getRelDataDir() + "sample_graph_" + eventType + ".png";
            if (generateSampleDataGraph(br, fn, sds, eventType)) {
                // TODO
            }
            ch.addLine("<div><img src=\"" + fn + "\"/></div>");
            // Alternative graph
            fn = br.getRelDataDir() + "sample_graph_" + eventType + "_alt.png";
            if (generateSampleDataGraphAlt(br, fn, sds, eventType)) {
                // TODO
            }
            ch.addLine("<div><img src=\"" + fn + "\"/></div>");
        }
    }

    private boolean generateSampleDataGraph(Report br, String fn, Vector<SampleData> sds, String eventType) {
        int w = 800;
        int h = 350;
        int cx = 100;
        int cy = 250;
        int gw = 600;
        int gh = 200;
        int nx = 750;
        int ny = 10;
        int tx = cx;
        int ty = cy;
        int th = 75;

        int maxNameW = 0;

        long firstTs = mPlugin.getFirstTs();
        long lastTs = mPlugin.getLastTs();

        // Need a font metrics before the actual image is created :-(
        FontMetrics fm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics();

        // Allocate colors for each value
        // Also count the number of distinct values
        HashMap<String, Color> mColors = new HashMap<String, Color>();
        int idx = 0, lh = fm.getHeight();
        if (lh < 18) {
            lh = 18;
        }
        for (SampleData sd : sds) {
            String name = sd.name;
            Color col = mColors.get(name);
            if (col == null) {
                int rgba = Util.getColor(idx++) | 0x40000000;
                col = new Color(rgba, true);
                mColors.put(name, col);
            }
            maxNameW = Math.max(maxNameW, fm.stringWidth(name));
        }
        w += maxNameW + 32;
        int maxNameH = ny * 2 + idx * lh;
        h = Math.max(h, maxNameH);

        // Create an empty image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, w - 1, h - 1);

        // Draw the legend
        for (Entry<String, Color> entry : mColors.entrySet()) {
            String name = entry.getKey();
            Color color = entry.getValue();
            g.setColor(color);
            g.fillRect(nx, ny + (lh - 16) / 2, 16, 16);
            g.drawRect(nx, ny + (lh - 16) / 2, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString(name, nx + 32, ny + fm.getAscent());
            ny += lh;
        }

        // Draw the axis
        int as = 5;
        g.setColor(Color.BLACK);
        g.drawLine(cx, cy, cx, cy - gh);
        g.drawLine(cx, cy, cx + gw, cy);
        g.drawLine(cx - as, cy - gh + as, cx, cy - gh);
        g.drawLine(cx + as, cy - gh + as, cx, cy - gh);
        g.drawLine(cx + gw - as, cy - as, cx + gw, cy);
        g.drawLine(cx + gw - as, cy + as, cx + gw, cy);

        // Draw the title
        g.drawString(eventType, 10, 10 + fm.getAscent());

        // Draw some guide lines
        int max = 110;
        int count = 5;
        int step = 20;
        Color colGuide = new Color(0xc0c0ff);
        for (int i = 1; i <= count; i++) {
            int value = i * step;
            if (value > max) break;
            int yv = cy - value * gh / max;
            g.setColor(colGuide);
            g.drawLine(cx + 1, yv, cx + gw, yv);
            g.setColor(Color.BLACK);
            String s = "" + value + "%  ";
            g.drawString(s, cx - fm.stringWidth(s) - 1, yv);
        }

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        if (duration <= 0) return false;
        for (SampleData sd : sds) {
            int bh = sd.perc * (gh - 1) / max;
            int bx = (int)((sd.ts - firstTs) * (gw - 1) / duration);
            int bw = (int)((sd.duration) * (gw - 1) / (lastTs - firstTs));
            if (bw < 3) {
                bx += (3 - bw);
                bw = 3;
            }
            g.setColor(mColors.get(sd.name));
            g.fillRect(cx + bx - bw, cy - bh, bw, bh);
            g.drawRect(cx + bx - bw, cy - bh, bw - 1, bh - 1);
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, g, tx, ty, gw, th, firstTs, lastTs, true)) {
            return false;
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean generateSampleDataGraphAlt(Report br, String fn, Vector<SampleData> sds, String eventType) {
        int marginTop = 50;
        int marginLeft = 100;
        int marginBottom = 100;
        int marginRight = 100;
        int graphTextPadding = 50;
        int graphWidth = 600;
        int graphHeight = 0; // will be calculated
        int timeBarHeight = 75;

        int maxNameW = 0;

        long firstTs = mPlugin.getFirstTs();
        long lastTs = mPlugin.getLastTs();

        // Need a font metrics before the actual image is created :-(
        FontMetrics fm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics();

        // Count the number of distinct values
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        int idx = 0, lh = Math.max(18, fm.getHeight());
        if (lh < 18) {
            lh = 18;
        }
        for (SampleData sd : sds) {
            String name = sd.name;
            if (!map.containsKey(name)) {
                map.put(name, idx++);
            }
            maxNameW = Math.max(maxNameW, fm.stringWidth(name));
        }
        graphHeight = idx * lh;
        int w = marginLeft + graphWidth + maxNameW + marginRight;
        int h = marginTop + graphHeight + marginBottom;
        int cx = marginLeft;
        int cy = marginTop + graphHeight;

        // Create an empty image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, w - 1, h - 1);

        // Draw the legend
        int nx = marginLeft + graphWidth + graphTextPadding;
        Color lightGray = new Color(0x20000000, true);
        for (Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer id = entry.getValue();
            int ny = marginTop + id * lh;
            g.setColor(lightGray);
            g.drawLine(cx, ny, nx + maxNameW, ny);
            g.setColor(Color.BLACK);
            g.drawString(name, nx, ny + fm.getAscent());
        }

        // Draw the axis
        int as = 5;
        g.setColor(Color.BLACK);
        g.drawLine(cx, cy, cx, cy - graphHeight);
        g.drawLine(cx, cy, cx + graphWidth, cy);
        g.drawLine(cx + graphWidth - as, cy - as, cx + graphWidth, cy);
        g.drawLine(cx + graphWidth - as, cy + as, cx + graphWidth, cy);

        // Draw the title
        g.drawString(eventType, 10, 10 + fm.getAscent());

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        if (duration <= 0) return false;
        for (SampleData sd : sds) {
            int id = map.get(sd.name);
            int bx = (int)((sd.ts - firstTs) * (graphWidth - 1) / duration);
            int by = marginTop + id * lh;
            int bw = (int)((sd.duration) * (graphWidth - 1) / (lastTs - firstTs));
            int bh = lh;
            if (bw < 3) {
                bx += (3 - bw);
                bw = 3;
            }
            g.setColor(getHeatmapColor(sd.perc));
            g.fillRect(cx + bx - bw, by, bw, bh);
            g.drawRect(cx + bx - bw, by, bw - 1, bh - 1);
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, g, cx, cy, graphWidth, timeBarHeight, firstTs, lastTs, true)) {
            return false;
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private Color getHeatmapColor(int perc) {
        perc = perc * 16 / 100;
        Color ret = mHeatmapColors.get(perc);
        if (ret == null) {
            int red = perc * 255 / 16;
            int green = 255 - red;
            ret = new Color(0x40000000 | (red << 16) | (green << 8), true);
            mHeatmapColors.put(perc, ret);
        }
        return ret;
    }

    private boolean generateSampleDataVCD(Report br, String fn, Vector<SampleData> sds, String eventType) {
        if (sds.size() == 0) return false;

        // In the first pass we need to find the unique ids, and also generate a sorted
        // list of events
        HashMap<String, Integer> ids = new HashMap<String, Integer>();
        Vector<SampleEvent> events = new Vector<SampleEvent>();
        int idx = 0;
        for (SampleData sd : sds) {
            String name = sd.name;

            // Associate an id to the name
            Integer idObj = ids.get(name);
            if (idObj == null) {
                idObj = new Integer(idx++);
                ids.put(name, idObj);
            }

            // Add the start and stop events
            events.add(new SampleEvent(true, sd.ts - sd.duration, idObj));
            events.add(new SampleEvent(false, sd.ts, idObj));
        }

        // Sort the events by timestamp
        Collections.sort(events, new Comparator<SampleEvent>() {
            @Override
            public int compare(SampleEvent o1, SampleEvent o2) {
                if (o1.ts < o2.ts) return -1;
                if (o1.ts > o2.ts) return +1;
                return 0;
            }
        });

        // Save the file
        try {
            PrintStream ps = new PrintStream(new File(br.getBaseDir() + fn));
            int bits = 8;

            // Write header
            ps.println("$timescale 1ms $end");
            ps.println("$scope am_logs $end");
            for (Entry<String, Integer> entry : ids.entrySet()) {
                String name = entry.getKey();
                int id = entry.getValue();
                name = Util.fixVCDName(name);
                ps.println("$var wire " + bits + " n" + id + " " + name + " $end");
            }
            ps.println("$upscope $end");
            ps.println("$enddefinitions $end");

            // Write initial values
            long lastTs = mPlugin.getFirstTs();
            ps.println("#" + lastTs);
            for (int id = 0; id < idx; id++) {
                ps.println("bZ n" + id);
            }

            // Write events
            int count[] = new int[idx];
            for (SampleEvent event : events) {
                if (event.ts != lastTs) {
                    ps.println("#" + event.ts);
                    lastTs = event.ts;
                }
                int id = event.id;
                if (event.start) {
                    count[id]++;
                } else {
                    count[id]--;
                }
                ps.println("b" + (count[id] == 0 ? "Z" : Util.toBinary(count[id], bits)) + " n" + id);
            }

            // Write final values
            lastTs = mPlugin.getLastTs();
            ps.println("#" + lastTs);
            for (int id = 0; id < idx; id++) {
                ps.println("bZ n" + id);
            }

            // Finish
            ps.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
