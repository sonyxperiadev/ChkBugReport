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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Button;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.HtmlNode;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.traceview.TraceModule.MethodRun;
import com.sonyericsson.chkbugreport.traceview.TraceModule.ThreadInfo;

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
        Chapter ch = new Chapter(rep.getContext(), "Trace tree files (lim. levels)");
        rep.addChapter(ch);

        // Save individual threads
        for (ThreadInfo t : rep.getThreadInfos()) {
            Chapter child = new Chapter(rep.getContext(), t.getFullName());
            ch.addChapter(child);
            int tid = t.id;
            String fn = br.getRelRawDir() + "thread_" + tid + ".tree.txt";
            saveTraceTree(rep, tid, fn);
            new Block(child).add("Full trace: ").add(new Link(fn, fn));
            DocNode tree = new Block(child).addStyle("traceview-tree");
            saveTraceTreeHtml(rep, tid, tree, MAX_LEVELS, 0);
        }

        // Create another variant where the display limit is based on duration
        ch = new Chapter(rep.getContext(), "Trace tree files (lim. duration)");
        rep.addChapter(ch);

        // Save individual threads
        for (ThreadInfo t : rep.getThreadInfos()) {
            Chapter child = new Chapter(rep.getContext(), t.getFullName());
            ch.addChapter(child);
            int tid = t.id;
            String fn = br.getRelRawDir() + "thread_" + tid + ".tree.txt";
            new Block(child).add("Full trace: ").add(new Link(fn, fn));
            DocNode tree = new Block(child).addStyle("traceview-tree");
            saveTraceTreeHtml(rep, tid, tree, MAX_LEVELS_W_DURATION, MIN_DURATION);
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

    private void saveTraceTreeHtml(TraceModule rep, int tid, DocNode tree, int level, int mindur) {
        ThreadInfo thread = rep.findThread(tid);
        if (thread.calls.size() == 0) return;
        nextId();
        String divId = getChildrenId();
        new Block(tree).add("Method calls on Thread-" + tid + " (" + thread.name + "):");
        if (mindur == 0) {
            new Block(tree).add("(Maximum " + level + " levels)");
        } else {
            new Block(tree).add("(Maximum " + level + " levels, showing only items with thread time duration above " + (mindur / 1000) + "ms)");
        }
        new Hint(tree).add("Note: you can collapse/expand items by clicking on them.");
        new Block(tree)
            .add(new Button("Expand all", "tvtrShow('" + divId + "')"))
            .add(new Button("Collapse all", "tvtrHide('" + divId + "')"));
        new Block(tree).add("# [duration in proc time, duration in thread time, nr calls] method name");

        printTraceTreeHtml(rep, tree, "", thread.calls, level, mindur);
    }

    private void printTraceTreeHtml(TraceModule rep, DocNode tree, String indent, Vector<MethodRun> calls, int level, int mindur) {
        int cnt = calls.size();
        if (cnt == 0) return;
        String divId = getChildrenId();
        HtmlNode list = new Block(tree).setId(divId);
        if (level != MAX_LEVELS) {
            list.addStyle("tv_tr_c");
        }
        if (level <= 0) {
            new Block(list).add(indent + "  ...");
        } else {
            // If we have duration limit, things are a bit trickier
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
                        printTraceTreeHtml(rep, list, indent, calls.get(i), (i < lastIdx), level - 1, mindur);
                    }
                }
            } else {
                for (int i = 0; i < cnt; i++) {
                    printTraceTreeHtml(rep, list, indent, calls.get(i), (i < cnt - 1), level - 1, mindur);
                }
            }
        }
    }

    private void printTraceTreeHtml(TraceModule rep, DocNode tree, String indent, MethodRun run, boolean last, int level, int mindur) {
        int dur = run.endTime - run.startTime;
        int durL = run.endLocalTime - run.startLocalTime;
        String name = rep.findMethod(run.mid).name;
        nextId();
        String divId = getId();
        HtmlNode list = new Block(tree).addStyle("tv_tr").setId(divId);
        list.add(indent + "+-[" + getDur(dur) + "," + getDur(durL) + "," + run.nrCalls + "] " + shadeName(name));
        String pref = last ? "|&nbsp;" : "&nbsp;&nbsp;";
        printTraceTreeHtml(rep, tree, indent + pref, run.calls, level, mindur);
    }

    private void saveTraceTree(TraceModule rep, int tid, String fn) {
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

    private void printTraceTree(TraceModule rep, PrintStream ps, String indent, Vector<MethodRun> calls) {
        int cnt = calls.size();
        for (int i = 0; i < cnt; i++) {
            printTraceTree(rep, ps, indent, calls.get(i), (i < cnt - 1));
        }
    }

    private void printTraceTree(TraceModule rep, PrintStream ps, String indent, MethodRun run, boolean last) {
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
