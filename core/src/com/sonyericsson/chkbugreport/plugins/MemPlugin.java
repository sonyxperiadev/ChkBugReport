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
import com.sonyericsson.chkbugreport.doc.SimpleText;
import com.sonyericsson.chkbugreport.doc.Table;
import com.sonyericsson.chkbugreport.doc.ThresholdedValue;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Note: some of the explanation is taken from: http://www.redhat.com/advice/tips/meminfo.html
 */
public class MemPlugin extends Plugin {

    private static final String TAG = "[MemPlugin]";

    private static final Pattern LIB_RANK_HEADER_PATTERN = Pattern.compile("\\s+RSStot\\s+VSS\\s+RSS\\s+PSS\\s+USS\\s+(Swap\\s+)?Name\\/PID.*");
    Pattern PROCRANK_HEADER_PATTERN = Pattern.compile("\\s+PID\\s+Vss\\s+Rss\\s+Pss\\s+Uss\\s+Swap\\s+PSwap\\s+USwap\\s+ZSwap\\s+cmdline.*");
    private static final Pattern K_PATTERN = Pattern.compile("(\\d+)K");

    private static final Pattern P_PROCESS_HEADER_LINE = Pattern.compile("\\*\\*\\s+MEMINFO in pid (\\d+) \\[(.*)\\] \\*\\*");

    private static final String MEM_TABLE_HEADER_L1 = "\\s+Pss\\s+Pss\\s+Shared\\s+Private\\s+Shared\\s+Private\\s+SwapPss\\s+Heap\\s+Heap\\s+Heap.*";
    private static final String MEM_TABLE_HEADER_L2 = "\\s+Total\\s+Clean\\s+Dirty\\s+Dirty\\s+Clean\\s+Clean\\s+Dirty\\s+Size\\s+Alloc\\s+Free.*";
    private static final String MEM_TABLE_HEADER_DIVIDER = "^[\\s-]+$";
    private static final Pattern MEM_TABLE_LINE = Pattern.compile("\\s+(.*?)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)?\\s+(\\d+)?\\s+(\\d+)?");

    private static final Pattern APP_SUMMARY_TABLE_LINE = Pattern.compile("\\s+(.*?)\\:\\s+(\\d+)(?:\\s+TOTAL SWAP PSS\\:\\s+(\\d+))?.*");

    private static final Pattern TWO_ITEM_TABLE_LINE = Pattern.compile("\\s+(.*?):\\s+(\\d+)(?:\\s+(.*):\\s+(\\d+))?");

    private static final String DATABASE_HEADER_L1 = "\\s+pgsz\\s+dbsz\\s+Lookaside\\(b\\)\\s+cache\\s+Dbname";
    private static final Pattern DATABASE_TABLE_LINE = Pattern.compile("\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+([\\d\\/]+)\\s+(\\S+).*");

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

    private enum Mode {
        MEMORY, DALVIK, SUMMARY, OBJECTS, SQL, DATABASES
    }

    public static class DatabaseInfo {
        DatabaseInfo(String from) {
            Matcher m = DATABASE_TABLE_LINE.matcher(from);
            if(!m.matches()) {
                throw new IllegalArgumentException("Invalid database row: " + from);
            }
            pgsz = Integer.parseInt(m.group(1));
            dbsz = Integer.parseInt(m.group(2));
            lookaside = Integer.parseInt(m.group(3));
            cache = m.group(4);
            name = m.group(5);
        }
        public int pgsz, dbsz, lookaside;
        public String name, cache;
    }

