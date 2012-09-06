package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ShadedValue;

public class DeepSleepDetector {

    private static final long THRESHHOLD = 500; // Half a second

    private LogData mLogData;
    private BugReportModule mMod;
    private KernelLogLines mLog;

    public DeepSleepDetector(LogData logData, BugReportModule mod, KernelLogLines log) {
        mLogData = logData;
        mMod = mod;
        mLog = log;
    }

    public void run() {
        int cnt = mLog.size();
        long lastRealTs = -1, lastTs = -1, diff = -1;
        int sleepCount = 0;
        long sleepTime = 0;
        DeepSleeps datas = new DeepSleeps();
        for (int i = 0; i < cnt; i++) {
            KernelLogLine l = mLog.get(i);
            if (l.getRealTs() < 0) continue;
            long curDiff = l.getRealTs() - l.ts;
            if (lastRealTs == -1) {
                diff = curDiff;
            } else {
                long sleep = curDiff - diff;
                if (sleep >= THRESHHOLD) {
                    // Sleep detected, annotate code
                    sleepCount++;
                    sleepTime += sleep;
                    diff = curDiff;
                    l.addMarker(null, null, "Slept " + Util.formatTS(sleep), null);
                    DeepSleep data = new DeepSleep(lastRealTs, lastTs, l.getRealTs(), l.ts);
                    datas.add(data);
                }
            }
            lastRealTs = l.getRealTs();
            lastTs = l.ts;
        }

        if (sleepCount > 0) {
            Chapter ch = new Chapter(mMod, "Deep sleep stats");
            mLogData.addChapter(ch);
            mMod.addInfo(DeepSleeps.INFO_ID, datas);

            long awakeTime = mLog.get(cnt - 1).ts - mLog.get(0).ts;
            long upTime = sleepTime + awakeTime;
            new Block(ch).add("Number of sleeps: " + sleepCount);
            new Block(ch)
                .add("Sleep time: ")
                .add(new ShadedValue(sleepTime))
                .add("ms = " + Util.formatTS(sleepTime))
                .add(" = ~" + (sleepTime * 100 / upTime) + "%");
            new Block(ch).add("Average sleep time: ").add(new ShadedValue(sleepTime / sleepCount)).add("ms");
            new Block(ch)
            .add("Awake time: ")
            .add(new ShadedValue(awakeTime))
            .add("ms = " + Util.formatTS(awakeTime))
            .add(" = ~" + (awakeTime * 100 / upTime) + "%");
        }

    }

}
