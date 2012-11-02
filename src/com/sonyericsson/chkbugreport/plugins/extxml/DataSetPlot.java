package com.sonyericsson.chkbugreport.plugins.extxml;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;

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

        // Draw some guide lines
        int heightPerc = 110;
        // FIXME
//        int count = 5;
//        int step = 20;
//        Color colGuide = new Color(0xc0c0ff);
//        for (int i = 1; i <= count; i++) {
//            int value = i * step;
//            if (value > max) break;
//            int yv = cy - value * h / max;
//            g.setColor(colGuide);
//            g.drawLine(cx + 1, yv, cx + w, yv);
//            g.setColor(Color.BLACK);
//            String s = "" + value + "%  ";
//            g.drawString(s, cx - mFm.stringWidth(s) - 1, yv);
//        }

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        for (DataSet ds : mDatas) {
            int cnt = ds.getDataCount();
            int lastX = 0, lastY = 0;
            int max = ds.getMax();
            int min = ds.getMin();
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
        return true;
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
