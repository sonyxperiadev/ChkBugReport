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
package com.sonyericsson.chkbugreport.plugins.logs.event;

import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.util.ColorUtil;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

/* package */ class SampleDatasGenerator {

    private EventLogPlugin mPlugin;
    private SampleDatas mSDs;

    public SampleDatasGenerator(EventLogPlugin plugin, SampleDatas data) {
        mPlugin = plugin;
        mSDs = data;
    }

    public void generate(Module br, Chapter mainCh) {
        Chapter ch = null;

        for (Entry<String, Vector<SampleData>> entry : mSDs.entrySet()) {
            String eventType = entry.getKey();
            Vector<SampleData> sds = entry.getValue();
            if (sds.size() <= 1) continue;

            // Create the chapter if not created yet
            if (ch == null) {
                ch = new Chapter(br.getContext(), "*_sample graphs");
                mainCh.addChapter(ch);
            }

            // create the graph
            // save the data as vcd file as well
            String fnVcd = br.getRelRawDir() + "sample_graph_" + eventType + ".vcd";
            generateSampleDataVCD(br, fnVcd, sds, eventType);
            String fn = "sample_graph_" + eventType + ".png";
            generateSampleDataGraph(br, fn, sds, eventType);
            // Alternative graph
            String fnAlt = "sample_graph_" + eventType + "___alt.png";
            generateSampleDataGraphAlt(br, fnAlt, sds, eventType);
            new Para(ch)
                .add("Graph built from " + eventType + " logs:")
                .add(new Hint()
                    .add("VCD file also generated: ")
                    .add(new Link(fnVcd, fnVcd)))
                .add(new Img(fn))
                .add(new Img(fnAlt));
        }
    }

    private boolean generateSampleDataGraph(Module br, String fn, Vector<SampleData> sds, String eventType) {
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
        ImageCanvas fm = new ImageCanvas(16, 16);

        // Allocate colors for each value
        // Also count the number of distinct values
        HashMap<String, Integer> mColors = new HashMap<String, Integer>();
        int idx = 0, lh = (int) fm.getFontHeight();
        if (lh < 18) {
            lh = 18;
        }
        for (SampleData sd : sds) {
            String name = sd.name;
            Integer col = mColors.get(name);
            if (col == null) {
                int rgba = ColorUtil.getColor(idx++) | 0x40000000;
                col = rgba;
                mColors.put(name, col);
            }
            maxNameW = Math.max(maxNameW, (int)fm.getStringWidth(name));
        }
        w += maxNameW + 32;
        int maxNameH = ny * 2 + idx * lh;
        h = Math.max(h, maxNameH);

        // Create an empty image
        ImageCanvas img = new ImageCanvas(w, h);
        img.setColor(ImageCanvas.WHITE);
        img.fillRect(0, 0, w, h);
        img.setColor(ImageCanvas.LIGHT_GRAY);
        img.drawRect(0, 0, w - 1, h - 1);

        // Draw the legend
        for (Entry<String, Integer> entry : mColors.entrySet()) {
            String name = entry.getKey();
            Integer color = entry.getValue();
            img.setColor(color);
            img.fillRect(nx, ny + (lh - 16) / 2, 16, 16);
            img.drawRect(nx, ny + (lh - 16) / 2, 15, 15);
            img.setColor(ImageCanvas.BLACK);
            img.drawString(name, nx + 32, ny + fm.getAscent());
            ny += lh;
        }

        // Draw the axis
        int as = 5;
        img.setColor(ImageCanvas.BLACK);
        img.drawLine(cx, cy, cx, cy - gh);
        img.drawLine(cx, cy, cx + gw, cy);
        img.drawLine(cx - as, cy - gh + as, cx, cy - gh);
        img.drawLine(cx + as, cy - gh + as, cx, cy - gh);
        img.drawLine(cx + gw - as, cy - as, cx + gw, cy);
        img.drawLine(cx + gw - as, cy + as, cx + gw, cy);

        // Draw the title
        img.drawString(eventType, 10, 10 + fm.getAscent());

        // Draw some guide lines
        int max = 110;
        int count = 5;
        int step = 20;
        int colGuide = 0xffc0c0ff;
        for (int i = 1; i <= count; i++) {
            int value = i * step;
            if (value > max) break;
            int yv = cy - value * gh / max;
            img.setColor(colGuide);
            img.drawLine(cx + 1, yv, cx + gw, yv);
            img.setColor(ImageCanvas.BLACK);
            String s = "" + value + "%  ";
            img.drawString(s, cx - fm.getStringWidth(s) - 1, yv);
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
            img.setColor(mColors.get(sd.name));
            img.fillRect(cx + bx - bw, cy - bh, bw, bh);
            img.drawRect(cx + bx - bw, cy - bh, bw - 1, bh - 1);
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, tx, ty, gw, th, firstTs, lastTs, true)) {
            return false;
        }

        // Save the image
        try {
            img.writeTo(new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean generateSampleDataGraphAlt(Module br, String fn, Vector<SampleData> sds, String eventType) {
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
        ImageCanvas fm = new ImageCanvas(16, 16);

        // Count the number of distinct values
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        int idx = 0, lh = Math.max(18, (int)fm.getFontHeight());
        if (lh < 18) {
            lh = 18;
        }
        for (SampleData sd : sds) {
            String name = sd.name;
            if (!map.containsKey(name)) {
                map.put(name, idx++);
            }
            maxNameW = Math.max(maxNameW, (int)fm.getStringWidth(name));
        }
        graphHeight = idx * lh;
        int w = marginLeft + graphWidth + maxNameW + marginRight;
        int h = marginTop + graphHeight + marginBottom;
        int cx = marginLeft;
        int cy = marginTop + graphHeight;

        // Create an empty image
        ImageCanvas img = new ImageCanvas(w, h);
        img.setColor(ImageCanvas.WHITE);
        img.fillRect(0, 0, w, h);
        img.setColor(ImageCanvas.LIGHT_GRAY);
        img.drawRect(0, 0, w - 1, h - 1);

        // Draw the legend
        int nx = marginLeft + graphWidth + graphTextPadding;
        int lightGray = 0x20000000;
        for (Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer id = entry.getValue();
            int ny = marginTop + id * lh;
            img.setColor(lightGray);
            img.drawLine(cx, ny, nx + maxNameW, ny);
            img.setColor(ImageCanvas.BLACK);
            img.drawString(name, nx, ny + fm.getAscent());
        }

        // Draw the axis
        int as = 5;
        img.setColor(ImageCanvas.BLACK);
        img.drawLine(cx, cy, cx, cy - graphHeight);
        img.drawLine(cx, cy, cx + graphWidth, cy);
        img.drawLine(cx + graphWidth - as, cy - as, cx + graphWidth, cy);
        img.drawLine(cx + graphWidth - as, cy + as, cx + graphWidth, cy);

        // Draw the title
        img.drawString(eventType, 10, 10 + fm.getAscent());

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
            img.setColor(getHeatmapColor(sd.perc));
            img.fillRect(cx + bx - bw, by, bw, bh);
            img.drawRect(cx + bx - bw, by, bw - 1, bh - 1);
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, cx, cy, graphWidth, timeBarHeight, firstTs, lastTs, true)) {
            return false;
        }

        // Save the image
        try {
            img.writeTo(new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private int getHeatmapColor(int perc) {
        int red = perc * 255 / 16;
        int green = 255 - red;
        return 0x40000000 | (red << 16) | (green << 8);
    }

    private boolean generateSampleDataVCD(Module br, String fn, Vector<SampleData> sds, String eventType) {
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
