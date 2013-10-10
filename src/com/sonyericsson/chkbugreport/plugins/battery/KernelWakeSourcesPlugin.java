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

public class KernelWakeSourcesPlugin extends Plugin {

    private static final String TAG = "[KernelWakeSourcesPlugin]";

    private static final String COLUMNS[] = {
        "name",
        "active_count",
        "event_count",
        "wakeup_count",
        "expire_count",
        "active_since",
        "total_time",
        "max_time",
        "last_change",
        "prevent_suspend_time",
    };

    private boolean mLoaded;
    private Vector<KernelWakeSource> mLocks = new Vector<KernelWakeSource>();

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
        Section section = br.findSection(Section.KERNEL_WAKE_SOURCES);
        if (section == null) {
            br.printErr(3, TAG + "Section not found: " + Section.KERNEL_WAKE_SOURCES + " (aborting plugin)");
            return;
        }
        String line = section.getLine(0);
        String columns[] = line.split("\t+");
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
            br.printErr(3, TAG + "Data format changed in section " + Section.KERNEL_WAKE_SOURCES + " (aborting plugin)");
            return;
        }
        int cnt = section.getLineCount();
        for (int i = 1; i < cnt; i++) {
            line = section.getLine(i);
            columns = line.split("\t+");
            if (columns.length < COLUMNS.length) {
                continue;
            }
            KernelWakeSource lock = new KernelWakeSource();
            lock.name = columns[0];
            if (lock.name.startsWith("\"") && lock.name.endsWith("\"")) {
                lock.name = lock.name.substring(1, lock.name.length() - 1);
            }
            lock.active_count = Long.parseLong(columns[1]);
            lock.event_count= Long.parseLong(columns[2]);
            lock.wakeup_count = Long.parseLong(columns[3]);
            lock.expire_count = Long.parseLong(columns[4]);
            lock.active_since = Long.parseLong(columns[5]);
            lock.total_time = Long.parseLong(columns[6]);
            lock.max_time = Long.parseLong(columns[7]);
            lock.last_change = Long.parseLong(columns[8]);
            lock.prevent_suspend_time = Long.parseLong(columns[9]);
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
        tg.setCSVOutput(rep, "kernel_wakesources");
        tg.setTableName(rep, "kernel_wakesources");
        tg.addColumn("Name", null, 0, "name varchar");
        tg.addColumn("Active count", null, Table.FLAG_ALIGN_RIGHT, "active_count int");
        tg.addColumn("Event count", null, Table.FLAG_ALIGN_RIGHT, "event_count int");
        tg.addColumn("Wakeup count", null, Table.FLAG_ALIGN_RIGHT, "wakeup_count int");
        tg.addColumn("Expire count", null, Table.FLAG_ALIGN_RIGHT, "expire_count int");
        tg.addColumn("Active since (ms)", null, Table.FLAG_ALIGN_RIGHT, "active_since_ms int");
        tg.addColumn("Total time (ms)", null, Table.FLAG_ALIGN_RIGHT, "total_time_ms int");
        if (uptime > 0) {
            tg.addColumn("Total time (%)", null, Table.FLAG_ALIGN_RIGHT, "total_time_p int");
        }
        tg.addColumn("Max time (ms)", null, Table.FLAG_ALIGN_RIGHT, "max_time_ms int");
        tg.addColumn("Last change", null, Table.FLAG_ALIGN_RIGHT, "last_change int");
        tg.addColumn("Prevent Suspend Time", null, Table.FLAG_ALIGN_RIGHT, "prevent_suspend_time int");
        tg.begin();

        for (KernelWakeSource lock : mLocks) {
            tg.addData(lock.name);
            tg.addData(lock.active_count);
            tg.addData(lock.event_count);
            tg.addData(lock.wakeup_count);
            tg.addData(lock.expire_count);
            long activeSinceMs = lock.active_since / 1000000L;
            tg.addData(Util.formatTS(activeSinceMs), new ShadedValue(activeSinceMs));
            long totalMs = lock.total_time / 1000000L;
            tg.addData(Util.formatTS(totalMs), new ShadedValue(totalMs));
            if (uptime > 0) {
                tg.addData(lock.total_time / 10000000L / uptime);
            }
            long maxMs = lock.max_time / 1000000L;
            tg.addData(Util.formatTS(maxMs), new ShadedValue(maxMs));
            tg.addData(lock.last_change);
            long pstMs = lock.prevent_suspend_time / 1000000L;
            tg.addData(Util.formatTS(pstMs), new ShadedValue(pstMs));
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

    private static class KernelWakeSource {
        String name;
        long active_count;
        long event_count;
        long wakeup_count;
        long expire_count;
        long active_since;
        long total_time;
        long max_time;
        long last_change;
        long prevent_suspend_time;
    }

}
