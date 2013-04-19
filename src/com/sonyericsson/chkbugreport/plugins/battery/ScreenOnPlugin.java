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
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;

public class ScreenOnPlugin extends ChartPlugin {

    private LogLines mEventLog;
    private long mFirstTs;
    private long mLastTs;

    @Override
    public long getFirstTs() {
        return mFirstTs;
    }

    @Override
    public long getLastTs() {
        return mLastTs;
    }

    @Override
    public boolean init(Module mod, ChartGenerator chart) {
        mEventLog = (LogLines) mod.getInfo(EventLogPlugin.INFO_ID_LOG);
        if (mEventLog == null || mEventLog.size() == 0) {
            return false;
        }

        // Collect data
        DataSet ds = new DataSet(DataSet.Type.STATE, "screen");
        ds.addColor(COL_GREEN);
        ds.addColor(COL_YELLOW);
        ds.addColor(COL_RED);
        for (LogLine l : mEventLog) {
            if (!"screen_toggled".equals(l.tag)) continue;
            int mode = Integer.parseInt(l.msg);
            ds.addData(new Data(l.ts, mode));
        }
        if (ds.getDataCount() == 0) {
            return false;
        }

        // fill data
        Data first = ds.getData(0);
        mFirstTs = first.time;
        ds.insertData(new Data(mFirstTs, first.value == 0 ? 2 : 0));
        Data last = ds.getData(ds.getDataCount() - 1);
        mLastTs = last.time;
        ds.addData(new Data(mLastTs, last.value));

        chart.add(ds);
        return true;
    }

}
