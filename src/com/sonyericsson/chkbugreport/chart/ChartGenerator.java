package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Img;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;

public class ChartGenerator {

    private Vector<ChartPlugin> mPlugins = new Vector<ChartPlugin>();

    private String mTitle;

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

    public ChartGenerator(String title) {
        mTitle = title;
    }

    public DocNode generate(Module mod, String fn, long firstTs, long lastTs) {
        // Plot the values (size)
        long duration = (lastTs - firstTs);
        if (duration <= 0) {
            return null;
        }

        // Calculate line height
        FontMetrics fm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics();
        int lh = fm.getHeight();
        if (lh < 18) {
            lh = 18;
        }

        // Initialize all plugins
        int stripCount = 0, plotCount = 0;
        int legendWidth = 0;
        Vector<ChartPlugin> plugins = new Vector<ChartPlugin>();
        for (ChartPlugin p : mPlugins) {
            if (p.init(mod)) {
                plugins.add(p);
                legendWidth = Math.max(legendWidth, p.getLegendWidth(fm));
                if (p.getType() == ChartPlugin.TYPE_STRIP) {
                    stripCount++;
                } else if (p.getType() == ChartPlugin.TYPE_PLOT) {
                    plotCount++;
                }
            }
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

            for (ChartPlugin p : plugins) {
                if (p.getType() == ChartPlugin.TYPE_PLOT) {
                    p.render(g, mPlotOrigoX, mPlotOrigoY - mPlotHeight, mPlotWidth, mPlotHeight, firstTs, lastTs);
                    p.renderLegend(g, lx, mPlotOrigoY - mPlotHeight, legendWidth, mPlotHeight);
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
        for (ChartPlugin p : plugins) {
            if (p.getType() == ChartPlugin.TYPE_STRIP) {
                p.render(g, sx, sy, mPlotWidth, mStripHeight - 1, firstTs, lastTs);
                g.setColor(Color.BLACK);
                g.drawString(p.getName(), lx, sy + mStripHeight - fm.getDescent() - 1);
                sy += mStripHeight;
            }
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(mod.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Finally build the report
        Block ret = new Block();
        Block preface = new Block(ret);
        ret.add(new Img(fn));
        Block appendix = new Block(ret);

        for (ChartPlugin p : plugins) {
            DocNode doc = p.getPreface();
            if (doc != null) {
                preface.add(doc);
            }
            doc = p.getAppendix();
            if (doc != null) {
                appendix.add(doc);
            }
        }

        return ret;
    }

    public void addPlugins(Vector<ChartPlugin> plugins) {
        for (ChartPlugin p : plugins) {
            addPlugin(p);
        }
    }

    public void addPlugin(ChartPlugin p) {
        mPlugins.add(p);
    }

}
