package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.chart.ChartPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.NetstatSample;
import com.sonyericsson.chkbugreport.plugins.logs.event.NetstatSamples;

import java.awt.Graphics2D;

public class NetstatSamplePlugin extends ChartPlugin {

    private NetstatSamples mLog;
    private String mInfoId;
    private String mLabel;

    public NetstatSamplePlugin(String infoId, String label) {
        mInfoId = infoId;
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
        mLog = (NetstatSamples) mod.getInfo(mInfoId);
        if (mLog != null) {
            if (mLog.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs) {
        int lastX = 0;
        int lastMode = -1;
        long duration = lastTs - firstTs;
        long lastTx = -1, lastRx = -1, diffTx = 0, diffRx = 0;
        long dataTx[] = new long[w];
        long dataRx[] = new long[w];
        for (NetstatSample l : mLog) {
            long tx = l.getData(NetstatSample.IDX_DEV_TX_BYTES);
            long rx = l.getData(NetstatSample.IDX_DEV_RX_BYTES);
            int cx = (int) ((l.getTs() - firstTs) * w / duration);
            cx = Math.max(0, Math.min(cx, w)); // Avoid index out of bounds

            if (lastTx < 0 || lastRx < 0) {
                // First sample
                lastTx = tx;
                lastRx = rx;
            } else if (tx + diffTx < lastTx || rx + diffRx < lastRx) {
                // History was reset
                diffTx = lastTx;
                diffRx = lastRx;
            }
            tx += diffTx;
            rx += diffRx;

            fillData(lastX, cx, lastTx, tx, dataTx);
            fillData(lastX, cx, lastRx, rx, dataRx);

            lastX = cx;
            lastTx = tx;
            lastRx = rx;
        }
        if (lastMode >= 0) {
            fillData(lastX, w - 1, lastTx, lastTx, dataTx);
            fillData(lastX, w - 1, lastRx, lastRx, dataRx);
        }

        // Calculate the derivative (or something like that)
        long maxDTx = 0, maxDRx = 0;
        long dataDTx[] = new long[w];
        long dataDRx[] = new long[w];
        for (int i = 1; i < w; i++) {
            dataDTx[i] = dataTx[i] - dataTx[i - 1];
            dataDRx[i] = dataRx[i] - dataRx[i - 1];
            if (i == 1) {
                dataDTx[0] = dataDTx[1];
                dataDRx[0] = dataDRx[1];
            }
            maxDTx = Math.max(maxDTx, dataDTx[i]);
            maxDRx = Math.max(maxDRx, dataDRx[i]);
        }

        // Finally render the graph
        long max = maxDTx + maxDRx;
        if (max == 0) {
            // No traffic (avoid division by zero)
            return;
        }

        g.translate(x, y);
        for (int i = 0; i < w; i++) {
            int ht = (int) (dataDTx[i] * h / max);
            int hr = (int) (dataDRx[i] * h / max);
            if (dataDTx[i] > 0) {
                g.setColor(COL_YELLOW);
                g.drawLine(i, h - 1, i, h - ht);
            }
            if (dataDRx[i] > 0) {
                g.setColor(COL_RED);
                g.drawLine(i, h - ht - 1, i, h - ht - hr);
            }
        }
        g.translate(-x, -y);
    }

    private void fillData(int i0, int i1, long v0, long v1, long[] data) {
        if (i0 == i1) {
            data[i1] = v1;
            return;
        }
        for (int i = i0; i <= i1; i++) {
            long v = v0 + (v1 - v0) * (i - i0) / (i1 - i0);
            if (i >= 0 && i < data.length) {
                data[i] = v;
            }
        }
    }

}
