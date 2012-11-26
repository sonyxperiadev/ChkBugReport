/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.extxml;

import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogFilter {

    private String mLog;
    private Pattern mPLine = null, mPTag = null, mPMsg = null;
    private String[] mDataset;
    private int[] values = null;
    private LogChart mChart;
    private String[] mStartTimers;
    private String[] mStopTimers;

    public LogFilter(LogChart chart, XMLNode node) {
        mChart = chart;
        mLog = node.getAttr("log");
        if (mLog == null) throw new RuntimeException("filter needs log attribute");
        String attr = node.getAttr("dataset");
        if (attr != null) {
            mDataset = attr.split(",");
        }
        attr = node.getAttr("value");
        if (attr != null) {
            String fields[] = attr.split(",");
            values = new int[fields.length];
            for (int i = 0; i < fields.length; i++) {
                values[i] = Integer.parseInt(fields[i]);
            }
        }
        attr = node.getAttr("matchLine");
        if (attr != null) {
            mPLine = Pattern.compile(attr);
        }
        attr = node.getAttr("matchTag");
        if (attr != null) {
            mPTag = Pattern.compile(attr);
        }
        attr = node.getAttr("matchMsg");
        if (attr != null) {
            mPMsg = Pattern.compile(attr);
        }
        if (mPLine == null && mPTag == null && mPMsg == null) {
            throw new RuntimeException("You need to specify at least one of matchLine, matchTag or matchMsg!");
        }
        attr = node.getAttr("startTimer");
        if (attr != null) {
            mStartTimers = attr.split(",");
        }
        attr = node.getAttr("stopTimer");
        if (attr != null) {
            mStopTimers = attr.split(",");
        }
    }

    public String getLog() {
        return mLog;
    }

    public void process(LogLine ll) {
        // First do the matching, and only after that do the extraction
        Matcher mLine = null, mTag = null, mMsg = null;
        if (mPLine != null) {
            mLine = mPLine.matcher(ll.line);
            if (!mLine.find()) {
                return;
            }
        }
        if (mPTag != null) {
            mTag = mPTag.matcher(ll.tag);
            if (!mTag.find()) {
                return;
            }
        }
        if (mPMsg != null) {
            mMsg = mPMsg.matcher(ll.msg);
            if (!mMsg.find()) {
                return;
            }
        }
        // If we got here, then everything which is specified matched
        // so let's extract the values
        if (mLine != null) {
            if (processLine(ll, mLine, mDataset)) {
                return; // Data extracted
            }
        }
        if (mTag != null) {
            if (processLine(ll, mTag, mDataset)) {
                return; // Data extracted
            }
        }
        if (mMsg != null) {
            if (processLine(ll, mMsg, mDataset)) {
                return; // Data extracted
            }
        }
        // If no data was extract, check if we should use timers
        boolean timersUsed = false;
        if (mStopTimers != null) {
            stopTimers(ll.ts);
            timersUsed = true;
        }
        if (mStartTimers != null) {
            startTimers(ll.ts);
            timersUsed = true;
        }
        if (timersUsed) {
            return;
        }
        // If no data was extracted, and no timers were used, then add fixed values
        if (values == null) {
            throw new RuntimeException("No data was extracted and no default values specified!");
        }
        for (int i = 0; i < values.length; i++) {
            DataSet ds = mChart.getDataset(mDataset[i]);
            ds.addData(new Data(ll.ts, values[i]));
        }
    }

    private void startTimers(long ts) {
        for (String timer : mStartTimers) {
            mChart.startTimer(timer, ts);
        }
    }

    private void stopTimers(long ts) {
        int cnt = mStopTimers.length;
        for (int i = 0; i < cnt; i++) {
            String timer = mStopTimers[i];
            long ret = mChart.stopTimer(timer, ts);
            if (ret != Long.MAX_VALUE) {
                if (mDataset == null) {
                    throw new RuntimeException("No dataset was specified in log filter!");
                }
                DataSet ds = mChart.getDataset(mDataset[i]);
                ds.addData(new Data(ts, (int) ret));
            }
        }
    }

    private boolean processLine(LogLine ll, Matcher m, String[] dataset) {
        int cnt = m.groupCount();
        if (cnt > 0) {
            if (dataset == null) {
                throw new RuntimeException("No dataset was specified in log filter!");
            }
            // Extract data
            for (int i = 0; i < cnt; i++) {
                String sValue = m.group(1 + i);
                int value = Integer.parseInt(sValue);
                DataSet ds = mChart.getDataset(dataset[i]);
                ds.addData(new Data(ll.ts, value));
            }
            return true;
        }
        return false;
    }


}
