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
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.traceview.TraceReport.MethodRun;
import com.sonyericsson.chkbugreport.traceview.TraceReport.ThreadInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

public class TreeViewPlugin extends Plugin {

    private static final int MAX_LEVELS = 4;

    private static final int MAX_LEVELS_W_DURATION = 15;
    private static final int MIN_DURATION = 10*1000; // 10ms

    private int mCurId;

    @Override
    public int getPrio() {
        return 40;
    }

    @Override
    public void load(Report br) {
        // NOP
    }

    @Override
    public void generate(Report br) {
        TraceReport rep = (TraceReport)br;
        Chapter ch = new Chapter(rep, "Trace tree files (lim. levels)");
        rep.addChapter(ch);

        // Save individual threads
        for (ThreadInfo t : rep.getThreadInfos()) {
            Chapter child = new Chapter(rep, t.getFullName());
            ch.addChapter(child);
            int tid = t.id;
            String fn = br.getRelRawDir() + "thread_" + tid + ".tree.txt";
            saveTraceTree(rep, tid, fn);
            child.addLine("<div>Full trace: <a href=\"" + fn + "\">" + fn + "</a></div>");

            child.addLine("<div class=\"traceview-tree\">");
            saveTraceTreeHtml(rep, tid, child, MAX_LEVELS, 0);
            child.addLine("</pre>");
        }

        // Create another variant where the display limit is based on duration
        ch = new Chapter(rep, "Trace tree files (lim. duration)");
        rep.addChapter(ch);

        // Save individual threads
        for (ThreadInfo t : rep.getThreadInfos()) {
            Chapter child = new Chapter(rep, t.getFullName());
            ch.addChapter(child);
            int tid = t.id;
            String fn = "raw/thread_" + tid + ".tree.txt";
            child.addLine("<div>Full trace: <a href=\"" + fn + "\">" + fn + "</a></div>");

            child.addLine("<div class=\"traceview-tree\">");
            saveTraceTreeHtml(rep, tid, child, MAX_LEVELS_W_DURATION, MIN_DURATION);
            child.addLine("</pre>");
        }
    }

    private void nextId() {
        mCurId++;
    }

    private String getId() {
        return "tv_tr_" + mCurId;
    }

    private String getChildrenId() {
        return "tv_tr_" + mCurId + "_c";
    }

    private void saveTraceTreeHtml(TraceReport rep, int tid, Chapter ch, int level, int mindur) {
        ThreadInfo thread = rep.findThread(tid);
        if (thread.calls.size() == 0) return;
        nextId();
        String divId = getChildrenId();
        ch.addLine("<div>Method calls on Thread-" + tid + " (" + thread.name + "):</div>");
        if (mindur == 0) {
            ch.addLine("<div>(Maximum " + level + " levels)</div>");
        } else {
            ch.addLine("<div>(Maximum " + level + " levels, showing only items with thread time duration above " + (mindur / 1000) + "ms)</div>");
        }
        ch.addLine("<div>Note: you can collapse/expand items by clicking on them.</div>");
        ch.addLine("<div><button onClick=\"tvtrShow('" + divId + "')\">Expand all</button> <button onClick=\"tvtrHide('" + divId + "')\">Collapse all</button></div>");
        ch.addLine("<div># [duration in proc time, duration in thread time, nr calls] method name</div>");

        printTraceTreeHtml(rep, ch, "", thread.calls, level, mindur);
    }

    private void printTraceTreeHtml(TraceReport rep, Chapter ch, String indent, Vector<MethodRun> calls, int level, int mindur) {
        int cnt = calls.size();
        if (cnt == 0) return;
        String divId = getChildrenId();
        String cssClass = "";
        if (level != MAX_LEVELS) {
            cssClass = "class=\"tv_tr_c\" ";
        }
        ch.addLine("<div " + cssClass + " id=\"" + divId + "\">");
        if (level <= 0) {
            ch.addLine("<div>" + indent + "  ...</div>");
        } else {
            // If we have duration limit, thigs are a bit trickier
            if (mindur > 0) {
                // We need two runs. First, find out the last visible child
                int lastIdx = -1;
                for (int i = 0; i < cnt; i++) {
                    MethodRun child = calls.get(i);
                    int dur = child.endLocalTime - child.startLocalTime;
                    if (dur >= mindur) {
                        lastIdx = i;
                    }
                }
                for (int i = 0; i < cnt; i++) {
                    MethodRun child = calls.get(i);
                    int dur = child.endLocalTime - child.startLocalTime;
                    if (dur >= mindur) {
                        printTraceTreeHtml(rep, ch, indent, calls.get(i), (i < lastIdx), level - 1, mindur);
                    }
                }
            } else {
                for (int i = 0; i < cnt; i++) {
                    printTraceTreeHtml(rep, ch, indent, calls.get(i), (i < cnt - 1), level - 1, mindur);
                }
            }
        }
        ch.addLine("</div>");
    }

    private void printTraceTreeHtml(TraceReport rep, Chapter ch, String indent, MethodRun run, boolean last, int level, int mindur) {
        int dur = run.endTime - run.startTime;
        int durL = run.endLocalTime - run.startLocalTime;
        String name = rep.findMethod(run.mid).name;
        nextId();
        String divId = getId();
        ch.addLine("<div class=\"tv_tr\" id=\"" + divId + "\">" + indent + "+-[" + getDur(dur) + "," + getDur(durL) + "," + run.nrCalls + "] " + shadeName(name) + "</div>");
        String pref = last ? "|&nbsp;" : "&nbsp;&nbsp;";
        printTraceTreeHtml(rep, ch, indent + pref, run.calls, level, mindur);
    }

    private void saveTraceTree(TraceReport rep, int tid, String fn) {
        System.out.println("Writing " + fn + "...");

        try {
            FileOutputStream fos = new FileOutputStream(rep.getBaseDir() + fn);
            PrintStream ps = new PrintStream(fos);

            ThreadInfo thread = rep.findThread(tid);
            ps.println("Method calls on Thread-" + tid + " (" + thread.name + "):");
            ps.println("# [duration in proc time, duration in thread time, nr calls] method name");

            printTraceTree(rep, ps, "", thread.calls);

            ps.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printTraceTree(TraceReport rep, PrintStream ps, String indent, Vector<MethodRun> calls) {
        int cnt = calls.size();
        for (int i = 0; i < cnt; i++) {
            printTraceTree(rep, ps, indent, calls.get(i), (i < cnt - 1));
        }
    }

    private void printTraceTree(TraceReport rep, PrintStream ps, String indent, MethodRun run, boolean last) {
        int dur = run.endTime - run.startTime;
        int durL = run.endLocalTime - run.startLocalTime;
        String name = rep.findMethod(run.mid).name;
        ps.println(indent + "+-[" + getDur(dur) + "," + getDur(durL) + "," + run.nrCalls + "] " + name);
        String pref = last ? "| " : "  ";
        printTraceTree(rep, ps, indent + pref, run.calls);
    }

    private String getDur(int dur) {
        if (dur > 1500) {
            return Integer.toString(dur / 1000) + "ms";
        }
        return Integer.toString(dur) + "us";
    }

    private String shadeName(String name) {
        int idx = name.lastIndexOf('.');
        int idx2 = name.lastIndexOf('(');
        if (idx < 0 || idx2 < 0) return name;
        String cls = name.substring(0, idx + 1);
        String param = name.substring(idx2);
        name = name.substring(idx + 1, idx2);
        return "<span class=\"traceview-name\">" + cls + "</span>" + name + "<span class=\"traceview-param\">" + param + "</span>";
    }

}
