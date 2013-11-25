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
package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.ImageCanvas;

/* package */ class Axis {

    private int mId;
    private long mMin;
    private long mMax;
    private boolean mInit = false;
    private int mColor;
    private boolean mDrawn = false;

    public Axis(int id) {
        mId = id;
    }

    public void add(DataSet ds) {
        if (mInit) {
            mMin = Math.min(mMin, ds.getMin());
            mMax = Math.max(mMax, ds.getMax());
        } else {
            mMin = ds.getMin();
            mMax = ds.getMax();
            mColor = ds.getColor(0);
            mInit = true;
        }
    }

    public int getId() {
        return mId;
    }

    public long getMin() {
        return mMin;
    }

    public long getMax() {
        return mMax;
    }

    public boolean isDrawn() {
        return mDrawn;
    }

    public void setDrawn(boolean drawn) {
        mDrawn = drawn;
    }

    public int render(ImageCanvas img, int cx, int cy, int w, int h, boolean drawGuides) {
        if (mMin >= mMax || mId < 0) {
            return 0;
        }

        // Adjust how much extra space is left at the top
        int heightPerc = 110;

        // Draw some guide lines
        int ret = 0;
        int count = 5;
        long step = (mMax - mMin) / count;
        long value = mMin;
        if (step == 0) {
            step = 1;
        }
        count = (int) ((mMax - mMin) / step);
        if (mMin < 0 && mMax > 0) {
            // Make sure we have a line for 0
            value = (value / step) * step; // Ugly way of rounding ;-)
        }
        int colGuide = 0xffc0c0ff;
        for (int i = 0; i <= count; i++) {
            int yv = (int) (cy - (value - mMin) * h * 100 / heightPerc / (mMax - mMin));
            img.setColor(colGuide);
            if (drawGuides) {
                img.drawLine(cx + 1, yv, cx + w, yv);
            } else {
                img.drawLine(cx - 10, yv, cx, yv);
            }
            img.setColor(mColor);
            String s = "" + value + "  ";
            int lw = (int) (img.getStringWidth(s) + 1);
            ret = Math.max(ret, lw);
            img.drawString(s, cx - lw, yv);
            value += step;
        }

        if (!drawGuides) {
            int yv1 = (int) (cy - h * 100 / heightPerc);
            img.setColor(colGuide);
            img.drawLine(cx, cy, cx, yv1);
        }

        return ret;
    }

}
