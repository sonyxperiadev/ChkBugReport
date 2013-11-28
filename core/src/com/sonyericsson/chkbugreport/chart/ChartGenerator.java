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
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.DataSet.Type;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Vector;

public class ChartGenerator {

    private Vector<ChartPlugin> mPlugins = new Vector<ChartPlugin>();
    private Vector<DataSet> mDataSets = new Vector<DataSet>();

    private String mTitle;
    private Chapter mChFlot;

    private int mMarginTop = 25;
    private int mMarginBottom = 25;
    private int mMarginLeft = 100;
    private int mMarginRight = 25;
    private int mTitleHeight = 25;
    private int mPlotOrigoX = 100;
    private int mPlotOrigoY = 250;
    private int mPlotWidth = 600;
    private int mPlotHeight = 200;
    private int mPlotLegendGap = 25;
    private int mTimeHeight = 75;
    private int mStripHeight = 25;
    private long mFirstTs;
    private long mLastTs;
    private Block mPreface;
    private Block mAppendix;
    private Vector<Marker> mMarkers = new Vector<Marker>();
    private String mOutputFile;
    private OutputStream mOutputStream;
    private HashMap<Integer, Axis> mAxes;

    public ChartGenerator(String title) {
        mTitle = title;
        mPreface = new Block();
        mAppendix = new Block();
    }