    public static class MemInfoRow {
        MemInfoRow(String from) {
            Matcher m = MEM_TABLE_LINE.matcher(from);
            if(!m.matches()) {
                throw new IllegalArgumentException("Invalid memory row: " + from);
            }
            pssTotal = Integer.parseInt(m.group(2));
            pssClean = Integer.parseInt(m.group(3));
            sharedDirty = Integer.parseInt(m.group(4));
            privateDirty = Integer.parseInt(m.group(5));
            sharedClean = Integer.parseInt(m.group(6));
            privateClean = Integer.parseInt(m.group(7));
            swapPssDirty = Integer.parseInt(m.group(8));
            heapSize = (m.group(9) != null) ? Integer.parseInt(m.group(9)) : -1;
            heapAlloc = (m.group(10) != null) ? Integer.parseInt(m.group(10)) : -1;
            heapFree = (m.group(11) != null) ? Integer.parseInt(m.group(11)) : -1;
        }
        public int pssTotal;
        public int pssClean;
        public int sharedDirty;
        public int privateDirty;
        public int sharedClean;
        public int privateClean;
        public int swapPssDirty;
        public int heapSize;
        public int heapAlloc;
        public int heapFree;
    }

    public static class MemInfo {
        public int pid;
        public String name;

        //Meminfo:
        public MemInfoRow nativeHeap;
        public MemInfoRow dalvikHeap;
        public MemInfoRow dalvikOther;
        public MemInfoRow stack;
        public MemInfoRow ashMem;
        public MemInfoRow gfxDev;
        public MemInfoRow otherDev;
        public MemInfoRow soMmap;
        public MemInfoRow jarMmap;
        public MemInfoRow apkMmap;
        public MemInfoRow ttfMmap;
        public MemInfoRow dexMmap;
        public MemInfoRow oatMmap;
        public MemInfoRow artMmap;
        public MemInfoRow otherMmap;
        public MemInfoRow eglMTrack;
        public MemInfoRow glMtrack;
        public MemInfoRow unknown;
        public MemInfoRow total;

        //Dalvic Details:
        public MemInfoRow dalvikDetailsHeap;
        public MemInfoRow dalvikDetailsLos;
        public MemInfoRow dalvikDetailsZygote;
        public MemInfoRow dalvikDetailsNonMoving;
        public MemInfoRow dalvikDetailsLinearAlloc;
        public MemInfoRow dalvikDetailsGc;
        public MemInfoRow dalvikDetailsIndirectRef;
        public MemInfoRow dalvikDetailsBootVdex;
        public MemInfoRow dalvikDetailsAppDex;
        public MemInfoRow dalvikDetailsAppVDex;
        public MemInfoRow dalvikDetailsAppArt;
        public MemInfoRow dalvikDetailsBootArt;

        //App Summary
        public int summaryJavaHeap;
        public int summaryNativeHeap;
        public int summaryCode;
        public int summaryStack;
        public int summaryGraphics;
        public int summaryPrivateOther;
        public int summarySystem;
        public int summaryTotal;
        public int summaryTotalSwapPSS;

        //Objects:
        public int views, viewRoots;
        public int appContexts, activities;
        public int assets, assetManagers;
        public int localBinders, proxyBinders;
        public int parcelMemory, parcelCount;
        public int deathRec, openSSLSockets;
        public int webViews;

        //SQL:
        public int sqlMemUsed, sqlPageCacheOverflow, sqlMallocSize;
        public Vector<DatabaseInfo> dbs = new Vector<DatabaseInfo>();

    }

