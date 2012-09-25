package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.ConnectivityLog;
import com.sonyericsson.chkbugreport.plugins.logs.ConnectivityLogs;

import java.awt.Color;
import java.awt.Graphics2D;

public class ConnectivityChangePlugin extends ChartPlugin {

    private ConnectivityLogs mLog;
    private String mInterf;
    private String mLabel;

    public ConnectivityChangePlugin(String interf, String label) {
        mInterf = interf;
        mLabel = label;
    }

    @Override
    public int getType() {
        return TYPE_STRIP;
    }

    @Override
    public String getName() {
        return mLabel;
    }

    @Override
    public boolean init(Module mod) {
        mLog = (ConnectivityLogs) mod.getInfo(ConnectivityLogs.INFO_ID);
        if (mLog != null) {
            if (mLog.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = x;
        int lastMode = -1;
        Color cols[] = {COL_GREEN, COL_RED};
        long duration = lastTs - firstTs;
        for (ConnectivityLog l : mLog) {
            if (!mInterf.equals(l.getInterface())) continue;
            int mode = "CONNECTED".equals(l.getState()) ? 1 : 0;
            if (lastMode == -1) {
                lastMode = (mode == 0) ? 1 : 0;
            }
            int cx = (int) (x + (l.getTs() - firstTs) * w / duration);
            g.setColor(cols[lastMode]);
            g.fillRect(lastX, y, cx - lastX + 1, h);
            lastX = cx;
            lastMode = mode;
        }
        if (lastMode >= 0) {
            g.setColor(cols[lastMode]);
            g.fillRect(lastX, y, x + w - lastX, h);
        }
    }

}
