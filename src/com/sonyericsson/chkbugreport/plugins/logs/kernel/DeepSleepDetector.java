package com.sonyericsson.chkbugreport.plugins.logs.kernel;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepSleepDetector {

    private static final Pattern PATTERN_ENTRY = Pattern.compile("PM: suspend entry ([0-9-]+ [0-9:.]+) .*");
    private static final Pattern PATTERN_EXIT = Pattern.compile("suspend: exit suspend, ret = 0 \\(([0-9-]+ [0-9:.]+) .*\\)");

    private LogData mLogData;
    private BugReportModule mMod;
    private KernelLogLines mLog;
    private long mGmtOffset;

    public DeepSleepDetector(LogData logData, BugReportModule mod, KernelLogLines log) {
        mLogData = logData;
        mMod = mod;
        mLog = log;
        mGmtOffset = mod.getContext().getGmtOffset() * Util.HOUR_MS;
    }

    public void run() {
        int cnt = mLog.size();
        long lastRealTs = -1, lastTs = -1, diff = -1;
        int sleepCount = 0;
        long sleepTime = 0;
        DeepSleeps datas = new DeepSleeps();
        for (int i = 0; i < cnt; i++) {
            KernelLogLine l = mLog.get(i);

            // Use the "PM: suspend entry" lines to sync the time
            Matcher m = PATTERN_ENTRY.matcher(l.mMsg);
            if (m.matches()) {
                // We use this log just to sync our internal state
                long rt = parseRT(m.group(1));
                diff = rt - l.ts;
                lastRealTs = rt;
                lastTs = l.ts;
                continue;
            }

            // Use the "suspend: exit suspend, ret = 0" lines to detect sleep time
            m = PATTERN_EXIT.matcher(l.mMsg);
            if (!m.matches()) continue;
            long rt = parseRT(m.group(1));
            long curDiff = rt - l.ts;
            if (lastRealTs == -1) {
                diff = curDiff;
            } else {
                long sleep = curDiff - diff;
                // Sleep detected, annotate code
                sleepCount++;
                sleepTime += sleep;
                diff = curDiff;
                l.addMarker(null, null, "Slept " + Util.formatTS(sleep), null);
                DeepSleep data = new DeepSleep(lastRealTs, lastTs, rt, l.ts);
                datas.add(data);
            }
            lastRealTs = rt;
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

    private long parseRT(String ts) {
        long mon = Long.parseLong(ts.substring(5, 7));
        long day = Long.parseLong(ts.substring(8, 10));
        long h = Long.parseLong(ts.substring(11, 13));
        long m = Long.parseLong(ts.substring(14, 16));
        long s = Long.parseLong(ts.substring(17, 19));
        long ms = Long.parseLong(ts.substring(20, 23));
        long ret = (((((mon * 31) + day) * 24 + h) * 60 + m) * 60 + s) * 1000 + ms;
        ret += mGmtOffset;
        return ret;
    }

}
