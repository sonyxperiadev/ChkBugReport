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
package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ChapterHelp;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.util.Util;

import java.util.Vector;

/**
 * Note: some information was taken from: http://lwn.net/Articles/400771/
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
    public void reset() {
        mLoaded = false;
        mLocks.clear();
    }

    @Override
    public void load(Module br) {
        Section section = br.findSection(Section.KERNEL_WAKELOCKS);
        if (section == null) {
            br.printErr(3, TAG + "Section not found: " + Section.KERNEL_WAKELOCKS + " (aborting plugin)");
            return;
        }
        String line = section.getLine(0);
        if (line.equals("*** /proc/wakelocks: No such file or directory")) {
            br.printErr(3, TAG + "No data found in section: " + Section.KERNEL_WAKELOCKS + " (aborting plugin)");
            return;
        }
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
            if (lock.name.startsWith("\"") && lock.name.endsWith("\"")) {
                lock.name = lock.name.substring(1, lock.name.length() - 1);
            }
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
    public void generate(Module rep) {
        if (!mLoaded) return;
        BugReportModule br = (BugReportModule) rep;

        Chapter ch = br.findOrCreateChapter("Battery info/Kernel wakelocks");
        addHelp(ch);

        long uptime = br.getUptime();
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(rep, "kernel_wakelocks");
        tg.setTableName(rep, "kernel_wakelocks");
        tg.addColumn("Name", "The name of the wakelock as provided by the driver", 0, "name varchar");
        tg.addColumn("Count", "The number of times that the wakelock has been acquired.", Table.FLAG_ALIGN_RIGHT, "count int");
        tg.addColumn("Expire count", "The number of times that the wakelock has timed out. This indicates that some application has an input device open, but is not reading from it, which is a bug.", Table.FLAG_ALIGN_RIGHT, "expire_count int");
        tg.addColumn("Wake count", "The number of times that the wakelock was the first to be acquired in the resume path.", Table.FLAG_ALIGN_RIGHT, "wake_count int");
        tg.addColumn("Active since (ms)", "Tracks how long a wakelock has been held since it was last acquired, or zero if it is not currently held." , Table.FLAG_ALIGN_RIGHT, "active_since_ms int");
        tg.addColumn("Total time (ms)", "Accumulates the total amount of time that the corresponding wakelock has been held.", Table.FLAG_ALIGN_RIGHT, "total_time_ms int");
        if (uptime > 0) {
            tg.addColumn("Total time (%)", "Total time as percentage of uptime", Table.FLAG_ALIGN_RIGHT, "total_time_p int");
        }
        tg.addColumn("Average time (ms)", "Total time divided by Count.", Table.FLAG_ALIGN_RIGHT, "average_time_ms int");
        tg.addColumn("Sleep time (ms)", "The total time that the wakelock was held while the display was powered off.", Table.FLAG_ALIGN_RIGHT, "sleep_time_ms int");
        tg.addColumn("Max time (ms)", "The longest hold time for the wakelock. This allows finding cases where wakelocks are held for too long, but are eventually released. (In contrast, active_since is more useful in the held-forever case.)", Table.FLAG_ALIGN_RIGHT, "max_time_ms int");
        tg.addColumn("Last change", "?", Table.FLAG_ALIGN_RIGHT, "last_change int");
        tg.begin();

        for (KernelWakelock lock : mLocks) {
            tg.addData(lock.name);
            tg.addData(lock.count);
            tg.addData(lock.expire_count);
            tg.addData(lock.wake_count);
            long activeSinceMs = lock.active_since / 1000000L;
            tg.addData(Util.formatTS(activeSinceMs), new ShadedValue(activeSinceMs));
            long totalMs = lock.total_time / 1000000L;
            tg.addData(Util.formatTS(totalMs), new ShadedValue(totalMs));
            if (uptime > 0) {
                tg.addData(lock.total_time / 10000000L / uptime);
            }
            long avgMs = (lock.count == 0) ? 0 : (lock.total_time / lock.count / 1000000L);
            tg.addData(Util.formatTS(avgMs), new ShadedValue(avgMs));
            long sleepMs = lock.sleep_time / 1000000L;
            tg.addData(Util.formatTS(sleepMs), new ShadedValue(sleepMs));
            long maxMs = lock.max_time / 1000000L;
            tg.addData(Util.formatTS(maxMs), new ShadedValue(maxMs));
            tg.addData(lock.last_change);
        }

        tg.end();
    }

    private void addHelp(Chapter ch) {
        new ChapterHelp(ch)
            .addText("Statistics based on kernel wakelocks.")
            .addHint("The data is collected from boot!")
            .addHint("Wakelocks created by the java code are aggregated into the PowerManagerService kernel wakelock!")
            .addSeeAlso("http://developer.android.com/reference/android/os/PowerManager.WakeLock.html", "Android documentation regarding WakeLock");
    }

    private static class KernelWakelock {
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
