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
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.plugins.extxml.DataSet.Type;

import java.awt.Graphics2D;

public class DataSetStrip extends ChartPlugin {

    private DataSet mData;

    public DataSetStrip(DataSet ds) {
        mData = ds;
    }

    @Override
    public int getType() {
        return TYPE_STRIP;
    }

    @Override
    public String getName() {
        return mData.getName();
    }

    @Override
    public boolean init(Module mod) {
        return mData.getDataCount() > 0;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = x;
        long lastMode = -1;
        long duration = lastTs - firstTs;
        for (Data d : mData) {
            int cx = (int) (x + (d.time - firstTs) * w / duration);
            if (mData.getType() == Type.STATE) {
                if (lastMode != -1) {
                    g.setColor(mData.getColor(lastMode));
                    g.fillRect(lastX, y, cx - lastX + 1, h);
                }
            } else if (mData.getType() == Type.EVENT) {
                g.setColor(mData.getColor(d.value));
                g.drawLine(cx, y, cx, y + h);
            }
            lastX = cx;
            lastMode = d.value;
        }
        if ((lastMode >= 0) && (mData.getType() == Type.STATE)) {
            g.setColor(mData.getColor(lastMode));
            g.fillRect(lastX, y, x + w - lastX, h);
        }
    }

}
