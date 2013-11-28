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
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.SimpleText;
import com.sonyericsson.chkbugreport.doc.Span;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.traceview.TraceModule.MethodInfo;
import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class StatsPlugin extends Plugin {

    private static final int NR_LINES = 100;

    @Override
    public int getPrio() {
        return 20;
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

        Chapter ch = new Chapter(rep.getContext(), "Statistics");
        rep.addChapter(ch);

        Chapter fullCh = new Chapter(rep.getContext(), "Statistics (full)");
        rep.addExtraFile(fullCh);

        Vector<MethodInfo> methodsByDur = new Vector<MethodInfo>();

        for (MethodInfo m : rep.getMethodInfos()) {
            methodsByDur.add(m);
        }

        Collections.sort(methodsByDur, new Comparator<MethodInfo>() {
            @Override
            public int compare(MethodInfo o1, MethodInfo o2) {
                return o2.dur - o1.dur;
            }
        });

        new Block(ch)
            .add("Showing only " + NR_LINES + " lines, to see the full statistics, ")
            .add(new Link(fullCh.getAnchor(), "click here"))
            .add("!");

        createTable(methodsByDur, ch, true);
        createTable(methodsByDur, fullCh, false);
    }

    private void createTable(Vector<MethodInfo> methodsByDur, Chapter ch, boolean limit) {
        new Block(ch).add("[P] = process time");
        new Block(ch).add("[T] = thread time");
        Table t = new Table(Table.FLAG_SORT, ch);
        t.addColumn("Method", "The name of method", Table.FLAG_NONE);
        t.addColumn("Duration[P]", "The total duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the process time.", Table.FLAG_NONE);
        t.addColumn("Duration[T]", "The total duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the thread time.", Table.FLAG_NONE);
        t.addColumn("Own dur[P]", "The total duration of all (non-recursive) method calls, excluding the time spent when calling other methods, measured using the process time.", Table.FLAG_NONE);
        t.addColumn("Own dur[T]", "The total duration of all (non-recursive) method calls, excluding the time spent when calling other methods, measured using the thread time.", Table.FLAG_NONE);
        t.addColumn("Non-rec. calls", "The number of non-recursive calls of this method", Table.FLAG_NONE);
        t.addColumn("Rec. calls", "The number of recursive calls of this method", Table.FLAG_NONE);
        t.addColumn("Avg dur[P]", "The average duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the process time.", Table.FLAG_NONE);
        t.addColumn("Avg dur[T]", "The average duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the thread time.", Table.FLAG_NONE);
        t.addColumn("Max dur[T]", "The maximum duration from all (non-recursive) method calls, including the time spent when calling other methods, measured using the thread time.", Table.FLAG_NONE);
        t.addColumn("Max/Avg dur[T]", "The ratio of the maximum and average duration", Table.FLAG_NONE);
        t.begin();

        int cnt = methodsByDur.size();
        for (int i = 0; i < cnt; i++) {
            MethodInfo m = methodsByDur.get(i);
            if (m.nrCalls == 0) {
                continue; // strange
            }
            float maxDurRatio = 0.0f;
            if (m.durL > 0) {
                maxDurRatio = (float)m.maxDurL * m.nrCalls / m.durL;
            }

            t.addData(shadeName(HtmlUtil.escape(m.shortName)));
            t.addData(new ShadedValue(m.dur));
            t.addData(new ShadedValue(m.durL));
            t.addData(new ShadedValue(m.durExc));
            t.addData(new ShadedValue(m.durExcL));
            t.addData(m.nrCalls);
            t.addData(m.nrRecCalls);
            t.addData(new ShadedValue((m.dur / m.nrCalls)));
            t.addData(new ShadedValue((m.durL / m.nrCalls)));
            t.addData(new ShadedValue(m.maxDurL));
            t.addData(String.format("%.2f", maxDurRatio));
            if (i >= NR_LINES && limit) {
                break;
            }
        }
    }

    private DocNode shadeName(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0) return new SimpleText(name);
        String cls = name.substring(0, idx + 1);
        name = name.substring(idx + 1);
        return new DocNode()
            .add(new Span().addStyle("traceview-name").add(cls))
            .add(name);
    }

}
