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
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;

public class CpuFreqPlugin extends Plugin {

    private static final String TAG = "[CpuFreqPlugin]";

    @Override
    public int getPrio() {
        return 85;
    }

    @Override
    public void load(Report br) {
        // NOP
    }

    @Override
    public void generate(Report br) {
        Section sec = br.findSection(Section.KERNEL_CPUFREQ);
        if (sec == null) {
            br.printErr(TAG + "Section not found: " + Section.KERNEL_CPUFREQ + " (aborting plugin)");
            return;
        }

        // Find the battery history
        int cnt = sec.getLineCount();
        int freqs[] = new int[cnt];
        int time[] = new int[cnt];
        int totalTime = 0;
        for (int i = 0; i < cnt; i++) {
            String buff[] = sec.getLine(i).split(" ");
            if (buff.length != 2) {
                cnt = i;
                break;
            }
            freqs[i] = Integer.parseInt(buff[0]);
            time[i] = Integer.parseInt(buff[1]);
            totalTime += time[i];
        }

        // Create the chapter
        Chapter ch = new Chapter(br, "CPU Freq");
        ch.addLine("<table class=\"cpufreq\">");
        ch.addLine("  <thead>");
        ch.addLine("    <tr>");
        ch.addLine("      <td>Frequency</td>");
        ch.addLine("      <td>Time(sec)</td>");
        ch.addLine("      <td>Time(%)</td>");
        ch.addLine("      <td><div style=\"width: 102px;\">Graph</div></td>");
        ch.addLine("    </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");
        for (int i = 0; i < cnt; i++) {
            ch.addLine("    <tr>");
            int perc = totalTime == 0 ? 0 : (time[i] * 100 / totalTime);
            ch.addLine(String.format("      <td>%dMHz</td>", freqs[i] / 1000));
            ch.addLine(String.format("      <td>%d</td>", time[i] / 100));
            ch.addLine(String.format("      <td>%d%%</td>", perc));
            ch.addLine(String.format("      <td><div style=\"background: #f00;border: solid 1px;height: 20px;width: %dpx;\"></div></td>", perc));
            ch.addLine("    </tr>");
        }
        ch.addLine("  </tbody>");
        ch.addLine("</table>");
        br.addChapter(ch);
    }

}
