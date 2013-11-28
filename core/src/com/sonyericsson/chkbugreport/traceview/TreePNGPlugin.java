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
package com.sonyericsson.chkbugreport.traceview;

import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.traceview.TraceModule.MethodInfo;
import com.sonyericsson.chkbugreport.traceview.TraceModule.MethodRun;
import com.sonyericsson.chkbugreport.traceview.TraceModule.ThreadInfo;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Vector;

public class TreePNGPlugin extends Plugin {

    private static final int TRACE_COUNT = 100;
    private static final int W = 600;
    private static final int H = 24;

    private static final int MIN_RUN_TIME = 1000; // 1ms

    public static class Chart {
        public int mid;
        public String fn;
        public ImageCanvas img;
    }

    @Override
    public int getPrio() {
        return 60;
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
    public void generate(Module br) {
        TraceModule rep = (TraceModule)br;

        Chapter ch = new Chapter(rep.getContext(), "Trace charts");
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
            Chapter cc = new Chapter(rep.getContext(), t.getFullName());

            new Para(cc)
                .add("Using thread local time.")
                .add("Showing only method with duration longer than or equal to " + (MIN_RUN_TIME / 1000) + "ms.")
                .add("Total duration: " + (duration / 1000) + "ms.");

            new Block(cc).add(new Img("ftrace-legend-dred.png")).add("Partially running");
            new Block(cc).add(new Img("ftrace-legend-red.png")).add("Running");
            new Block(cc).add(new Img("ftrace-legend-yellow.png")).add("Waiting");
            new Block(cc).add(new Img("ftrace-legend-black.png")).add("Sleeping");

            Table tb = new Table(Table.FLAG_DND, cc);
            tb.addColumn("Name", Table.FLAG_NONE);
            tb.addColumn("Trace", Table.FLAG_NONE);
            tb.begin();

            String fn = "tv_trace_" + t.id + "_time.png";
            if (Util.createTimeBar(br, fn, W, 0, duration / 1000)) { // us -> ms
                tb.addData("");
                tb.addData(new Img(fn));
            }

            boolean odd = false;
            int nrLines = 0;
            for (Chart chart : charts.values()) {
                odd = !odd;
                savePng(chart, rep);
                MethodInfo m = rep.findMethod(chart.mid);
                tb.addData(m.shortName);
                tb.addData(new Img(chart.fn));
                nrLines++;
            }

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
            chart.fn = String.format("trace_%d_%d.png", tid, run.mid);
            createEmptyChart(chart);
            charts.put(run.mid, chart);
        }
        queue.add(run);
        return true;
    }

    private void createEmptyChart(Chart chart) {
        // Create the empty image
        ImageCanvas img = new ImageCanvas(W, H);
        img.setColor(ImageCanvas.BLACK);
        img.fillRect(0, 0, W, H);
        chart.img = img;
    }

    private void createTracePng(Module rep, int tid, Vector<MethodRun> queue,
            LinkedHashMap<Integer, Chart> charts, MethodRun run, int duration) {
        // Setup initial data
        int startTime = run.startLocalTime;
        int lastX = (int)(startTime * W / duration);

        // Render the trace
        Chart chart = charts.get(run.mid);
        if (chart == null) return; // something wrong
        ImageCanvas img = chart.img;

        int darkRed = 0xff800000;
        int cnt = run.calls.size();
        for (int i = 0; i < cnt; i++) {
            MethodRun child = run.calls.get(i);
            addToQueue(rep, tid, queue, charts, child);

            // Render the segment where this method was running
            int x = (int)((child.startLocalTime) * W / duration);
            if (lastX == x) {
                img.setColor(darkRed);
                img.fillRect(lastX, 0, 1, H);
            } else {
                img.setColor(ImageCanvas.RED);
                img.fillRect(lastX + 1, 0, x - lastX + 1, H);
            }
            lastX = x;

            // Render the segment where the child method was running
            x = (int)(child.endLocalTime * W / duration);
            img.setColor(ImageCanvas.YELLOW);
            img.drawLine(lastX, H/2, x, H/2);
            lastX = x;
        }

        // Render the last segment where this method was running
        int x = (int)((run.endLocalTime) * W / duration);
        if (lastX == x) {
            img.setColor(darkRed);
            img.fillRect(lastX, 0, 1, H);
        } else {
            img.setColor(ImageCanvas.RED);
            img.fillRect(lastX + 1, 0, x - lastX + 1, H);
        }
        lastX = x;
    }

    private void savePng(Chart chart, TraceModule rep) {
        // Save the image
        try {
            chart.img.writeTo(new File(rep.getBaseDir() + chart.fn));
            chart.img = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
