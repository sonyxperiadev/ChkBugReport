/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 *
 * This file is part of ChkBugReport.
 *
 * ChkBugReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChkBugReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChkBugReport.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sonyericsson.chkbugreport.plugins;

import com.sonyericsson.chkbugreport.Bug;
import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;

public class SurfaceFlingerPlugin extends Plugin {

    private static final String TAG = "[SurfaceFlingerPlugin]";

    public static final int OR_PORTRAIT = 0;
    public static final int OR_90       = 1;
    public static final int OR_180      = 2;
    public static final int OR_270      = 3;

    public static final int FLAG_HIDDEN      = 0x01;
    public static final int FLAG_FROZEN      = 0x02;
    public static final int FLAG_DITHER      = 0x04;
    public static final int FLAG_FILTER      = 0x08;
    public static final int FLAG_BLUR_FREEZE = 0x10;

    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 480;

    private Vector<Layer> mLayers = new Vector<Layer>();
    private Vector<Buffer> mBuffers = new Vector<Buffer>();
    private int mWidth = 0;
    private int mHeight = 0;
    private float mScale = 1.0f;

    private BufferedImage mImg;
    private Graphics2D mG;

    private int mOrientation;

    private boolean mLoaded;

    private float mTotalAllocBuff;

    private Chapter mMainCh;

    @Override
    public int getPrio() {
        return 80;
    }

    @Override
    public void load(Report rep) {
        BugReport br = (BugReport)rep;

        // Reset
        mLayers.clear();
        mBuffers.clear();
        mWidth = 0;
        mHeight = 0;
        mScale = 1.0f;
        mOrientation = OR_PORTRAIT;
        mLoaded = false;
        mTotalAllocBuff = -1.0f;
        mMainCh = new Chapter(br, "SurfaceFlinger");

        // Load data
        Section sec = br.findSection(Section.DUMP_OF_SERVICE_SURFACEFLINGER);
        if (sec == null) {
            br.printErr(TAG + "Section not found: " + Section.DUMP_OF_SERVICE_SURFACEFLINGER + " (aborting plugin)");
            return;
        }

        // Load the data
        if (!scan(br, sec)) {
            return;
        }

        // Find out the scale factor
        calcScaleFactor();

        mLoaded = true;
    }

    @Override
    public void generate(Report br) {
        if (!mLoaded) {
            // If the SF dump is missing, still try to collect relevant logs
            generateLogs(br, mMainCh);
            if (mMainCh.getChildCount() > 0) {
                br.addChapter(mMainCh);
            }
            return;
        }

        // Generate the report
        br.addChapter(mMainCh);

        if (mWidth == 0 || mHeight == 0) {
            mMainCh.addLine("<p>Something's wrong with the SurfaceFlinger data: detected screen size: " + mWidth + "," + mHeight + "! Aborting plugin!</p>");
            return;
        }

        generateLayers(br, mMainCh);
        generateBuffers(br, mMainCh);
        generateLogs(br, mMainCh);
    }

