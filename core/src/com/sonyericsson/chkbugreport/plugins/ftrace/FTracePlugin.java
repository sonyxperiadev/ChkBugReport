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
package com.sonyericsson.chkbugreport.plugins.ftrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.ps.PSRecord;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

public class FTracePlugin extends Plugin {

    public static final String TAG = "[FTracePlugin]";

    private static final int TRACE_W = 600;
    private static final int TRACE_H = 24;

    private String mTimeBarName;

    @Override
    public int getPrio() {
        return 50;
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
    public void generate(Module rep) {
        BugReportModule br = (BugReportModule)rep;

        // Locate the ftrace section
        Section ftrace = br.findSection(Section.FTRACE);
        if (ftrace == null) {
            br.printErr(3, TAG + "Cannot find section: " + Section.FTRACE);
            return;
        }

        OldParser parser = new OldParser(br, this);
        FTraceData data = parser.parse(ftrace);
        if (data == null) {
            NewParser newParser = new NewParser(br, this);
            data = newParser.parse(ftrace);
        }
        if (data == null || data.isEmpty()) {
            // Give up
            return;
        }
        long duration = data.getDuration();

        VCDGenerator vcdGen = new VCDGenerator(br);
        vcdGen.execute(data);


        // Map ftrace process records to bugreport process records
        Vector<FTraceProcessRecord> list = data.sort();
        for (FTraceProcessRecord pr : list) {
            boolean useParent = false;
            ProcessRecord procRec = br.getProcessRecord(pr.pid, true, false);
            ProcessRecord pProcRec = null;
            PSRecord psr = br.getPSRecord(pr.pid);
            if (psr != null) {
                pProcRec = br.getProcessRecord(psr.getParentPid(), false, false);
                if (pProcRec != null && !"zygote".equals(pProcRec.getProcName())) {
                    useParent = true;
                }
            }
            if (procRec == null || (useParent && pProcRec != null)) {
                procRec = pProcRec;
            }
            if (procRec != null) {
                pr.procRec = procRec;
            }
        }

        // Create report
        Chapter ch, main = new Chapter(br.getContext(), "FTrace");

        // Create statistics
        ch = new Chapter(br.getContext(), "Statistics");
        main.addChapter(ch);
        new Hint(ch)
            .add("VCD file saved as (you can use GTKWave to open it): ")
            .add(new Link(vcdGen.getFileName(), vcdGen.getFileName()));

        Table t = beginStatTbl(ch, br, duration, true, true, 0);
        for (FTraceProcessRecord pr : list) {
            addStatTblRow(br, t, pr, duration, true);
        }
        t.end();

        // Create Trace chapter
        ch = new Chapter(br.getContext(), "Trace");
        main.addChapter(ch);
        t = beginTraceTbl(ch, br, duration, true, true, true);
        for (FTraceProcessRecord pr : list) {
            // Create the trace image
            String png = "ftrace_" + pr.pid + ".png";
            createTracePng(br.getBaseDir() + png, pr, data.getFirstTraceRecord(), duration);
            // Add the table row
            addTraceTblRow(br, t, pr, true);
        }
        t.end();

        // Add some stats and traces to the individual process records
        HashSet<ProcessRecord> usedPR = new HashSet<ProcessRecord>();
        for (FTraceProcessRecord fpr_dummy : list) {
            ProcessRecord pr = fpr_dummy.procRec;
            if (pr != null && !usedPR.contains(pr)) {
                usedPR.add(pr);

                // Add the statistics
                t = beginStatTbl(pr, br, duration, true, false, pr.getPid());
                for (FTraceProcessRecord fpr : list) {
                    if (fpr.procRec != pr) continue;
                    addStatTblRow(br, t, fpr, duration, false);
                }
                t.end();

                // Add the trace
                t = beginTraceTbl(pr, br, duration, true, false, false);
                for (FTraceProcessRecord fpr : list) {
                    if (fpr.procRec != pr) continue;
                    addTraceTblRow(br, t, fpr, false);
                }
                t.end();
            }
        }

        // Create the parallel-histogrram
        ch = new Chapter(br.getContext(), "Parallel process histogram");
        main.addChapter(ch);
        createParallelHist(ch, br, data.getFirstTraceRecord(), duration, TRACE_W);

        br.addChapter(main);
    }

    private DocNode makeProcName(BugReportModule br, FTraceProcessRecord pr, boolean addLink) {
        DocNode ret = new DocNode();

        // Add priority info
        if (addLink) {
            ret.add(new ProcessLink(br, pr.pid, pr.name));
        } else {
            ret.add(pr.getName());
        }
        PSRecord ps = br.getPSRecord(pr.pid);
        if (ps != null) {
            ret.add(new Img(Util.getNiceImg(ps.getNice())));
            ret.add(new Img(Util.getSchedImg(ps.getPolicy())));
        }

        return ret;
    }

    private Table beginStatTbl(Chapter ch, Module br, long duration, boolean addTotal, boolean addExplanation, int pid) {
        new Para(ch)
            .add("Process runtime statistics (total trace duration: ")
            .add(new ShadedValue(duration))
            .add("us):");
        String csv = "ftrace_stat";
        if (pid > 0) {
            csv += "_pid_" + pid;
        }

        Table t = new Table(Table.FLAG_SORT, ch);
        t.setCSVOutput(br, csv);
        t.setTableName(br, csv);
        t.addColumn("Name", Table.FLAG_NONE, "name varchar");
        t.addColumn("Run time (us)", Table.FLAG_ALIGN_RIGHT, "runtime_us int");
        t.addColumn("(%)", Table.FLAG_ALIGN_RIGHT, "runtime_p int");
        t.addColumn("Wait time (us)", Table.FLAG_ALIGN_RIGHT, "waittime_us int");
        t.addColumn("(%)", Table.FLAG_ALIGN_RIGHT, "waittime_p int");
        t.addColumn("Avg. (us)", Table.FLAG_ALIGN_RIGHT, "waittime_avg_us int");
        t.addColumn("Max. (us)", Table.FLAG_ALIGN_RIGHT, "waittime_max_us int");
        t.addColumn("Wait/Run", Table.FLAG_ALIGN_RIGHT, "waittime_run float");
        t.addColumn("IOWait time (us)", Table.FLAG_ALIGN_RIGHT, "iowaittime_us int");
        t.addColumn("(%)", Table.FLAG_ALIGN_RIGHT, "iowaittime_p int");
        t.addColumn("Avg. (us)", Table.FLAG_ALIGN_RIGHT, "iowaittime_avg_us int");
        t.addColumn("Max. (us)", Table.FLAG_ALIGN_RIGHT, "iowaittime_max_us int");
        t.addColumn("IOWait/Run", Table.FLAG_ALIGN_RIGHT, "iowaittime_run float");
        t.begin();
        return t;
    }

    private void addStatTblRow(BugReportModule br, Table t, FTraceProcessRecord pr, long duration, boolean addLink) {
        float waitOverRun = 0.0f;
        float diskOverRun = 0.0f;
        int avgWaitTime = 0;
        int avgDiskTime = 0;
        if (pr.runTime > 0) {
            waitOverRun = (float)pr.waitTime / pr.runTime;
            diskOverRun = (float)pr.diskTime / pr.runTime;
        }
        if (pr.waitTimeCnt > 0) {
            avgWaitTime = (int)(pr.waitTime / pr.waitTimeCnt);
        }
        if (pr.diskTimeCnt > 0) {
            avgDiskTime = (int)(pr.diskTime / pr.diskTimeCnt);
        }
        t.addData(makeProcName(br, pr, addLink));
        t.addData(new ShadedValue(pr.runTime));
        t.addData(String.format("%3.1f", (pr.runTime * 100.0 / duration)));
        t.addData(new ShadedValue(pr.waitTime));
        t.addData(String.format("%3.1f", (pr.waitTime * 100.0 / duration)));
        t.addData(new ShadedValue(avgWaitTime));
        t.addData(new ShadedValue(pr.waitTimeMax));
        t.addData(String.format("%3.2f", waitOverRun));
        t.addData(new ShadedValue(pr.diskTime));
        t.addData(String.format("%3.1f", (pr.diskTime * 100.0 / duration)));
        t.addData(new ShadedValue(avgDiskTime));
        t.addData(new ShadedValue(pr.diskTimeMax));
        t.addData(String.format("%3.2f", diskOverRun));
    }

    private Table beginTraceTbl(Chapter ch, Module br, long duration, boolean addTimeBar, boolean addParallelChart, boolean addExplanation) {
        new Para(ch).add("Process trace overview:");

        if (addExplanation) {
            new Block(ch).add(new Img("ftrace-legend-dred.png")).add("Partially running");
            new Block(ch).add(new Img("ftrace-legend-red.png")).add("Running");
            new Block(ch).add(new Img("ftrace-legend-dcyan.png")).add("Partially waiting");
            new Block(ch).add(new Img("ftrace-legend-cyan.png")).add("Waiting");
            new Block(ch).add(new Img("ftrace-legend-yellow.png")).add("Waiting for IO");
            new Block(ch).add(new Img("ftrace-legend-black.png")).add("Sleeping");
        }

        Table t = new Table(Table.FLAG_DND, ch);
        t.addColumn("Name", Table.FLAG_NONE);
        t.addColumn("Trace", Table.FLAG_NONE);
        t.begin();

        if (addTimeBar) {
            String fnTimeBar = getTimeBarName(br, duration);
            if (fnTimeBar != null) {
                t.addData("Relative time");
                t.addData(new Img(fnTimeBar));
            }
        }

        if (addParallelChart) {
            t.addData("Number of processes wanting to run in parallel");
            t.addData(new Img(getParallelChartName()));
        }

        return t;
    }

    private void addTraceTblRow(BugReportModule br, Table t, FTraceProcessRecord pr, boolean addLink) {
        String png = "ftrace_" + pr.pid + ".png";
        t.addData(makeProcName(br, pr, addLink));
        t.addData(new Img(png));
    }

    private String getParallelChartName() {
        return "ftrace_nr_parallel.png";
    }

    private String getTimeBarName(Module br, long duration) {
        if (mTimeBarName == null) {
            String fnTimeBar = "ftrace_time.png";
            if (Util.createTimeBar(br, fnTimeBar, TRACE_W, 0, duration / 1000)) { // us -> ms
                mTimeBarName = fnTimeBar;
            }
        }
        return mTimeBarName;
    }

    private void createTracePng(String fileName, FTraceProcessRecord pr, TraceRecord head, long duration) {
        // Setup initial data
        int w = TRACE_W;
        int h = TRACE_H;
        long startTime = head.time;
        int pid = pr.pid;
        int lastState = 'S';
        if (pr.initState == Const.STATE_RUN) {
            lastState = 'R';
        } else if (pr.initState == Const.STATE_DISK) {
            lastState = 'D';
        }
        int lastX = 0;

        // Create the empty image
        ImageCanvas img = new ImageCanvas(w, h);
        img.setColor(ImageCanvas.BLACK);
        img.fillRect(0, 0, w, h);

        // Render the trace
        int darkRed = 0xff800000;
        int darkCyan = 0xff008080;
        while (head != null) {
            if (head.prevPid == pid) {
                // This process was switched away, render something
                int x = (int)((head.time - startTime) * w / duration);
                if (lastX != -1) {
                    if (lastX == x) {
                        img.setColor(darkRed);
                        img.fillRect(lastX, 0, 1, h);
                    } else {
                        img.setColor(ImageCanvas.RED);
                        img.fillRect(lastX + 1, 0, x - lastX + 1, h);
                    }
                }
                lastX = x;
                lastState = head.prevState;
            }
            if (head.nextPid == pid) {
                // This process was resumed (or at least woken up),
                int x = (int)((head.time - startTime) * w / duration);
                if (lastX != -1) {
                    if (lastState == 'D') {
                        img.setColor(ImageCanvas.YELLOW);
                        img.drawLine(lastX, h/2, x, h/2);
                    } else if (lastState == 'R' && pid != 0) {
                        if (lastX == x) {
                            img.setColor(darkCyan);
                            img.fillRect(lastX, 0, 1, h);
                        } else {
                            img.setColor(ImageCanvas.CYAN);
                            img.fillRect(lastX + 1, 0, x - lastX + 1, h);
                        }
                    }
                }
                lastX = x;
                lastState = head.nextState;
            }
            head = head.next;
        }

        // Save the image
        try {
            img.writeTo(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createParallelHist(Chapter ch, BugReportModule br, TraceRecord head, long duration, int w) {
        // Setup initial data
        int max = 16;
        long durations[] = new long[max];
        int count = 0;
        int maxUsed = 0;
        long startTime = head.time;
        long lastTime = head.time;
        int lastX = 0;

        // Create the empty image
        int stepSize = 8;
        int h = stepSize * max;
        ImageCanvas img = new ImageCanvas(w, h);
        int minNr[] = newIntArr(w, Integer.MAX_VALUE);
        int maxNr[] = newIntArr(w, 0);
        img.setColor(ImageCanvas.BLACK);
        img.fillRect(0, 0, w, h);
        img.setColor(ImageCanvas.RED);

        // Process the trace
        while (head != null) {
            long now = head.time;

            int newCount = head.nrRunWait;
            if (newCount != count) {
                int x = (int)((head.time - startTime) * w / duration);
                fillMinMax(lastX, x, count, minNr, maxNr);
                lastX = x;

                // We need to add the duration with the previous count
                long dur = now - lastTime;
                lastTime = now;
                int idx = count;
                if (idx >= max) {
                    idx = max-1;
                }
                if (idx > maxUsed) {
                    maxUsed = idx;
                }
                durations[idx] += dur;

                // And only now we update the count to the new value
                count = newCount;
            }

            head = head.next;
        }

        // Now do the actual rendering
        int cmin = 0xffff0000;
        int cmax = 0xff800000;
        for (int i = 0; i < w; i++) {
            if (minNr[i] > maxNr[i]) {
                // Skip -> no data
                continue;
            }
            int ymin = h - 1 - stepSize * minNr[i];
            if (ymin < 0) ymin = 0;
            int ymax = h - 1 - stepSize * maxNr[i];
            if (ymax < 0) ymax = 0;
            img.setColor(cmin);
            img.fillRect(i, ymin, 1, h - ymin);
            img.setColor(cmax);
            img.fillRect(i, ymax, 1, ymin - ymax);
        }

        new Para(ch).add("The following table shows how many processes were either running or waiting at the same time:");

        String fnHist = "par_proc_hist.png";
        new Block(ch).addStyle("float-right").add(new Img(fnHist));
        Table t = new Table(Table.FLAG_SORT, ch);
        t.addStyle("auto-width");
        t.addColumn("Number of parallel processes", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Run time (us)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Run time (%)", Table.FLAG_ALIGN_RIGHT);
        t.begin();
        for (int i = 0; i <= maxUsed; i++) {
            t.addData("" + i + (i == max-1 ? " or more" : ""));
            t.addData(new ShadedValue(durations[i]));
            t.addData(String.format("%3.1f", (durations[i] * 100.0 / duration)));
        }
        t.end();

        // Add some guidelines
        img.setColor(0x80ffffff);
        for (int i = 0; i < max; i++) {
            int yy = h - 1 - i * stepSize;
            img.drawLine(0, yy, w, yy);
        }

        // Save the image
        try {
            img.writeTo(new File(br.getBaseDir() + getParallelChartName()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create the histogram image
        int hw = 400, hh = 400, hmax = 350;
        int hml = 64, hmr = 32, hmt = 32, hmb = 64;
        int thw = hml + hw + hmr;
        int thh = hmt + hh + hmb;
        img = new ImageCanvas(thw, thh);
        img.setColor(ImageCanvas.WHITE);
        img.fillRect(0, 0, thw, thh);
        img.setColor(ImageCanvas.BLACK);

        // Draw the axis
        img.drawLine(hml, hmt, hml, hmt + hh);
        img.drawLine(hml - 5, hmt + 5, hml, hmt);
        img.drawLine(hml + 5, hmt + 5, hml, hmt);
        img.drawLine(hml + hw, hmt + hh, hml, hmt + hh);
        img.drawLine(hml + hw - 5, hmt + hh - 5, hml + hw, hmt + hh);
        img.drawLine(hml + hw - 5, hmt + hh + 5, hml + hw, hmt + hh);

        // Draw the labels on the Y axis
        for (int i = 10; i <= 100; i += 10) {
            int yy = hmt + hh - (hmax * i / 100);
            img.setColor(ImageCanvas.BLACK);
            img.drawLine(hml - 5, yy, hml, yy);
            String s = Integer.toString(i) + "% ";
            img.drawString(s, hml - 5 - img.getStringWidth(s), yy);
            img.setColor(ImageCanvas.LIGHT_GRAY);
            img.drawLine(hml, yy, hml + hw, yy);
        }

        // Draw the bars and the labels on the X axis
        int cnt = durations.length;
        int bd = (hw / cnt);
        int bm = 2, bw = bd - 2 * bm;
        for (int i = 0; i < durations.length; i++) {
            int green = (cnt - i) * 255 / cnt;
            int red = i * 255 / cnt;
            int rgb = 0xff000000 | (red << 16) | (green << 8);
            img.setColor(rgb);
            int bh = (int)(hmax * durations[i] / duration);
            int bx = hml + i * bd + bm;
            img.fillRect(bx, hmt + hh - bh, bw, bh);
            img.setColor(ImageCanvas.BLACK);
            bx += bw / 2;
            img.drawLine(bx, hmt + hh, bx, hmt + hh + 5);
            String s = Integer.toString(i);
            img.drawString(s, bx - img.getStringWidth(s) / 2, hmt + hh + 5 + img.getAscent());
        }

        // Draw the title
        img.drawString("Parallel process histogram", 10, 10 + img.getAscent());

        try {
            img.writeTo(new File(br.getBaseDir() + fnHist));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int[] newIntArr(int size, int defValue) {
        int ret[] = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = defValue;
        }
        return ret;
    }

    private void fillMinMax(int from, int to, int value, int[] minNr, int[] maxNr) {
        from = Math.max(0, from);
        to = Math.min(minNr.length - 1, to);
        for (int i = from; i <= to; i++) {
            if (value < minNr[i]) {
                minNr[i] = value;
            }
            if (value > maxNr[i]) {
                maxNr[i] = value;
            }
        }
    }

}