    static class LRMemInfo {
        String memName;
        String procName;
        int pid;
        int vss, rss, pss, uss, swap;
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
        Matcher header = PROCRANK_HEADER_PATTERN.matcher(sec.getLine(0));
        if(!header.matches()) {
            mod.printErr(3, TAG + "procrank section has unknown format... ignoring it");
            return;
        }

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
            //Split and parse based on spaces:
            String splitLine[] = line.trim().split("\\s+");
            int pid = Util.parseInt(splitLine[0], 0);
            int vss = Util.parseInt(removeK(mod, splitLine[1]), 0);
            int rss = Util.parseInt(removeK(mod, splitLine[2]), 0);
            int pss = Util.parseInt(removeK(mod, splitLine[3]), 0);
            int uss = Util.parseInt(removeK(mod, splitLine[4]), 0);
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
        Mode mode = Mode.MEMORY;
        while (true) {
            String line = (idx < cnt) ? sec.getLine(idx++) : null;

            if (line == null) {
                break; // no more lines to process
            }

            if(line.trim().equals("")) {
                continue; //Skip blank lines.
            }

            Matcher processStartMatcher = P_PROCESS_HEADER_LINE.matcher(line);
            if (processStartMatcher.matches()) { //Detect start of new process + Mode.MEMORY
                // Start processing new pid
                memInfo = new MemInfo();

                Block blk = new Block();
                blk.add(new Para().add("Mem info from 'meminfo' service:"));
                memInfoLines = new PreText().addStyle("box");
                blk.add(memInfoLines);
                memInfoLines.addln(line);

                memInfo.pid = Integer.parseInt(processStartMatcher.group(1));
                memInfo.name = processStartMatcher.group(2);

                mode = Mode.MEMORY; // memory
                mMemInfos.add(memInfo);

                //Confirm that the header has not changed:
                String header1 = sec.getLine(idx++);
                String header2 = sec.getLine(idx++);
                String header3 = sec.getLine(idx++);
                memInfoLines.addln(header1);
                memInfoLines.addln(header2);
                memInfoLines.addln(header3);
                if(!header1.matches(MEM_TABLE_HEADER_L1) || !header2.matches(MEM_TABLE_HEADER_L2) || !header3.matches(MEM_TABLE_HEADER_DIVIDER)) {
                    throw new IllegalArgumentException("Meminfo table format does not match parser");
                }

                ProcessRecord pr = mod.getProcessRecord(memInfo.pid, true, true);
                pr.add(blk);
                pr.suggestName(memInfo.name, 45);

            } else if(line.trim().equals("Dalvik Details")) {
                mode = Mode.DALVIK;
                memInfoLines.addln(line);
            } else if(line.trim().equals("App Summary")) {
                mode = Mode.SUMMARY;
                memInfoLines.addln(line);
            } else if(line.trim().equals("Objects")) {
                mode = Mode.OBJECTS;
                memInfoLines.addln(line);
            } else if(line.trim().equals("SQL")) {
                mode = Mode.SQL;
                memInfoLines.addln(line);
            } else if(line.trim().equals("DATABASES")) {
                mode = Mode.DATABASES;
                memInfoLines.addln(line);

                String databaseHeader = sec.getLine(idx++);
                memInfoLines.addln(databaseHeader);
                if(!databaseHeader.matches(DATABASE_HEADER_L1)) {
                    throw new IllegalArgumentException("Unexpected database table format!");
                }
            } else { //Process section contents based on the mode:
                // Add more data to started pid
                memInfoLines.addln(line);

                Matcher parsedMemLine = MEM_TABLE_LINE.matcher(line);
                Matcher twoItemLine = TWO_ITEM_TABLE_LINE.matcher(line);

                switch(mode) {
                    case MEMORY:
                        if(!parsedMemLine.matches()) {
                            continue;
                        }
                        switch(parsedMemLine.group(1)) {
                            case "Native Heap":
                                memInfo.nativeHeap = new MemInfoRow(line);
                                break;
                            case "Dalvik Heap":
                                memInfo.dalvikHeap = new MemInfoRow(line);
                                break;
                            case "Dalvik Other":
                                memInfo.dalvikOther = new MemInfoRow(line);
                                break;
                            case "Stack":
                                memInfo.stack = new MemInfoRow(line);
                                break;
                            case "Ashmem":
                                memInfo.ashMem = new MemInfoRow(line);
                                break;
                            case "Gfx dev":
                                memInfo.gfxDev = new MemInfoRow(line);
                                break;
                            case "Other dev":
                                memInfo.otherDev = new MemInfoRow(line);
                                break;
                            case ".so mmap":
                                memInfo.soMmap = new MemInfoRow(line);
                                break;
                            case ".jar mmap":
                                memInfo.jarMmap = new MemInfoRow(line);
                                break;
                            case ".apk mmap":
                                memInfo.apkMmap = new MemInfoRow(line);
                                break;
                            case ".ttf mmap":
                                memInfo.ttfMmap = new MemInfoRow(line);
                                break;
                            case ".dex mmap":
                                memInfo.dexMmap = new MemInfoRow(line);
                                break;
                            case ".oat mmap":
                                memInfo.oatMmap = new MemInfoRow(line);
                                break;
                            case ".art mmap":
                                memInfo.artMmap = new MemInfoRow(line);
                                break;
                            case "Other mmap":
                                memInfo.otherMmap = new MemInfoRow(line);
                                break;
                            case "EGL mtrack":
                                memInfo.eglMTrack = new MemInfoRow(line);
                            case "GL mtrack":
                                memInfo.glMtrack = new MemInfoRow(line);
                                break;
                            case "Unknown":
                                memInfo.unknown = new MemInfoRow(line);
                                break;
                            case "TOTAL":
                                memInfo.total = new MemInfoRow(line);
                                break;
                            default:
                                mod.printErr(4, TAG + "Unknown row in memory table: " + line);
                        }
                        break;
                    case DALVIK:
                        if(!parsedMemLine.matches()) {
                            continue;
                        }
                        switch(parsedMemLine.group(1)) {
                            case ".Heap":
                                memInfo.dalvikDetailsHeap = new MemInfoRow(line);
                                break;
                            case ".LOS":
                                memInfo.dalvikDetailsLos = new MemInfoRow(line);
                                break;
                            case ".Zygote":
                                memInfo.dalvikDetailsZygote = new MemInfoRow(line);
                                break;
                            case ".NonMoving":
                                memInfo.dalvikDetailsNonMoving = new MemInfoRow(line);
                                break;
                            case ".LinearAlloc":
                                memInfo.dalvikDetailsLinearAlloc = new MemInfoRow(line);
                                break;
                            case ".GC":
                                memInfo.dalvikDetailsGc = new MemInfoRow(line);
                                break;
                            case ".IndirectRef":
                                memInfo.dalvikDetailsIndirectRef = new MemInfoRow(line);
                                break;
                            case ".Boot vdex":
                                memInfo.dalvikDetailsBootVdex = new MemInfoRow(line);
                                break;
                            case ".App dex":
                                memInfo.dalvikDetailsAppDex = new MemInfoRow(line);
                                break;
                            case ".App vdex":
                                memInfo.dalvikDetailsAppVDex = new MemInfoRow(line);
                                break;
                            case ".App art":
                                memInfo.dalvikDetailsAppArt = new MemInfoRow(line);
                                break;
                            case ".Boot art":
                                memInfo.dalvikDetailsBootArt = new MemInfoRow(line);
                                break;
                            default:
                                mod.printErr(4, TAG + "Unknown row in dalvik details table: " + line);
                        }
                        break;
                    case SUMMARY:
                        Matcher summaryMatcher = APP_SUMMARY_TABLE_LINE.matcher(line);
                        if(!summaryMatcher.matches()) {
                            continue; //Skip unknown lines
                        }
                        switch(summaryMatcher.group(1)) {
                            case "Java Heap":
                                memInfo.summaryJavaHeap = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "Native Heap":
                                memInfo.summaryNativeHeap = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "Code":
                                memInfo.summaryCode = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "Stack":
                                memInfo.summaryStack = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "Graphics":
                                memInfo.summaryGraphics = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "Private Other":
                                memInfo.summaryPrivateOther = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "System":
                                memInfo.summarySystem = Integer.parseInt(summaryMatcher.group(2));
                                break;
                            case "TOTAL":
                                memInfo.summaryTotal = Integer.parseInt(summaryMatcher.group(2));
                                memInfo.summaryTotalSwapPSS = Integer.parseInt(summaryMatcher.group(3));
                                break;
                        }
                        break;
                    case OBJECTS:
                        if(!twoItemLine.matches()) {
                            continue; //Skip unknown lines
                        }
                        //We assume that first/second column match known patterns if this changes we would need to update this.
                        switch (twoItemLine.group(1)) {
                            case "Views":
                                memInfo.views = Integer.parseInt(twoItemLine.group(2));
                                memInfo.viewRoots = Integer.parseInt(twoItemLine.group(4));
                                break;
                            case "AppContexts":
                                memInfo.appContexts = Integer.parseInt(twoItemLine.group(2));
                                memInfo.activities = Integer.parseInt(twoItemLine.group(4));
                                break;
                            case "Assets":
                                memInfo.assets = Integer.parseInt(twoItemLine.group(2));
                                memInfo.assetManagers = Integer.parseInt(twoItemLine.group(4));
                                break;
                            case "Local Binders":
                                memInfo.localBinders = Integer.parseInt(twoItemLine.group(2));
                                memInfo.proxyBinders = Integer.parseInt(twoItemLine.group(4));
                                break;
                            case "Parcel memory":
                                memInfo.parcelMemory = Integer.parseInt(twoItemLine.group(2));
                                memInfo.parcelCount = Integer.parseInt(twoItemLine.group(4));
                                break;
                            case "Death Recipients":
                                memInfo.deathRec = Integer.parseInt(twoItemLine.group(2));
                                memInfo.openSSLSockets = Integer.parseInt(twoItemLine.group(4));
                                break;
                            case "WebViews":
                                memInfo.webViews = Integer.parseInt(twoItemLine.group(2));
                                break;
                            default:
                                mod.printErr(4, TAG + "Unknown object line: " + mode);
                        }
                        break;
                    case SQL:
                        if(!twoItemLine.matches()) {
                            continue; //Skip unknown lines
                        }
                        //We assume that first/second column match known patterns if this changes we would need to update this.
                        switch (twoItemLine.group(1)) {
                            case "MEMORY_USED":
                                memInfo.sqlMemUsed = Integer.parseInt(twoItemLine.group(2));
                                break;
                            case "PAGECACHE_OVERFLOW":
                                memInfo.sqlPageCacheOverflow = Integer.parseInt(twoItemLine.group(2));
                                memInfo.sqlMallocSize = Integer.parseInt(twoItemLine.group(4));
                                break;
                            default:
                                mod.printErr(4, TAG + "Unknown object line: " + line);
                        }
                    case DATABASES:
                        Matcher databaseLine = DATABASE_TABLE_LINE.matcher(line);
                        if(!databaseLine.matches()) {
                            continue; //Skip unknown lines
                        }
                        DatabaseInfo databaseInfo = new DatabaseInfo(line);
                        memInfo.dbs.add(databaseInfo);
                        break;
                    default:
                        mod.printErr(1, TAG + "Unhandled parsing mode: " + mode);
                }

            }
        }
    }

