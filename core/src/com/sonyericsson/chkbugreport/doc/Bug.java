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
package com.sonyericsson.chkbugreport.doc;


import java.util.Comparator;
import java.util.HashMap;

public class Bug extends DocNode {

    public enum Type {
        PHONE_ERR("bug-phone-err"),
        PHONE_WARN("bug-phone-warn"),
        TOOL_ERR("bug-tool-err"),
        TOOL_WARN("bug-tool-warn");

        String mCss;

        Type(String css) {
            mCss = css;
        }
    }

    /* These priorities are used as IDs as well, so they must be unique also */
    public static final int PRIO_ALERT_KERNEL_LOG = 130;
    public static final int PRIO_NATIVE_CRASH = 120;
    public static final int PRIO_JAVA_CRASH_EVENT_LOG = 111;
    public static final int PRIO_JAVA_CRASH_SYSTEM_LOG = 110;
    public static final int PRIO_ANR_EVENT_LOG = 102;
    public static final int PRIO_ANR_SYSTEM_LOG = 101;
    public static final int PRIO_ANR_MONKEY = 100;
    public static final int PRIO_DEADLOCK = 95;
    public static final int PRIO_FATAL_LOG = 92;
    public static final int PRIO_MAIN_VIOLATION = 90;
    public static final int PRIO_KPI = 75;
    public static final int PRIO_STRICTMODE = 50;
    public static final int PRIO_SF_NO_BUFF = 45;
    public static final int PRIO_HPROF = 30;
    public static final int PRIO_JAVA_EXCEPTION_SYSTEM_LOG = 20;
    public static final int PRIO_POWER_CONSUMPTION = 15;
    public static final int PRIO_WRONG_WINDOW_ORDER = 11;
    public static final int PRIO_MULTIPLE_WINDOWS = 10;
    public static final int PRIO_LOG_TIMEJUMP = 6;
    public static final int PRIO_INCORRECT_LOG_ORDER = 5;
    public static final int PRIO_LOG_TIMEWINDOW = 4;

    /* These are used in another report type, so they must be unique only within this group */
    public static final int PRIO_TRACEVIEW_DELAYED_DRAW = 50;
    public static final int PRIO_TRACEVIEW_SLOW_METHOD = 30;

    public static final String ATTR_FIRST_LINE      = "firstLine";
    public static final String ATTR_LAST_LINE       = "lastLine";
    public static final String ATTR_LOG_INFO_ID     = "logInfoId";
    public static final String ATTR_PACKAGE         = "package";
    public static final String ATTR_PID             = "pid";
    public static final String ATTR_REASON          = "reason";

    private static BugComparator mComparator;

    private String mName;
    private int mPrio;
    private long mTimeStamp;
    private Type mType;
    private Icon mIcon;
    private HashMap<String, Object> mAttrs = new HashMap<String, Object>();

    public Bug(Type type, int prio, long timeStamp, String name) {
        mType = type;
        mName = name;
        mIcon = new Icon(Icon.TYPE_SMALL, mType.mCss);
        mPrio = prio;
        mTimeStamp = timeStamp;
        add(new InternalRef());
    }

    public String getName() {
        return mName;
    }

    public Icon getIcon() {
        return mIcon;
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
