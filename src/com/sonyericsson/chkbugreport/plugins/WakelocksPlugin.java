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
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.util.TableGen;

import java.util.Vector;

/**
 * Note: some information was taken from: {@link http://lwn.net/Articles/400771/}
 *
 */
public class WakelocksPlugin extends Plugin {

    private static final String TAG = "[WakelocksPlugin]";

    private static final String COLUMNS[] = {
        "name",
        "count",
        "expire_count",
        "wake_count",
        "active_since",
        "total_time",
        "sleep_time",
        "max_time",
        "last_change",
    };

    private boolean mLoaded;
    private Vector<KernelWakelock> mLocks = new Vector<KernelWakelock>();

    @Override
    public int getPrio() {
        return 90;
    }

    @Override
    public void load(Report br) {
        mLoaded = false;
        mLocks.clear();
        Section section = br.findSection(Section.KERNEL_WAKELOCKS);
        if (section == null) {
            br.printErr(3, TAG + "Section not found: " + Section.KERNEL_WAKELOCKS + " (aborting plugin)");
            return;
        }
        String line = section.getLine(0);
        String columns[] = line.split("\t");
        boolean ok = true;
        if (columns.length < COLUMNS.length) {
            ok = false;
        }
        for (int i = 0; i < COLUMNS.length; i++) {
            if (!COLUMNS[i].equals(columns[i])) {
                ok = false;
            }
        }
        if (!ok) {
            br.printErr(3, TAG + "Data format changed in section " + Section.KERNEL_WAKELOCKS + " (aborting plugin)");
            return;
        }
        int cnt = section.getLineCount();
        for (int i = 1; i < cnt; i++) {
            line = section.getLine(i);
            columns = line.split("\t");
            if (columns.length < COLUMNS.length) {
                continue;
            }
            KernelWakelock lock = new KernelWakelock();
            lock.name = columns[0];
            lock.count = Long.parseLong(columns[1]);
            lock.expire_count = Long.parseLong(columns[2]);
            lock.wake_count = Long.parseLong(columns[3]);
            lock.active_since = Long.parseLong(columns[4]);
            lock.total_time = Long.parseLong(columns[5]);
            lock.sleep_time = Long.parseLong(columns[6]);
            lock.max_time = Long.parseLong(columns[7]);
            lock.last_change = Long.parseLong(columns[8]);
            mLocks.add(lock);
        }
        mLoaded = true;
    }


    @Override
    public void generate(Report rep) {
        if (!mLoaded) return;

        Chapter ch = new Chapter(rep, "Kernel wakelocks");
        rep.addChapter(ch);

        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(rep, "kernel_wakelocks");
        tg.addColumn("Name", "The name of the wakelock as provided by the driver", 0);
        tg.addColumn("Count", "The number of times that the wakelock has been acquired.", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Expire count", "The number of times that the wakelock has timed out. This indicates that some application has an input device open, but is not reading from it, which is a bug.", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Wake count", "The number of times that the wakelock was the first to be acquired in the resume path.", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Active since (ms)", "Tracks how long a wakelock has been held since it was last acquired, or zero if it is not currently held." , TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Total time (ms)", "Accumulates the total amount of time that the corresponding wakelock has been held.", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Average time (ms)", "Total time divided by Count.", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Sleep time (ms)", "The total time that the wakelock was held while the display was powered off.", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Max time (ms)", "The longest hold time for the wakelock. This allows finding cases where wakelocks are held for too long, but are eventually released. (In contrast, active_since is more useful in the held-forever case.)", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Last change", "?", TableGen.FLAG_ALIGN_RIGHT);
        tg.begin();

        for (KernelWakelock lock : mLocks) {
            tg.addData(lock.name);
            tg.addData(lock.count);
            tg.addData(lock.expire_count);
            tg.addData(lock.wake_count);
            long activeSinceMs = lock.active_since / 1000000L;
            tg.addData(null, Util.formatTS(activeSinceMs), Util.shadeValue(activeSinceMs), 0);
            long totalMs = lock.total_time / 1000000L;
            tg.addData(null, Util.formatTS(totalMs), Util.shadeValue(totalMs), 0);
            long avgMs = (lock.count == 0) ? 0 : (lock.total_time / lock.count / 1000000L);
            tg.addData(null, Util.formatTS(avgMs), Util.shadeValue(avgMs), 0);
            long sleepMs = lock.sleep_time / 1000000L;
            tg.addData(null, Util.formatTS(sleepMs), Util.shadeValue(sleepMs), 0);
            long maxMs = lock.max_time / 1000000L;
            tg.addData(null, Util.formatTS(maxMs), Util.shadeValue(maxMs), 0);
            tg.addData(lock.last_change);
        }

        tg.end();
    }

    static class KernelWakelock {
        String name;
        long count;
        long expire_count;
        long wake_count;
        long active_since;
        long total_time;
        long sleep_time;
        long max_time;
        long last_change;
    }

}