    public void generateLayers(Report br, Chapter mainCh) {
        Chapter ch = new Chapter(br, "Layers");
        mainCh.addChapter(ch);

        ch.addLine("<div class=\"sf-layer\">");
        switch (mOrientation) {
            case OR_PORTRAIT:
                ch.addLine("<p>Current orientation: PORTRAIT</p>");
                break;
            case OR_90:
                ch.addLine("<p>Current orientation: 90</p>");
                break;
            case OR_180:
                ch.addLine("<p>Current orientation: 180</p>");
                break;
            case OR_270:
                ch.addLine("<p>Current orientation: 270</p>");
                break;
            default:
                ch.addLine("<p>Current orientation: UNKNOWN</p>");
                break;
        }
        if (mOrientation != OR_PORTRAIT) {
            ch.addLine("<p>NOTE: currently if the bugreport was taken while in landscape mode, the SurfaceFlinger data cannot be handled properly!</p>");
        }

        // Generate window list (for now)
        ch.addLine("<p>Layer list:</p>");
        ch.addLine("<div class=\"winlist\">");
        for (Layer l : mLayers) {
            String vis = "vis";
            if ((0 != (l.flags & FLAG_HIDDEN))) {
                vis = "gone";
            } else if (l.alpha == 0) {
                vis = "invis";
            }
            String anchor = getLayerAnchor(l);
            String icon = "<div class=\"winlist-icon winlist-icon-item\"> </div>";
            ch.addLine("<div class=\"winlist-" + vis + "\">" + icon + Util.simplifyComponent(l.name) + " <a href=\"#" + anchor + "\">(...)</a></div>");
        }
        ch.addLine("</div>");

        // Render layer images
        ch.addLine("<p>Here is a composited image showing the visible regions of all layers (rendered twice using 50% and 100% opacity for the regions):</p>");
        ch.addLine("<div><img src=\"" + saveComposit(br, 127) + "\"/>");
        ch.addLine("<img src=\"" + saveComposit(br, 255) + "\"/></div>");
        ch.addLine("<p>And here are the layers individually:</p>");
        ch.addLine("</div>");

        for (int i = 0; i < mLayers.size(); i++) {
            Layer l = mLayers.get(i);
            int color = Util.getColor(i);
            String tr = String.format("[ %.2f %.2f ][ %.2f %.2f ]", l.tr[0][0], l.tr[0][1], l.tr[1][0], l.tr[1][1]);
            ch.addLine("<div class=\"sf-layer\">");
            ch.addLine("<a name=\"" + getLayerAnchor(l) + "\"></a>");
            ch.addLine("<p>" + l.type + ": " + l.name + ":</p>");
            ch.addLine("<table>");
            ch.addLine("  <tr>");
            ch.addLine("    <td>Transp. region:</td>");
            ch.addLine("    <td>Visible region:</td>");
            ch.addLine("    <td>Layer attributes:</td>");
            ch.addLine("  </tr>");
            ch.addLine("  <tr>");
            ch.addLine("    <td><img src=\"" + createPng(br, color, l, l.regTransScreen) + "\"/></td>");
            ch.addLine("    <td><img src=\"" + createPng(br, color, l, l.regVisScreen) + "\"/></td>");
            ch.addLine("    <td>");
            ch.addLine("      <div class=\"sf-attr\">ID: " + l.id + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Type: " + l.type + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Needs blending: " + l.needsBlending + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Needs dithering: " + l.needsDithering + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Invalidate: " + l.invalidate + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Alpha: " + l.alpha + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Flags: 0x" + Integer.toHexString(l.flags) + " (");
            if (0 != (l.flags & FLAG_HIDDEN)) { ch.addLine("HIDDEN"); }
            if (0 != (l.flags & FLAG_FROZEN)) { ch.addLine("FROZEN"); }
            if (0 != (l.flags & FLAG_DITHER)) { ch.addLine("DITHER"); }
            if (0 != (l.flags & FLAG_FILTER)) { ch.addLine("FILTER"); }
            if (0 != (l.flags & FLAG_BLUR_FREEZE)) { ch.addLine("BLUR_FREEZE"); }
            ch.addLine("      )</div>");
            ch.addLine("      <div class=\"sf-attr\">Transform: " + tr + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Identity: " + l.identity + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Format: " + getFormatName(l.format) + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Status: " + l.status + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Freezelock: " + l.freezeLock + "</div>");
            ch.addLine("      <div class=\"sf-attr\">Bypass: " + l.bypass + "</div>");
            if (l.head != -1 && l.available != -1 && l.queued != -1) {
                String colors[] = {"sf-red", "sf-yellow", "sf-green"};
                ch.addLine("      <div class=\"sf-attr\">Head: " + l.head + "</div>");
                String col = colors[Math.min(colors.length - 1, l.available)];
                ch.addLine("      <div class=\"sf-attr " + col + "\">Available: " + l.available+ "</div>");
                ch.addLine("      <div class=\"sf-attr\">Queued: " + l.queued+ "</div>");
            }
            ch.addLine("    </td>");
            ch.addLine("  </tr>");
            ch.addLine("</table>");
            ch.addLine("</div>");
        }
    }

