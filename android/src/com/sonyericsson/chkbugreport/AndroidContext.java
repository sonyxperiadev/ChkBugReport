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
package com.sonyericsson.chkbugreport;

import android.util.Log;

public class AndroidContext extends Context implements OutputListener {

    private static final String TAG = "ChkBugReport";
    private AnalyzeTask mTask;

    public AndroidContext(AnalyzeTask analyzeTask) {
        setOutputListener(this);
        mTask = analyzeTask;
    }

    @Override
    public void onPrint(int level, int type, String msg) {
        mTask.onPrint(level, type, msg);
        if (type == TYPE_ERR) {
            Log.e(TAG, "<" + level + "> " + msg);
        } else {
            Log.i(TAG, "<" + level + "> " + msg);
        }
    }
}
