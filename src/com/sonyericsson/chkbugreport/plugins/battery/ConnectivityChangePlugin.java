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
package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.chart.Data;
import com.sonyericsson.chkbugreport.chart.DataSet;
import com.sonyericsson.chkbugreport.plugins.logs.ConnectivityLog;
import com.sonyericsson.chkbugreport.plugins.logs.ConnectivityLogs;

/* package */ class ConnectivityChangePlugin extends ChartPlugin {

    private ConnectivityLogs mLog;
    private long mFirstTs;
    private long mLastTs;

    @Override
    public boolean init(Module mod, ChartGenerator chart) {
        mLog = (ConnectivityLogs) mod.getInfo(ConnectivityLogs.INFO_ID);
        if (mLog == null || mLog.size() == 0) {
            return false;
        }
        // Build data sets
        DataSet dsMobile = new DataSet(DataSet.Type.STATE, "mobile conn");
        DataSet dsWifi = new DataSet(DataSet.Type.STATE, "wifi conn");
        dsMobile.addColor(COL_GREEN);
        dsMobile.addColor(COL_RED);
        dsWifi.addColor(COL_GREEN);
        dsWifi.addColor(COL_RED);
        for (ConnectivityLog l : mLog) {
            int mode = "CONNECTED".equals(l.getState()) ? 1 : 0;
            if ("mobile".equals(l.getInterface())) {
                dsMobile.addData(new Data(l.getTs(), mode));
            } else if ("WIFI".equals(l.getInterface())) {
                dsWifi.addData(new Data(l.getTs(), mode));
            }
        }
        fill(dsMobile, chart);
        fill(dsWifi, chart);
        if (dsMobile.getDataCount() > 0) {
            chart.add(dsMobile);
        }
        if (dsWifi.getDataCount() > 0) {
            chart.add(dsWifi);
        }
        return true;
    }

    private void fill(DataSet ds, ChartGenerator chart) {
        if (ds.getDataCount() == 0) {
            return;
        }
        // It could be dangerous the guess the previous state
        Data first = ds.getData(0);
        mFirstTs = first.time;
        // ds.insertData(new Data(mFirstTs, first.value == 0 ? 1 : 0));
        Data last = ds.getData(ds.getDataCount() - 1);
        mLastTs = last.time;
        ds.addData(new Data(mLastTs, last.value));
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
