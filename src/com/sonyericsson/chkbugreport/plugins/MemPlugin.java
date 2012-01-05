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

import com.sonyericsson.chkbugreport.BugReport;
import com.sonyericsson.chkbugreport.Chapter;
import com.sonyericsson.chkbugreport.Lines;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Report;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.Util;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;

/*
 * Note: some of the explanation is taken from: http://www.redhat.com/advice/tips/meminfo.html
 */
public class MemPlugin extends Plugin {

    private static final String TAG = "[MemPlugin]";

    private static final int GW = 128;
    private static final int GH = 256;
    private static final int GMT = 16;
    private static final int GML = 128;
    private static final int GMB = 16;
    private static final int GMR = 128;
    private static final int IW = GW + GML + GMR;
    private static final int IH = GH + GMT + GMB;

    private int mTotMem = 0;
    private int mFreeMem = 0;
    private int mBuffers = 0;
    private int mCaches = 0;
    private int mSlabR = 0;
    private int mSlabU = 0;

    private Vector<MemInfo> mMemInfos = new Vector<MemInfo>();
    private HashMap<Integer, LRMemInfoList> mLRMemInfoPid = new HashMap<Integer, LRMemInfoList>();
    private HashMap<String, LRMemInfoList> mLRMemInfoMem = new HashMap<String, LRMemInfoList>();

    private int mMemInfoSvcFmt;

    @Override
    public int getPrio() {
        return 20;
    }

    @Override
    public void load(Report rep) {
        BugReport br = (BugReport)rep;

        // Reset
        mMemInfos.clear();
        mLRMemInfoMem.clear();
        mLRMemInfoPid.clear();
        mTotMem = 0;
        mFreeMem = 0;
        mBuffers = 0;
        mCaches = 0;
        mSlabR = 0;
        mSlabU = 0;
        mMemInfoSvcFmt = 0;

        // Load
        loadServMeminfoSec(br);
        loadLibrankSec(br);
    }

    @Override
    public void generate(Report rep) {
        BugReport br = (BugReport)rep;

        Chapter ch = new Chapter(br, "Memory info");
        Section sec = null;

        // Handle the memory info section
        sec = br.findSection(Section.MEMORY_INFO);
        if (sec == null) {
            br.printErr(TAG + "Section not found: " + Section.MEMORY_INFO + " (ignoring section)");
        } else {
            generateMemoryInfoSec(br, ch, sec);
        }

        // Handle the procrank section
        sec = br.findSection(Section.PROCRANK);
        if (sec == null) {
            br.printErr(TAG + "Section not found: " + Section.PROCRANK + " (ignoring section)");
        } else {
            generateProcrankSec(br, ch, sec);
        }

        // Handle the meminfo section
        generateServMeminfoSec(br, ch);

        // Handle the librank section
        generateLibrankSec(br, ch);

        if (ch.getLineCount() > 0 || ch.getChildCount() > 0) {
            br.addChapter(ch);
        } else {
            br.printErr(TAG + "No usable sections found (aborting plugin)");
        }

    }

