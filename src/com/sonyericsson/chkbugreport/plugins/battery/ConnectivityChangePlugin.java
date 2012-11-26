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
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.ConnectivityLog;
import com.sonyericsson.chkbugreport.plugins.logs.ConnectivityLogs;

import java.awt.Color;
import java.awt.Graphics2D;

public class ConnectivityChangePlugin extends ChartPlugin {

    private ConnectivityLogs mLog;
    private String mInterf;
    private String mLabel;

    public ConnectivityChangePlugin(String interf, String label) {
        mInterf = interf;
        mLabel = label;
    }

    @Override
    public int getType() {
        return TYPE_STRIP;
    }

    @Override
    public String getName() {
        return mLabel;
    }

    @Override
    public boolean init(Module mod) {
        mLog = (ConnectivityLogs) mod.getInfo(ConnectivityLogs.INFO_ID);
        if (mLog != null) {
            if (mLog.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = x;
        int lastMode = -1;
        Color cols[] = {COL_GREEN, COL_RED};
        long duration = lastTs - firstTs;
        for (ConnectivityLog l : mLog) {
            if (!mInterf.equals(l.getInterface())) continue;
            int mode = "CONNECTED".equals(l.getState()) ? 1 : 0;
            if (lastMode == -1) {
                lastMode = (mode == 0) ? 1 : 0;
            }
            int cx = (int) (x + (l.getTs() - firstTs) * w / duration);
            g.setColor(cols[lastMode]);
            g.fillRect(lastX, y, cx - lastX + 1, h);
            lastX = cx;
            lastMode = mode;
        }
        if (lastMode >= 0) {
            g.setColor(cols[lastMode]);
            g.fillRect(lastX, y, x + w - lastX, h);
        }
    }

}
