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
