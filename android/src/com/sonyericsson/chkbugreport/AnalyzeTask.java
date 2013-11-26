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

import com.sonyericsson.chkbugreport.android.R;
import com.sonyericsson.chkbugreport.android.StatusActivity;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;

public class AnalyzeTask extends AsyncTask<Void, Void, Void> {

    private BugReportModule mMod;
    private Service mContext;
    private OutputListener mListener;
    private String mFileName;
    private Exception mErr;

    public AnalyzeTask(Service service, OutputListener listener, String fileName) {
        mContext = service;
        mListener = listener;
        mFileName = fileName;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Builder b = new Notification.Builder(mContext);
        b.setContentTitle("ChkBugReport working...");
        b.setContentText(mFileName);
        b.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher));
        b.setSmallIcon(R.drawable.ic_launcher);
        Intent intent = new Intent(mContext, StatusActivity.class);
        b.setContentIntent(PendingIntent.getActivity(mContext, 1, intent, 0));
        b.setOngoing(true);
        Notification notification = b.build();
        mContext.startForeground(1, notification);
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            AndroidContext ctx = new AndroidContext(mListener);
            mMod = new BugReportModule(ctx);
            mMod.addFile(mFileName, null, false);
            mMod.generate();
        } catch (Exception e) {
            e.printStackTrace();
            mErr = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mContext.stopForeground(true);
        if (mErr == null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setComponent(new ComponentName("com.android.chrome", "com.android.chrome.Main"));
            intent.setData(Uri.fromFile(new File(mMod.getIndexHtmlFileName())));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } else {
            Toast.makeText(mContext, "Failed to proces file: " + mErr.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}

