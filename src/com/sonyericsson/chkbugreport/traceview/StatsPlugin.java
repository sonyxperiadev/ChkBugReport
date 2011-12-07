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
import com.sonyericsson.chkbugreport.Lines;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.traceview.TraceReport.MethodInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
    public void load(Report br) {
        // NOP
    }

    @Override
    public void generate(Report br) {
        TraceReport rep = (TraceReport)br;
        String fnFull = br.getRelDataDir() + "stats.html";
        Lines fullStat = new Lines(null);

        Chapter ch = new Chapter(rep, "Statistics");
        rep.addChapter(ch);

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

        ch.addLine("<div>Showing only " + NR_LINES + " lines, to see the full statistics, click <a href=\"" + fnFull + "\">here</a>!</div>");
        Lines tmp = new Lines(null);
        tmp.addLine("<div>[P] = process time</div>");
        tmp.addLine("<div>[T] = thread time</div>");
        tmp.addLine("<div>(NOTE: Click on the headers to sort the data)</div>");
        tmp.addLine("<table class=\"tablesorter traceview-stat\">");
        tmp.addLine("  <thead>");
        tmp.addLine("    <tr>");
        tmp.addLine("      <th title=\"The name of method\">Method</td>");
        tmp.addLine("      <th title=\"The total duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the process time.\">Duration[P]</td>");
        tmp.addLine("      <th title=\"The total duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the thread time.\">Duration[T]</td>");
        tmp.addLine("      <th title=\"The total duration of all (non-recursive) method calls, excluding the time spent when calling other methods, measured using the process time.\">Own dur[P]</td>");
        tmp.addLine("      <th title=\"The total duration of all (non-recursive) method calls, excluding the time spent when calling other methods, measured using the thread time.\">Own dur[T]</td>");
        tmp.addLine("      <th title=\"The number of non-recursive calls of this method\">Non-rec. calls</td>");
        tmp.addLine("      <th title=\"The number of recursive calls of this method\">Rec. calls</td>");
        tmp.addLine("      <th title=\"The average duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the process time.\">Avg dur[P]</td>");
        tmp.addLine("      <th title=\"The average duration of all (non-recursive) method calls, including the time spent when calling other methods, measured using the thread time.\">Avg dur[T]</td>");
        tmp.addLine("      <th title=\"The maximum duration from all (non-recursive) method calls, including the time spent when calling other methods, measured using the thread time.\">Max dur[T]</td>");
        tmp.addLine("      <th title=\"The ratio of the maximum and average duration\">Max/Avg dur[T]</td>");
        tmp.addLine("    </tr>");
        tmp.addLine("  </thead>");
        tmp.addLine("  <tbody>");
        ch.addLines(tmp);
        fullStat.addLines(tmp);

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
            tmp.clear();
            tmp.addLine("    <tr>");
            tmp.addLine("      <td>" + shadeName(Util.escape(m.shortName)) + "</td>");
            tmp.addLine("      <td>" + shadeDur(m.dur) + "</td>");
            tmp.addLine("      <td>" + shadeDur(m.durL) + "</td>");
            tmp.addLine("      <td>" + shadeDur(m.durExc) + "</td>");
            tmp.addLine("      <td>" + shadeDur(m.durExcL) + "</td>");
            tmp.addLine("      <td>" + m.nrCalls + "</td>");
            tmp.addLine("      <td>" + m.nrRecCalls + "</td>");
            tmp.addLine("      <td>" + shadeDur((m.dur / m.nrCalls)) + "</td>");
            tmp.addLine("      <td>" + shadeDur((m.durL / m.nrCalls)) + "</td>");
            tmp.addLine("      <td>" + shadeDur(m.maxDurL) + "</td>");
            tmp.addLine("      <td>" + String.format("%.2f", maxDurRatio) + "</td>");
            tmp.addLine("    </tr>");
            if (i < NR_LINES) {
                ch.addLines(tmp);
            }
            fullStat.addLines(tmp);
        }

        tmp.clear();
        tmp.addLine("  </tbody>");
        tmp.addLine("</table>");
        ch.addLines(tmp);
        fullStat.addLines(tmp);

        // Save standalone file
        try {
            FileOutputStream fos = new FileOutputStream(rep.getBaseDir() + fnFull);
            PrintStream ps = new PrintStream(fos);
            Util.writeHTMLHeader(ps, "TraceView statistics", "");
            fullStat.writeTo(ps);
            Util.writeHTMLFooter(ps);
            ps.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String shadeName(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0) return name;
        String cls = name.substring(0, idx + 1);
        name = name.substring(idx + 1);
        return "<span class=\"traceview-name\">" + cls + "</span>" + name;
    }

    private String shadeDur(int dur) {
        StringBuffer sb = new StringBuffer();
        int ms = dur / 1000;
        int us = dur % 1000;
        if (ms != 0) {
            sb.append(ms);
        }
        sb.append("<span class=\"traceview-us\">");
        if (us < 100) {
            sb.append('0');
        }
        if (us < 10) {
            sb.append('0');
        }
        sb.append(us);
        sb.append("</span>");
        return sb.toString();
    }

}
