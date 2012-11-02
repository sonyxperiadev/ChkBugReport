package com.sonyericsson.chkbugreport.plugins.extxml;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.plugins.extxml.DataSet.Type;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private XMLNode mCode;
    private Vector<DataSet> mDataSets = new Vector<DataSet>();
    private HashMap<String, DataSet> mDataSetMap = new HashMap<String, DataSet>();
    private long mFirstTs;
    private long mLastTs;

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
                filterLog(node);
            } else {
                mMod.printErr(4, "Unknown tag in logchart: " + tag);
            }
        }

        // When all data is parsed, we need to sort the datasets, to make
        // sure the timestamps are in order
        for (DataSet ds : mDataSets) {
            ds.sort();
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
            mCh.add(ret);
        }
    }

    private void filterLog(XMLNode node) {
        Pattern pLine = null, pTag = null, pMsg = null;
        String log = node.getAttr("log");
        if (log == null) throw new RuntimeException("filter needs log attribute");
        String[] dataset = node.getAttr("dataset").split(",");
        int[] values = null;
        String attr = node.getAttr("value");
        if (attr != null) {
            String fields[] = attr.split(",");
            values = new int[fields.length];
            for (int i = 0; i < fields.length; i++) {
                values[i] = Integer.parseInt(fields[i]);
            }
        }
        attr = node.getAttr("matchLine");
        if (attr != null) {
            pLine = Pattern.compile(attr);
        }
        attr = node.getAttr("matchTag");
        if (attr != null) {
            pTag = Pattern.compile(attr);
        }
        attr = node.getAttr("matchMsg");
        if (attr != null) {
            pMsg = Pattern.compile(attr);
        }

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
        }

        // Save the range, use the first log for that
        if (mFirstTs == 0 && mLastTs == 0) {
            mFirstTs = logs.get(0).ts;
            mLastTs = logs.get(logs.size() - 1).ts;

        }

        // Now try match each line
        for (LogLine ll : logs) {
            boolean matched = false;
            boolean extracted = false;
            // Match line
            if (pLine != null) {
                Matcher m = pLine.matcher(ll.line);
                if (m.find()) {
                    matched = true;
                    if (processLine(ll, m, dataset)) {
                        extracted = true;
                    }
                }
            }
            // Match message
            if (pMsg != null) {
                Matcher m = pMsg.matcher(ll.msg);
                if (m.find()) {
                    matched = true;
                    if (processLine(ll, m, dataset)) {
                        extracted = true;
                    }
                }
            }
            // Match tag
            if (pTag != null) {
                Matcher m = pTag.matcher(ll.tag);
                if (m.find()) {
                    matched = true;
                    if (processLine(ll, m, dataset)) {
                        extracted = true;
                    }
                }
            }
            // If no data was extracted, then add fixed values
            if (matched && !extracted) {
                if (values == null) {
                    throw new RuntimeException("No data was extracted and no default values specified!");
                }
                for (int i = 0; i < values.length; i++) {
                    DataSet ds = getDataset(dataset[i]);
                    ds.addData(new Data(ll.ts, values[i]));
                }
            }
        }
    }

    private boolean processLine(LogLine ll, Matcher m, String[] dataset) {
        int cnt = m.groupCount();
        if (cnt > 0) {
            // Extract data
            for (int i = 0; i < cnt; i++) {
                String sValue = m.group(1 + i);
                int value = Integer.parseInt(sValue);
                DataSet ds = getDataset(dataset[i]);
                ds.addData(new Data(ll.ts, value));
            }
            return true;
        }
        return false;
    }

    private DataSet getDataset(String dataset) {
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

        mDataSets.add(ds);
        mDataSetMap.put(ds.getId(), ds);
    }

}
