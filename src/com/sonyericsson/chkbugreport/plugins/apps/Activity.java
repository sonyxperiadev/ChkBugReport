/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.apps;

public class Activity {

    private String mName;
    private int mPid;
    private Task mTask;
    private View mViews;

    public Activity(String actName, int pid, Task task) {
        mName = actName;
        mPid = pid;
        mTask = task;
    }

    /* package */ void setViewHierarchy(View view) {
        mViews = view;
    }

    public String getName() {
        return mName;
    }

    public int getPid() {
        return mPid;
    }

    public Task getTask() {
        return mTask;
    }

    public View getViewHierarchy() {
        return mViews;
    }

}
