/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport.traceview;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.traceview.TraceReport.MethodInfo;
import com.sonyericsson.chkbugreport.traceview.TraceReport.MethodRun;
import com.sonyericsson.chkbugreport.traceview.TraceReport.ThreadInfo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.imageio.ImageIO;

public class TreePNGPlugin extends Plugin {

    private static final int TRACE_COUNT = 100;
    private static final int W = 600;
    private static final int H = 24;

    private static final int MIN_RUN_TIME = 1000; // 1ms

    public static class Chart {
        public int mid;
        public String fn;
        public BufferedImage img;
        public Graphics2D g;
    }

    @Override
    public int getPrio() {
        return 60;
    }

    @Override
    public void load(Module br) {
        // NOP
    }

    @Override
    public void generate(Module br) {
        TraceReport rep = (TraceReport)br;

        Chapter ch = new Chapter(rep, "Trace charts");
        rep.addChapter(ch);

        Vector<ThreadInfo> threads = rep.getThreadInfos();
        for (ThreadInfo t : threads) {
            int cnt = t.calls.size();
            if (cnt == 0) continue;
            int duration = t.calls.get(cnt-1).endLocalTime - t.calls.get(0).startLocalTime;

            // Add main methods to the queue
            LinkedHashMap<Integer, Chart> charts = new LinkedHashMap<Integer, Chart>();
            Vector<MethodRun> queue = new Vector<MethodRun>();
            for (int i = 0; i < cnt; i++) {
                addToQueue(br, t.id, queue, charts, t.calls.get(i));
            }

            // Render queue (and also add new methods)
            for (int i = 0; i < queue.size(); i++) {
                MethodRun run = queue.get(i);
                createTracePng(br, t.id, queue, charts, run, duration);
            }

            // Save images
            Chapter cc = new Chapter(rep, t.getFullName());

            cc.addLine("<p>");
            cc.addLine("Using thread local time.");
            cc.addLine("Showing only method with duration longer than or equal to " + (MIN_RUN_TIME / 1000) + "ms.");
            cc.addLine("Total duration: " + (duration / 1000) + "ms.");
            cc.addLine("</p>");

            cc.addLine("<div><img src=\"data/ftrace-legend-dred.png\"/> Partially running</div>");
            cc.addLine("<div><img src=\"data/ftrace-legend-red.png\"/> Running</div>");
            cc.addLine("<div><img src=\"data/ftrace-legend-yellow.png\"/> Waiting</div>");
            cc.addLine("<div><img src=\"data/ftrace-legend-black.png\"/> Sleeping</div>");

            cc.addLine("<p>NOTE: you can drag and move table rows to reorder them!</p>");

            cc.addLine("<table class=\"ftrace-trace tablednd\"><!-- I know, tables are evil, but I still have to learn to use floats -->");
            cc.addLine("  <thead>");
            cc.addLine("  <tr class=\"ftrace-trace-header\">");
            cc.addLine("    <th>Name</td>");
            cc.addLine("    <th>Trace</td>");
            cc.addLine("  </tr>");

            String fn = "tv_trace_" + t.id + "_time.png";
            if (Util.createTimeBar(br, fn, W, 0, duration / 1000)) { // us -> ms
                cc.addLine("  <tr>");
                cc.addLine("    <th></td>");
                cc.addLine("    <th><img src=\"" + fn + "\"/></td>");
                cc.addLine("  </tr>");
            }

            cc.addLine("  </thead>");
            cc.addLine("  <tbody>");

            boolean odd = false;
            int nrLines = 0;
            for (Chart chart : charts.values()) {
                odd = !odd;
                savePng(chart, rep);
                MethodInfo m = rep.findMethod(chart.mid);
                cc.addLine("  <tr class=\"ftrace-trace-" + (odd ? "odd" : "even") + "\">");
                cc.addLine("    <td>" + m.shortName + "</td>");
                cc.addLine("    <td><img src=\"" + chart.fn + "\"/></td>");
                cc.addLine("  </tr>");
                nrLines++;
            }

            cc.addLine("  </tbody>");
            cc.addLine("</table>");
            if (nrLines > 0) {
                ch.addChapter(cc);
            }
        }
    }

    private boolean addToQueue(Module rep, int tid, Vector<MethodRun> queue, LinkedHashMap<Integer, Chart> charts, MethodRun run) {
        if (run.endLocalTime - run.startLocalTime < MIN_RUN_TIME) {
            // Too short
            return false;
        }
        Chart chart = charts.get(run.mid);
        if (chart == null) {
            // method not used yet, so create an empty chart, if there is still space
            if (charts.size() >= TRACE_COUNT) {
                return false;
            }
            chart = new Chart();
            chart.mid = run.mid;
            chart.fn = rep.getRelDataDir() + String.format("trace_%d_%d.png", tid, run.mid);
            createEmptyChart(chart);
            charts.put(run.mid, chart);
        }
        queue.add(run);
        return true;
    }

    private void createEmptyChart(Chart chart) {
        // Create the empty image
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        chart.img = img;
        chart.g = g;
    }

    private void createTracePng(Module rep, int tid, Vector<MethodRun> queue,
            LinkedHashMap<Integer, Chart> charts, MethodRun run, int duration) {
        // Setup initial data
        int startTime = run.startLocalTime;
        int lastX = (int)(startTime * W / duration);

        // Render the trace
        Chart chart = charts.get(run.mid);
        if (chart == null) return; // something wrong
        Graphics2D g = chart.g;

        Color darkRed = new Color(0x800000);
        int cnt = run.calls.size();
        for (int i = 0; i < cnt; i++) {
            MethodRun child = run.calls.get(i);
            addToQueue(rep, tid, queue, charts, child);

            // Render the segment where this method was running
            int x = (int)((child.startLocalTime) * W / duration);
            if (lastX == x) {
                g.setColor(darkRed);
                g.fillRect(lastX, 0, 1, H);
            } else {
                g.setColor(Color.RED);
                g.fillRect(lastX + 1, 0, x - lastX + 1, H);
            }
            lastX = x;

            // Render the segment where the child method was running
            x = (int)(child.endLocalTime * W / duration);
            g.setColor(Color.YELLOW);
            g.drawLine(lastX, H/2, x, H/2);
            lastX = x;
        }

        // Render the last segment where this method was running
        int x = (int)((run.endLocalTime) * W / duration);
        if (lastX == x) {
            g.setColor(darkRed);
            g.fillRect(lastX, 0, 1, H);
        } else {
            g.setColor(Color.RED);
            g.fillRect(lastX + 1, 0, x - lastX + 1, H);
        }
        lastX = x;
    }

    private void savePng(Chart chart, TraceReport rep) {
        // Save the image
        try {
            ImageIO.write(chart.img, "png", new File(rep.getBaseDir() + chart.fn));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
