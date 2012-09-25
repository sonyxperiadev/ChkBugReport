package com.sonyericsson.chkbugreport.chart;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.DocNode;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public abstract class ChartPlugin {

    // Some color suggestions
    public static final Color COL_GREEN     = new Color(0x4080ff80, true);
    public static final Color COL_YELLOW    = new Color(0x80ffff80, true);
    public static final Color COL_RED       = new Color(0xc0ff8080, true);

    public static final int TYPE_PLOT = 1;
    public static final int TYPE_STRIP = 2;

    public abstract boolean init(Module mod);

    public abstract int getType();

    public abstract String getName();

    public abstract void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs);

    public DocNode getPreface() {
        return null; // NOP
    }

    public DocNode getAppendix() {
        return null; // NOP
    }

    public int getLegendWidth(FontMetrics fm) {
        return getName() == null ? 0 : fm.stringWidth(getName());
    }

    public boolean renderLegend(Graphics2D g, int lx, int i, int legendWidth, int mPlotHeight) {
        return false;
    }

}
