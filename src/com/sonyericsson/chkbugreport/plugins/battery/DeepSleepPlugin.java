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
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.DeepSleep;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.DeepSleeps;

import java.awt.Color;
import java.awt.Graphics2D;

public class DeepSleepPlugin extends ChartPlugin {

    private DeepSleeps mData;

    @Override
    public int getType() {
        return TYPE_STRIP;
    }

    @Override
    public String getName() {
        return "sleep";
    }

    @Override
    public boolean init(Module mod) {
        mData = (DeepSleeps) mod.getInfo(DeepSleeps.INFO_ID);
        if (mData != null) {
            if (mData.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DocNode getAppendix() {
        if (mData.size() > 0) {
            Hint ret = new Hint();
            ret.add("Note: when detecting CPU sleeps from the kernel log, the timestamps are in UTC time, so you might need to use the --gmt:offset argument to adjust it to the log's timezone!");
            return ret;
        }
        return null;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = -1;
        Color colAwake = COL_RED;
        Color colSleep = COL_GREEN;
        long duration = lastTs - firstTs;
        for (DeepSleep sleep : mData) {
            // Render awake period before the sleep
            int cx = (int) (x + (sleep.getLastRealTs() - firstTs) * w / duration);
            if (lastX != -1) {
                g.setColor(colAwake);
                g.fillRect(lastX, y, cx - lastX + 1, h);
            }
            lastX = cx;
            // Render sleep period
            cx = (int) (x + (sleep.getRealTs() - firstTs) * w / duration);
            g.setColor(colSleep);
            g.fillRect(lastX, y, cx - lastX + 1, h);
            lastX = cx;
        }
        // Render final awake period
        g.setColor(colAwake);
        g.fillRect(lastX, y, x + w - lastX, h);
    }

}
