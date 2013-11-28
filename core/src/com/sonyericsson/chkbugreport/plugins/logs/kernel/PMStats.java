/*
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
package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

/* package */ class PMStats {

    private KernelLogData mLog;
    private Vector<SuspendAttempt> mStats = new Vector<SuspendAttempt>();
    private HashMap<String, SuspendBlockerStat> mBlockers = new HashMap<String, SuspendBlockerStat>();
    private int mFailedCount;
    private int mSuccessCount;
    private HashMap<String, Integer> mWakeups = new HashMap<String, Integer>();
    private String mId;

    public PMStats(KernelLogData log, BugReportModule br) {
        mLog = log;
        mId = log.getId();
    }

    public void load() {
        int cnt = mLog.getLineCount();
        SuspendAttempt cur = null;
        for (int i = 0; i < cnt; i++) {
            LogLine line = mLog.getLine(i);
            String msg = line.msg;

            // Check for wakeups
            if (msg.startsWith("wakeup wake lock: ")) {
                String lock = msg.substring(18);
                Integer count = mWakeups.get(lock);
                if (count == null) {
                    mWakeups.put(lock, 1);
                } else {
                    mWakeups.put(lock, count + 1);
                }
            }

            // Check for suspend attempts
            if (cur != null) {
                if (msg.startsWith("active wake lock ")) {
                    String name = msg.substring(17);
                    int idx = name.indexOf(',');
                    if (idx >= 0) {
                        name = name.substring(0, idx);
                    }
                    cur.addWakelock(name);
                    cur.log.addLine(line.line);
                    continue;
                } else {
                    if (cur.state == SuspendAttempt.STATE_FAILED) {
                        mStats.add(cur);
                        cur = null;
                        mFailedCount++;
                    }
                }
            }

            if (msg.startsWith("Freezing user space processes ...")) {
                if (cur == null) {
                    cur = new SuspendAttempt();
                    cur.log.addLine(line.line);
                }
            } else if (msg.startsWith("suspend: exit suspend")) {
                if (cur != null) {
                    cur.state = SuspendAttempt.STATE_FAILED;
                    cur.log.addLine(line.line);
                }
            } else if (msg.startsWith("Disabling non-boot CPUs")) {
                if (cur != null) {
                    cur.state = SuspendAttempt.STATE_SUCCEEDED;
                    cur.log.addLine(line.line);
                    mStats.add(cur);
                    cur = null;
                    mSuccessCount++;
                    line.addMarker(null, "Suspend", "...zzzZZZZ");
                }
            } else {
                if (cur != null) {
                    cur.log.addLine(line.line);
                }
            }
        }

        for (SuspendAttempt sa : mStats) {
            for (String name : sa.wakelocks) {
                SuspendBlockerStat bs = mBlockers.get(name);
                if (bs == null) {
                    bs = new SuspendBlockerStat();
                    bs.wakelock = name;
                    mBlockers.put(name, bs);
                }
                bs.count++;
                bs.proportionalCount += 1.0f / sa.wakelocks.size();
            }
        }
    }

    public void generate(BugReportModule br, Chapter mainCh) {
        genWakeupStat(br, mainCh);
        genSuspendAttempts(br, mainCh);
    }

    private void genSuspendAttempts(BugReportModule br, Chapter mainCh) {
        if (mStats.isEmpty()) return;

        Chapter ch = new Chapter(br.getContext(), "Suspend attempts");
        mainCh.addChapter(ch);
        new Block(ch).add("Suspend failed " + mFailedCount + " times and succeeded " + mSuccessCount + " times.");

        int total = 0;
        float totalProp = 0.0f;
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, mId + "_log_suspend_blockers");
        tg.setTableName(br, mId + "_log_suspend_blockers");
        tg.addColumn("Wakelock", "The name of the kernel wake lock.", Table.FLAG_NONE, "wakelock varchar");
        tg.addColumn("Count", "The number of times this wake lock was the reason (or one of the reasons) the CPU couldn't suspend.", Table.FLAG_ALIGN_RIGHT, "count int");
        tg.addColumn("Proportional Count", "Similar to count, but also counting the number of blocking wake locks.", Table.FLAG_ALIGN_RIGHT, "prop_count float");
        tg.begin();
        for (SuspendBlockerStat sb : mBlockers.values()) {
            tg.addData(sb.wakelock);
            tg.addData(sb.count);
            tg.addData(String.format("%.2f", sb.proportionalCount));
            total += sb.count;
            totalProp += sb.proportionalCount;
        }
        tg.addSeparator();
        tg.addData("TOTAL");
        tg.addData(total);
        tg.addData(String.format("%.2f", totalProp));
        tg.end();
    }

    public void genWakeupStat(BugReportModule br, Chapter mainCh) {
        if (mWakeups.isEmpty()) return;

        int total = 0;
        Chapter ch = new Chapter(br.getContext(), "Wakelock wakeups");
        mainCh.addChapter(ch);
        Table tg = new Table(Table.FLAG_SORT, ch);
        tg.setCSVOutput(br, mId + "_log_wakeups");
        tg.setTableName(br, mId + "_log_wakeups");
        tg.addColumn("Wakelock", "The name of the kernel wake lock.", Table.FLAG_NONE, "wakelock varchar");
        tg.addColumn("Count", "The number of times the CPU was woken up by this wakelock.", Table.FLAG_ALIGN_RIGHT, "count int");
        tg.begin();
        for (Entry<String, Integer> item : mWakeups.entrySet()) {
            tg.addData(item.getKey());
            tg.addData(item.getValue());
            total += item.getValue();
        }
        tg.addSeparator();
        tg.addData("TOTAL");
        tg.addData(total);
        tg.end();
    }

}
