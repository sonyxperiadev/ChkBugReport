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
package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartGenerator;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.chart.Data;
import com.sonyericsson.chkbugreport.chart.DataSet;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevel;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;

public class BatteryLevelChart extends ChartPlugin {

    private static final int COLB = 0xff000000;
    private static final int COLV = 0x20000000;
    private static final int COLT = 0x40ff4040;

    public static final int ALL         = 0;
    public static final int LEVEL_ONLY  = 1;

    private BatteryLevels mData;
    private int mFlags;

    public BatteryLevelChart(BatteryLevels batteryLevels) {
        this(batteryLevels, ALL);
    }

    public BatteryLevelChart(BatteryLevels batteryLevels, int flags) {
        mData = batteryLevels;
        mFlags = flags;
    }

    @Override
    public DocNode getPreface() {
        Para ret = new Para();
        ret.add("Graph built from battery_level logs");
        if (mFlags == ALL) {
            ret.add(" (voltage range: " + mData.getMinVolt() + ".." + mData.getMaxVolt());
            ret.add(", temperature range: " + mData.getMinTemp() + ".." + mData.getMaxTemp() + ")");
        }
        ret.add(":");
        return ret;
    }

    @Override
    public boolean init(Module mod, ChartGenerator chart) {
        int cnt = mData.getCount();
        if (cnt == 0) {
            return false;
        }
        DataSet dsPerc = new DataSet(DataSet.Type.PLOT, "Battery level (%)", COLB);
        DataSet dsVolt = new DataSet(DataSet.Type.PLOT, "Battery voltage (mV)", COLV);
        DataSet dsTemp = new DataSet(DataSet.Type.PLOT, "Battery temperature (C*10)", COLT);
        dsPerc.setAxisId(1);
        dsVolt.setAxisId(2);
        dsTemp.setAxisId(3);
        for (int i = 0; i < cnt; i++) {
            BatteryLevel bl = mData.get(i);
            dsPerc.addData(new Data(bl.getTs(), bl.getLevel()));
            if (mFlags == ALL) {
                dsVolt.addData(new Data(bl.getTs(), bl.getVolt()));
                dsTemp.addData(new Data(bl.getTs(), bl.getTemp()));
            }
        }
        chart.add(dsPerc);
        if (mFlags == ALL) {
            chart.add(dsVolt);
            chart.add(dsTemp);
        }
        return true;
    }

    @Override
    public long getFirstTs() {
        return mData.getFirstTs();
    }

    @Override
    public long getLastTs() {
        return mData.getLastTs();
    }

}
