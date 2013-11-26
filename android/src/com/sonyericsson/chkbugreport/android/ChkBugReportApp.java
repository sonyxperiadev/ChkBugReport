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
package com.sonyericsson.chkbugreport.android;

import com.sonyericsson.chkbugreport.OutputListener;

import android.app.Application;
import android.util.Log;

public class ChkBugReportApp extends Application implements OutputListener {

    private static final String TAG = "ChkBugReport";

    private OutputListener mOutputListener;
    private Object mLogLock = new Object();

    public void setOutputListener(OutputListener listener) {
        synchronized (mLogLock) {
            mOutputListener = listener;
        }
    }

    @Override
    public void onPrint(int level, int type, String msg) {
        synchronized (mLogLock) {
            if (type == TYPE_ERR) {
                Log.e(TAG, "<" + level + "> " + msg);
            } else {
                Log.i(TAG, "<" + level + "> " + msg);
            }
            if (mOutputListener != null) {
                mOutputListener.onPrint(level, type, msg);
            }
        }
    }

}
