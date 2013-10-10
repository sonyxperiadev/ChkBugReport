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

import java.util.Vector;

public class Task {

    private String mName;
    private int mId;
    private Vector<Activity> mActivities = new Vector<Activity>();

    public Task(String taskName, int taskId) {
        mName = taskName;
        mId = taskId;
    }

    public String getName() {
        return mName;
    }

    public int getId() {
        return mId;
    }

    public void add(Activity act) {
        mActivities.add(act);
    }

    public int getActivityCount() {
        return mActivities.size();
    }

    public Activity getActivity(int idx) {
        return mActivities.get(idx);
    }

}