    public void generateBuffers(Report br, Chapter mainCh) {
        if (mBuffers.size() == 0 || mTotalAllocBuff <= 0) {
            return;
        }

        Chapter ch = new Chapter(br, "Buffers");
        mainCh.addChapter(ch);

        ch.addLine("<p>Allocated buffers: (total " + String.format("%.2f", mTotalAllocBuff) + " KB):</p>");

        ch.addLine("<table class=\"sf-buffers tablesorter\">");
        ch.addLine("  <thead>");
        ch.addLine("    <tr class=\"sf-buffers-header\">");
        ch.addLine("      <th>Address</th>");
        ch.addLine("      <th>Size (KB)</th>");
        ch.addLine("      <th>W</th>");
        ch.addLine("      <th>H</th>");
        ch.addLine("      <th>Stride</th>");
        ch.addLine("      <th>Format</th>");
        ch.addLine("      <th>Usage</th>");
        ch.addLine("    </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");

        for (Buffer b : mBuffers) {
            ch.addLine("    <tr>");
            ch.addLine("      <td>" + String.format("0x%08x", b.ptr) + "</td>");
            ch.addLine("      <td>" + String.format("%.2f", b.size) + "</td>");
            ch.addLine("      <td>" + b.w + "</td>");
            ch.addLine("      <td>" + b.h + "</td>");
            ch.addLine("      <td>" + b.stride + "</td>");
            ch.addLine("      <td>" + getFormatName(b.format) + "</td>");
            ch.addLine("      <td>" + getUsage(b.usage) + "</td>");
            ch.addLine("    </tr>");
        }
        ch.addLine("  </tbody>");
        ch.addLine("</table>");
    }

    public void generateLogs(Report br, Chapter mainCh) {
        generateLogs(br, mainCh, "SystemLogPlugin");
        generateLogs(br, mainCh, "MainLogPlugin");
    }


    private void generateLogs(Report br, Chapter mainCh, String pluginName) {
        LogPlugin plugin = (LogPlugin)br.getPlugin(pluginName);
        if (plugin == null) return;
        Chapter ch = null;
        int cnt = plugin.getParsedLineCount();
        for (int i = 0; i < cnt; i++) {
            LogLine sl = plugin.getParsedLine(i);
            if (!sl.ok) continue;
            boolean interesting = false;
            if (sl.tag.equals("Surface") || sl.tag.equals("SharedBufferStack")) {
                interesting = true;
            }
            if (sl.tag.equals("GraphicBufferAllocator")) {
                interesting = true;
            }
            if (sl.tag.equals("SurfaceFlinger") || sl.tag.endsWith(".gralloc")) {
                interesting = true;
            }
            if (sl.tag.equals("Adreno200-EGL") || sl.tag.endsWith("libEGL")) {
                interesting = true;
            }
            if (sl.tag.equals("kernel") && sl.msg.contains(" kgsl:")) {
                interesting = true;
            }
            if (interesting) {
                if (ch == null) {
                    ch = new Chapter(br, "Related logs from " + pluginName);
                    mainCh.addChapter(ch);
                    ch.addLine("<p>Related logs from " + pluginName);
                    ch.addLine("<div class=\"log\">");
                }
                ch.addLine(sl.html);
            }
        }
        if (ch != null) {
            ch.addLine("</div>");
        }
    }

