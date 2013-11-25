/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Table;

public class CpuFreqPlugin extends Plugin {

    private static final String TAG = "[CpuFreqPlugin]";
    private boolean mLoaded;
    private int mFreqCount;
    private int mTotalTime;
    private int[] mFreqs;
    private int[] mTimes;

    @Override
    public int getPrio() {
        return 85;
    }

    @Override
    public void reset() {
        mLoaded = false;
    }

    @Override
    public void load(Module mod) {
        Section sec = mod.findSection(Section.KERNEL_CPUFREQ);
        if (sec == null) {
            mod.printErr(3, TAG + "Section not found: " + Section.KERNEL_CPUFREQ + " (aborting plugin)");
            return;
        }

        // Find the battery history
        mFreqCount = sec.getLineCount();
        mFreqs = new int[mFreqCount];
        mTimes = new int[mFreqCount];
        mTotalTime = 0;
        for (int i = 0; i < mFreqCount; i++) {
            String buff[] = sec.getLine(i).split(" ");
            if (buff.length != 2) {
                mFreqCount = i;
                break;
            }
            mFreqs[i] = Integer.parseInt(buff[0]);
            mTimes[i] = Integer.parseInt(buff[1]);
            mTotalTime += mTimes[i];
        }
        mLoaded = true;
    }

    @Override
    public void generate(Module mod) {
        if (!mLoaded) return;

        // Create the chapter
        Chapter ch = mod.findOrCreateChapter("CPU/Frequencies");
        Table t = new Table();
        ch.add(t);
        t.setCSVOutput(mod, "cpufreq");
        t.setTableName(mod, "cpufreq");
        t.addColumn("Frequency (MHz)", Table.FLAG_ALIGN_RIGHT, "freq int");
        t.addColumn("Time (sec)", Table.FLAG_ALIGN_RIGHT, "time_sec int");
        t.addColumn("Time (%)", Table.FLAG_ALIGN_RIGHT, "time_p int");
        t.begin();
        for (int i = 0; i < mFreqCount; i++) {
            int perc = mTotalTime == 0 ? 0 : (mTimes[i] * 100 / mTotalTime);
            t.addData(mFreqs[i] / 1000);
            t.addData(mTimes[i] / 100);
            t.addData(perc);
        }
        t.end();
    }

}
