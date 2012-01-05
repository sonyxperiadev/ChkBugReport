/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport;

import java.util.Comparator;
import java.util.HashMap;

public class Bug extends Lines {

    /* These priorities are used as IDs as well, so they must be unique also */
    public static final int PRIO_NATIVE_CRASH = 120;
    public static final int PRIO_JAVA_CRASH_EVENT_LOG = 111;
    public static final int PRIO_JAVA_CRASH_SYSTEM_LOG = 110;
    public static final int PRIO_ANR_EVENT_LOG = 102;
    public static final int PRIO_ANR_SYSTEM_LOG = 101;
    public static final int PRIO_ANR_MONKEY = 100;
    public static final int PRIO_DEADLOCK = 95;
    public static final int PRIO_MAIN_VIOLATION = 90;
    public static final int PRIO_KPI = 75;
    public static final int PRIO_STRICTMODE = 50;
    public static final int PRIO_SF_NO_BUFF = 45;
    public static final int PRIO_HPROF = 30;
    public static final int PRIO_JAVA_EXCEPTION_SYSTEM_LOG = 20;
    public static final int PRIO_POWER_CONSUMPTION = 15;
    public static final int PRIO_MULTIPLE_WINDOWS = 10;

    /* These are used in another report type, so they must be unique only within this group */
    public static final int PRIO_TRACEVIEW_DELAYED_DRAW = 50;
    public static final int PRIO_TRACEVIEW_SLOW_METHOD = 30;

    private static BugComparator mComparator;

    private int mPrio;
    private long mTimeStamp;

    private HashMap<String, Object> mAttrs = new HashMap<String, Object>();

    public Bug(int prio, long timeStamp, String name) {
        super(name);
        mPrio = prio;
        mTimeStamp = timeStamp;
    }

    public int getPrio() {
        return mPrio;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public void setAttr(String key, Object value) {
        mAttrs.put(key, value);
    }

    public Object getAttr(String key) {
        return mAttrs.get(key);
    }

    public static Comparator<? super Bug> getComparator() {
        if (mComparator == null) {
            mComparator = new BugComparator();
        }
        return mComparator;
    }

    static class BugComparator implements Comparator<Bug> {

        @Override
        public int compare(Bug o1, Bug o2) {
            return o2.mPrio - o1.mPrio;
        }

    }

}
