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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Vector;

public class DataSetPlot extends ChartPlugin {

    private FontMetrics mFm;
    private Vector<DataSet> mDatas = new Vector<DataSet>();

    public DataSetPlot() {
    }

    public void add(DataSet ds) {
        mDatas.add(ds);
    }

    @Override
    public int getLegendWidth(FontMetrics fm) {
        mFm = fm;
        int ret = 0;
        for (DataSet ds : mDatas) {
            ret = Math.max(ret, fm.stringWidth(ds.getName()));
        }
        return ret;
    }

    @Override
    public boolean renderLegend(Graphics2D g, int x, int y, int w, int h) {
        for (DataSet ds : mDatas) {
            g.setColor(ds.getColor(0));
            g.drawString(ds.getName(), x, y + mFm.getAscent());
            y += mFm.getHeight();
        }
        return true;
    }

    @Override
    public void render(Graphics2D g, int cx, int y, int w, int h, long firstTs, long lastTs) {
        int cy = y + h;

        // Adjust how much extra space is left at the top
        int heightPerc = 110;

        // Draw some guide lines
        DataSet firstDs = mDatas.get(0);
        if (firstDs.getMin() < firstDs.getMax()) {
            int count = 5;
            long max = firstDs.getMax();
            long min = firstDs.getMin();
            long step = (max - min) / count;
            long value = min;
            if (min < 0 && max > 0) {
                // Make sure we have a line for 0
                value = (value / step) * step; // Ugly way of rounding ;-)
            }
            Color colGuide = new Color(0xc0c0ff);
            for (int i = 0; i <= count; i++) {
                int yv = (int) (cy - value * h * 100 / heightPerc / max);
                g.setColor(colGuide);
                g.drawLine(cx + 1, yv, cx + w, yv);
                g.setColor(Color.BLACK);
                String s = "" + value + "  ";
                g.drawString(s, cx - mFm.stringWidth(s) - 1, yv);
                value += step;
            }
        }

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        for (DataSet ds : mDatas) {
            int cnt = ds.getDataCount();
            int lastX = 0, lastY = 0;
            long max = ds.getMax();
            long min = ds.getMin();
            for (int i = 0; i < cnt; i++) {
                Data d = ds.getData(i);
                int x = cx + (int)((d.time - firstTs) * (w - 1) / duration);
                int yv = cy;
                if (max != min) {
                    yv = (int) (cy - (d.value - min) * (h - 1) * 100 / heightPerc / (max - min));
                }
                if (i > 0) {
                    g.setColor(ds.getColor(0));
                    g.drawLine(lastX, lastY, x, yv);
                }
                lastX = x;
                lastY = yv;
            }
        }
    }

    @Override
    public boolean init(Module mod) {
        for (DataSet ds : mDatas) {
            if (ds.getDataCount() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getType() {
        return TYPE_PLOT;
    }

    @Override
    public String getName() {
        return mDatas.get(0).getName();
    }

}