    public DocNode generate(Module mod) {
        // Initialize all plugins
        long firstTs = Long.MAX_VALUE;
        long lastTs = Long.MIN_VALUE;
        int stripCount = 0, plotCount = 0;
        int legendWidth = 0;
        for (ChartPlugin p : mPlugins) {
            if (p.init(mod, this)) {
                firstTs = Math.min(firstTs, p.getFirstTs());
                lastTs = Math.max(lastTs, p.getLastTs());
                DocNode doc = p.getPreface();
                if (doc != null) {
                    mPreface.add(doc);
                }
                doc = p.getAppendix();
                if (doc != null) {
                    mAppendix.add(doc);
                }
            }
        }

        // Note: need to adjust firstTs and lastTs if not set yet
        for (DataSet ds : mDataSets) {
            firstTs = Math.min(firstTs, ds.getFirstTs());
            lastTs = Math.max(lastTs, ds.getLastTs());
        }

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        if (duration <= 0) {
            return null;
        }
        mFirstTs = firstTs;
        mLastTs = lastTs;

        // Calculate line height
        ImageCanvas tmp = new ImageCanvas(16, 16);
        float lh = tmp.getFontHeight();
        if (lh < 18) {
            lh = 18;
        }

        // Count strips and plots (also calculate legend width)
        mAxes = new HashMap<Integer,Axis>();
        for (DataSet ds : mDataSets) {
            legendWidth = Math.max(legendWidth, (int)tmp.getStringWidth(ds.getName()));
            if (ds.getType() == DataSet.Type.PLOT) {
                plotCount++;
                int axisId = ds.getAxisId();
                Axis axis = mAxes.get(axisId);
                if (axis == null) {
                    axis = new Axis(axisId);
                    mAxes.put(axisId, axis);
                }
                axis.add(ds);
            } else {
                stripCount++;
            }
        }
        if (stripCount == 0 && plotCount == 0) {
            return null;
        }
        int totalWidth = mMarginLeft + mPlotWidth + mMarginRight;
        int totalHeight = mMarginTop + mTimeHeight + mTimeHeight + mMarginBottom;
        if (legendWidth > 0) {
            totalWidth += mPlotLegendGap + legendWidth;
        }
        totalHeight += stripCount * mStripHeight;
        if (plotCount > 0) {
            totalHeight += mPlotHeight;
        }

        // Create an empty image
        ImageCanvas img = new ImageCanvas(totalWidth, totalHeight);
        img.setColor(ImageCanvas.WHITE);
        img.fillRect(0, 0, totalWidth, totalHeight);
        img.setColor(ImageCanvas.LIGHT_GRAY);
        img.drawRect(0, 0, totalWidth - 1, totalHeight - 1);

        // Draw the title
        if (mTitle != null) {
            img.setColor(ImageCanvas.BLACK);
            img.drawString(mTitle, 10, 10 + img.getAscent());
        }

        int tx = mMarginLeft, ty = mMarginTop + mTitleHeight;
        int sx = mMarginLeft, sy = mMarginTop + mTitleHeight;
        int lx = mMarginLeft + mPlotWidth + mPlotLegendGap;
        if (plotCount > 0) {
            // Draw the axis
            int as = 5;
            img.setColor(ImageCanvas.BLACK);
            img.drawLine(mPlotOrigoX, mPlotOrigoY, mPlotOrigoX, mPlotOrigoY - mPlotHeight);
            img.drawLine(mPlotOrigoX, mPlotOrigoY, mPlotOrigoX + mPlotWidth, mPlotOrigoY);
            img.drawLine(mPlotOrigoX - as, mPlotOrigoY - mPlotHeight + as, mPlotOrigoX, mPlotOrigoY - mPlotHeight);
            img.drawLine(mPlotOrigoX + as, mPlotOrigoY - mPlotHeight + as, mPlotOrigoX, mPlotOrigoY - mPlotHeight);
            img.drawLine(mPlotOrigoX + mPlotWidth - as, mPlotOrigoY - as, mPlotOrigoX + mPlotWidth, mPlotOrigoY);
            img.drawLine(mPlotOrigoX + mPlotWidth - as, mPlotOrigoY + as, mPlotOrigoX + mPlotWidth, mPlotOrigoY);

            // Draw the values on the axis
            int axisPos = 0;
            Axis firstPlotAxis = null;
            for (DataSet ds : mDataSets) {
                if (ds.getType() == DataSet.Type.PLOT) {
                    Axis axis = mAxes.get(ds.getAxisId());
                    if (firstPlotAxis == null) {
                        firstPlotAxis = axis;
                    }
                    if (axis.isDrawn()) continue;
                    axisPos += axis.render(img, mPlotOrigoX - axisPos, mPlotOrigoY, mPlotWidth, mPlotHeight, axisPos == 0);
                    axis.setDrawn(true);
                }
            }

            // Render the markers
            for (Marker m : mMarkers) {
                img.setColor(m.getColor());
                if (m.getType() == Marker.Type.X) {
                    long ts = m.getValue();
                    if (ts >= firstTs && ts <= lastTs) {
                        int x = mPlotOrigoX + (int)((ts - firstTs) * (mPlotWidth - 1) / (duration));
                        img.drawLine(x, mPlotOrigoY - mPlotHeight, x, mPlotOrigoY);
                    }
                } else if (firstPlotAxis != null) {
                    long min = firstPlotAxis.getMin();
                    long max = firstPlotAxis.getMax();
                    if (min < max) {
                        int my = (int) ((m.getValue() - min) * mPlotHeight / (max - min));
                        if (my >= 0 && my < mPlotHeight) {
                            my = mPlotOrigoY - my;
                            img.drawLine(mPlotOrigoX + 1, my, mPlotOrigoX + mPlotWidth, my);
                        }
                    }
                }
            }

            // Render the plot
            int ly = mPlotOrigoY - mPlotHeight;
            for (DataSet ds : mDataSets) {
                if (ds.getType() == DataSet.Type.PLOT) {
                    Axis axis = mAxes.get(ds.getAxisId());
                    renderPlot(ds, axis, img, mPlotOrigoX, mPlotOrigoY - mPlotHeight, mPlotWidth, mPlotHeight, firstTs, lastTs);
                    img.drawString(ds.getName(), lx, ly);
                    ly += img.getFontHeight();
                }
            }

            // If we have a plot, the time bar follows exactly after the plot
            ty += mPlotHeight;
            // And then the strips are following
            sy = ty + mTimeHeight;
        } else {
            // If we don't have a plot, the strips come first
            // And then follows the time bar
            ty += stripCount * mStripHeight;
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, tx, ty, mPlotWidth, mTimeHeight, firstTs, lastTs, true)) {
            return null;
        }

