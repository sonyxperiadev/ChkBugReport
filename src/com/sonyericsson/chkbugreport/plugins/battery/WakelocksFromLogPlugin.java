/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.SystemLogPlugin;
import com.sonyericsson.chkbugreport.util.Util;

import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WakelocksFromLogPlugin extends Plugin {

    private static final Pattern P_ACQUIRE = Pattern.compile("acquireWakeLock flags=0x(.*) tag=(.*)");
    private static final Pattern P_RELEASE = Pattern.compile("releaseWakeLock flags=0x(.*) tag=(.*)");
    private static final String LOG_TAG = "PowerManagerService";

    private boolean mLoaded;
    private Wakelocks mLocks = new Wakelocks();
    private WakelockEvents mEvents = new WakelockEvents();

    @Override
    public int getPrio() {
        return 90;
    }

    @Override
    public void reset() {
        mLoaded = false;
        mLocks.clear();
        mEvents.clear();
    }

    @Override
    public void load(Module br) {
        LogLines logs = (LogLines) br.getInfo(SystemLogPlugin.INFO_ID_SYSTEMLOG);
        if (logs == null || logs.isEmpty()) {
            // System log missing
            return;
        }
        scanLogAndExtractData(logs);
        createStatistics();
        mLoaded = true;
    }

    private void scanLogAndExtractData(LogLines logs) {
        for (LogLine l : logs) {
            if (!LOG_TAG.equals(l.tag)) continue;
            Matcher m = P_ACQUIRE.matcher(l.msg);
            if (m.matches()) {
                int flags = Integer.parseInt(m.group(1), 16);
                String name = m.group(2);
                Wakelock lock = getWakelock(name);
                WakelockEvent event = new WakelockEvent(lock, true, flags, l.ts);
                mEvents.add(event);
                lock.nesting++;
                lock.maxNested = Math.max(lock.maxNested, lock.nesting);
                lock.cachedFlags = flags;
                continue;
            }
            m = P_RELEASE.matcher(l.msg);
            if (m.matches()) {
                int flags = Integer.parseInt(m.group(1), 16);
                String name = m.group(2);
                Wakelock lock = getWakelock(name);
                WakelockEvent event = new WakelockEvent(lock, false, flags, l.ts);
                mEvents.add(event);
                if (lock.nesting > 0) {
                    lock.nesting--;
                } else {
                    // Need to generate fake acquire event
                    event = new WakelockEvent(lock, true, flags, logs.firstElement().ts);
                    mEvents.insertElementAt(event, 0);
                }
                continue;
            }
        }
        // Need to make sure all wakelocks have release events
        for (Wakelock lock : mLocks.values()) {
            while (lock.nesting > 0) {
                int flags = lock.cachedFlags;
                long ts = logs.lastElement().ts;
                WakelockEvent event = new WakelockEvent(lock, false, flags, ts);
                mEvents.add(event);
                lock.nesting--;
            }
        }
    }

    private void createStatistics() {
        Vector<Wakelock> active = new Vector<WakelocksFromLogPlugin.Wakelock>();
        Wakelock loneRanger = null;
        long loneAcqTime = 0;
        for (WakelockEvent event : mEvents) {
            final Wakelock lock = event.lock;
            if (event.acquire) {
                if (0 == lock.nesting++) {
                    if (active.isEmpty()) {
                        loneRanger = lock;
                        loneAcqTime = event.ts;
                    } else if (loneRanger != null) {
                        loneRanger.loneTime += (event.ts - loneAcqTime);
                        loneRanger.loneCount++;
                    }
                    active.add(lock);
                    lock.count++;
                    lock.acquireTime = event.ts;
                }
            } else {
                if (0 == --lock.nesting) {
                    active.remove(lock);
                    lock.totalTime += event.ts - lock.acquireTime;
                    if (active.size() == 1) {
                        loneRanger = active.firstElement();
                        loneAcqTime = event.ts;
                    } else if (active.isEmpty() && loneRanger != null) {
                        loneRanger.loneTime += (event.ts - loneAcqTime);
                        loneRanger.loneCount++;
                    }
                }
            }
        }
    }

    private Wakelock getWakelock(String name) {
        Wakelock ret = mLocks.get(name);
        if (ret == null) {
            ret = new Wakelock(name);
            mLocks.put(name, ret);
        }
        return ret;
    }

    @Override
    public void generate(Module rep) {
        if (!mLoaded || mEvents.isEmpty()) return;
        BugReportModule br = (BugReportModule) rep;

        Chapter ch = br.findOrCreateChapter("Battery info/Wakelocks from log");

        addHelp(ch);
        createStatsTable(rep, ch);
    }

    private void addHelp(Chapter ch) {
        new Para(ch).add("This information is based on the extracted PowerManagerService logs. "+
                "Note that the information is limited, so for example it is not known if the wakelocks are " +
                "reference counted or not. We assume they are, since that's the default value. " +
                "However if you see strange nesting values for certain wakelocks, then they are probably " +
                "not reference counted, so the extracted values cannot be trusted.");
    }

    private void createStatsTable(Module rep, Chapter ch) {
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(rep, "wakelocks_from_logs");
        tg.setTableName(rep, "wakelocks_from_logs");
        tg.addColumn("Name", "The name of the wakelock", 0, "name varchar");
        tg.addColumn("Count", "The number of times that the wakelock has been acquired (not counting nested wakelocks).", Table.FLAG_ALIGN_RIGHT, "count int");
        tg.addColumn("Total time (ms)", "The total amount of times, in milliseconds, this wakelock was taken.", Table.FLAG_ALIGN_RIGHT, "total_time_ms int");
        tg.addColumn("Lone", "The number of times this wakelock was the only wakelock taken.", Table.FLAG_ALIGN_RIGHT, "lone_count int");
        tg.addColumn("Lone time (ms)", "The total amount of times, in milliseconds, this wakelock was the only wakelock taken.", Table.FLAG_ALIGN_RIGHT, "lone_time_ms int");
        tg.addColumn("Max nesting", "The maximum nested level of this wakelock.", Table.FLAG_ALIGN_RIGHT, "max_nesting int");
        tg.begin();

        for (Wakelock lock : mLocks.values()) {
            tg.addData(lock.name);
            tg.addData(lock.count);
            tg.addData(Util.formatTS(lock.totalTime), new ShadedValue(lock.totalTime));
            tg.addData(lock.loneCount);
            tg.addData(Util.formatTS(lock.loneTime), new ShadedValue(lock.loneTime));
            tg.addData(lock.maxNested);
        }

        tg.end();
    }

    public static class Wakelock {
        /** The name of the wakelock */
        public String name;
        /** The number of times this wakelock was taken, not counting nested takes */
        public int count;
        /** The total amount of times, in milliseconds, this wakelock was taken */
        public long totalTime;
        /** The maximum nested level of this wakelock */
        public int maxNested;
        /** The number of times this wakelock was the only wakelock taken */
        public int loneCount;
        /** The total amount of times, in milliseconds, this wakelock was the only wakelock taken */
        public long loneTime;

        /* package */ int cachedFlags;
        /* package */ int nesting;
        /* package */ long acquireTime;

        public Wakelock(String name) {
            this.name = name;
        }
    }

    @SuppressWarnings("serial")
    public static class Wakelocks extends HashMap<String, Wakelock> {
    }

    public static class WakelockEvent {
        public Wakelock lock;
        public int flags;
        public boolean acquire;
        public long ts;

        public WakelockEvent(Wakelock lock, boolean acquire, int flags, long ts) {
            this.lock = lock;
            this.acquire = acquire;
            this.flags = flags;
            this.ts = ts;
        }
    }

    @SuppressWarnings("serial")
    public static class WakelockEvents extends Vector<WakelockEvent> {
    }

}
