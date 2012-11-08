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
        return true;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = x;
        int lastMode = -1;
        long duration = lastTs - firstTs;
        for (Data d : mData) {
            int cx = (int) (x + (d.time - firstTs) * w / duration);
            if (mData.getType() == Type.STATE) {
                if (lastMode == -1) {
                    lastMode = mData.getGuessFor(d.value);
                }
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
