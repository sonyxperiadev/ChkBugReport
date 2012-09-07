package com.sonyericsson.chkbugreport.plugins.battery;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevel;
import com.sonyericsson.chkbugreport.plugins.logs.event.BatteryLevels;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;

public class BatteryLevelGenerator {

    private BatteryLevels mData;
    private Vector<ChartPlugin> mPlugins = new Vector<ChartPlugin>();

    public BatteryLevelGenerator(BatteryLevels batteryLevels) {
        mData = batteryLevels;
    }

    public void generate(Module br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Battery level");
        mainCh.addChapter(ch);

        generateGraph(br, ch);
    }

    private boolean generateGraph(Module br, Chapter ch) {
        String fn = "eventlog_batterylevel_graph.png";
        int w = 800;
        int h = 350;
        int cx = 100;
        int cy = 250;
        int gw = 600;
        int gh = 200;
        int nx = 750;
        int ny = 10;
        int tx = cx;
        int ty = cy;
        int th = 75;
        int sh = 25;

        long firstTs = mData.getFirstTs();
        long lastTs = mData.getLastTs();

        // Initialize all plugins
        int stripCount = 0;
        Vector<ChartPlugin> plugins = new Vector<ChartPlugin>();
        for (ChartPlugin p : mPlugins) {
            if (p.init(br)) {
                plugins.add(p);
                if (p.getType() == ChartPlugin.TYPE_STRIP) {
                    stripCount++;
                }
            }
        }
        h += stripCount * sh;

        Color colB = new Color(0xff000000, true);
        Color colM = new Color(0x8000ff00, true);
        Color colP = new Color(0x804040ff, true);

        // Need a font metrics before the actual image is created :-(
        FontMetrics fm = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB).getGraphics().getFontMetrics();

        // Allocate colors for each value
        // Also count the number of distinct values
        HashMap<String, Color> mColors = new HashMap<String, Color>();
        int lh = fm.getHeight();
        if (lh < 18) {
            lh = 18;
        }

        // Create an empty image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, w - 1, h - 1);

        // Draw the legend
        for (Entry<String, Color> entry : mColors.entrySet()) {
            String name = entry.getKey();
            Color color = entry.getValue();
            g.setColor(color);
            g.fillRect(nx, ny + (lh - 16) / 2, 16, 16);
            g.drawRect(nx, ny + (lh - 16) / 2, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString(name, nx + 32, ny + fm.getAscent());
            ny += lh;
        }

        // Draw the axis
        int as = 5;
        g.setColor(Color.BLACK);
        g.drawLine(cx, cy, cx, cy - gh);
        g.drawLine(cx, cy, cx + gw, cy);
        g.drawLine(cx - as, cy - gh + as, cx, cy - gh);
        g.drawLine(cx + as, cy - gh + as, cx, cy - gh);
        g.drawLine(cx + gw - as, cy - as, cx + gw, cy);
        g.drawLine(cx + gw - as, cy + as, cx + gw, cy);

        // Draw the title
        g.setColor(colB);
        g.drawString("Battery level", 10, 10 + fm.getAscent());
        g.setColor(colM);
        g.drawString("ms/percentage", 310, 10 + fm.getAscent());
        g.setColor(colP);
        g.drawString("percentage/hour", 510, 10 + fm.getAscent());

        // Draw some guide lines
        int max = 110;
        int count = 5;
        int step = 20;
        Color colGuide = new Color(0xc0c0ff);
        for (int i = 1; i <= count; i++) {
            int value = i * step;
            if (value > max) break;
            int yv = cy - value * gh / max;
            g.setColor(colGuide);
            g.drawLine(cx + 1, yv, cx + gw, yv);
            g.setColor(Color.BLACK);
            String s = "" + value + "%  ";
            g.drawString(s, cx - fm.stringWidth(s) - 1, yv);
        }

        // Plot the values (size)
        long duration = (lastTs - firstTs);
        if (duration <= 0) return false;
        int cnt = mData.getCount();
        int lastX = 0, lastYB = 0, lastYM = 0, lastYP = 0;
        for (int i = 0; i < cnt; i++) {
            BatteryLevel bl = mData.get(i);
            int x = cx + (int)((bl.getTs() - firstTs) * (gw - 1) / duration);
            int yb = cy - bl.getLevel() * (gh - 1) / max;
            int ym = (int) (cy - bl.getMsPerPerc() * (gh - 1) * 100 / max / mData.getMaxMsPerPerc());
            ym = Math.min(ym, cy);
            int yp = (int) (cy - bl.getPercPerHour() * (gh - 1) * 100 / max / mData.getMaxPercPerHour());
            yp = Math.min(yp, cy);
            if (i > 0) {
                g.setColor(colP);
                g.drawLine(lastX, lastYP, x, yp);
                g.setColor(colM);
                g.drawLine(lastX, lastYM, x, ym);
                g.setColor(colB);
                g.drawLine(lastX, lastYB, x, yb);
            }
            lastX = x;
            lastYB = yb;
            lastYM = ym;
            lastYP = yp;
        }

        // Draw the time line
        if (!Util.renderTimeBar(img, g, tx, ty, gw, th, firstTs, lastTs, true)) {
            return false;
        }

        // Draw the extra strips
        int sy = ty + th;
        for (ChartPlugin p : plugins) {
            if (p.getType() == ChartPlugin.TYPE_STRIP) {
                p.render(g, cx, sy, gw, sh - 1, firstTs, lastTs);
                g.setColor(Color.BLACK);
                g.drawString(p.getName(), cx  + gw + 5, sy + sh - fm.getDescent() - 1);
                sy += sh;
            }
        }

        // Save the image
        try {
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        // Finally build the report
        Block ret = new Block(ch);
        new Para(ret).add("Graph built from battery_level logs:");
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

        return true;
    }

    public void addPlugins(Vector<ChartPlugin> plugins) {
        for (ChartPlugin p : plugins) {
            addPlugin(p);
        }
    }

    private void addPlugin(ChartPlugin p) {
        mPlugins.add(p);
    }

}
