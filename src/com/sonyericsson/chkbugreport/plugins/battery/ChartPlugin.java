package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;

import java.awt.Graphics2D;

public abstract class ChartPlugin {

    public static final int TYPE_STRIP = 1;

    public abstract boolean init(Module mod);

    public abstract int getType();

    public abstract String getName();

    public abstract void render(Graphics2D g, int x, int y, int w, int h, long firstTs, long lastTs);

}
