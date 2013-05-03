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
package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.DataSet.Type;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.util.Util;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;

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
        FontMetrics fm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics();
        int lh = fm.getHeight();
        if (lh < 18) {
            lh = 18;
        }

        // Count strips and plots (also calculate legend width)
        HashMap<Integer,Axis> axes = new HashMap<Integer,Axis>();
        for (DataSet ds : mDataSets) {
            legendWidth = Math.max(legendWidth, fm.stringWidth(ds.getName()));
            if (ds.getType() == DataSet.Type.PLOT) {
                plotCount++;
                int axisId = ds.getAxisId();
                Axis axis = axes.get(axisId);
                if (axis == null) {
                    axis = new Axis(axisId);
                    axes.put(axisId, axis);
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
        BufferedImage img = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, totalWidth, totalHeight);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, totalWidth - 1, totalHeight - 1);

        // Draw the title
        if (mTitle != null) {
            g.setColor(Color.BLACK);
            g.drawString(mTitle, 10, 10 + fm.getAscent());
        }

        int tx = mMarginLeft, ty = mMarginTop + mTitleHeight;
        int sx = mMarginLeft, sy = mMarginTop + mTitleHeight;
        int lx = mMarginLeft + mPlotWidth + mPlotLegendGap;
        if (plotCount > 0) {
            // Draw the axis
            int as = 5;
            g.setColor(Color.BLACK);
            g.drawLine(mPlotOrigoX, mPlotOrigoY, mPlotOrigoX, mPlotOrigoY - mPlotHeight);
            g.drawLine(mPlotOrigoX, mPlotOrigoY, mPlotOrigoX + mPlotWidth, mPlotOrigoY);
            g.drawLine(mPlotOrigoX - as, mPlotOrigoY - mPlotHeight + as, mPlotOrigoX, mPlotOrigoY - mPlotHeight);
            g.drawLine(mPlotOrigoX + as, mPlotOrigoY - mPlotHeight + as, mPlotOrigoX, mPlotOrigoY - mPlotHeight);
            g.drawLine(mPlotOrigoX + mPlotWidth - as, mPlotOrigoY - as, mPlotOrigoX + mPlotWidth, mPlotOrigoY);
            g.drawLine(mPlotOrigoX + mPlotWidth - as, mPlotOrigoY + as, mPlotOrigoX + mPlotWidth, mPlotOrigoY);

            // Draw the values on the axis
            int axisPos = 0;
            Axis firstPlotAxis = null;
            for (DataSet ds : mDataSets) {
                if (ds.getType() == DataSet.Type.PLOT) {
                    Axis axis = axes.get(ds.getAxisId());
                    if (firstPlotAxis == null) {
                        firstPlotAxis = axis;
                    }
                    if (axis.isDrawn()) continue;
                    axisPos += axis.render(g, fm, mPlotOrigoX - axisPos, mPlotOrigoY, mPlotWidth, mPlotHeight, axisPos == 0);
                    axis.setDrawn(true);
                }
            }

            // Render the markers
            for (Marker m : mMarkers) {
                g.setColor(m.getColor());
                if (m.getType() == Marker.Type.X) {
                    long ts = m.getValue();
                    if (ts >= firstTs && ts <= lastTs) {
                        int x = mPlotOrigoX + (int)((ts - firstTs) * (mPlotWidth - 1) / (duration));
                        g.drawLine(x, mPlotOrigoY - mPlotHeight, x, mPlotOrigoY);
                    }
                } else if (firstPlotAxis != null) {
                    long min = firstPlotAxis.getMin();
                    long max = firstPlotAxis.getMax();
                    if (min < max) {
                        int my = (int) ((m.getValue() - min) * mPlotHeight / (max - min));
                        if (my >= 0 && my < mPlotHeight) {
                            my = mPlotOrigoY - my;
                            g.drawLine(mPlotOrigoX + 1, my, mPlotOrigoX + mPlotWidth, my);
                        }
                    }
                }
            }

            // Render the plot
            int ly = mPlotOrigoY - mPlotHeight;
            for (DataSet ds : mDataSets) {
                if (ds.getType() == DataSet.Type.PLOT) {
                    Axis axis = axes.get(ds.getAxisId());
                    renderPlot(ds, axis, g, fm, mPlotOrigoX, mPlotOrigoY - mPlotHeight, mPlotWidth, mPlotHeight, firstTs, lastTs);
                    g.drawString(ds.getName(), lx, ly);
                    ly += fm.getHeight();
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
        if (!Util.renderTimeBar(img, g, tx, ty, mPlotWidth, mTimeHeight, firstTs, lastTs, true)) {
            return null;
        }

        // Draw the extra strips
        for (DataSet ds : mDataSets) {
            if (ds.getType() != DataSet.Type.PLOT) {
                renderStrip(ds, g, sx, sy, mPlotWidth, mStripHeight - 1, firstTs, lastTs);
                g.setColor(Color.BLACK);
                g.drawString(ds.getName(), lx, sy + mStripHeight - fm.getDescent() - 1);
                sy += mStripHeight;
            }
        }

        // Save the image
        try {
            if (mOutputFile != null) {
                ImageIO.write(img, "png", new File(mod.getBaseDir() + mOutputFile));
            } else if (mOutputStream != null) {
                ImageIO.write(img, "png", mOutputStream);
            } else {
                throw new RuntimeException("No output specified!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (mOutputFile != null) {
            // Finally build the report
            mChFlot = new Chapter(mod, mTitle + " - interactive chart");
            mChFlot.add(new FlotGenerator(mDataSets, axes.values(), mMarkers, mFirstTs, mLastTs));
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

    public void renderPlot(DataSet ds, Axis axis, Graphics2D g, FontMetrics fm, int cx, int y, int w, int h, long firstTs, long lastTs) {
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
                g.setColor(ds.getColor(0));
                g.drawLine(lastX, lastY, x, yv);
            }
            lastX = x;
            lastY = yv;
        }
    }

    public void renderStrip(DataSet ds, Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
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
                g.setColor(ds.getColor(0));
                if (lastY != -1) {
                    g.drawLine(lastX, lastY, cx, cy);
                }
                lastY = cy;
            } else if (ds.getType() == Type.STATE) {
                if (lastMode != -1) {
                    g.setColor(ds.getColor(lastMode));
                    g.fillRect(lastX, y, cx - lastX + 1, h);
                }
            } else if (ds.getType() == Type.EVENT) {
                g.setColor(ds.getColor(d.value));
                g.drawLine(cx, y, cx, y + h);
            }
            lastX = cx;
            lastMode = d.value;
        }
        if ((lastMode >= 0) && (ds.getType() == Type.STATE)) {
            g.setColor(ds.getColor(lastMode));
            g.fillRect(lastX, y, x + w - lastX, h);
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
