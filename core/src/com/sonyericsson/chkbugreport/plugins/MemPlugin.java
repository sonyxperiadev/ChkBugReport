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

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ImageCanvas;
import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Plugin;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.Section;
import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Hint;
import com.sonyericsson.chkbugreport.doc.Img;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.Para;
import com.sonyericsson.chkbugreport.doc.PreText;
import com.sonyericsson.chkbugreport.doc.ProcessLink;
import com.sonyericsson.chkbugreport.doc.ShadedValue;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

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
        public Anchor anchor;
        public Anchor fileAnchor;

        public LRMemInfoList(int id, String name) {
            this.id = id;
            this.name = name;
            anchor = new Anchor("librank_list_" + id);
            fileAnchor = new Anchor("librank_list_" + id);
        }

    }

    @Override
    public int getPrio() {
        return 20;
    }

    @Override
    public void reset() {
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
    }

    @Override
    public void load(Module mod) {
        loadServMeminfoSec(mod);
        loadLibrankSec(mod);
    }

    @Override
    public void generate(Module mod) {
        Chapter ch = new Chapter(mod.getContext(), "Memory info");
        Section sec = null;

        // Handle the memory info section
        sec = mod.findSection(Section.MEMORY_INFO);
        if (sec == null) {
            mod.printErr(3, TAG + "Section not found: " + Section.MEMORY_INFO + " (ignoring section)");
        } else {
            generateMemoryInfoSec(mod, ch, sec);
        }

        // Handle the procrank section
        sec = mod.findSection(Section.PROCRANK);
        if (sec == null) {
            mod.printErr(3, TAG + "Section not found: " + Section.PROCRANK + " (ignoring section)");
        } else {
            generateProcrankSec(mod, ch, sec);
        }

        // Handle the meminfo section
        generateServMeminfoSec(mod, ch);

        // Handle the librank section
        generateLibrankSec(mod, ch);

        if (!ch.isEmpty()) {
            mod.addChapter(ch);
        } else {
            mod.printErr(3, TAG + "No usable sections found (aborting plugin)");
        }

    }

    private void generateMemoryInfoSec(Module mod, Chapter mainCh, Section sec) {
        Chapter ch = new Chapter(mod.getContext(), "From /proc/meminfo");

        // Create the chapter
        Table t = new Table();
        ch.add(t);
        t.addColumn("Memory type", Table.FLAG_NONE);
        t.addColumn("Value (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Explanation", Table.FLAG_NONE);
        t.begin();

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
                mod.printErr(4, TAG + "Error parsing number: " + value);
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
            t.addData(key);
            t.addData(new ShadedValue(intValue));
            t.addData(expl);
        }
        t.end();


        // Create some nice chart about it as well
        ImageCanvas img = new ImageCanvas(IW, IH);
        img.setColor(ImageCanvas.WHITE);
        img.fillRect(0, 0, IW, IH);

        // Draw the total memory area
        int b = 3;
        img.setColor(ImageCanvas.BLACK);
        img.fillRect(GML - b, GMT - b, GW + 2*b, GH + 2*b);
        img.setColor(ImageCanvas.LIGHT_GRAY);
        img.fillRect(GML, GMT, GW, GH);

        // Draw the free memory
        int yy = 0;
        int hh = mFreeMem * GH / mTotMem;
        drawBox(img, 0x00ff00, yy, hh);
        drawLabel(img, 0, hh, "Free");

        // Draw the buffers
        yy += hh;
        hh = mBuffers * GH / mTotMem;
        drawBox(img, 0x40ffff, yy, hh);
        int startPossFree = yy;

        // Draw the cache
        yy += hh;
        hh = mCaches * GH / mTotMem;
        drawBox(img, 0x8080ff, yy, hh);

        // Draw the slab-r
        yy += hh;
        hh = mSlabR * GH / mTotMem;
        drawBox(img, 0xffff000, yy, hh);

        // Draw the slab-u
        yy += hh;
        hh = mSlabU * GH / mTotMem;
        drawBox(img, 0xff00000, yy, hh);
        int endPossFree = yy;
        drawLabel(img, startPossFree, endPossFree, "Can be freed");

        // Write some more text on the chart
        img.drawString("Memory", 10, 10 + img.getAscent());
        img.drawString("overview", 10, 10 + img.getAscent() + img.getFontHeight());

        // Save the chart
        try {
            String fn = "meminfo.png";
            img.writeTo(new File(mod.getBaseDir() + fn));
            ch.add(new Block().add(new Img(fn)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainCh.addChapter(ch);
    }

    private void generateProcrankSec(Module mod, Chapter ch, Section sec) {
        try {
            generateProcrankSecUnsafe(mod, ch, sec);
        } catch (NumberFormatException e) {
            mod.printErr(3, TAG + "Failed gathering data from procrank output... Maybe procrank is buggy again?: " + e);
        }
    }

    private void generateProcrankSecUnsafe(Module mod_, Chapter mainCh, Section sec) {
        BugReportModule mod = (BugReportModule) mod_;
        if (sec.getLineCount() < 10) {
            // Suspiciously small...
            mod.printErr(3, TAG + "procrank section is suspiciously small... ignoring it");
            return;
        }
        boolean showPerc = mTotMem > 0;
        Chapter ch = new Chapter(mod.getContext(), "From procrank");
        mainCh.addChapter(ch);
        Table t = new Table();
        ch.add(t);
        t.addColumn("Process", Table.FLAG_NONE);
        t.addColumn("Vss (KB)", "Virtual memory used in kilobytes", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Rss (KB)", "Real memory used in kilobytes", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Pss (KB)", "Unique and ratio of shared memory used in kilobytes", Table.FLAG_ALIGN_RIGHT);
        if (showPerc){
            t.addColumn("(%)", "Pss as percentage of total memory (available for android)", Table.FLAG_ALIGN_RIGHT);
        }
        t.addColumn("Sum(Pss,0..i)", "The sum of this and all previous Pss values", Table.FLAG_ALIGN_RIGHT);
        if (showPerc) {
            t.addColumn("(%)", "The sum of Pss as percentage of total memory (available for android)", Table.FLAG_ALIGN_RIGHT);
        }
        t.addColumn("Uss (KB)", "Unique (used by only this process) memory used in kilobytes", Table.FLAG_ALIGN_RIGHT);
        if (showPerc) {
            t.addColumn("(%)", "Uss as percentage of total memory (available for android)", Table.FLAG_ALIGN_RIGHT);
        }
        t.addColumn("Sum(Uss,0..i)", "The sum of this and all previous Uss values", Table.FLAG_ALIGN_RIGHT);
        if (showPerc) {
            t.addColumn("%", "The sum of Uss as percentage of total memory (available for android)", Table.FLAG_ALIGN_RIGHT);
        }
        t.begin();

        int sumPss = 0, sumUss = 0;
        // Read data, skip first (header) line
        for (int i = 1; i < sec.getLineCount(); i++) {
            String line = sec.getLine(i);
            if (line.startsWith("[")) break;
            if (line.startsWith("      ")) break;
            int pid = Util.parseInt(line, 0, 5, 0);
            int vss = Util.parseInt(line, 6, 13, 0);
            int rss = Util.parseInt(line, 15, 22, 0);
            int pss = Util.parseInt(line, 24, 31, 0);
            int uss = Util.parseInt(line, 33, 40, 0);
            sumPss += pss;
            sumUss += uss;

            t.addData(new ProcessLink(mod, pid));
            t.addData(new ShadedValue(vss));
            t.addData(new ShadedValue(rss));
            t.addData(new ShadedValue(pss));
            if (showPerc) {
                t.addData(String.format("%.1f%%", pss * 100.0f / mTotMem));
            }
            t.addData(new ShadedValue(sumPss));
            if (showPerc) {
                t.addData(String.format("%.1f%%", sumPss * 100.0f / mTotMem));
            }
            t.addData(new ShadedValue(uss));
            if (showPerc) {
                t.addData(String.format("%.1f%%", uss * 100.0f / mTotMem));
            }
            t.addData(new ShadedValue(sumUss));
            if (showPerc) {
                t.addData(String.format("%.1f%%", sumUss * 100.0f / mTotMem));
            }
        }

        t.end();
    }

    private void loadServMeminfoSec(Module mod) {
        try {
            loadServMeminfoSecUnsafe(mod);
        } catch (Exception e) {
            mod.printErr(3, TAG + "Failed gathering data from meminfo service output... Maybe output format has changed?: " + e);
            mMemInfos.clear();
        }
    }

    private void loadServMeminfoSecUnsafe(Module mod_) {
        BugReportModule mod = (BugReportModule) mod_;

        Section sec = mod.findSection(Section.DUMP_OF_SERVICE_MEMINFO);
        if (sec == null) {
            mod.printErr(3, TAG + "Section not found: " + Section.DUMP_OF_SERVICE_MEMINFO + " (ignoring section)");
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
        DocNode memInfoLines = null;
        char mode = 'm';
        while (true) {
            String line = (idx < cnt) ? sec.getLine(idx++) : null;
            if (line == null || line.startsWith(key)) {
                // Finish previous data
                memInfo = null;
                memInfoLines = null;
            }
            if (line == null) {
                break; // no more lines to process
            }
            if (line.startsWith(key)) {
                // Start processing new pid
                memInfo = new MemInfo();

                Block blk = new Block();
                blk.add(new Para().add("Mem info from 'meminfo' service:"));
                memInfoLines = new PreText().addStyle("box");
                blk.add(memInfoLines);
                memInfoLines.addln(line);

                line = line.substring(key.length());
                int spc = line.indexOf(' ');
                memInfo.pid = Integer.parseInt(line.substring(0, spc));
                int nameS = line.indexOf('[');
                int nameE = line.indexOf(']');
                memInfo.name = line.substring(nameS + 1, nameE);
                mode = 'm'; // memory
                mMemInfos.add(memInfo);

                ProcessRecord pr = mod.getProcessRecord(memInfo.pid, true, true);
                pr.add(blk);
                pr.suggestName(memInfo.name, 45);

            } else {
                // Add more data to started pid
                memInfoLines.addln(line);

                if (mode == 'm') {
                    if (line.startsWith("            size:")) {
                        memInfo.sizeNative = Util.parseInt(line, 18, 26, 0);
                        memInfo.sizeDalvik = Util.parseInt(line, 27, 35, 0);
                        memInfo.sizeTotal = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("       allocated:")) {
                        memInfo.allocNative = Util.parseInt(line, 18, 26, 0);
                        memInfo.allocDalvik = Util.parseInt(line, 27, 35, 0);
                        memInfo.allocTotal = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("            free:")) {
                        memInfo.freeNative = Util.parseInt(line, 18, 26, 0);
                        memInfo.freeDalvik = Util.parseInt(line, 27, 35, 0);
                        memInfo.freeTotal = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("           (Pss):")) {
                        memInfo.pssNative = Util.parseInt(line, 18, 26, 0);
                        memInfo.pssDalvik = Util.parseInt(line, 27, 35, 0);
                        memInfo.pssOther = Util.parseInt(line, 36, 44, 0);
                        memInfo.pssTotal = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("  (shared dirty):")) {
                        memInfo.sharedNative = Util.parseInt(line, 18, 26, 0);
                        memInfo.sharedDalvik = Util.parseInt(line, 27, 35, 0);
                        memInfo.sharedOther = Util.parseInt(line, 36, 44, 0);
                        memInfo.sharedTotal = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("    (priv dirty):")) {
                        memInfo.privNative = Util.parseInt(line, 18, 26, 0);
                        memInfo.privDalvik = Util.parseInt(line, 27, 35, 0);
                        memInfo.privOther = Util.parseInt(line, 36, 44, 0);
                        memInfo.privTotal = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("           Views:")) {
                        memInfo.views = Util.parseInt(line, 18, 26, 0);
                        memInfo.viewRoots = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("     AppContexts:")) {
                        memInfo.appContexts = Util.parseInt(line, 18, 26, 0);
                        memInfo.activities = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("          Assets:")) {
                        memInfo.assets = Util.parseInt(line, 18, 26, 0);
                        memInfo.assetManagers = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("   Local Binders:")) {
                        memInfo.localBinders = Util.parseInt(line, 18, 26, 0);
                        memInfo.proxyBinders = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("Death Recipients:")) {
                        memInfo.deathRec = Util.parseInt(line, 18, 26, 0);
                    } else if (line.startsWith(" OpenSSL Sockets:")) {
                        memInfo.openSSLSockets = Util.parseInt(line, 18, 26, 0);
                    } else if (line.startsWith(" SQL")) {
                        mode = 's';
                    }
                } else if (mode == 's') {
                    if (line.startsWith("               heap:")) {
                        // 2.3
                        memInfo.sqlHeap = Util.parseInt(line, 21, 29, 0);
                        memInfo.sqlMemUsed = Util.parseInt(line, 51, 59, 0);
                    } else if (line.startsWith(" PAGECACHE_OVERFLOW:")) {
                        // 2.3
                        memInfo.sqlPageCacheOverflow = Util.parseInt(line, 21, 29, 0);
                        memInfo.sqlMallocSize = Util.parseInt(line, 51, 59, 0);
                    } else if (line.startsWith("            heap:")) {
                        // < 2.3
                        memInfo.sqlHeap = Util.parseInt(line, 18, 26, 0);
                        memInfo.sqlMemUsed = Util.parseInt(line, 45, 53, 0);
                    } else if (line.startsWith("pageCacheOverflo:")) {
                        // < 2.3
                        memInfo.sqlPageCacheOverflow = Util.parseInt(line, 18, 26, 0);
                        memInfo.sqlMallocSize = Util.parseInt(line, 45, 53, 0);
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
                                dbInfo.lookaside = Util.parseInt(line, 20, 34, 0);
                            }
                        } else {
                            // < 2.3
                            dbInfo.lookaside = Util.parseInt(line, 20, 30, 0);
                            dbInfo.name = line.substring(32);
                        }
                        if (!dbInfo.name.contains("(pooled")) {
                            dbInfo.pgsz = Util.parseInt(line, 1, 10, 0);
                            dbInfo.dbsz = Util.parseInt(line, 11, 19, 0);
                        }
                        memInfo.dbs.add(dbInfo);
                    }
                }
            }
        }
    }

    private void generateServMeminfoSec(Module mod_, Chapter mainCh) {
        BugReportModule mod = (BugReportModule) mod_;

        if (mMemInfos.size() == 0) return;

        Chapter ch = new Chapter(mod.getContext(), "From meminfo service");
        mainCh.addChapter(ch);

        // Sort mem info based on pss
        Collections.sort(mMemInfos, new Comparator<MemInfo>() {
            @Override
            public int compare(MemInfo o1, MemInfo o2) {
                return o2.pssTotal - o1.pssTotal;
            }
        });

        boolean showPerc = mTotMem > 0;
        int sumPss = 0;

        Table t = new Table();
        ch.add(t);
        t.addColumn("Process", Table.FLAG_NONE);
        t.addColumn("Native size (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Native alloc (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Native free (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Dalvik size (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Dalvik alloc (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Dalvik free (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Pss other (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Shared dirty other (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Priv dirty other (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Pss total (KB)", Table.FLAG_ALIGN_RIGHT);
        if (showPerc){
            t.addColumn("(%)", "Pss as percentage of total memory (available for android)", Table.FLAG_ALIGN_RIGHT);
        }
        t.addColumn("Sum(Pss,0..i)", "The sum of this and all previous Pss values", Table.FLAG_ALIGN_RIGHT);
        if (showPerc) {
            t.addColumn("(%)", "The sum of Pss as percentage of total memory (available for android)", Table.FLAG_ALIGN_RIGHT);
        }
        t.addColumn("Shared dirty total (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("Priv dirty total (KB)", Table.FLAG_ALIGN_RIGHT);

        t.begin();
        for (MemInfo mi : mMemInfos) {
            sumPss += mi.pssTotal;
            t.addData(new ProcessLink(mod, mi.pid));
            t.addData(new ShadedValue(mi.sizeNative));
            t.addData(new ShadedValue(mi.allocNative));
            t.addData(new ShadedValue(mi.freeNative));
            t.addData(new ShadedValue(mi.sizeDalvik));
            t.addData(new ShadedValue(mi.allocDalvik));
            t.addData(new ShadedValue(mi.freeDalvik));
            t.addData(new ShadedValue(mi.pssOther));
            t.addData(new ShadedValue(mi.sharedOther));
            t.addData(new ShadedValue(mi.privOther));
            t.addData(new ShadedValue(mi.pssTotal));
            if (showPerc) {
                t.addData(String.format("%.1f%%", mi.pssTotal * 100.0f / mTotMem));
            }
            t.addData(new ShadedValue(sumPss));
            if (showPerc) {
                t.addData(String.format("%.1f%%", sumPss * 100.0f / mTotMem));
            }
            t.addData(new ShadedValue(mi.sharedTotal));
            t.addData(new ShadedValue(mi.privTotal));
        }
        t.end();
    }

    private void drawLabel(ImageCanvas g, int y0, int y1, String msg) {
        y0 += GMT;
        y1 += GMT;
        int xx = GML + GW;
        g.setColor(ImageCanvas.DARK_GRAY);
        g.drawLine(xx, y0, xx + 16, y0);
        g.drawLine(xx, y1, xx + 16, y1);
        g.drawLine(xx + 16, y0, xx + 16, y1);
        g.drawString(msg, xx + 24, (y0 + y1) / 2);
    }

    private void drawBox(ImageCanvas g, int color, int yy, int hh) {
        int lightColor = (color & 0xfefefe + 0xfefefe) / 2;
        g.setColor(0xff000000 | color);
        g.fillRect(GML, GMT + yy, GW, hh);
        g.setColor(0xff000000 | lightColor);
        g.drawRect(GML, GMT + yy, GW - 1, hh - 1);
    }

    private void loadLibrankSec(Module mod) {
        try {
            loadLibrankSecUnsafe(mod);
        } catch (Exception e) {
            mod.printErr(3, TAG + "Failed gathering data from librank output... Maybe it's broken again?: " + e);
            mLRMemInfoMem.clear();
            mLRMemInfoPid.clear();
        }
    }

    private void loadLibrankSecUnsafe(Module mod) {
        Section s = mod.findSection(Section.LIBRANK);
        if (s == null) {
            // Suspiciously small...
            mod.printErr(3, TAG + "librank section is missing... ignoring it");
            return;
        }
        int cnt = s.getLineCount();
        if (cnt < 10) {
            // Suspiciously small...
            mod.printErr(3, TAG + "librank section is suspiciously small... ignoring it");
            return;
        }
        String line = s.getLine(0);
        if (!line.equals(" RSStot      VSS      RSS      PSS      USS  Name/PID")) {
            mod.printErr(3, TAG + "librank section format not supported... ignoring it");
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
                    mod.printErr(3, TAG + "Parse error in librank output... trying to continue though (line: " + i + ")");
                } else {
                    // Parse the data
                    LRMemInfo mi = new LRMemInfo();
                    mi.memName = memName;
                    mi.vss = Util.parseInt(line, 8, 15, 0);
                    mi.rss = Util.parseInt(line, 17, 24, 0);
                    mi.pss = Util.parseInt(line, 26, 33, 0);
                    mi.uss = Util.parseInt(line, 35, 42, 0);
                    line = line.substring(46);
                    mi.procName = Util.extract(line, " ", " ");
                    mi.pid = Util.parseInt(Util.extract(line, "[", "]"), 0);

                    // Add to the mem stat
                    LRMemInfoList list = mLRMemInfoMem.get(memName);
                    if (list == null) {
                        list = new LRMemInfoList(mi.pid, memName);
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

    private void generateLibrankSec(Module mod_, Chapter mainCh) {
        BugReportModule mod = (BugReportModule) mod_;
        if (mLRMemInfoMem.size() == 0 || mLRMemInfoPid.size() == 0) {
            return;
        }

        // First create a chapter for memory ranges
        Chapter ch = new Chapter(mod.getContext(), "Librank - by pid");
        mainCh.addChapter(ch);

        // Create copies of the list and sort them
        Vector<LRMemInfoList> tmp1 = new Vector<LRMemInfoList>();
        Vector<LRMemInfoList> tmp2 = new Vector<LRMemInfoList>();
        for (LRMemInfoList list : mLRMemInfoPid.values()) {
            tmp1.add(list);
            tmp2.add(list);
        }
        Collections.sort(tmp1, new Comparator<LRMemInfoList>() {
            @Override
            public int compare(LRMemInfoList o1, LRMemInfoList o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        Collections.sort(tmp2, new Comparator<LRMemInfoList>() {
            @Override
            public int compare(LRMemInfoList o1, LRMemInfoList o2) {
                return o1.id - o2.id;
            }
        });

        Table t = new Table();
        ch.add(t);
        t.addColumn("Process sorted by name", Table.FLAG_NONE);
        t.addColumn("Process sorted by pid", Table.FLAG_NONE);
        t.begin();
        for (int i = 0; i < mLRMemInfoPid.size(); i++) {
            LRMemInfoList list1 = tmp1.get(i);
            t.addData(new Link(list1.anchor, list1.name + "(" + list1.id + ")"));
            LRMemInfoList list2 = tmp2.get(i);
            t.addData(new Link(list2.anchor, list2.name + "(" + list2.id + ")"));
        }
        t.end();

        // Generate the actual data
        for (LRMemInfoList list : mLRMemInfoPid.values()) {
            sortList(list);
            DocNode data = generateLibrankStat(mod, list, 10);
            ProcessRecord pr = mod.getProcessRecord(list.id, true, true);
            pr.add(data);

            // Save the full table as well, but in separate file
            DocNode longData = generateLibrankStat(mod, list, Integer.MAX_VALUE);
            String title = "Memory usage of process " + list.name + " (" + list.id + ") from librank output:";
            Chapter extFile = new Chapter(mod.getContext(), title);
            extFile.add(longData);
            mod.addExtraFile(extFile);
        }
    }

    private void sortList(LRMemInfoList list) {
        Collections.sort(list, new Comparator<LRMemInfo>() {
            @Override
            public int compare(LRMemInfo o1, LRMemInfo o2) {
                return o2.pss - o1.pss;
            }
        });
    }

    private DocNode generateLibrankStat(BugReportModule mod, LRMemInfoList list, int limit) {
        Block ret = new Block();

        boolean standalone = limit == Integer.MAX_VALUE;

        Para header = new Para();
        ret.add(header);
        header.add("Memory usage of process ");
        header.add(new ProcessLink(mod, list.id));
        header.add(" from librank output:");

        if (!standalone) {
            ret.add(list.anchor);
            Hint hint = new Hint(ret);
            hint.add("Limited to " + limit + " items, ");
            hint.add(new Link(list.fileAnchor, "click here for a full list"));
        } else {
            ret.add(list.fileAnchor);
        }

        Table t = new Table(Table.FLAG_SORT);
        ret.add(t);
        t.addColumn("Memory", Table.FLAG_NONE);
        t.addColumn("VSS (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("RSS (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("PSS (KB)", Table.FLAG_ALIGN_RIGHT);
        t.addColumn("USS (KB)", Table.FLAG_ALIGN_RIGHT);
        t.begin();

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

            t.addData(mi.memName);
            t.addData(new ShadedValue(mi.vss));
            t.addData(new ShadedValue(mi.rss));
            t.addData(new ShadedValue(mi.pss));
            t.addData(new ShadedValue(mi.uss));
        }

        t.addSeparator();

        if (limitReached) {
            t.addData("...");
            t.addData("...");
            t.addData("...");
            t.addData("...");
            t.addData("...");
        } else {
            t.addData("TOTAL:");
            t.addData(new ShadedValue(sumVss));
            t.addData(new ShadedValue(sumRss));
            t.addData(new ShadedValue(sumPss));
            t.addData(new ShadedValue(sumUss));
        }
        t.end();

        return ret;
    }

    public int getTotalMem() {
        return mTotMem;
    }


}
