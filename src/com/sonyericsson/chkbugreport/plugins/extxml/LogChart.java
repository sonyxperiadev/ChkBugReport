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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.Data;
import com.sonyericsson.chkbugreport.chart.DataSet;
import com.sonyericsson.chkbugreport.chart.DataSet.Type;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class LogChart {

    private static final HashMap<String,DataSet.Type> TYPE_TBL;
    static {
        TYPE_TBL = new HashMap<String, DataSet.Type>();
        TYPE_TBL.put("plot", DataSet.Type.PLOT);
        TYPE_TBL.put("miniplot", DataSet.Type.MINIPLOT);
        TYPE_TBL.put("state", DataSet.Type.STATE);
        TYPE_TBL.put("event", DataSet.Type.EVENT);
    }

    private Module mMod;
    private Chapter mCh;
    private XMLNode mCode;
    private Vector<DataSet> mDataSets = new Vector<DataSet>();
    private HashMap<String, DataSet> mDataSetMap = new HashMap<String, DataSet>();
    private Vector<LogFilter> mFilters = new Vector<LogFilter>();
    private long mFirstTs;
    private long mLastTs;
    private HashMap<String, Long> mTimers = new HashMap<String, Long>();

    public LogChart(Module mod, Chapter ch, XMLNode code) {
        mMod = mod;
        mCh = ch;
        mCode = code;
    }

    public void exec() {
        // Collect the data
        for (XMLNode node : mCode) {
            String tag = node.getName();
            if (tag == null) {
                // NOP
            } else if ("dataset".equals(tag)) {
                addDataSet(node);
            } else if ("filter".equals(tag)) {
                addFilter(node);
            } else {
                mMod.printErr(4, "Unknown tag in logchart: " + tag);
            }
        }

        filterLog();

        // When all data is parsed, we need to sort the datasets, to make
        // sure the timestamps are in order
        for (DataSet ds : mDataSets) {
            ds.sort();
        }

        // Remove empty data
        for (Iterator<DataSet> i = mDataSets.iterator(); i.hasNext();) {
            DataSet ds = i.next();
            if (ds.isEmpty()) {
                i.remove();
            }
        }

        // Guess missing data when possible
        for (DataSet ds : mDataSets) {
            Data firstData = ds.getData(0);
            if (firstData.time > mFirstTs) {
                int initValue = ds.getGuessFor((int) firstData.value);
                if (initValue != -1) {
                    ds.insertData(new Data(mFirstTs, initValue));
                    // If we are allowed to guess the initial value, the guess the final value as well
                    Data lastData = ds.getData(ds.getDataCount() - 1);
                    if (lastData.time < mLastTs) {
                        ds.addData(new Data(mLastTs, lastData.value));
                    }
                }
            }
        }

        // And finally create the chart
        String title = mCode.getAttr("name");
        String fn = mCode.getAttr("file");

        ChartGenerator chart = new ChartGenerator(title);
        for (DataSet ds : mDataSets) {
            chart.add(ds);
        }

        DocNode ret = chart.generate(mMod, fn, mFirstTs, mLastTs);
        if (ret != null) {
            mCh.add(ret);
        } else {
            mCh.add(new Para().add("Chart data missing!"));
        }
    }

    private void addFilter(XMLNode node) {
        LogFilter filter = new LogFilter(this, node);
        mFilters.add(filter);
    }

    private void filterLog() {
        // Find out which logs are needed
        Vector<String> lognames = new Vector<String>();
        for (LogFilter f : mFilters) {
            if (!lognames.contains(f.getLog())) {
                lognames.add(f.getLog());
            }
        }

        // Process each log
        for (String log : lognames) {
            // Find the log
            LogLines logs = null;
            if (log.equals("event")) {
                logs = (LogLines) mMod.getInfo(EventLogPlugin.INFO_ID_LOG);
            } else if (log.equals("system")) {
                logs = (LogLines) mMod.getInfo(MainLogPlugin.INFO_ID_SYSTEMLOG);
            } else if (log.equals("main")) {
                logs = (LogLines) mMod.getInfo(MainLogPlugin.INFO_ID_MAINLOG);
            }
            if (logs == null || logs.size() == 0) {
                mMod.printErr(4, "Log '" + log + "' not found or empty!");
                continue;
            }

            // Save the range, use the first log for that
            if (mFirstTs == 0 && mLastTs == 0) {
                mFirstTs = logs.get(0).ts;
                mLastTs = logs.get(logs.size() - 1).ts;
            }

            // Now try match each line
            for (LogLine ll : logs) {
                for (LogFilter f : mFilters) {
                    if (!f.getLog().equals(log)) continue;
                    f.process(ll);
                }
            }
        }
    }

    public DataSet getDataset(String dataset) {
        DataSet ds = mDataSetMap.get(dataset);
        if (ds == null) {
            throw new RuntimeException("Cannot find dataset: " + dataset);
        }
        return ds;
    }

    private void addDataSet(XMLNode node) {
        String name = node.getAttr("name");
        Type type = TYPE_TBL.get(node.getAttr("type"));
        DataSet ds = new DataSet(type, name);
        ds.setId(node.getAttr("id"));

        // Parse optional color array
        String attr = node.getAttr("colors");
        if (attr != null) {
            for (String rgb : attr.split(",")) {
                ds.addColor(rgb);
            }
        }

        // Parse optional min/max values
        attr = node.getAttr("min");
        if (attr != null) {
            ds.setMin(Integer.parseInt(attr));
        }
        attr = node.getAttr("max");
        if (attr != null) {
            ds.setMax(Integer.parseInt(attr));
        }

        // Parse optional guess map, used to guess the previous state from the current one
        attr = node.getAttr("guessmap");
        if (attr != null) {
            ds.setGuessMap(attr);
        }

        // Parse optional axis id attribute
        attr = node.getAttr("axis");
        if (attr != null) {
            ds.setAxisId(Integer.parseInt(attr));
        }

        mDataSets.add(ds);
        mDataSetMap.put(ds.getId(), ds);
    }

    public void startTimer(String timer, long ts) {
        mTimers.put(timer, ts);
    }

    public long stopTimer(String timer, long ts) {
        if (!mTimers.containsKey(timer)) {
            return Long.MAX_VALUE;
        }
        long ret = ts - mTimers.get(timer);
        mTimers.remove(timer);
        return ret;
    }

}
