package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WakeLock {
    String mUID;
    String mName;
    String mType;
    int mCount;
    int mMax = -1;
    int mActual = -1;
    long mDurationMs;
    Boolean mIsRunningLocked = false;
    Boolean mIsRunning = false; //Only valid if mIsRunningLocked is true

    private static final Pattern P_WL_OUTER = Pattern.compile("Wake lock (?:(\\S+) )?(\\S+)(:.*)? realtime");
    private static final Pattern P_WL_INNER = Pattern.compile(": (.*?)(?: (\\D+) )?\\((\\d+) times\\)(?: max=(\\d+))?(?: actual=(\\d+))?(?: \\(running for (\\d+)ms\\))?( \\(running\\))?");

    private String returnEmptyOrValue(String value) {
        return value == null ? "" : value;
    }

    public int getUID() {
        return mUID == null ? -1 : Util.parseUid(mUID);
    }

    public String getUIDString() {
        return returnEmptyOrValue(mUID);
    }

    public String getType() {
        return returnEmptyOrValue(mType);
    }

    public String getName() {
        return returnEmptyOrValue(mName);
    }

    public int getCount() {
        return mCount;
    }

    public long getDurationMs() {
        return mDurationMs;
    }

    public int getMax() {
        return mMax;
    }

    public int getActual() {
        return mActual;
    }

    private void extractInner(String s) {
        if(s != null) {
            Matcher mInner = P_WL_INNER.matcher(s);
            if(mInner.matches()) {
                String sTime = mInner.group(1);
                mType = mInner.group(2);
                String sCount =  mInner.group(3);
                String sMax = mInner.group(4);
                String sActual = mInner.group(5);
                String sRunningTime = mInner.group(6);
                String sRunning = mInner.group(7);
                if(sMax != null) {
                    mMax = Integer.parseInt(sMax);
                }
                if(sActual != null) {
                    mActual = Integer.parseInt(sActual);
                }
                if(sRunningTime != null || sRunning != null) {
                    mIsRunningLocked = true;
                }
                if(sRunning != null) {
                    mIsRunning = true;
                }
                mDurationMs = Util.parseRelativeTimestamp(sTime.replace(" ", ""));
                mCount = Integer.parseInt(sCount);
            } else {
                System.err.println("WL: Could not parse line: " + s);
            }
        }
    }

    //Create WaitLock from log line
    //See:  frameworks/base/core/java/android/os/BatteryStats.java
    //      See dumpLocked All Partial WakeLocks section.
    public WakeLock(String s) {
        Matcher m = P_WL_OUTER.matcher(s);
        if(m.matches()) {
            mUID = m.group(1);
            mName = m.group(2);
            extractInner(m.group(3));
        } else {
            System.err.println("WL: Could not parse line: " + s);
        }
    }

    //Create waitlock with UID
    //See:  frameworks/base/core/java/android/os/BatteryStats.java
    //      See dumpLocked Wake locks by UID section
    public WakeLock(String sUID, String s) {
        mUID = sUID;
        Matcher m = P_WL_OUTER.matcher(s);
        if(m.matches()) {
            mName = m.group(2);
            extractInner(m.group(3));
        } else {
            System.err.println("WL: Could not parse line: " + s);
        }
    }
}