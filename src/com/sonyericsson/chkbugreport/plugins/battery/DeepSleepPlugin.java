package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.DeepSleep;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.DeepSleeps;

import java.awt.Color;
import java.awt.Graphics2D;

public class DeepSleepPlugin extends ChartPlugin {

    private DeepSleeps mData;

    @Override
    public int getType() {
        return TYPE_STRIP;
    }

    @Override
    public String getName() {
        return "sleep";
    }

    @Override
    public boolean init(Module mod) {
        mData = (DeepSleeps) mod.getInfo(DeepSleeps.INFO_ID);
        if (mData != null) {
            if (mData.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = -1;
        Color colAwake = COL_RED;
        Color colSleep = COL_GREEN;
        long duration = lastTs - firstTs;
        for (DeepSleep sleep : mData) {
            // Render awake period before the sleep
            int cx = (int) (x + (sleep.getLastRealTs() - firstTs) * w / duration);
            if (lastX != -1) {
                g.setColor(colAwake);
                g.fillRect(lastX, y, cx - lastX + 1, h);
            }
            lastX = cx;
            // Render sleep period
            cx = (int) (x + (sleep.getRealTs() - firstTs) * w / duration);
            g.setColor(colSleep);
            g.fillRect(lastX, y, cx - lastX + 1, h);
            lastX = cx;
        }
        // Render final awake period
        g.setColor(colAwake);
        g.fillRect(lastX, y, x + w - lastX, h);
    }

}
