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
package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogFilter {

    private String mLog;
    private Pattern mPLine = null, mPTag = null, mPMsg = null;
    private String[] mDataset;
    private int[] mValues = null;
    private LogFilterChartPlugin mChart;
    private String[] mStartTimers;
    private String[] mStopTimers;

    public LogFilter(LogFilterChartPlugin chart) {
        mChart = chart;
    }

    public static LogFilter parse(LogFilterChartPlugin chart, XMLNode node) {
        LogFilter ret = new LogFilter(chart);
        String attr = node.getAttr("log");
        if (attr == null) throw new RuntimeException("filter needs log attribute");
        ret.setLog(attr);

        attr = node.getAttr("dataset");
        if (attr != null) {
            ret.setDataSet(attr.split(","));
        }
        attr = node.getAttr("value");
        if (attr != null) {
            String fields[] = attr.split(",");
            int[] values = new int[fields.length];
            for (int i = 0; i < fields.length; i++) {
                values[i] = Integer.parseInt(fields[i]);
            }
            ret.setValues(values);
        }
        attr = node.getAttr("matchLine");
        if (attr != null) {
            ret.setLinePattern(Pattern.compile(attr));
        }
        attr = node.getAttr("matchTag");
        if (attr != null) {
            ret.setTagPattern(Pattern.compile(attr));
        }
        attr = node.getAttr("matchMsg");
        if (attr != null) {
            ret.setMessagePattern(Pattern.compile(attr));
        }
        attr = node.getAttr("startTimer");
        if (attr != null) {
            ret.setStartTimers(attr.split(","));
        }
        attr = node.getAttr("stopTimer");
        if (attr != null) {
            ret.setStopTimer(attr.split(","));
        }
        return ret;
    }

    public void setLinePattern(Pattern p) {
        mPLine = p;
    }

    public void setTagPattern(Pattern p) {
        mPTag = p;
    }

    public void setMessagePattern(Pattern p) {
        mPMsg = p;
    }

    private void setStartTimers(String[] timers) {
        mStartTimers = timers;
    }

    private void setStopTimer(String[] timers) {
        mStopTimers = timers;
    }

    public void setValues(int[] values) {
        mValues = values;
    }

    public void setDataSet(String[] dsNames) {
        mDataset = dsNames;
    }

    public void setLog(String log) {
        mLog = log;
    }

    public String getLog() {
        return mLog;
    }

    public void process(LogLine ll) {
        if (mPLine == null && mPTag == null && mPMsg == null) {
            throw new RuntimeException("You need to specify at least one of matchLine, matchTag or matchMsg!");
        }

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
        if (mValues == null) {
            throw new RuntimeException("No data was extracted and no default values specified!");
        }
        for (int i = 0; i < mValues.length; i++) {
            DataSet ds = mChart.getDataset(mDataset[i]);
            ds.addData(new Data(ll.ts, mValues[i]));
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
