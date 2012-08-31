package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.util.TableGen;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

public class PMStats {

    private KernelLogPlugin mPlugin;
    private Vector<SuspendAttempt> mStats = new Vector<SuspendAttempt>();
    private HashMap<String, SuspendBlockerStat> mBlockers = new HashMap<String, SuspendBlockerStat>();
    private int mFailedCount;
    private int mSuccessCount;
    private HashMap<String, Integer> mWakeups = new HashMap<String, Integer>();

    public PMStats(KernelLogPlugin plugin, BugReport br) {
        mPlugin = plugin;
    }

    public void load() {
        int cnt = mPlugin.getLineCount();
        SuspendAttempt cur = null;
        for (int i = 0; i < cnt; i++) {
            KernelLogLine line = mPlugin.getLine(i);
            String msg = line.mMsg;

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
                    cur.log.addLine(line.mLine);
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
                    cur.log.addLine(line.mLine);
                }
            } else if (msg.startsWith("suspend: exit suspend")) {
                if (cur != null) {
                    cur.state = SuspendAttempt.STATE_FAILED;
                    cur.log.addLine(line.mLine);
                }
            } else if (msg.startsWith("Disabling non-boot CPUs")) {
                if (cur != null) {
                    cur.state = SuspendAttempt.STATE_SUCCEEDED;
                    cur.log.addLine(line.mLine);
                    mStats.add(cur);
                    cur = null;
                    mSuccessCount++;
                }
            } else {
                if (cur != null) {
                    cur.log.addLine(line.mLine);
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

    public void generate(BugReport br, Chapter mainCh) {
        genWakeupStat(br, mainCh);
        genSuspendAttempts(br, mainCh);
    }

    private void genSuspendAttempts(BugReport br, Chapter mainCh) {
        if (mStats.isEmpty()) return;

        Chapter ch = new Chapter(br, "Suspend attempts");
        mainCh.addChapter(ch);
        ch.addLine("<div>Suspend failed " + mFailedCount + " times and succeeded " + mSuccessCount + " times.</div>");

        int total = 0;
        float totalProp = 0.0f;
        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, "kernel_log_suspend_blockers");
        tg.setTableName(br, "kernel_log_suspend_blockers");
        tg.addColumn("Wakelock", "The name of the kernel wake lock.", "wakelock varchar", TableGen.FLAG_NONE);
        tg.addColumn("Count", "The number of times this wake lock was the reason (or one of the reasons) the CPU couldn't suspend.", "count int", TableGen.FLAG_ALIGN_RIGHT);
        tg.addColumn("Proportional Count", "Similar to count, but also counting the number of blocking wake locks.", "prop_count float", TableGen.FLAG_ALIGN_RIGHT);
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

    public void genWakeupStat(BugReport br, Chapter mainCh) {
        if (mWakeups.isEmpty()) return;

        int total = 0;
        Chapter ch = new Chapter(br, "Wakelock wakeups");
        mainCh.addChapter(ch);
        TableGen tg = new TableGen(ch, TableGen.FLAG_SORT);
        tg.setCSVOutput(br, "kernel_log_wakeups");
        tg.setTableName(br, "kernel_log_wakeups");
        tg.addColumn("Wakelock", "The name of the kernel wake lock.", "wakelock varchar", TableGen.FLAG_NONE);
        tg.addColumn("Count", "The number of times the CPU was woken up by this wakelock.", "count int", TableGen.FLAG_ALIGN_RIGHT);
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