        // Draw the extra strips
        for (DataSet ds : mDataSets) {
            if (ds.getType() != DataSet.Type.PLOT) {
                renderStrip(ds, img, sx, sy, mPlotWidth, mStripHeight - 1, firstTs, lastTs);
                img.setColor(ImageCanvas.BLACK);
                img.drawString(ds.getName(), lx, sy + mStripHeight - img.getDescent() - 1);
                sy += mStripHeight;
            }
        }

        // Save the image
        try {
            if (mOutputFile != null) {
                img.writeTo(new File(mod.getBaseDir() + mOutputFile));
            } else if (mOutputStream != null) {
                img.writeTo(mOutputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (mOutputFile != null) {
            // Finally build the report
            mChFlot = new Chapter(mod.getContext(), mTitle + " - interactive chart");
            mChFlot.add(createFlotVersion());
            mod.addExtraFile(mChFlot);
            Block ret = new Block();
            ret.add(mPreface);
            new Hint(ret).add(new Link(mChFlot.getAnchor(), "Click here for interactive version"));
            ret.add(new Img(mOutputFile));
            ret.add(mAppendix);
            return ret;
        } else {
            return null;
        }
    }

    public DocNode createFlotVersion() {
        return new FlotGenerator(mDataSets, mAxes.values(), mMarkers, mFirstTs, mLastTs);
    }

    public void renderPlot(DataSet ds, Axis axis, ImageCanvas img, int cx, int y, int w, int h, long firstTs, long lastTs) {
        int cy = y + h;
        final long max = axis.getMax();
        final long min = axis.getMin();
        if (min > max) {
            return;
        }

        // Adjust how much extra space is left at the top
        int heightPerc = 110;

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        int cnt = ds.getDataCount();
        int lastX = 0, lastY = 0;
        for (int i = 0; i < cnt; i++) {
            Data d = ds.getData(i);
            int x = cx + (int)((d.time - firstTs) * (w - 1) / duration);
            int yv = cy;
            if (max != min) {
                yv = (int) (cy - (d.value - min) * (h - 1) * 100 / heightPerc / (max - min));
            }
            if (i > 0) {
                img.setColor(ds.getColor(0));
                img.drawLine(lastX, lastY, x, yv);
            }
            lastX = x;
            lastY = yv;
        }
    }

    public void renderStrip(DataSet ds, ImageCanvas img, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = x;
        int lastY = -1;
        long lastMode = -1;
        long duration = lastTs - firstTs;
        long min = ds.getMin();
        long max = ds.getMax();
        for (Data d : ds) {
            int cx = (int) (x + (d.time - firstTs) * w / duration);
            if (ds.getType() == Type.MINIPLOT) {
                int cy = y + h;
                if (min < max) {
                    cy -= (d.value - min) * h / (max - min);
                }
                img.setColor(ds.getColor(0));
                if (lastY != -1) {
                    img.drawLine(lastX, lastY, cx, cy);
                }
                lastY = cy;
            } else if (ds.getType() == Type.STATE) {
                if (lastMode != -1) {
                    img.setColor(ds.getColor(lastMode));
                    img.fillRect(lastX, y, cx - lastX + 1, h);
                }
            } else if (ds.getType() == Type.EVENT) {
                img.setColor(ds.getColor(d.value));
                img.drawLine(cx, y, cx, y + h);
            }
            lastX = cx;
            lastMode = d.value;
        }
        if ((lastMode >= 0) && (ds.getType() == Type.STATE)) {
            img.setColor(ds.getColor(lastMode));
            img.fillRect(lastX, y, x + w - lastX, h);
        }
    }

    public void addPlugins(Vector<ChartPlugin> plugins) {
        for (ChartPlugin p : plugins) {
            addPlugin(p);
        }
    }

    public void addPlugin(ChartPlugin p) {
        mPlugins.add(p);
    }

    public void add(DataSet ds) {
        mDataSets.add(ds);
    }

    public void addPreface(DocNode node) {
        mPreface.add(node);
    }

    public void addAppendix(DocNode node) {
        mAppendix.add(node);
    }

    public void addMarker(Marker m) {
        mMarkers.add(m);
    }

    public void setOutput(String fn) {
        mOutputFile = fn;
    }

    public void setOutput(OutputStream out) {
        mOutputStream = out;
    }
}
