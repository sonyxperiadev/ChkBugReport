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
import com.sonyericsson.chkbugreport.plugins.logs.event.NetstatSample;
import com.sonyericsson.chkbugreport.plugins.logs.event.NetstatSamples;

/* package */ class NetstatSamplePlugin extends ChartPlugin {

    private NetstatSamples mLog;
    private long mFirstTs = Long.MAX_VALUE;
    private long mLastTs = Long.MIN_VALUE;

    @Override
    public boolean init(Module mod, ChartGenerator chart) {
        boolean ret = false;
        if (init(mod, chart, NetstatSamples.INFO_ID_MOBILE, "mobile traffic")) {
            ret = true;
        }
        if (init(mod, chart, NetstatSamples.INFO_ID_WIFI, "wifi traffic")) {
            ret = true;
        }
        return ret;
    }

    private boolean init(Module mod, ChartGenerator chart, String infoId, String name) {
        mLog = (NetstatSamples) mod.getInfo(infoId);
        if (mLog == null || mLog.size() == 0) {
            return false;
        }

        mFirstTs = Math.min(mFirstTs, mLog.get(0).getTs());
        mLastTs = Math.max(mLastTs, mLog.get(mLog.size() - 1).getTs());

        DataSet dsTx = new DataSet(DataSet.Type.MINIPLOT, name + " (TX)");
        DataSet dsRx = new DataSet(DataSet.Type.MINIPLOT, name + " (RX)");
        long lastTx = -1, lastRx = -1;
        for (NetstatSample l : mLog) {
            long tx = l.getData(NetstatSample.IDX_DEV_TX_BYTES);
            long rx = l.getData(NetstatSample.IDX_DEV_RX_BYTES);
            if (lastTx < 0 || lastRx < 0) {
                // First sample
                lastTx = tx;
                lastRx = rx;
            } else if (lastTx > tx || lastRx > rx) {
                // History was reset, so save absolute value
                dsTx.addData(new Data(l.getTs(), tx));
                dsRx.addData(new Data(l.getTs(), rx));
            } else {
                // Save delta
                dsTx.addData(new Data(l.getTs(), tx - lastTx));
                dsRx.addData(new Data(l.getTs(), rx - lastRx));
            }
            lastTx = tx;
            lastRx = rx;
        }
        chart.add(dsTx);
        chart.add(dsRx);
        return true;
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