    private String getUsage(int usage) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("0x%08x: ", usage));
        switch (usage & 0xf) {
            case 0: sb.append("SW_READ_NEVER "); break;
            case 2: sb.append("SW_READ_RARELY "); break;
            case 3: sb.append("SW_READ_OFTEN "); break;
        }
        switch ((usage >> 4) & 0xf) {
            case 0: sb.append("SW_WRITE_NEVER "); break;
            case 2: sb.append("SW_WRITE_RARELY "); break;
            case 3: sb.append("SW_WRITE_OFTEN "); break;
        }
        if (0 != (usage & 0x100)) {
            sb.append("HW_TEXTURE ");
        }
        if (0 != (usage & 0x200)) {
            sb.append("HW_RENDER ");
        }
        if (0 != (usage & 0xC00)) {
            sb.append("HW_2D ");
        }
        if (0 != (usage & 0x2000)) {
            sb.append("HW_PMEM ");
        }
        if (0 != (usage & 0x1000)) {
            sb.append("HW_FB ");
        }
        return sb.toString();
    }

    private String getLayerAnchor(Layer l) {
        return "sf_layer_" + l.identity;
    }

    private String getFormatName(int format) {
        switch (format) {
            case 0: return "EMPTY(0)";
            case 1: return "RGBA8888(1)";
            case 2: return "RGBX8888(2)";
            case 3: return "RGB888(3)";
            case 4: return "RGB565(4)";
            case 5: return "BGRA8888(5)";
            case 6: return "RGBA5551(6)";
            case 7: return "RGBA4444(7)";
            default: return "UNKNOWN(" + format + ")";
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    private String saveComposit(Report br, int opacity) {
        beginPng();

        // render the visible layers
        for (int i = 0; i < mLayers.size(); i++) {
            Layer l = mLayers.get(i);
            if (0 != (l.flags & FLAG_HIDDEN)) continue;
            int color = Util.getColor(i);
            Region reg = l.regVisScreen;
            for (int j = 0; j < reg.getCount(); j++) {
                Rect r = reg.get(j);
                renderRect(r, (opacity << 24) | color);
            }
        }

        // Render the orientation arrow
        renderOrientationArrow();

        // Save the image
        String fn = br.getRelDataDir() + "sf_layer_all_" + opacity + ".png";
        endPng(br.getBaseDir() + fn);
        return fn;
    }

    private void renderOrientationArrow() {
        Stroke stroke = mG.getStroke();
        mG.setStroke(new BasicStroke(3.0f));
        mG.setColor(Color.BLACK);
        switch (mOrientation) {
            case OR_PORTRAIT:
                mG.drawLine(20, 10, 10, 20);
                mG.drawLine(20, 10, 30, 20);
                mG.drawLine(20, 10, 20, 40);
                break;
            case OR_90:
                mG.drawLine(40, 20, 30, 10);
                mG.drawLine(40, 20, 30, 30);
                mG.drawLine(40, 20, 10, 20);
                break;
            case OR_270:
                mG.drawLine(10, 20, 20, 10);
                mG.drawLine(10, 20, 20, 30);
                mG.drawLine(10, 20, 40, 20);
                break;
        }
        mG.setStroke(stroke);
    }

    private String createPng(Report br, int color, Layer l, Region reg) {
        beginPng();

        // Now render the layer area with a light color
        renderRect(l.rect, 0x40000000 | color);

        // And now render each region
        for (int i = 0; i < reg.getCount(); i++) {
            Rect r = reg.get(i);
            renderRect(r, 0xff000000 | color);
        }

        // Save the image
        String fn = br.getRelDataDir() + "sf_layer_" + Integer.toHexString(l.hashCode()) + "_" + Integer.toHexString(reg.hashCode()) + ".png";
        endPng(br.getBaseDir() + fn);
        return fn;
    }

    private void renderRect(Rect rect, int argb) {
        int x1 = (int)(rect.x * mScale);
        int y1 = (int)(rect.y * mScale);
        int x2 = (int)((rect.x + rect.w) * mScale);
        int y2 = (int)((rect.y + rect.h) * mScale);
        mG.setColor(new Color(argb, true));
        mG.fillRect(x1, y1, x2 - x1, y2 - y1);
        argb = (argb & 0xff000000) + ((argb & 0x00fefefe) / 2);
        mG.setColor(new Color(argb, true));
        mG.drawRect(x1, y1, x2 - x1 - 1, y2 - y1 - 1);
    }

    private void beginPng() {
        // Create image
        mImg = new BufferedImage(mWidth, mHeight, BufferedImage.TYPE_INT_RGB);
        mG = (Graphics2D)mImg.getGraphics();

        // Fill the background with a non-white color and draw a border to visualize the screen
        mG.setColor(new Color(0xdddddd));
        mG.fillRect(0, 0, mWidth, mHeight);
        mG.setColor(new Color(0xbbbbbb));
        mG.drawRect(0, 0, mWidth - 1, mHeight - 1);
    }

    private void endPng(String fn) {
        try {
            ImageIO.write(mImg, "png", new File(fn));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calcScaleFactor() {
        float maxAR = (float)MAX_WIDTH / MAX_HEIGHT;
        float ar = (float)mWidth / mHeight;
        if (ar >= maxAR) {
            if (mWidth > MAX_WIDTH) {
                mScale = (float)MAX_WIDTH / mWidth;
                mWidth = MAX_WIDTH;
                mHeight = (int)(mHeight * mScale);
            }
        } else {
            if (mHeight > MAX_HEIGHT) {
                mScale = (float)MAX_HEIGHT / mHeight;
                mHeight = MAX_HEIGHT;
                mWidth = (int)(mWidth * mScale);
            }
        }
    }

    private boolean scan(BugReport br, Section sec) {
        int count = 0;
        int expectedCount = 0;
        int line = 0;

        // Read the number of layers
        String buff = sec.getLine(0);
        if (buff.startsWith("Visible layers")) {
            int idx0 = buff.indexOf('=');
            int idx1 = buff.indexOf(')');
            if (idx0 < 0 || idx1 < 0 || idx0 >= idx1 + 2) {
                br.printErr(TAG + "Error parsing: cannot find 'count' in first line!");
                return false;
            }
            expectedCount = Integer.parseInt(buff.substring(idx0 + 2, idx1));
            line++;
        } else if (buff.startsWith("+ Layer")) {
            // NOP
        } else {
            br.printErr(TAG + "Error parsing: cannot recognize section!");
            return false;
        }

        // Read each layer
        while (line < sec.getLineCount()) {
            // Parse first line (+ LayerType id)
            buff = sec.getLine(line++);
            if (!buff.startsWith("+ Layer")) {
                line--; // rewind
                // No more layers
                break;
            }

            Layer layer = new Layer();
            mLayers.add(layer);
            count++;

            String fields[] = buff.split(" ");
            layer.type = fields[1];
            layer.id = fields[2];
            if (br.getAndroidVersionSdk() >= BugReport.SDK_ICS) {
                int idx0 = buff.indexOf('(');
                int idx1 = buff.indexOf(')');
                if (idx0 > 0 && idx1 > idx0) {
                    layer.name = buff.substring(idx0 + 1, idx1);
                }
            }

            // Parse the remaining of the lines
            while (true) {
                buff = sec.getLine(line++);
                if (buff.startsWith("      name=")) {
                    layer.name = buff.substring(11);
                } else if (buff.startsWith("            ")) {
                    // skip it for now, these lines were introduces in ICS
                } else if (buff.startsWith("      ")) {
                    readAttributes(br, layer, buff);
                } else if (buff.startsWith("  Region ")) {
                    line = readRegion(br, layer, buff, sec, line);
                } else {
                    line--; // rewind
                    break;
                }
            }

            // Detect some errors (like not-available buffers)
            if (0 == (layer.flags & FLAG_HIDDEN)) {
                if (layer.available == 0) {
                    Bug bug = new Bug(Bug.PRIO_SF_NO_BUFF, 0, "No available buffer in SurfaceFlinger for layer " + layer.name + "!");
                    bug.addLine("<p>The layer " + layer.name + " (with identify " + layer.identity + ") does not have");
                    bug.addLine("any available buffers. This could be a false-alarm (if the bugreport was happened to be taken at a critical");
                    bug.addLine("point), but more probably it will result in either one window or the whole screen to freeze.");
                    bug.addLine("(in which case \"waitForCondition\" messages will be printed to the system log.)</p>");
                    bug.addLine("<p><a href=\"" + br.createLinkTo(mMainCh, getLayerAnchor(layer)) + "\">(Link to layer info)</a></p>");
                    br.addBug(bug);
                }
            }

        }

        if (expectedCount > 0 && expectedCount != count) {
            br.printErr(TAG + "Error parsing: count mismatch! Expected: " + expectedCount + ", found: " + count);
            return false;
        }

        // Search for the line containing "orientation="
        String key = "orientation=";
        mOrientation = 0;
        while (line < sec.getLineCount()) {
            buff = sec.getLine(line++);
            int idx = buff.indexOf(key);
            if (idx > 0) {
                char c = buff.charAt(idx + key.length());
                if (c >= '0' && c <= '3') {
                    mOrientation = c - '0';
                }
                break;
            }
        }

        // Search for the line "Allocated buffers"
        key = "Allocated buffers:";
        boolean found = false;
        while (line < sec.getLineCount()) {
            buff = sec.getLine(line++);
            if (key.equals(buff)) {
                found = true;
                break;
            }
        }
        if (found) {
            key = "Total allocated";
            mTotalAllocBuff = -1.0f;
            try {
                while (line < sec.getLineCount()) {
                    buff = sec.getLine(line++);
                    // Check end of list
                    if (buff.startsWith(key)) {
                        buff = buff.substring(buff.indexOf(':') + 2);
                        int idx = buff.indexOf(' ');
                        if (idx > 0) {
                            mTotalAllocBuff = Float.parseFloat(buff.substring(0, idx));
                        }
                        break;
                    }
                    // Parse buffer
                    Buffer buffer = new Buffer();
                    buffer.ptr = Util.parseHex(buff, 0, 10);
                    buffer.size = Util.parseFloat(buff, 12, 19);
                    buffer.w = Util.parseInt(buff, 26, 30);
                    if (buff.charAt(38) == '|') {
                        // pre 2.3
                        buffer.stride = buffer.w;
                        buffer.h = Util.parseInt(buff, 33, 37);
                        int idx = buff.indexOf('|', 40);
                        buffer.format = buff.charAt(idx - 2) - '0';
                        buffer.usage = Util.parseHex(buff, idx + 2, idx + 12);
                    } else {
                        // 2.3
                        buffer.stride = Util.parseInt(buff, 32, 36);
                        buffer.h = Util.parseInt(buff, 40, 44);
                        int idx = buff.indexOf('|', 46);
                        buffer.format = buff.charAt(idx - 2) - '0';
                        buffer.usage = Util.parseHex(buff, idx + 2, idx + 12);
                    }
                    mBuffers.add(buffer);
                }
            } catch (NumberFormatException nfe) {
                // Ignore it for now
                br.printErr(TAG + "Error parsing buffer list: " + nfe);
            } catch (StringIndexOutOfBoundsException e) {
                // Ignore it for now
                br.printErr(TAG + "Error parsing buffer list: " + e);
            }
        }

        // Adjust the layer size based on the transform
        for (Layer layer : mLayers) {
            if (0 == (layer.flags & FLAG_HIDDEN)) {
                if (layer.tr[0][0] != 1.0f || layer.tr[1][1] != 1.0f) {
                    // Assume the TR and BL values are 0
                    layer.rect.w *= layer.tr[0][0];
                    layer.rect.h *= layer.tr[1][1];
                }
            }
        }

        // Adjust the position based on orientation
        for (Layer layer : mLayers) {
            if (0 == (layer.flags & FLAG_HIDDEN)) {
                if (mOrientation == OR_90) {
                    // swap w & h
                    int tmp = layer.rect.w;
                    layer.rect.w = layer.rect.h;
                    layer.rect.h = tmp;

                    // adjust x
                    layer.rect.x -= layer.rect.w;
                } else if (mOrientation == OR_270) {
                    // swap w & h
                    int tmp = layer.rect.w;
                    layer.rect.w = layer.rect.h;
                    layer.rect.h = tmp;

                    // adjust y
                    layer.rect.y -= layer.rect.h;
                }
            }
        }

        // Update the detected screen size
        for (Layer layer : mLayers) {
            if (0 == (layer.flags & FLAG_HIDDEN)) {
                mWidth = Math.max(mWidth, layer.rect.w + layer.rect.x);
                mHeight = Math.max(mHeight, layer.rect.h + layer.rect.y);
            }
        }

        return true;
    }

    private int readRegion(Report br, Layer layer, String buff, Section sec, int line) {
        Region reg = null;
        String fields[] = buff.substring(2).split(" ");
        String type = fields[1];
        String s = fields[3];
        if ("transparentRegion".equals(type)) {
            reg = layer.regTrans;
        } else  if ("transparentRegionScreen".equals(type)) {
            reg = layer.regTransScreen;
        } else  if ("visibleRegionScreen".equals(type)) {
            reg = layer.regVisScreen;
        } else {
            br.printErr("Warning: Unknown region: " + type);
            reg = new Region(type);
            layer.regExtra.add(reg);
        }
        int idx0 = s.indexOf('=');
        int idx1 = s.indexOf(')');
        int count = Integer.parseInt(s.substring(idx0 + 1, idx1));
        for (int i = 0; i < count; i++) {
            buff = sec.getLine(line++);
            buff = buff.replace('[', ' ').replace(']', ' ');
            String xyxy[] = buff.split(",");
            int x0 = Integer.parseInt(Util.strip(xyxy[0]));
            int y0 = Integer.parseInt(Util.strip(xyxy[1]));
            int x1 = Integer.parseInt(Util.strip(xyxy[2]));
            int y1 = Integer.parseInt(Util.strip(xyxy[3]));
            reg.add(new Rect(x0, y0, x1 - x0, y1 - y0));
        }
        return line;
    }

    private void readAttributes(BugReport br, Layer layer, String buff) {
        Tokenizer tok = new Tokenizer(buff);
        while (tok.hasMoreTokens()) {
            String key = tok.nextToken();
            if ("z".equals(key)) {
                String v = tok.nextToken();
                int idx = v.indexOf('/');
                if (idx > 0) {
                    v = v.substring(0, idx);
                }
                layer.z = Integer.parseInt(v);
            } else if ("pos".equals(key)) {
                layer.rect.x = tok.nextInt();
                layer.rect.y = tok.nextInt();
            } else if ("size".equals(key)) {
                layer.rect.w = tok.nextInt();
                layer.rect.h = tok.nextInt();
            } else if ("needsBlending".equals(key)) {
                layer.needsBlending = tok.nextInt();
            } else if ("needsDithering".equals(key)) {
                layer.needsDithering = tok.nextInt();
            } else if ("invalidate".equals(key)) {
                layer.invalidate = tok.nextInt();
            } else if ("alpha".equals(key)) {
                layer.alpha = tok.nextInt();
            } else if ("flags".equals(key)) {
                layer.flags = tok.nextInt();
            } else if ("tr".equals(key)) {
                layer.tr[0][0] = tok.nextFloat();
                layer.tr[0][1] = tok.nextFloat();
                layer.tr[1][0] = tok.nextFloat();
                layer.tr[1][1] = tok.nextFloat();
            } else if ("client".equals(key)) {
                layer.client = tok.nextInt();
            } else if ("identity".equals(key)) {
                layer.identity = tok.nextInt();
            } else if ("status".equals(key)) {
                layer.status = tok.nextInt();
            } else if ("format".equals(key)) {
                layer.format = tok.nextInt();
                if (br.getAndroidVersionSdk() < BugReport.SDK_ICS) {
                    tok.nextToken(); // skip: empty
                    tok.nextToken(); // skip: 480x854:480
                    tok.nextToken(); // skip: empty
                    tok.nextToken(); // skip: 480x854:480
                }
            } else if ("freezeLock".equals(key)) {
                layer.freezeLock = tok.nextInt();
            } else if ("bypass".equals(key)) {
                layer.bypass = tok.nextInt();
            } else if ("dq-q-time".equals(key)) {
                tok.nextToken(); // skip
            } else if ("".equals(key)) {
                // This means the line starts with [
                // simply only this token
            } else if ("head".equals(key)) {
                layer.head = tok.nextInt();
            } else if ("available".equals(key)) {
                layer.available = tok.nextInt();
            } else if ("queued".equals(key)) {
                layer.queued = tok.nextInt();
            } else if ("activeBuffer".equals(key)) {
                tok.nextToken(); // eg: 960x 854: 960
                tok.nextToken(); // eg: 2
            } else {
                // Unknown, ignore value
                tok.nextToken();
            }
        }
    }

    static class Tokenizer extends StringTokenizer {

        public Tokenizer(String buff) {
            super(buff, "=,()[]");
        }

        @Override
        public String nextToken() {
            return Util.strip(super.nextToken());
        }

        public int nextInt() {
            String s = nextToken();
            if (s.startsWith("0x")) {
                return Integer.parseInt(s.substring(2), 16);
            }
            return Integer.parseInt(s);
        }

        public float nextFloat() {
            return Float.parseFloat(nextToken());
        }
    }

    static class Rect {
        public int x, y, w, h;
        public Rect() {
        }
        public Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    static class Region {
        private Vector<Rect> mRects = new Vector<Rect>();
        private String mName;
        public Region(String name) {
            mName = name;
        }
        public int getCount() { return mRects.size(); }
        public Rect get(int idx) { return mRects.get(idx); }
        public void add(Rect rect) { mRects.add(rect); }
        public String getName() {
            return mName;
        }
    }

    static class Layer {
        public int bypass;
        public int freezeLock;
        public int format;
        public int status;
        public int identity;
        public int client;
        public int flags;
        public int alpha;
        public int invalidate;
        public int needsDithering;
        public int needsBlending;
        public int z;
        public String type;
        public String id;
        public Rect rect = new Rect();
        public float tr[][] = new float[2][2];
        public String name;
        public int queued = -1;
        public int available = -1;
        public int head = -1;
        public Region regTrans = new Region("transparentRegion");
        public Region regTransScreen = new Region("transparentRegionScreen");
        public Region regVisScreen = new Region("visibleRegionScreen");
        public Vector<Region> regExtra = new Vector<Region>();
    }

    static class Buffer {
        public int ptr;
        public int w, h, stride;
        public int format, usage;
        public float size;
    }

}
