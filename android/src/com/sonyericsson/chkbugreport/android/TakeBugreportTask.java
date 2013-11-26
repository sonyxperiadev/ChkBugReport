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

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TakeBugreportTask extends AsyncTask<Void, Void, Void> {

    private static final String ROOT_DIR = "/sdcard/bugreports";
    private Service mContext;
    private String mMode;
    private String mFileName;
    private FileOutputStream mOut;
    private PrintStream mPs;

    public TakeBugreportTask(Service service, String mode) {
        mContext = service;
        mMode = mode;
    }

    @Override
    protected void onPreExecute() {
        mFileName = findFileName();
        if (mFileName == null) {
            mContext.stopSelf();
            return;
        }
        Builder b = new Notification.Builder(mContext);
        b.setContentTitle("Saving bugreport...");
        b.setContentText(mFileName);
        b.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher));
        b.setSmallIcon(R.drawable.ic_launcher);
        Intent intent = new Intent(mContext, StatusActivity.class);
        b.setContentIntent(PendingIntent.getActivity(mContext, 1, intent, 0));
        b.setOngoing(true);
        Notification notification = b.build();
        mContext.startForeground(1, notification);
    }

    private String findFileName() {
        File dir = new File(ROOT_DIR);
        if (!dir.isDirectory()) {
            dir.mkdirs();
            if (!dir.isDirectory()) {
                Toast.makeText(mContext, "Cannot create directory: " + ROOT_DIR, Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        long ts = System.currentTimeMillis();
        int idx = 0;
        while (true) {
            String fileName = String.format("%s/bugreport_%d_%d.txt", ROOT_DIR, ts, idx);
            File f = new File(fileName);
            if (!f.exists()) {
                return fileName;
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (mFileName == null) {
            return null;
        }
        try {
            mOut = new FileOutputStream(new File(mFileName));
            mPs = new PrintStream(mOut);

            if (TakeBugreportService.MODE_MINI.equals(mMode)) {
                runMini();
            } else {
                exec("bugreport");
            }

            mPs.close();
            mOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void runMini() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat();
        String ts = sdf.format(date);
        mPs.println("========================================================");
        mPs.println("== dumpstate: " + ts + " (mini)");
        mPs.println("========================================================");
        mPs.println();
        mPs.println("Build: " + Build.VERSION.CODENAME);
        mPs.println("Build fingerprint: " + Build.FINGERPRINT);
        mPs.println("Bootloader: " + Build.BOOTLOADER);
        mPs.println("Radio: " + Build.RADIO);
        mPs.println("Hardware: " + Build.HARDWARE);
        mPs.println();
        mPs.println("------ UPTIME (uptime) ------");
        exec("uptime");
        mPs.println("------ MEMORY INFO (/proc/meminfo) ------");
        cat("/proc/meminfo");
        mPs.println("------ CPU INFO (top -n 1 -d 1 -m 30 -t) ------");
        exec("top", "-n", "1", "-d", "1", "-m", "30", "-t");
        mPs.println("------ SYSTEM LOG (logcat -v threadtime -d *:v) ------");
        exec("logcat", "-v", "threadtime", "-d", "*:v");
        mPs.println("------ EVENT LOG (logcat -b events -v threadtime -d *:v) ------");
        exec("logcat", "-b", "events", "-v", "threadtime", "-d", "*:v");
        mPs.println("------ RADIO LOG (logcat -b radio -v threadtime -d *:v) ------");
        exec("logcat", "-b", "radio", "-v", "threadtime", "-d", "*:v");
        mPs.println("------ VM TRACES AT LAST ANR (/data/anr/traces.txt) ------");
        cat("/data/anr/traces.txt");
    }

    private void cat(String fileName) {
        try {
            dump(new FileInputStream(new File(fileName)));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void exec(String ...cmd) {
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            dump(proc.getInputStream());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void dump(InputStream is) throws IOException {
        byte buff[] = new byte[0x10000];
        while (true) {
            int read = is.read(buff, 0, buff.length);
            if (read <= 0) {
                break;
            }
            mOut.write(buff, 0, read);
        }
        is.close();
    }

    @Override
    protected void onPostExecute(Void result) {
        if (mFileName == null) {
            return;
        }
        mContext.stopForeground(true);
        Intent intent = new Intent(mContext, StatusActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mFileName)), "text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

}

