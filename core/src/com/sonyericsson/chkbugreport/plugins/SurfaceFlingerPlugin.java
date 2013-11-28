/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.AndroidVersions;
import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Icon;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogPlugin;
import com.sonyericsson.chkbugreport.util.ColorUtil;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private ImageCanvas mImg;

    private int mOrientation;

    private boolean mLoaded;

    private float mTotalAllocBuff;

    private Chapter mMainCh;

    private HashSet<String> mUnknownAttrs = new HashSet<String>();

    @Override
    public int getPrio() {
        return 80;
    }

    @Override
    public void reset() {
        // Reset
        mLayers.clear();
        mBuffers.clear();
        mWidth = 0;
        mHeight = 0;
        mScale = 1.0f;
        mOrientation = OR_PORTRAIT;
        mLoaded = false;
        mTotalAllocBuff = -1.0f;
        mMainCh = null;
        mUnknownAttrs.clear();
    }

    @Override
    public void load(Module rep) {
        BugReportModule br = (BugReportModule)rep;

        // Load data
        Section sec = br.findSection(Section.DUMP_OF_SERVICE_SURFACEFLINGER);
        if (sec == null) {
            br.printErr(3, TAG + "Section not found: " + Section.DUMP_OF_SERVICE_SURFACEFLINGER + " (aborting plugin)");
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
    public void generate(Module br) {
        mMainCh = br.findOrCreateChapter("SurfaceFlinger");
        if (!mLoaded) {
            // If the SF dump is missing, still try to collect relevant logs
            generateLogs(br, mMainCh);
            return;
        }

        // Generate the report
        if (mWidth == 0 || mHeight == 0) {
            new Para(mMainCh).add("Something's wrong with the SurfaceFlinger data: detected screen size: " + mWidth + "," + mHeight + "! Aborting plugin!");
            return;
        }

        generateLayers(br, mMainCh);
        generateBuffers(br, mMainCh);
        generateLogs(br, mMainCh);
    }

    public void generateLayers(Module br, Chapter mainCh) {
        Chapter ch = new Chapter(br.getContext(), "Layers");
        mainCh.addChapter(ch);
        DocNode out = new Block(ch).addStyle("sf-layer");
        switch (mOrientation) {
            case OR_PORTRAIT:
                new Para(out).add("Current orientation: PORTRAIT");
                break;
            case OR_90:
                new Para(out).add("Current orientation: 90");
                break;
            case OR_180:
                new Para(out).add("Current orientation: 180");
                break;
            case OR_270:
                new Para(out).add("Current orientation: 270");
                break;
            default:
                new Para(out).add("Current orientation: UNKNOWN");
                break;
        }
        if (mOrientation != OR_PORTRAIT) {
            new Para(out).add("NOTE: currently if the bugreport was taken while in landscape mode, the SurfaceFlinger data cannot be handled properly!");
        }

        // Generate window list (for now)
        new Para(out).add("Layer list:");
        DocNode list = new Block(out).addStyle("winlist");
        int count = mLayers.size();
        for (int i = 0; i < count; i++) {
            DocNode item = new Block(list);
            Layer l = mLayers.get(i);
            l.anchor = new Anchor("sf_layer_" + l.identity);
            String vis = "vis";
            if ((0 != (l.flags & FLAG_HIDDEN))) {
                vis = "gone";
            } else if (l.alpha == 0) {
                vis = "invis";
            }
            String hint = "|";
            if (i == 0) {
                hint = "bottom";
            } else if (i == count - 2) {
                hint = "v";
            } else if (i == count - 1) {
                hint  = "top";
            }
            new Block(item).addStyle("winlist-hint").add(hint);
            new Block(item).addStyle("winlist-" + vis);
            item.add(new Icon(Icon.TYPE_SMALL, "item"));
            item.add(Util.simplifyComponent(l.name));
            item.add(new Link(l.anchor, "(...)"));
        }

        // Render layer images
        new Para(out).add("Here is a composited image showing the visible regions of all layers (rendered twice using 50% and 100% opacity for the regions):");
        new Block(out)
            .add(new Img(saveComposit(br, 127)))
            .add(new Img(saveComposit(br, 255)));
        new Para(out).add("And here are the layers individually:");

        for (int i = 0; i < count; i++) {
            Layer l = mLayers.get(i);
            int color = ColorUtil.getColor(i);
            String tr = String.format("[ %.2f %.2f ][ %.2f %.2f ]", l.tr[0][0], l.tr[0][1], l.tr[1][0], l.tr[1][1]);
            DocNode item = new Block(ch).addStyle("sf-layer");
            item.add(l.anchor);
            new Para(item).add(l.type + ": " + l.name + ":");
            DocNode attrs = new DocNode();
            Table t = new Table(Table.FLAG_NONE, item);
            t.addColumn("Transp. region:", Table.FLAG_NONE);
            t.addColumn("Visible region:", Table.FLAG_NONE);
            t.addColumn("Layer attributes:", Table.FLAG_NONE);
            t.begin();
            t.addData(new Img(createPng(br, color, l, l.regTransScreen)));
            t.addData(new Img(createPng(br, color, l, l.regVisScreen)));
            t.addData(attrs);
            new Block(attrs).addStyle("sf-attr").add("ID: " + l.id);
            new Block(attrs).addStyle("sf-attr").add("Type: " + l.type);
            new Block(attrs).addStyle("sf-attr").add("Needs blending: " + l.needsBlending);
            new Block(attrs).addStyle("sf-attr").add("Needs dithering: " + l.needsDithering);
            new Block(attrs).addStyle("sf-attr").add("Invalidate: " + l.invalidate);
            new Block(attrs).addStyle("sf-attr").add("Alpha: " + l.alpha);
            DocNode flags = new DocNode();
            new Block(attrs).addStyle("sf-attr").add(flags);
            flags.add("Flags: 0x" + Integer.toHexString(l.flags) + " (");
            if (0 != (l.flags & FLAG_HIDDEN)) { flags.add("HIDDEN"); }
            if (0 != (l.flags & FLAG_FROZEN)) { flags.add("FROZEN"); }
            if (0 != (l.flags & FLAG_DITHER)) { flags.add("DITHER"); }
            if (0 != (l.flags & FLAG_FILTER)) { flags.add("FILTER"); }
            if (0 != (l.flags & FLAG_BLUR_FREEZE)) { flags.add("BLUR_FREEZE"); }
            flags.add(")");
            new Block(attrs).addStyle("sf-attr").add("Transform: " + tr);
            new Block(attrs).addStyle("sf-attr").add("Identity: " + l.identity);
            new Block(attrs).addStyle("sf-attr").add("Format: " + getFormatName(l.format));
            new Block(attrs).addStyle("sf-attr").add("Status: " + l.status);
            new Block(attrs).addStyle("sf-attr").add("Freezelock: " + l.freezeLock);
            new Block(attrs).addStyle("sf-attr").add("Bypass: " + l.bypass);
            if (l.head != -1 && l.available != -1 && l.queued != -1) {
                String colors[] = {"sf-red", "sf-yellow", "sf-green"};
                new Block(attrs).addStyle("sf-attr").add("Head: " + l.head + "</div>");
                String col = colors[Math.min(colors.length - 1, l.available)];
                new Block(attrs).addStyle("sf-attr").addStyle(col).add("Available: " + l.available);
                new Block(attrs).addStyle("sf-attr").add("Queued: " + l.queued+ "</div>");
            }
        }
    }

    public void generateBuffers(Module br, Chapter mainCh) {
        if (mBuffers.size() == 0 || mTotalAllocBuff <= 0) {
            return;
        }

        Chapter ch = new Chapter(br.getContext(), "Buffers");
        mainCh.addChapter(ch);

        new Para(ch).add("Allocated buffers: (total " + String.format("%.2f", mTotalAllocBuff) + " KB):");
        Table t = new Table(Table.FLAG_SORT, ch);
        t.setCSVOutput(br, "sf_buffers");
        t.setTableName(br, "sf_buffers");
        t.addColumn("Address", Table.FLAG_NONE, "addr varchar");
        t.addColumn("Size (KB)", Table.FLAG_NONE, "size int");
        t.addColumn("W", Table.FLAG_ALIGN_RIGHT, "width int");
        t.addColumn("H", Table.FLAG_ALIGN_RIGHT, "height int");
        t.addColumn("Stride", Table.FLAG_ALIGN_RIGHT, "stride int");
        t.addColumn("Format", Table.FLAG_NONE, "format varchar");
        t.addColumn("Usage", Table.FLAG_NONE, "usage varchar");
        t.begin();
        for (Buffer b : mBuffers) {
            t.addData(String.format("0x%08x", b.ptr));
            t.addData(String.format("%.2f", b.size));
            t.addData(b.w);
            t.addData(b.h);
            t.addData(b.stride);
            t.addData(getFormatName(b.format));
            t.addData(getUsage(b.usage));
        }
        t.end();
    }

    public void generateLogs(Module br, Chapter mainCh) {
        generateLogs(br, mainCh, "SystemLogPlugin");
        generateLogs(br, mainCh, "MainLogPlugin");
    }


    private void generateLogs(Module br, Chapter mainCh, String pluginName) {
        LogPlugin plugin = (LogPlugin)br.getPlugin(pluginName);
        if (plugin == null) return;
        Chapter ch = null;
        DocNode log = null;
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
                    ch = new Chapter(br.getContext(), "Related logs from " + pluginName);
                    mainCh.addChapter(ch);
                    new Para(ch).add("Related logs from " + pluginName);
                    log = new Block(ch).addStyle("log");
                }
                log.add(sl.symlink());
            }
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

    private String saveComposit(Module br, int opacity) {
        beginPng();

        // render the visible layers
        for (int i = 0; i < mLayers.size(); i++) {
            Layer l = mLayers.get(i);
            if (0 != (l.flags & FLAG_HIDDEN)) continue;
            int color = ColorUtil.getColor(i);
            Region reg = l.regVisScreen;
            for (int j = 0; j < reg.getCount(); j++) {
                Rect r = reg.get(j);
                renderRect(r, (opacity << 24) | color);
            }
        }

        // Render the orientation arrow
        renderOrientationArrow();

        // Save the image
        String fn = "sf_layer_all_" + opacity + ".png";
        endPng(br.getBaseDir() + fn);
        return fn;
    }

    private void renderOrientationArrow() {
        float stroke = mImg.getStrokeWidth();
        mImg.setStrokeWidth(3.0f);
        mImg.setColor(ImageCanvas.BLACK);
        switch (mOrientation) {
            case OR_PORTRAIT:
                mImg.drawLine(20, 10, 10, 20);
                mImg.drawLine(20, 10, 30, 20);
                mImg.drawLine(20, 10, 20, 40);
                break;
            case OR_90:
                mImg.drawLine(40, 20, 30, 10);
                mImg.drawLine(40, 20, 30, 30);
                mImg.drawLine(40, 20, 10, 20);
                break;
            case OR_270:
                mImg.drawLine(10, 20, 20, 10);
                mImg.drawLine(10, 20, 20, 30);
                mImg.drawLine(10, 20, 40, 20);
                break;
        }
        mImg.setStrokeWidth(stroke);
    }

    private String createPng(Module br, int color, Layer l, Region reg) {
        beginPng();

        // Now render the layer area with a light color
        renderRect(l.rect, 0x40000000 | color);

        // And now render each region
        for (int i = 0; i < reg.getCount(); i++) {
            Rect r = reg.get(i);
            renderRect(r, 0xff000000 | color);
        }

        // Save the image
        String fn = "sf_layer_" + Integer.toHexString(l.hashCode()) + "_" + Integer.toHexString(reg.hashCode()) + ".png";
        endPng(br.getBaseDir() + fn);
        return fn;
    }

    private void renderRect(Rect rect, int argb) {
        int x1 = (int)(rect.x * mScale);
        int y1 = (int)(rect.y * mScale);
        int x2 = (int)((rect.x + rect.w) * mScale);
        int y2 = (int)((rect.y + rect.h) * mScale);
        mImg.setColor(argb);
        mImg.fillRect(x1, y1, x2 - x1, y2 - y1);
        argb = (argb & 0xff000000) + ((argb & 0x00fefefe) / 2);
        mImg.setColor(argb);
        mImg.drawRect(x1, y1, x2 - x1 - 1, y2 - y1 - 1);
    }

    private void beginPng() {
        // Create image
        mImg = new ImageCanvas(mWidth, mHeight);

        // Fill the background with a non-white color and draw a border to visualize the screen
        mImg.setColor(0xffdddddd);
        mImg.fillRect(0, 0, mWidth, mHeight);
        mImg.setColor(0xffbbbbbb);
        mImg.drawRect(0, 0, mWidth - 1, mHeight - 1);
    }

    private void endPng(String fn) {
        try {
            mImg.writeTo(new File(fn));
            mImg = null;
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

    private boolean scan(BugReportModule br, Section sec) {
        int count = 0;
        int expectedCount = 0;
        int line = 0;

        // Read the number of layers
        String buff = sec.getLine(0);
        if (buff.startsWith("Build configuration:")) {
            buff = sec.getLine(++line);
        }
        if (buff.startsWith("Visible layers")) {
            int idx0 = buff.indexOf('=');
            int idx1 = buff.indexOf(')');
            if (idx0 < 0 || idx1 < 0 || idx0 >= idx1 + 2) {
                br.printErr(3, TAG + "Error parsing: cannot find 'count' in first line!");
                return false;
            }
            expectedCount = Integer.parseInt(buff.substring(idx0 + 2, idx1));
            line++;
        } else if (buff.startsWith("+ Layer")) {
            // NOP
        } else {
            br.printErr(3, TAG + "Error parsing: cannot recognize section!");
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
            if (br.getAndroidVersionSdk() >= AndroidVersions.SDK_ICS) {
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
                    Bug bug = new Bug(Bug.Type.PHONE_WARN, Bug.PRIO_SF_NO_BUFF, 0, "No available buffer in SurfaceFlinger for layer " + layer.name + "!");
                    new Para(bug)
                        .addln("The layer " + layer.name + " (with identify " + layer.identity + ") does not have")
                        .addln("any available buffers. This could be a false-alarm (if the bugreport was happened to be taken at a critical")
                        .addln("point), but more probably it will result in either one window or the whole screen to freeze.")
                        .addln("(in which case \"waitForCondition\" messages will be printed to the system log.)</p>");
                    bug.add(new Link(layer.anchor, "(Link to layer info)"));
                    br.addBug(bug);
                }
            }

        }

        if (expectedCount > 0 && expectedCount != count) {
            br.printErr(3, TAG + "Error parsing: count mismatch! Expected: " + expectedCount + ", found: " + count);
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
            Pattern p = Pattern.compile("0x([0-9a-f]+): +([0-9.]+) KiB \\| +([0-9]+) \\( *([0-9]+)\\) x +([0-9]+) \\| +       ([0-9]+) \\| 0x([0-9a-f]+)");
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
                    if (buff.charAt(38) == '|') {
                        // pre 2.3
                        buffer.ptr = Util.parseHex(buff, 0, 10, 0);
                        buffer.size = Util.parseFloat(buff, 12, 19);
                        buffer.w = Util.parseInt(buff, 26, 30, 0);
                        buffer.stride = buffer.w;
                        buffer.h = Util.parseInt(buff, 33, 37, 0);
                        int idx = buff.indexOf('|', 40);
                        buffer.format = buff.charAt(idx - 2) - '0';
                        buffer.usage = Util.parseHex(buff, idx + 2, idx + 12, 0);
                    } else {
                        // 2.3
                        Matcher m = p.matcher(buff);
                        if (m.matches()) {
                            buffer.ptr = Integer.parseInt(m.group(1), 16);
                            buffer.size = Float.parseFloat(m.group(2));
                            buffer.w = Integer.parseInt(m.group(3));
                            buffer.stride = Integer.parseInt(m.group(4));
                            buffer.h = Integer.parseInt(m.group(5));
                            buffer.format = Integer.parseInt(m.group(6));
                            buffer.usage = Integer.parseInt(m.group(7), 16);
                        } else {
                            br.printErr(4, "Cannot parse buffer: " + buffer);
                        }
                    }
                    mBuffers.add(buffer);
                }
            } catch (NumberFormatException nfe) {
                // Ignore it for now
                br.printErr(4, TAG + "Error parsing buffer list: " + nfe);
            } catch (StringIndexOutOfBoundsException e) {
                // Ignore it for now
                br.printErr(4, TAG + "Error parsing buffer list: " + e);
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

    private int readRegion(Module br, Layer layer, String buff, Section sec, int line) {
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
            br.printErr(4, "Warning: Unknown region: " + type);
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

    private void readAttributes(BugReportModule br, Layer layer, String buff) {
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
                if (br.getAndroidVersionSdk() < AndroidVersions.SDK_ICS) {
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
            } else if ("crop".equals(key)) {
                tok.nextToken(); // x
                tok.nextToken(); // y
                tok.nextToken(); // w
                tok.nextToken(); // h
            } else {
                if (!mUnknownAttrs.contains(key)) {
                    mUnknownAttrs.add(key);
                    br.printErr(4, TAG + "Unknown layer attribute (this could brake parsing): " + key);
                }
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
        public Anchor anchor;
    }

    static class Buffer {
        public int ptr;
        public int w, h, stride;
        public int format, usage;
        public float size;
    }

}
