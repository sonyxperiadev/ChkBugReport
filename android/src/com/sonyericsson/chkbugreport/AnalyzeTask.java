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

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

public class AnalyzeTask extends AsyncTask<String, String, Void> {

    private LogViewer mLogViewer;
    private BugReportModule mMod;

    public AnalyzeTask(LogViewer logViewer) {
        mLogViewer = logViewer;
    }

    @Override
    protected Void doInBackground(String... params) {
        AndroidContext ctx = new AndroidContext(this);
        mMod = new BugReportModule(ctx);
        mMod.addFile(params[0], null, false);
        try {
            mMod.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        mLogViewer.log(values[0]);
    }

    public void onPrint(int level, int type, String msg) {
        publishProgress(msg);
    }

    @Override
    protected void onPostExecute(Void result) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setComponent(new ComponentName("com.android.chrome", "com.android.chrome.Main"));
        intent.setData(Uri.fromFile(new File(mMod.getIndexHtmlFileName())));
        mLogViewer.getContext().startActivity(intent);
    }

}

