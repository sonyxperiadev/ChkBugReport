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

import com.sonyericsson.chkbugreport.LogViewer;
import com.sonyericsson.chkbugreport.OutputListener;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class StatusActivity extends Activity implements OnClickListener, OutputListener {

    private ChkBugReportApp mApp;
    private LogViewer logViewer;
    private String mFileName;
    private Button mBtnAnalyze;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        mBtnAnalyze = (Button) findViewById(R.id.btnAnalyze);
        mBtnAnalyze.setOnClickListener(this);
        logViewer = (LogViewer)findViewById(R.id.logViewer);
        mApp = (ChkBugReportApp) getApplication();
        Intent intent = getIntent();
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                if (uri != null) {
                    mFileName = uri.getPath();
                }
                System.out.println("mFileName=" + mFileName);
            }
        }
        if (mFileName == null) {
            mBtnAnalyze.setVisibility(View.GONE);
        } else {
            mBtnAnalyze.setVisibility(View.VISIBLE);
            mBtnAnalyze.setText("Analyze " + mFileName + "...");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mApp.setOutputListener(this);
    }

    @Override
    protected void onStop() {
        mApp.setOutputListener(null);
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnAnalyze) {
            mBtnAnalyze.setVisibility(View.GONE);
            Intent intent = new Intent(this, AnalyzerService.class);
            intent.setData(Uri.fromParts("", mFileName, ""));
            startService(intent);
        }
    }

    @Override
    public void onPrint(int level, int type, String msg) {
        logViewer.log(msg);
    }

}


