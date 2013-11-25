package com.sonyericsson.chkbugreport.android;


import com.sonyericsson.chkbugreport.AnalyzeTask;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

public class AnalyzerService extends Service {

    private ChkBugReportApp mApp;

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = (ChkBugReportApp)getApplication();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String fileName = intent.getData().getSchemeSpecificPart();
        new AnalyzeTask(this, mApp, fileName).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return START_STICKY;
    }

}
