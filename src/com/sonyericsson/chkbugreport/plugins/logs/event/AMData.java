/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.logs.event;

/**
 * A data representing an AM (ActivityManager) event
 */
/* package */ class AMData {

    public static final int ON_CREATE = 1;
    public static final int ON_DESTROY = 2;
    public static final int ON_PAUSE = 3;
    public static final int ON_RESTART = 4;
    public static final int ON_RESUME = 5;
    public static final int SCHEDULE_SERVICE_RESTART = 6;
    public static final int PROC_KILL = 7;
    public static final int PROC_DIED = 8;
    public static final int PROC_START = 9;

    public static final int PROC = 0;
    public static final int SERVICE = 1;
    public static final int ACTIVITY = 2;

    private int mType;
    private int mAction;
    private int mPid;
    private String mComponent;
    private long mTS;
    private String mExtra;

    public AMData(int type, int action, int pid, String component, long ts) {
        mType = type;
        mAction = action;
        mPid = pid;
        mComponent = component;
        mTS = ts;
    }

    public int getType() {
        return mType;
    }

    public int getPid() {
        return mPid;
    }

    public int getAction() {
        return mAction;
    }

    public String getComponent() {
        return mComponent;
    }

    public long getTS() {
        return mTS;
    }

    public void setPid(int pid) {
        mPid = pid;
    }

    public void setComponent(String component) {
        mComponent = component;
    }

    public void setExtra(String string) {
        mExtra = string;
    }

    public String getExtra() {
        return mExtra;
    }

}