    private void generateServMeminfoSec(Module mod_, Chapter mainCh) {
        BugReportModule mod = (BugReportModule) mod_;

        if (mMemInfos.size() == 0) return;

        Chapter ch_meminfo_service_overview = new Chapter(mod.getContext(), "meminfo service - Overview");
        mainCh.addChapter(ch_meminfo_service_overview);

        Chapter ch_meminfo_service_pss_summary = new Chapter(mod.getContext(), "meminfo service - PSS Summary");
        mainCh.addChapter(ch_meminfo_service_pss_summary);

        Chapter ch_meminfo_service_objects = new Chapter(mod.getContext(), "meminfo service - Objects");
        mainCh.addChapter(ch_meminfo_service_objects);

        Chapter ch_meminfo_service_sql = new Chapter(mod.getContext(), "meminfo service - SQL");
        mainCh.addChapter(ch_meminfo_service_sql);

        Chapter ch_meminfo_service_databases = new Chapter(mod.getContext(), "meminfo service - Databases");
        mainCh.addChapter(ch_meminfo_service_databases);

        // Sort mem info based on pss
        Collections.sort(mMemInfos, new Comparator<MemInfo>() {
            @Override
            public int compare(MemInfo o1, MemInfo o2) {
                return o2.total.pssTotal - o1.total.pssTotal;
            }
        });

        Table overviewTable = new Table(Table.FLAG_SORT);
        ch_meminfo_service_overview.add(overviewTable);
        overviewTable.addColumn("Process", Table.FLAG_NONE);
        overviewTable.addColumn("Pss Total (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("PSS Clean (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Shared Dirty (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Private Dirty (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Shared Clean (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Private Clean (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("SwapPss Dirty", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Heap Size (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Heap Alloc (KB)", Table.FLAG_ALIGN_RIGHT);
        overviewTable.addColumn("Heap Free (KB)", Table.FLAG_ALIGN_RIGHT);

        overviewTable.begin();
        for (MemInfo mi : mMemInfos) {
            overviewTable.addData(new ProcessLink(mod, mi.pid));
            overviewTable.addData(new ShadedValue(mi.total.pssTotal));
            overviewTable.addData(new ShadedValue(mi.total.pssClean));
            overviewTable.addData(new ShadedValue(mi.total.sharedDirty));
            overviewTable.addData(new ShadedValue(mi.total.privateDirty));
            overviewTable.addData(new ShadedValue(mi.total.sharedClean));
            overviewTable.addData(new ShadedValue(mi.total.privateClean));
            overviewTable.addData(new ShadedValue(mi.total.swapPssDirty));
            overviewTable.addData(new ShadedValue(mi.total.heapSize));
            overviewTable.addData(new ShadedValue(mi.total.heapAlloc));
            overviewTable.addData(new ShadedValue(mi.total.heapFree));
        }
        overviewTable.end();

        Table pssSummaryTable = new Table(Table.FLAG_SORT);
        ch_meminfo_service_pss_summary.add(pssSummaryTable);
        pssSummaryTable.addColumn("Process", Table.FLAG_NONE);
        pssSummaryTable.addColumn("Java Heap (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Native Heap (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Code (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Stack (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Graphics (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Private Other (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("System", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Total (KB)", Table.FLAG_ALIGN_RIGHT);
        pssSummaryTable.addColumn("Total SWAP Pss (KB)", Table.FLAG_ALIGN_RIGHT);

        pssSummaryTable.begin();
        for (MemInfo mi : mMemInfos) {
            pssSummaryTable.addData(new ProcessLink(mod, mi.pid));
            pssSummaryTable.addData(new ShadedValue(mi.summaryJavaHeap));
            pssSummaryTable.addData(new ShadedValue(mi.summaryNativeHeap));
            pssSummaryTable.addData(new ShadedValue(mi.summaryCode));
            pssSummaryTable.addData(new ShadedValue(mi.summaryStack));
            pssSummaryTable.addData(new ShadedValue(mi.summaryGraphics));
            pssSummaryTable.addData(new ShadedValue(mi.summaryPrivateOther));
            pssSummaryTable.addData(new ShadedValue(mi.summarySystem));
            pssSummaryTable.addData(new ShadedValue(mi.summaryTotal));
            pssSummaryTable.addData(new ShadedValue(mi.summaryTotalSwapPSS));
        }
        pssSummaryTable.end();

        Table objectsTable = new Table(Table.FLAG_SORT);
        ch_meminfo_service_objects.add(objectsTable);
        objectsTable.addColumn("Process", Table.FLAG_NONE);
        objectsTable.addColumn("Views", Table.FLAG_NONE);
        objectsTable.addColumn("ViewRootImpl", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("AppContexts", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Activities", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Assets", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("AssetManagers", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Local Binders", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Proxy Binders", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Parcel Memory (KB)", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Parcel Count", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("Death Recipients", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("OpenSSL Sockets", Table.FLAG_ALIGN_RIGHT);
        objectsTable.addColumn("WebViews", Table.FLAG_ALIGN_RIGHT);

        objectsTable.begin();
        for (MemInfo mi : mMemInfos) {
            objectsTable.addData(new ProcessLink(mod, mi.pid));
            objectsTable.addData(new SimpleText(String.valueOf(mi.views)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.viewRoots)));
            objectsTable.addData(new ThresholdedValue(mi.appContexts, 20));
            objectsTable.addData(new SimpleText(String.valueOf(mi.activities)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.assets)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.assetManagers)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.localBinders)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.proxyBinders)));
            objectsTable.addData(new ShadedValue(mi.parcelMemory));
            objectsTable.addData(new SimpleText(String.valueOf(mi.parcelCount)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.deathRec)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.openSSLSockets)));
            objectsTable.addData(new SimpleText(String.valueOf(mi.webViews)));
        }
        objectsTable.end();


        Table sqlTable = new Table(Table.FLAG_SORT);
        ch_meminfo_service_sql.add(sqlTable);
        sqlTable.addColumn("Process", Table.FLAG_NONE);
        sqlTable.addColumn("Memory used (KB)", Table.FLAG_ALIGN_RIGHT);
        sqlTable.addColumn("Page Cache Overflow (KB)", Table.FLAG_ALIGN_RIGHT);
        sqlTable.addColumn("Malloc Size (KB)", Table.FLAG_ALIGN_RIGHT);

        sqlTable.begin();
        for (MemInfo mi : mMemInfos) {
            sqlTable.addData(new ProcessLink(mod, mi.pid));
            sqlTable.addData(new ShadedValue(mi.sqlMemUsed));
            sqlTable.addData(new ShadedValue(mi.sqlPageCacheOverflow));
            sqlTable.addData(new ShadedValue(mi.sqlMallocSize));
        }
        sqlTable.end();

        Table databasesTable = new Table(Table.FLAG_SORT);
        ch_meminfo_service_databases.add(databasesTable);
        databasesTable.addColumn("Process", Table.FLAG_NONE);
        databasesTable.addColumn("Database Name", Table.FLAG_NONE);
        databasesTable.addColumn("Page Size (KB)", Table.FLAG_ALIGN_RIGHT);
        databasesTable.addColumn("Database Size (KB)", Table.FLAG_ALIGN_RIGHT);
        databasesTable.addColumn("Look aside", Table.FLAG_ALIGN_RIGHT);
        databasesTable.addColumn("Cache", Table.FLAG_NONE);

        databasesTable.begin();
        for (MemInfo mi : mMemInfos) {
            for(DatabaseInfo db: mi.dbs) {
                databasesTable.addData(new ProcessLink(mod, mi.pid));
                databasesTable.addData(new SimpleText(db.name));
                databasesTable.addData(new ShadedValue(db.pgsz));
                databasesTable.addData(new ShadedValue(db.dbsz));
                databasesTable.addData(new SimpleText(String.valueOf(db.lookaside)));
                databasesTable.addData(new SimpleText(db.cache));
            }
        }
        databasesTable.end();
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

    private String removeK(Module mod, String input) {
        Matcher m = K_PATTERN.matcher(input);
        if(!m.matches()) {
            mod.printErr(3, TAG + "Failed to remove K from value: " + input);
            return input;
        }
        return m.group(1);
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
        Matcher header = LIB_RANK_HEADER_PATTERN.matcher(line);
        if (!header.matches()) {
            mod.printErr(3, TAG + "librank section format not supported... ignoring it");
            return;
        }

        //Check if has swap:
        int idxSwap = -1;
        int idxName = 4;
        int idxPid = 5;

        if(header.group(1) != null && header.group(1).trim().equals("Swap")) {
            idxSwap = 4;
            idxName++;
            idxPid++;
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
                    String lineSections[] = line.trim().split("\\s+");
                    mi.vss = Util.parseInt(removeK(mod, lineSections[0]), 0);
                    mi.rss = Util.parseInt(removeK(mod, lineSections[1]), 0);
                    mi.pss = Util.parseInt(removeK(mod, lineSections[2]), 0);
                    mi.uss = Util.parseInt(removeK(mod, lineSections[3]), 0);
                    if(idxSwap >= 0) {
                        mi.swap = Util.parseInt(removeK(mod, lineSections[idxSwap]), 0);
                    }

                    mi.procName = lineSections[idxName];
                    mi.pid = Util.parseInt(Util.extract(lineSections[idxPid], "[", "]"), 0);

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

            pr.suggestName(list.name, 30);

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

    public Vector<MemInfo> getMemInfos() {
        return mMemInfos;
    }
}
