/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class LogFilterChartPlugin extends ChartPlugin {

    private Vector<DataSet> mDataSets = new Vector<DataSet>();
    private HashMap<String, DataSet> mDataSetMap = new HashMap<String, DataSet>();
    private Vector<LogFilter> mFilters = new Vector<LogFilter>();
    private long mFirstTs;
    private long mLastTs;
    private HashMap<String, Long> mTimers = new HashMap<String, Long>();

    public LogFilterChartPlugin() {
        // NOP
    }

    @Override
    public boolean init(Module mod, ChartGenerator chart) {
        if (mDataSets.size() == 0 || mFilters.size() == 0) {
            return false;
        }

        filterLog(mod);

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
                }
                // We are pretty sure about the last value
                Data lastData = ds.getData(ds.getDataCount() - 1);
                if (lastData.time < mLastTs) {
                    ds.addData(new Data(mLastTs, lastData.value));
                }
            }
        }

        // Update the chart
        for (DataSet ds : mDataSets) {
            chart.add(ds);
        }

        // Expose all the datasets to the ChartEditorPlugin
        for (DataSet ds : mDataSets) {
            mod.getChartPluginRepo().add(new DataSetInfo(ds, "External"));
        }

        return true;
    }

    private void filterLog(Module mod) {
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
                logs = (LogLines) mod.getInfo(EventLogPlugin.INFO_ID_LOG);
            } else if (log.equals("system")) {
                logs = (LogLines) mod.getInfo(MainLogPlugin.INFO_ID_SYSTEMLOG);
            } else if (log.equals("main")) {
                logs = (LogLines) mod.getInfo(MainLogPlugin.INFO_ID_MAINLOG);
            }
            if (logs == null || logs.size() == 0) {
                mod.printErr(4, "Log '" + log + "' not found or empty!");
                continue;
            }

            // Save the range, use the first log for that
            // FIXME: is this the correct behavior?
            if (mFirstTs == 0 && mLastTs == 0) {
                mFirstTs = logs.get(0).ts;
                mLastTs = logs.get(logs.size() - 1).ts;
            }

            // Now try match each line
            for (LogLine ll : logs) {
                for (LogFilter f : mFilters) {
                    if (!f.getLog().equals(log)) continue;
                    try {
                        f.process(ll);
                    } catch (Exception e) {
                        // if something happens, just ignore this line
                        mod.printErr(4, "Error processing line: " + ll.line + ": " + e);
                    }
                }
            }
        }
    }

    public void addDataset(DataSet ds) {
        mDataSets.add(ds);
        mDataSetMap.put(ds.getId(), ds);
    }

    public DataSet getDataset(String dataset) {
        DataSet ds = mDataSetMap.get(dataset);
        if (ds == null) {
            throw new RuntimeException("Cannot find dataset: " + dataset);
        }
        return ds;
    }

    public void addFilter(LogFilter filter) {
        mFilters.add(filter);
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

    public static LogFilterChartPlugin parse(Module mod, XMLNode code) {
        LogFilterChartPlugin ret = new LogFilterChartPlugin();
        // Collect the data
        for (XMLNode node : code) {
            String tag = node.getName();
            if (tag == null) {
                // NOP
            } else if ("dataset".equals(tag)) {
                DataSet ds = DataSet.parse(node);
                if (ds != null) {
                    ret.addDataset(ds);
                }
            } else if ("filter".equals(tag)) {
                LogFilter filter = LogFilter.parse(ret, node);
                if (filter != null) {
                    ret.addFilter(filter);
                }
            } else {
                mod.printErr(4, "Unknown tag in logchart: " + tag);
            }
        }
        return ret;
    }

    @Override
    public long getFirstTs() {
        return mFirstTs;
    }

    @Override
    public long getLastTs() {
        return mLastTs;
    }

}
