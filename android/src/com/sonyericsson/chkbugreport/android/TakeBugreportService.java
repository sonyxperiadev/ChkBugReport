package com.sonyericsson.chkbugreport.android;


import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

public class TakeBugreportService extends Service {

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_FULL = "full";
    public static final String MODE_MINI = "mini";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String mode = intent.getExtras().getString(EXTRA_MODE);
        new TakeBugreportTask(this, mode).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        return START_NOT_STICKY;
    }

}
