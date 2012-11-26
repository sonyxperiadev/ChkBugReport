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
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.plugins.extxml.DataSet.Type;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class LogChart {

    private static final HashMap<String,DataSet.Type> TYPE_TBL;
    static {
        TYPE_TBL = new HashMap<String, DataSet.Type>();
        TYPE_TBL.put("plot", DataSet.Type.PLOT);
        TYPE_TBL.put("state", DataSet.Type.STATE);
        TYPE_TBL.put("event", DataSet.Type.EVENT);
    }

    private Module mMod;
    private Chapter mCh;
    private Chapter mChFlot;
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
        mChFlot = new Chapter(mod, ch.getName() + " - interactive chart");
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
        DataSetPlot plot = null;
        for (DataSet ds : mDataSets) {
            if (ds.getType() == Type.PLOT) {
                if (plot == null) {
                    plot = new DataSetPlot();
                    chart.addPlugin(plot);
                }
                plot.add(ds);
            } else {
                chart.addPlugin(new DataSetStrip(ds));
            }
        }

        DocNode ret = chart.generate(mMod, fn, mFirstTs, mLastTs);
        if (ret != null) {
            new Hint(mCh).add(new Link(mChFlot.getAnchor(), "Click here for interactive version"));
            mChFlot.add(new FlotGenerator());
            mMod.addExtraFile(mChFlot);
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
        DataSet ds = new DataSet();
        ds.setId(node.getAttr("id"));
        ds.setName(node.getAttr("name"));
        ds.setType(TYPE_TBL.get(node.getAttr("type")));

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

        mDataSets.add(ds);
        mDataSetMap.put(ds.getId(), ds);
    }

    class FlotGenerator extends DocNode {

        @Override
        public void render(Renderer r) throws IOException {
            r.println("<h1>!!! UNDER CONSTRUCTION !!!</h1>");
            r.println("<button id=\"zoomOutBtn\">Zoom out</button>");

            // Check how manu TYPE_PLOT we have
            int typePlotCount = 0;
            for (DataSet ds : mDataSets) {
                if (ds.getType() == Type.PLOT) {
                    typePlotCount++;

                }
            }

            // Insert the placeholders
            boolean typePlotCreated = false;
            int labelWidth = 200;
            Vector<String> ids = new Vector<String>();
            for (int i = 0; i < mDataSets.size(); i++) {
                DataSet ds = mDataSets.get(i);
                if (ds.getType() == Type.PLOT) {
                    if (!typePlotCreated) {
                        typePlotCreated = true;
                        r.println("<div id=\"chart\" style=\"width: 800px; height: 400px;\"></div>");
                        ids.add("");
                    }
                } else {
                    r.println("<div id=\"chart" + i + "\" style=\"width: 800px; height: 50px;\"></div>");
                    ids.add(Integer.toString(i));
                }
            }

            r.println("<script type=\"text/javascript\">");

            r.println("$(function(){");

            // Add zooming support
            r.println("function onZoomSelection(event, ranges) {");
            for (String id : ids) {
                r.println("  plot" + id + " = $.plot(chart" + id + ", data" + id + ",");
                r.println("                $.extend(true, {}, options" + id + ", {");
                r.println("                    xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }");
                r.println("                 }));");
            }
            r.println("}");
            r.println("function onZoomOut(e) {");
            r.println("  e.preventDefault();");
            for (String id : ids) {
                r.println("  plot" + id + " = $.plot(chart" + id + ", data" + id + ", options" + id + ");");
            }
            r.println("}");
            r.println("$(\"#zoomOutBtn\").click(onZoomOut);");

            if (typePlotCount > 0) {
                // First step: plot the non-strip values
                r.println("var data = [");
                int yaxisCounter = 0;
                for (int i = 0; i < mDataSets.size(); i++) {
                    DataSet ds = mDataSets.get(i);
                    if (ds.getType() == Type.PLOT) {
                        yaxisCounter++;
                        r.println("  {");
                        r.println("  label: \"" + ds.getName() + "\",");
                        r.println("  yaxis: " + yaxisCounter + ",");
                        r.println("  data: [");
                        int cnt = ds.getDataCount();
                        for (int j = 0; j < cnt; j++) {
                            Data d = ds.getData(j);
                            if (j != 0) {
                                r.print(", ");
                            }
                            if (0 == (j & 7)) {
                                r.println("");
                                r.print("    ");
                            }
                            r.print("[" + d.time + "," + d.value + "]");
                        }
                        r.println("]");
                        r.println("  },");
                    }
                }
                r.println("];");
                r.println("var chart = $(\"#chart\");");

                // Build options
                r.println("var options = {");
                r.println("  selection: { mode: 'x' },");
                r.println("  legend: { position: 'nw', margin: [ -" + labelWidth + ", 0 ], }, ");
                r.println("  yaxes: [");
                for (int i = 0; i < typePlotCount; i++) {
                    r.println("  {");
                    if (i == 0) {
                        r.println("    labelWidth: " + labelWidth + ",");
                    } else {
                        r.println("    show: false,");
                    }
                    r.println("  },");

                }
                r.println("  ],");
                r.println("  xaxis: {");
                r.println("    mode: \"time\",");
                r.println("    min: " + mFirstTs + ",");
                r.println("    max: " + mLastTs + ",");
                r.println("  },");
                r.println("};");

                // Add zooming support
                r.println("chart.bind(\"plotselected\", onZoomSelection);");

                // Generate the chart
                r.println("var plot = $.plot(chart, data, options);");
            }

            // Next step: plot the rest
            for (int i = 0; i < mDataSets.size(); i++) {
                DataSet ds = mDataSets.get(i);
                if (ds.getType() != Type.PLOT) {
                    r.println("var data" + i + " = [");
                    r.println("  {");
                    r.println("  label: \"" + ds.getName() + "\",");
                    r.println("  lines: { show: true, fill: true },");
                    r.println("  data: [");
                    int cnt = ds.getDataCount();
                    for (int j = 0; j < cnt; j++) {
                        Data d = ds.getData(j);
                        if (j != 0) {
                            r.print(", ");
                        }
                        if (0 == (j & 7)) {
                            r.println("");
                            r.print("    ");
                        }
                        r.print("[" + d.time + "," + d.value + "]");
                    }
                    r.println("]");
                    r.println("  },");
                    r.println("];");
                    r.println("var chart" + i + " = $(\"#chart" + i + "\");");

                    // Build options
                    r.println("var options" + i + " = {");
                    r.println("  selection: { mode: 'x' },");
                    r.println("  legend: { position: 'nw', margin: [ -" + labelWidth + ", 0 ], }, ");
                    r.println("  yaxis: {");
                    r.println("    labelWidth: " + labelWidth + ",");
                    r.println("    ticks: [ ],");
                    r.println("  },");
                    r.println("  xaxis: {");
                    r.println("    show: false,");
                    r.println("    min: " + mFirstTs + ",");
                    r.println("    max: " + mLastTs + ",");
                    r.println("  },");
                    r.println("};");

                    // Add zooming support
                    r.println("chart" + i + ".bind(\"plotselected\", onZoomSelection);");

                    // Generate the chart
                    r.println("var plot" + i + " = $.plot(chart" + i + ", data" + i + ", options" + i + ");");
                }
            }

            // End of script
            r.println("});");
            r.println("</script>");
        }

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