    private void generateMemoryInfoSec(Report br, Chapter mainCh, Section sec) {
        Chapter ch = new Chapter(br, "From /proc/meminfo");

        // Create the chapter
        ch.addLine("<table class=\"meminfo\">");
        ch.addLine("  <thead>");
        ch.addLine("    <tr>");
        ch.addLine("      <td>Memory type</td>");
        ch.addLine("      <td style=\"text-align: right\">Value (KB)</td>");
        ch.addLine("      <td>Explanation</td>");
        ch.addLine("    </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");

        // Parse the values
        int cnt = sec.getLineCount();
        for (int i = 0; i < cnt; i++) {
            String line = sec.getLine(i);
            int idx = line.indexOf(':');
            if (idx < 0) continue;
            String key = line.substring(0, idx);
            String value = Util.strip(line.substring(idx + 1, 24));
            int intValue = 0;
            try {
                intValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                br.printErr(TAG + "Error parsing number: " + value);
                continue;
            }
            String expl = null;

            // Lookup the explanation
            if ("MemTotal".equals(key)) {
                expl = "The total amount of memory available for the android system. This does not count memory used by kernel code or the modem side.";
                mTotMem = intValue;
            } else if ("MemFree".equals(key)) {
                expl = "This is the amount of yet completely not used memory.";
                mFreeMem = intValue;
            } else if ("Buffers".equals(key)) {
                expl = "Memory in buffer cache. This memory can be freed by the kernel when needed.";
                mBuffers = intValue;
            } else if ("Cached".equals(key)) {
                expl = "Memory in disk cache (memory copy of disk content in order to speed disk IO). This memory can be freed by the kernel when needed.";
                mCaches = intValue;
            } else if ("SwapCached".equals(key)) {
                expl = "Memory that once was swapped out, is swapped back in but still also is in the swapfile (if memory is needed it doesn't need to be swapped out AGAIN because it is already in the swapfile. This saves I/O). Not used in android (since officially android doesn't use a swap partition).";
            } else if ("SwapTotal".equals(key)) {
                expl = "Total amount of physical swap memory. Not used in android (since officially android doesn't use a swap partition).";
            } else if ("SwapFree".equals(key)) {
                expl = "Total amount of swap memory free. Not used in android (since officially android doesn't use a swap partition).";
            } else if ("Active".equals(key)) {
                expl = "Memory that has been used more recently and usually not reclaimed unless absolutely necessary.";
            } else if ("Inactive".equals(key)) {
                expl = "Memory which has been less recently used.  It is more eligible to be reclaimed for other purposes.";
            } else if ("Dirty".equals(key)) {
                expl = "Memory which is waiting to get written back to the disk.";
            } else if ("Writeback".equals(key)) {
                expl = "Memory which is actively being written back to the disk.";
            } else if ("AnonPages".equals(key)) {
                expl = "Non-file backed pages mapped into userspace page tables.";
            } else if ("Mapped".equals(key)) {
                expl = "Files which have been mmaped, such as libraries.";
            } else if ("Slab".equals(key)) {
                expl = "In-kernel data structures cache.";
            } else if ("SReclaimable".equals(key)) {
                expl = "Part of Slab, that might be reclaimed, such as caches.";
                mSlabR = intValue;
            } else if ("SUnreclaim".equals(key)) {
                expl = "Part of Slab, that cannot be reclaimed on memory pressure.";
                mSlabU = intValue;
            } else if ("PageTables".equals(key)) {
                expl = "Amount of memory dedicated to the lowest level of page tables.";
            } else if ("VmallocTotal".equals(key)) {
                expl = "Total size of vmalloc memory area.";
            } else if ("VmallocUsed".equals(key)) {
                expl = "Amount of vmalloc area which is used.";
            } else if ("VmallocChunk".equals(key)) {
                expl = "Largest contiguous block of vmalloc area which is free.";
            } else if ("Shmem".equals(key)) {
                expl = "Used shared memory (usually used by IPC)";
            }

            // Add the data to the table
            if (expl == null) continue;
            ch.addLine("    <tr>");
            ch.addLine("      <td>" + key + "</td>");
            ch.addLine("      <td style=\"text-align: right\">" + Util.shadeValue(intValue, "kb") + "</td>");
            ch.addLine("      <td>" + expl + "</td>");
            ch.addLine("    </tr>");

        }

        ch.addLine("  </tbody>");
        ch.addLine("</table>");

        // Create some nice chart about it as well
        BufferedImage img = new BufferedImage(IW, IH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, IW, IH);

        // Draw the total memory area
        int b = 3;
        g.setColor(Color.BLACK);
        g.fillRect(GML - b, GMT - b, GW + 2*b, GH + 2*b);
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(GML, GMT, GW, GH);

        // Draw the free memory
        int yy = 0;
        int hh = mFreeMem * GH / mTotMem;
        drawBox(g, 0x00ff00, yy, hh);
        drawLabel(g, 0, hh, "Free");

        // Draw the buffers
        yy += hh;
        hh = mBuffers * GH / mTotMem;
        drawBox(g, 0x40ffff, yy, hh);
        int startPossFree = yy;

        // Draw the cache
        yy += hh;
        hh = mCaches * GH / mTotMem;
        drawBox(g, 0x8080ff, yy, hh);

        // Draw the slab-r
        yy += hh;
        hh = mSlabR * GH / mTotMem;
        drawBox(g, 0xffff000, yy, hh);

        // Draw the slab-u
        yy += hh;
        hh = mSlabU * GH / mTotMem;
        drawBox(g, 0xff00000, yy, hh);
        int endPossFree = yy;
        drawLabel(g, startPossFree, endPossFree, "Can be freed");

        // Write some more text on the chart
        FontMetrics fm = g.getFontMetrics();
        g.drawString("Memory", 10, 10 + fm.getAscent());
        g.drawString("overview", 10, 10 + fm.getAscent() + fm.getHeight());

        // Save the chart
        try {
            String fn = br.getRelDataDir() + "meminfo.png";
            ImageIO.write(img, "png", new File(br.getBaseDir() + fn));
            ch.addLine("<div><img src=\"" + fn + "\"/></div>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainCh.addChapter(ch);
    }

    private void generateProcrankSec(BugReport br, Chapter ch, Section sec) {
        try {
            generateProcrankSecUnsafe(br, ch, sec);
        } catch (NumberFormatException e) {
            br.printErr(TAG + "Failed gathering data from procrank output... Maybe procrank is buggy again?: " + e);
        }
    }

    private void generateProcrankSecUnsafe(BugReport br, Chapter mainCh, Section sec) {
        if (sec.getLineCount() < 10) {
            // Suspiciously small...
            br.printErr(TAG + "procrank section is suspiciously small... ignoring it");
            return;
        }
        Chapter ch = new Chapter(br, "From procrank");
        ch.addLine("<p>Memory used by applications:</p>");

        boolean showPerc = mTotMem > 0;

        ch.addLine("<table class=\"procrank-stat\">");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"procrank-header\">");
        ch.addLine("    <th>Process</td>");
        ch.addLine("    <th title=\"Virtual memory used in kilobytes\">Vss (KB)</td>");
        ch.addLine("    <th title=\"Real memory used in kilobytes\">Rss (KB)</td>");
        ch.addLine("    <th title=\"Unique and ratio of shared memory used in kilobytes\">Pss (KB)</td>");
        if (showPerc){
            ch.addLine("    <th title=\"Pss as percentage of total memory (available for android)\">...%</td>");
        }
        ch.addLine("    <th title=\"The sum of this and all previous Pss values\">Sum(Pss,0..i)</td>");
        if (showPerc) {
            ch.addLine("    <th title=\"The sum of Pss as percentage of total memory (available for android)\">...%</td>");
        }
        ch.addLine("    <th title=\"Unique (used by only this process) memory used in kilobytes\">Uss (KB)</td>");
        if (showPerc) {
            ch.addLine("    <th title=\"Uss as percentage of total memory (available for android)\">...%</td>");
        }
        ch.addLine("    <th title=\"The sum of this and all previous Uss values\">Sum(Uss,0..i)</td>");
        if (showPerc) {
            ch.addLine("    <th title=\"The sum of Uss as percentage of total memory (available for android)\">...%</td>");
        }
        ch.addLine("  </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");

        int sumPss = 0, sumUss = 0;
        // Read data, skip first (header) line
        for (int i = 1; i < sec.getLineCount(); i++) {
            String line = sec.getLine(i);
            if (line.startsWith("[")) break;
            int pid = Util.parseInt(line, 0, 5);
            int vss = Util.parseInt(line, 6, 13);
            int rss = Util.parseInt(line, 15, 22);
            int pss = Util.parseInt(line, 24, 31);
            int uss = Util.parseInt(line, 33, 40);
            String procName = line.substring(43);
            sumPss += pss;
            sumUss += uss;

            String prA0 = "", prA1 = "";
            boolean create = !procName.startsWith("/");
            ProcessRecord pr = br.getProcessRecord(pid, create, false);
            if (pr != null) {
                prA0 = "<a href=\"" + br.createLinkToProcessRecord(pid) + "\">";
                prA1 = "</a>";
            }
            ch.addLine("  <tr>");
            ch.addLine("    <td>" + prA0 + procName + "(" + pid + ")" + prA1 + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(vss, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(rss, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(pss, "kb") + "</td>");
            if (showPerc) {
                ch.addLine("    <td>" + String.format("%.1f%%", pss * 100.0f / mTotMem) + "</td>");
            }
            ch.addLine("    <td>" + Util.shadeValue(sumPss, "kb") + "</td>");
            if (showPerc) {
                ch.addLine("    <td>" + String.format("%.1f%%", sumPss * 100.0f / mTotMem) + "</td>");
            }
            ch.addLine("    <td>" + Util.shadeValue(uss, "kb") + "</td>");
            if (showPerc) {
                ch.addLine("    <td>" + String.format("%.1f%%", uss * 100.0f / mTotMem) + "</td>");
            }
            ch.addLine("    <td>" + Util.shadeValue(sumUss, "kb") + "</td>");
            if (showPerc) {
                ch.addLine("    <td>" + String.format("%.1f%%", sumUss * 100.0f / mTotMem) + "</td>");
            }
            ch.addLine("  </tr>");
        }

        ch.addLine("  </tbody>");
        ch.addLine("</table>");
        mainCh.addChapter(ch);
    }

    private void loadServMeminfoSec(BugReport br) {
        try {
            loadServMeminfoSecUnsafe(br);
        } catch (Exception e) {
            br.printErr(TAG + "Failed gathering data from meminfo service output... Maybe output format has changed?: " + e);
            mMemInfos.clear();
        }
    }

    private void loadServMeminfoSecUnsafe(BugReport br) {
        Section sec = br.findSection(Section.DUMP_OF_SERVICE_MEMINFO);
        if (sec == null) {
            br.printErr(TAG + "Section not found: " + Section.DUMP_OF_SERVICE_MEMINFO + " (ignoring section)");
            return;
        }

        String key = "** MEMINFO in pid ";

        // validate section
        int cnt = sec.getLineCount();
        if (cnt < 10) {
            return; // very suspicious
        }
        if (!sec.getLine(0).startsWith("Applications Memory Usage")) {
            return;
        }
        if (!sec.getLine(1).startsWith("Uptime:")) {
            return;
        }
        int idx = 3; // start scanning from 3rd line
        MemInfo memInfo = null;
        Lines memInfoLines = null;
        char mode = 'm';
        while (true) {
            String line = (idx < cnt) ? sec.getLine(idx++) : null;
            if (line == null || line.startsWith(key)) {
                // Finish previous data
                if (memInfo != null) {
                    ProcessRecord pr = br.getProcessRecord(memInfo.pid, true, false);
                    if (pr != null && memInfoLines != null) {
                        pr.addLine("<p>Mem info from 'meminfo' service:</p>");
                        pr.addLine("<pre class=\"box\">");
                        pr.addLines(memInfoLines);
                        pr.addLine("</pre>");
                    }
                }
                memInfo = null;
                memInfoLines = null;
            }
            if (line == null) {
                break; // no more lines to process
            }
            if (line.startsWith(key)) {
                // Start processing new pid
                memInfo = new MemInfo();
                memInfoLines = new Lines("");
                memInfoLines.addLine(line);
                line = line.substring(key.length());
                int spc = line.indexOf(' ');
                memInfo.pid = Integer.parseInt(line.substring(0, spc));
                int nameS = line.indexOf('[');
                int nameE = line.indexOf(']');
                memInfo.name = line.substring(nameS + 1, nameE);
                mode = 'm'; // memory
                mMemInfos.add(memInfo);
                ProcessRecord pr = br.getProcessRecord(memInfo.pid, true, true);
                pr.suggestName(memInfo.name, 45);

            } else {
                // Add more data to started pid
                memInfoLines.addLine(line);

                if (mode == 'm') {
                    if (line.startsWith("            size:")) {
                        memInfo.sizeNative = Util.parseInt(line, 18, 26);
                        memInfo.sizeDalvik = Util.parseInt(line, 27, 35);
                        memInfo.sizeTotal = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("       allocated:")) {
                        memInfo.allocNative = Util.parseInt(line, 18, 26);
                        memInfo.allocDalvik = Util.parseInt(line, 27, 35);
                        memInfo.allocTotal = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("            free:")) {
                        memInfo.freeNative = Util.parseInt(line, 18, 26);
                        memInfo.freeDalvik = Util.parseInt(line, 27, 35);
                        memInfo.freeTotal = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("           (Pss):")) {
                        memInfo.pssNative = Util.parseInt(line, 18, 26);
                        memInfo.pssDalvik = Util.parseInt(line, 27, 35);
                        memInfo.pssOther = Util.parseInt(line, 36, 44);
                        memInfo.pssTotal = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("  (shared dirty):")) {
                        memInfo.sharedNative = Util.parseInt(line, 18, 26);
                        memInfo.sharedDalvik = Util.parseInt(line, 27, 35);
                        memInfo.sharedOther = Util.parseInt(line, 36, 44);
                        memInfo.sharedTotal = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("    (priv dirty):")) {
                        memInfo.privNative = Util.parseInt(line, 18, 26);
                        memInfo.privDalvik = Util.parseInt(line, 27, 35);
                        memInfo.privOther = Util.parseInt(line, 36, 44);
                        memInfo.privTotal = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("           Views:")) {
                        memInfo.views = Util.parseInt(line, 18, 26);
                        memInfo.viewRoots = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("     AppContexts:")) {
                        memInfo.appContexts = Util.parseInt(line, 18, 26);
                        memInfo.activities = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("          Assets:")) {
                        memInfo.assets = Util.parseInt(line, 18, 26);
                        memInfo.assetManagers = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("   Local Binders:")) {
                        memInfo.localBinders = Util.parseInt(line, 18, 26);
                        memInfo.proxyBinders = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("Death Recipients:")) {
                        memInfo.deathRec = Util.parseInt(line, 18, 26);
                    } else if (line.startsWith(" OpenSSL Sockets:")) {
                        memInfo.openSSLSockets = Util.parseInt(line, 18, 26);
                    } else if (line.startsWith(" SQL")) {
                        mode = 's';
                    }
                } else if (mode == 's') {
                    if (line.startsWith("               heap:")) {
                        // 2.3
                        memInfo.sqlHeap = Util.parseInt(line, 21, 29);
                        memInfo.sqlMemUsed = Util.parseInt(line, 51, 59);
                    } else if (line.startsWith(" PAGECACHE_OVERFLOW:")) {
                        // 2.3
                        memInfo.sqlPageCacheOverflow = Util.parseInt(line, 21, 29);
                        memInfo.sqlMallocSize = Util.parseInt(line, 51, 59);
                    } else if (line.startsWith("            heap:")) {
                        // < 2.3
                        memInfo.sqlHeap = Util.parseInt(line, 18, 26);
                        memInfo.sqlMemUsed = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith("pageCacheOverflo:")) {
                        // < 2.3
                        memInfo.sqlPageCacheOverflow = Util.parseInt(line, 18, 26);
                        memInfo.sqlMallocSize = Util.parseInt(line, 45, 53);
                    } else if (line.startsWith(" DATABASES")) {
                        mode = 'd';
                    }
                } else if (mode == 'd') {
                    if (line.length() <= 1) {
                        mode = 'x';
                    } else if (line.startsWith("      pgsz")) {
                        // Ignore header
                        mMemInfoSvcFmt = 23;
                    } else if (line.startsWith("  Pagesize")) {
                        // Ignore header
                        mMemInfoSvcFmt = 22;
                    } else {
                        DatabaseInfo dbInfo = new DatabaseInfo();
                        if (mMemInfoSvcFmt == 23) {
                            // 2.3
                            dbInfo.name = line.substring(36);
                            if (!dbInfo.name.contains("(")) {
                                dbInfo.lookaside = Util.parseInt(line, 20, 34);
                            }
                        } else {
                            // < 2.3
                            dbInfo.lookaside = Util.parseInt(line, 20, 30);
                            dbInfo.name = line.substring(32);
                        }
                        if (!dbInfo.name.contains("(pooled")) {
                            dbInfo.pgsz = Util.parseInt(line, 1, 10);
                            dbInfo.dbsz = Util.parseInt(line, 11, 19);
                        }
                        memInfo.dbs.add(dbInfo);
                    }
                }
            }
        }
    }

    private void generateServMeminfoSec(BugReport br, Chapter mainCh) {
        if (mMemInfos.size() == 0) return;

        Chapter ch = new Chapter(br, "From meminfo service");
        ch.addLine("<p>Memory used by applications (KB):</p>");

        // Sort mem info based on pss
        Collections.sort(mMemInfos, new Comparator<MemInfo>() {
            @Override
            public int compare(MemInfo o1, MemInfo o2) {
                return o2.pssTotal - o1.pssTotal;
            }
        });

        boolean showPerc = mTotMem > 0;

        ch.addLine("<table class=\"procrank-stat\">");
        ch.addLine("  <thead>");
        ch.addLine("  <tr class=\"procrank-header\">");
        ch.addLine("    <th>Process</td>");
        ch.addLine("    <th>Native size</td>");
        ch.addLine("    <th>Native alloc</td>");
        ch.addLine("    <th>Native free</td>");
        ch.addLine("    <th>Dalvik size</td>");
        ch.addLine("    <th>Dalvik alloc</td>");
        ch.addLine("    <th>Dalvik free</td>");
        ch.addLine("    <th>Pss other</td>");
        ch.addLine("    <th>Shared dirty other</td>");
        ch.addLine("    <th>Priv dirty other</td>");
        ch.addLine("    <th>Pss total</td>");
        if (showPerc){
            ch.addLine("    <th title=\"Pss as percentage of total memory (available for android)\">...%</td>");
        }
        ch.addLine("    <th title=\"The sum of this and all previous Pss values\">Sum(Pss,0..i)</td>");
        if (showPerc) {
            ch.addLine("    <th title=\"The sum of Pss as percentage of total memory (available for android)\">...%</td>");
        }
        ch.addLine("    <th>Shared dirty total</td>");
        ch.addLine("    <th>Priv dirty total</td>");
        ch.addLine("  </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");

        int sumPss = 0;

        for (MemInfo mi : mMemInfos) {
            sumPss += mi.pssTotal;

            String prA0 = "", prA1 = "";
            boolean create = !mi.name.startsWith("/");
            ProcessRecord pr = br.getProcessRecord(mi.pid, create, true);
            if (pr != null) {
                prA0 = "<a href=\"" + br.createLinkToProcessRecord(mi.pid) + "\">";
                prA1 = "</a>";
            }
            ch.addLine("  <tr>");
            ch.addLine("    <td>" + prA0 + mi.name + "(" + mi.pid + ")" + prA1 + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.sizeNative, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.allocNative, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.freeNative, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.sizeDalvik, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.allocDalvik, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.freeDalvik, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.pssOther, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.sharedOther, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.privOther, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.pssTotal, "kb") + "</td>");
            if (showPerc) {
                ch.addLine("    <td>" + String.format("%.1f%%", mi.pssTotal * 100.0f / mTotMem) + "</td>");
            }
            ch.addLine("    <td>" + Util.shadeValue(sumPss, "kb") + "</td>");
            if (showPerc) {
                ch.addLine("    <td>" + String.format("%.1f%%", sumPss * 100.0f / mTotMem) + "</td>");
            }
            ch.addLine("    <td>" + Util.shadeValue(mi.sharedTotal, "kb") + "</td>");
            ch.addLine("    <td>" + Util.shadeValue(mi.privTotal, "kb") + "</td>");
            ch.addLine("  </tr>");
        }

        ch.addLine("  </tbody>");
        ch.addLine("</table>");

        mainCh.addChapter(ch);
    }

    private void drawLabel(Graphics2D g, int y0, int y1, String msg) {
        y0 += GMT;
        y1 += GMT;
        int xx = GML + GW;
        g.setColor(Color.DARK_GRAY);
        g.drawLine(xx, y0, xx + 16, y0);
        g.drawLine(xx, y1, xx + 16, y1);
        g.drawLine(xx + 16, y0, xx + 16, y1);
        g.drawString(msg, xx + 24, (y0 + y1) / 2);
    }

    private void drawBox(Graphics2D g, int color, int yy, int hh) {
        int lightColor = (color & 0xfefefe + 0xfefefe) / 2;
        g.setColor(new Color(color));
        g.fillRect(GML, GMT + yy, GW, hh);
        g.setColor(new Color(lightColor));
        g.drawRect(GML, GMT + yy, GW - 1, hh - 1);
    }

    private void loadLibrankSec(BugReport br) {
        try {
            loadLibrankSecUnsafe(br);
        } catch (Exception e) {
            br.printErr(TAG + "Failed gathering data from librank output... Maybe it's broken again?: " + e);
            mLRMemInfoMem.clear();
            mLRMemInfoPid.clear();
        }
    }

    private void loadLibrankSecUnsafe(BugReport br) {
        Section s = br.findSection(Section.LIBRANK);
        int cnt = s.getLineCount();
        if (cnt < 10) {
            // Suspiciously small...
            br.printErr(TAG + "librank section is suspiciously small... ignoring it");
            return;
        }
        String line = s.getLine(0);
        if (!line.equals(" RSStot      VSS      RSS      PSS      USS  Name/PID")) {
            br.printErr(TAG + "librank section format not supported... ignoring it");
            return;
        }
        String memName = null;
        for (int i = 1; i < cnt; i++) {
            line = s.getLine(i);
            if (line.startsWith("[")) break; // EOF
            if (line.length() == 0) {
                memName = null;
                continue;
            }
            if (line.startsWith("        ")) {
                // add more data to the same memory block
                if (memName == null) {
                    br.printErr(TAG + "Parse error in librank output... trying to continue though (line: " + i + ")");
                } else {
                    // Parse the data
                    LRMemInfo mi = new LRMemInfo();
                    mi.memName = memName;
                    mi.vss = Util.parseInt(line, 8, 15);
                    mi.rss = Util.parseInt(line, 17, 24);
                    mi.pss = Util.parseInt(line, 26, 33);
                    mi.uss = Util.parseInt(line, 35, 42);
                    line = line.substring(46);
                    mi.procName = Util.extract(line, " ", " ");
                    mi.pid = Util.parseInt(Util.extract(line, "[", "]"));

                    // Add to the mem stat
                    LRMemInfoList list = mLRMemInfoMem.get(memName);
                    if (list == null) {
                        list = new LRMemInfoList(0, memName);
                        mLRMemInfoMem.put(memName, list);
                    }
                    list.add(mi);

                    // Add to the proc stat
                    list = mLRMemInfoPid.get(mi.pid);
                    if (list == null) {
                        list = new LRMemInfoList(mi.pid, mi.procName);
                        mLRMemInfoPid.put(mi.pid, list);
                    }
                    list.add(mi);
                }
            } else {
                // found new memory block
                memName = line.substring(45);
            }
        }
    }

    private void generateLibrankSec(BugReport br, Chapter mainCh) {
        if (mLRMemInfoMem.size() == 0 || mLRMemInfoPid.size() == 0) {
            return;
        }

        // First create a chapter for memory ranges
        Chapter ch = new Chapter(br, "Librank - by pid");
        mainCh.addChapter(ch);

        // Create a copy of the list of the lists
        Vector<LRMemInfoList> tmp = new Vector<LRMemInfoList>();
        for (LRMemInfoList list : mLRMemInfoPid.values()) {
            tmp.add(list);
        }

        ch.addLine("<table>");
        ch.addLine("  <thead>");
        ch.addLine("    <tr>");
        ch.addLine("      <th>Process sorted by name</th>");
        ch.addLine("      <th>Process dorted by pid</th>");
        ch.addLine("    </tr>");
        ch.addLine("  </thead>");
        ch.addLine("  <tbody>");
        ch.addLine("    <tr>");

        // Sort by name and list
        ch.addLine("      <td><ul>");
        Collections.sort(tmp, new Comparator<LRMemInfoList>() {
            @Override
            public int compare(LRMemInfoList o1, LRMemInfoList o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        for (LRMemInfoList list : tmp) {
            ch.addLine("<li><a href=\"#" + getAnchorTo(list) + "\">" + list.name + "(" + list.id + ")</a></li>");
        }
        ch.addLine("      </ul></td>");

        // Sort by pid and list
        ch.addLine("      <td><ul>");
        Collections.sort(tmp, new Comparator<LRMemInfoList>() {
            @Override
            public int compare(LRMemInfoList o1, LRMemInfoList o2) {
                return o1.id - o2.id;
            }
        });
        for (LRMemInfoList list : tmp) {
            ch.addLine("<li><a href=\"#" + getAnchorTo(list) + "\">" + list.name + "(" + list.id + ")</a></li>");
        }
        ch.addLine("      </ul></td>");

        ch.addLine("    </tr>");
        ch.addLine("  </tbody>");
        ch.addLine("</table>");

        // Generate the actual data
        for (LRMemInfoList list : tmp) {
            sortList(list);
            Lines lines = generateLibrankStat(br, list, 10);
            ch.addLines(lines);
            ProcessRecord pr = br.getProcessRecord(list.id, true, false);
            if (pr != null) {
                pr.addLines(lines);
            }

            // Save the full table as well, but in separate file
            lines = generateLibrankStat(br, list, Integer.MAX_VALUE);
            try {
                FileOutputStream fos = new FileOutputStream(br.getOutDir() + "data/" + getHtmlLinkTo(list));
                PrintStream out = new PrintStream(fos);

                String title = "Memory usage of process " + list.name + " (" + list.id + ") from librank output:";
                Util.writeHTMLHeader(out, title, "");
                lines.writeTo(out);
                Util.writeHTMLFooter(out);
            } catch (IOException e) {
                br.printErr(TAG + "Error saving librank pid stat table: " + e);
            }
        }

    }

    private String getAnchorTo(LRMemInfoList list) {
        return "lrm_pid_" + list.id;
    }

    private String getHtmlLinkTo(LRMemInfoList list) {
        return "lrm_pid_" + list.id + ".html";
    }

    private void sortList(LRMemInfoList list) {
        Collections.sort(list, new Comparator<LRMemInfo>() {
            @Override
            public int compare(LRMemInfo o1, LRMemInfo o2) {
                return o2.pss - o1.pss;
            }
        });
    }

    private Lines generateLibrankStat(BugReport br, LRMemInfoList list, int limit) {
        boolean standalone = limit == Integer.MAX_VALUE;
        Lines ret = new Lines("");
        String a1 = "";
        String a2 = "";
        ProcessRecord pr = br.getProcessRecord(list.id, true, false);
        if (pr != null) {
            a1 = "<a href=\"" + br.createLinkToProcessRecord(list.id) + "\">";
            a2 = "</a>";
        }

        ret.addLine("<a name=\"" + getAnchorTo(list) + "\">");
        ret.addLine("<p>Memory usage of process " + a1 + list.name + " (" + list.id + ")" + a2 + " from librank output:</p>");
        if (!standalone) {
            ret.addLine("<p class=\"hint\">(Limited to " + limit + " items, <a href=\"" +
                    br.getRelDataDir() + getHtmlLinkTo(list) + "\">click here for a full list</a>)</p>");
        }
        ret.addLine("</a>");

        ret.addLine("<table class=\"librank-stat tablesorter\">");
        ret.addLine("  <thead>");
        ret.addLine("  <tr class=\"librank-stat-header\">");
        ret.addLine("    <th>Memory</td>");
        ret.addLine("    <th>VSS (KB)</td>");
        ret.addLine("    <th>RSS (KB)</td>");
        ret.addLine("    <th>PSS (KB)</td>");
        ret.addLine("    <th>USS (KB)</td>");
        ret.addLine("  </tr>");
        ret.addLine("  </thead>");
        ret.addLine("  <tbody>");

        int sumVss = 0;
        int sumRss = 0;
        int sumPss = 0;
        int sumUss = 0;

        int cnt = 0;
        boolean limitReached = false;
        for (LRMemInfo mi : list) {
            if (++cnt > limit) {
                limitReached = true;
                break;
            }
            sumVss += mi.vss;
            sumRss += mi.rss;
            sumPss += mi.pss;
            sumUss += mi.uss;

            ret.addLine("  <tr>");
            ret.addLine("    <td>" + mi.memName + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(mi.vss, "kb") + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(mi.rss, "kb") + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(mi.pss, "kb") + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(mi.uss, "kb") + "</td>");
            ret.addLine("  </tr>");
        }

        ret.addLine("  </tbody>");
        ret.addLine("  <tbody>");
        ret.addLine("  <tr>");
        if (limitReached) {
            ret.addLine("    <td>...</td>");
            ret.addLine("    <td>...</td>");
            ret.addLine("    <td>...</td>");
            ret.addLine("    <td>...</td>");
            ret.addLine("    <td>...</td>");
        } else {
            ret.addLine("    <td>TOTAL:</td>");
            ret.addLine("    <td>" + Util.shadeValue(sumVss, "kb") + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(sumRss, "kb") + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(sumPss, "kb") + "</td>");
            ret.addLine("    <td>" + Util.shadeValue(sumUss, "kb") + "</td>");
        }
        ret.addLine("  </tr>");
        ret.addLine("  </tbody>");
        ret.addLine("</table>");

        return ret;
    }


    static class DatabaseInfo {
        int pgsz, dbsz, lookaside;
        String name;
    }

    static class MemInfo {
        int pid;
        String name;
        int sizeNative, sizeDalvik, sizeTotal;
        int allocNative, allocDalvik, allocTotal;
        int freeNative, freeDalvik, freeTotal;
        int pssNative, pssDalvik, pssOther, pssTotal;
        int sharedNative, sharedDalvik, sharedOther, sharedTotal;
        int privNative, privDalvik, privOther, privTotal;
        int views, viewRoots;
        int appContexts, activities;
        int assets, assetManagers;
        int localBinders, proxyBinders;
        int deathRec, openSSLSockets;
        int sqlHeap, sqlMemUsed, sqlPageCacheOverflow, sqlMallocSize;
        Vector<DatabaseInfo> dbs = new Vector<DatabaseInfo>();
    }

    static class LRMemInfo {
        String memName;
        String procName;
        int pid;
        int vss, rss, pss, uss;
    }

    static class LRMemInfoList extends Vector<LRMemInfo> {
        private static final long serialVersionUID = 1L;
        public int id;
        public String name;

        public LRMemInfoList(int id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    public int getTotalMem() {
        return mTotMem;
    }

}
