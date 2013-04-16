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
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.DeepSleep;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.DeepSleeps;

/* package */ class DeepSleepPlugin extends ChartPlugin {

    private Module mMod;
    private DeepSleeps mData;
    private long mFirstTs;
    private long mLastTs;

    @Override
    public boolean init(Module mod, ChartGenerator chart) {
        mMod = mod;
        mData = (DeepSleeps) mod.getInfo(DeepSleeps.INFO_ID);
        if (mData == null || mData.size() == 0) {
            return false;
        }

        DataSet ds = new DataSet(DataSet.Type.STATE, "sleep");
        ds.addColor(COL_RED);
        ds.addColor(COL_GREEN);
        for (DeepSleep sleep : mData) {
            ds.addData(new Data(sleep.getLastRealTs(), 1));
            ds.addData(new Data(sleep.getRealTs(), 0));
        }

        // Fill data
        Data first = ds.getData(0);
        mFirstTs = first.time;
        ds.insertData(new Data(mFirstTs, 0));
        Data last = ds.getData(ds.getDataCount() - 1);
        mLastTs = last.time;
        ds.addData(new Data(mLastTs, last.value));

        chart.add(ds);
        return true;
    }

    @Override
    public DocNode getAppendix() {
        if (mData.size() > 0) {
            Hint ret = new Hint();
            ret.add("Note: when detecting CPU sleeps from the kernel log, the timestamps are in " +
                    "UTC time, so you might need to use the --gmt:offset argument to adjust it to "+
                    "the log's timezone! Currently GMT offset is set to: " +
                    mMod.getContext().getGmtOffset());
            return ret;
        }
        return null;
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
