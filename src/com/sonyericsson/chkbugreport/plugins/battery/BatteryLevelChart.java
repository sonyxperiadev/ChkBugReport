package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevel;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class BatteryLevelChart extends ChartPlugin {

    private BatteryLevels mData;
    private FontMetrics mFm;

    private static final Color COLB = new Color(0xff000000, true);
    private static final Color COLM = new Color(0x8000ff00, true);
    private static final Color COLP = new Color(0x804040ff, true);

    private static final String LEGEND[] = {
            "Battery level",
            "ms/percentage",
            "percentage/hour",
    };

    private static final Color LEGEND_COL[] = { COLB, COLM, COLP };

    public BatteryLevelChart(BatteryLevels batteryLevels) {
        mData = batteryLevels;
    }

    @Override
    public int getLegendWidth(FontMetrics fm) {
        mFm = fm;
        int ret = 0;
        for (String s : LEGEND) {
            ret = Math.max(ret, fm.stringWidth(s));
        }
        return ret;
    }

    @Override
    public DocNode getPreface() {
        return new Para().add("Graph built from battery_level logs:");
    }

    @Override
    public boolean renderLegend(Graphics2D g, int x, int y, int w, int h) {
        int cnt = LEGEND.length;
        for (int i = 0; i < cnt; i++) {
            g.setColor(LEGEND_COL[i]);
            g.drawString(LEGEND[i], x, y + mFm.getAscent());
            y += mFm.getHeight();
        }
        return true;
    }

    @Override
    public void render(Graphics2D g, int cx, int y, int w, int h, long firstTs, long lastTs) {
        int cy = y + h;

        // Draw some guide lines
        int max = 110;
        int count = 5;
        int step = 20;
        Color colGuide = new Color(0xc0c0ff);
        for (int i = 1; i <= count; i++) {
            int value = i * step;
            if (value > max) break;
            int yv = cy - value * h / max;
            g.setColor(colGuide);
            g.drawLine(cx + 1, yv, cx + w, yv);
            g.setColor(Color.BLACK);
            String s = "" + value + "%  ";
            g.drawString(s, cx - mFm.stringWidth(s) - 1, yv);
        }

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        int cnt = mData.getCount();
        int lastX = 0, lastYB = 0, lastYM = 0, lastYP = 0;
        for (int i = 0; i < cnt; i++) {
            BatteryLevel bl = mData.get(i);
            int x = cx + (int)((bl.getTs() - firstTs) * (w - 1) / duration);
            int ym = cy, yp = cy, yb = cy - bl.getLevel() * (h - 1) / max;
            if (mData.getMaxMsPerPerc() != 0) {
                ym = (int) (cy - bl.getMsPerPerc() * (h - 1) * 100 / max / mData.getMaxMsPerPerc());
                ym = Math.min(ym, cy);
            }
            if (mData.getMaxPercPerHour() != 0) {
                yp = (int) (cy - bl.getPercPerHour() * (h - 1) * 100 / max / mData.getMaxPercPerHour());
                yp = Math.min(yp, cy);
            }
            if (i > 0) {
                g.setColor(COLP);
                g.drawLine(lastX, lastYP, x, yp);
                g.setColor(COLM);
                g.drawLine(lastX, lastYM, x, ym);
                g.setColor(COLB);
                g.drawLine(lastX, lastYB, x, yb);
            }
            lastX = x;
            lastYB = yb;
            lastYM = ym;
            lastYP = yp;
        }
    }

    @Override
    public boolean init(Module mod) {
        return true;
    }

    @Override
    public int getType() {
        return TYPE_PLOT;
    }

    @Override
    public String getName() {
        return "Battery level";
    }

}
