package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WakeLock {
    String mUID;
    String mName;
    int mCount;
    long mDurationMs;

    private final Pattern pPWL = Pattern.compile("Wake lock (.*?) (.*): (.*?) \\((.*?) times\\).*");


    public int getUID() {
        return Util.parseUid(mUID);
    }

    public String getUIDString() {
        return mUID;
    }

    public String getName() {
        return mName;
    }

    public int getCount() {
        return mCount;
    }

    public long getDurationMs() {
        return mDurationMs;
    }

    //Create waitlock from log line
    public WakeLock(String s) {
        Matcher m = pPWL.matcher(s);
        if (m.find()) {
            mUID = m.group(1);
            mName = m.group(2);
            String sTime = m.group(3);
            String sCount = m.group(4);
            mDurationMs = Util.parseRelativeTimestamp(sTime.replace(" ", ""));
            mCount = Integer.parseInt(sCount);
        } else {
            System.err.println("WL: Could not parse line: " + s);
        }
    }
}