package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;

import java.util.Vector;

// TODO: not finished yet
public class PMStats {

    private KernelLogPlugin mPlugin;
    private BugReport mBr;
    private Vector<SuspendAttempt> mStats = new Vector<SuspendAttempt>();
    private int mFailedCount;
    private int mSuccessCount;

    public PMStats(KernelLogPlugin plugin, BugReport br) {
        mPlugin = plugin;
        mBr = br;
    }

    public void load() {
        int cnt = mPlugin.getLineCount();
        SuspendAttempt cur = null;
        for (int i = 0; i < cnt; i++) {
            KernelLogLine line = mPlugin.getLine(i);
            String msg = line.mMsg;

            if (cur != null) {
                if (msg.startsWith("active wake lock ")) {
                    String name = msg.substring(17);
                    int idx = name.indexOf(',');
                    if (idx >= 0) {
                        name = name.substring(0, idx);
                    }
                    cur.addWakelock(name);
                    continue;
                } else {
                    if (cur.state == SuspendAttempt.STATE_FAILED) {
                        mStats.add(cur);
                        cur = null;
                        mFailedCount++;
                        continue;
                    }
                }
            }

            if (msg.startsWith("Freezing user space processes ...")) {
                cur = new SuspendAttempt();
            } else if (msg.startsWith("suspend: exit suspend")) {
                if (cur != null) {
                    cur.state = SuspendAttempt.STATE_FAILED;
                }
            } else if (msg.startsWith("Disabling non-boot CPUs")) {
                if (cur != null) {
                    cur.state = SuspendAttempt.STATE_SUCCEEDED;
                    mStats.add(cur);
                    cur = null;
                    mSuccessCount++;
                }
            }
        }
    }

    public void generate(BugReport br, Chapter mainCh) {
        // TODO Auto-generated method stub
    }


}
