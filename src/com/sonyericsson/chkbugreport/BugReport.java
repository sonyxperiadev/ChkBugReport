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
package com.sonyericsson.chkbugreport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import com.sonyericsson.chkbugreport.plugins.BatteryInfoPlugin;
import com.sonyericsson.chkbugreport.plugins.CpuFreqPlugin;
import com.sonyericsson.chkbugreport.plugins.EventLogPlugin;
import com.sonyericsson.chkbugreport.plugins.KernelLogPlugin;
import com.sonyericsson.chkbugreport.plugins.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.MemPlugin;
import com.sonyericsson.chkbugreport.plugins.PackageInfoPlugin;
import com.sonyericsson.chkbugreport.plugins.ScreenShotPlugin;
import com.sonyericsson.chkbugreport.plugins.SummaryPlugin;
import com.sonyericsson.chkbugreport.plugins.SurfaceFlingerPlugin;
import com.sonyericsson.chkbugreport.plugins.SysPropsPlugin;
import com.sonyericsson.chkbugreport.plugins.SystemLogPlugin;
import com.sonyericsson.chkbugreport.plugins.WindowManagerPlugin;
import com.sonyericsson.chkbugreport.plugins.ftrace.FTracePlugin;
import com.sonyericsson.chkbugreport.plugins.stacktrace.StackTracePlugin;

public class BugReport extends Report {

    public static final boolean USE_FRAMES = true;

    public static final int SDK_DONUT = 4;
    public static final int SDK_ECLAIR = 5;
    public static final int SDK_ECLAIR_0_1 = 6;
    public static final int SDK_ECLAIR_MR1 = 7;
    public static final int SDK_FROYO = 8;
    public static final int SDK_GB = 9;
    public static final int SDK_GB_MR1 = 10;
    public static final int SDK_HC = 11;
    public static final int SDK_HC_MR1 = 12;
    public static final int SDK_HC_MR2 = 13;
    public static final int SDK_ICS = 14;

    private static final String FN_TOC_HTML = "data/toc.html";

    private static final String SECTION_DIVIDER = "-------------------------------------------------------------------------------";

    private Vector<ProcessRecord> mProcessRecords = new Vector<ProcessRecord>();
    private HashMap<Integer, ProcessRecord> mProcessRecordMap = new HashMap<Integer, ProcessRecord>();
    private Chapter mChProcesses;
    private HashMap<Integer, PSRecord> mPSRecords = new HashMap<Integer, PSRecord>();

    private int mVerMaj;
    private int mVerMin;
    private int mVerRel;
    private float mVer;
    private int mVerSdk;

    private Calendar mTimestamp;

    {
        addPlugin(new MemPlugin());
        addPlugin(new StackTracePlugin());
        addPlugin(new SystemLogPlugin());
        addPlugin(new MainLogPlugin());
        addPlugin(new EventLogPlugin());
        addPlugin(new KernelLogPlugin());
        addPlugin(new FTracePlugin());
        addPlugin(new BatteryInfoPlugin());
        addPlugin(new CpuFreqPlugin());
        addPlugin(new SurfaceFlingerPlugin());
        addPlugin(new WindowManagerPlugin());
        addPlugin(new SysPropsPlugin());
        addPlugin(new PackageInfoPlugin());
        addPlugin(new SummaryPlugin());
        addPlugin(new ScreenShotPlugin());
    }

    public BugReport(String fileName) {
        super(fileName);

        String chapterName = "Processes";
        mChProcesses = new Chapter(this, chapterName);
    }

    public Calendar getTimestamp() {
        return mTimestamp;
    }

    @Override
    public void load(InputStream is) throws IOException {
        load(is, false, null);
    }

    protected void load(InputStream is, boolean partial, String secName) throws IOException {
        printOut(1, "Loading input...");
        LineReader br = new LineReader(is);
        String buff;
        Section curSection = null;
        mTimestamp = null;
        int lineNr = 0;
        int skipCount = 5;
        boolean formatOk = partial;
        while (null != (buff = br.readLine())) {
            if (!formatOk) {
                // Sill need file format validation
                // Check if this is a dropbox file
                if (0 == lineNr && buff.startsWith("Process: ")) {
                    loadFromDopBox(br, buff);
                    return;
                }

                if (0 == lineNr) {
                    // Not detected yet
                    if (buff.startsWith("==============")) {
                        // Ok, pass through and start processing
                    } else {
                        if (0 == --skipCount) {
                            // give up (simply pass through and let if fail later)
                        } else {
                            // Give another chance
                            continue;
                        }
                    }
                }

                // Verify file format (just a simple sanity check)
                lineNr++;

                if (1 == lineNr && !buff.startsWith("==============")) break;
                if (2 == lineNr && !buff.startsWith("== dumpstate")) break;
                if (3 == lineNr && !buff.startsWith("==============")) break;
                if (4 == lineNr) {
                    formatOk = true;
                }

                // Extract timestamp of crash
                Calendar ts = Util.parseTimestamp(this, buff);
                if (ts != null) {
                    mTimestamp = ts;
                }
            }

            // Parse sections and sub-sections
            if (buff.startsWith("------ ")) {
                // build up file name
                int e = buff.indexOf(" ------");
                if (e >= 0) {
                    String sectionName = buff.substring(7, e);

                    // Workaround for SMAP spamming
                    boolean newSection = true;
                    if (curSection != null && curSection.getName().equals("SMAPS OF ALL PROCESSES")) {
                        if (sectionName.startsWith("SHOW MAP ")) {
                            newSection = false;
                        }
                    }
                    if (newSection) {
                        Section section = new Section(this, sectionName);
                        addSection(section);
                        curSection = section;
                        continue;
                    }
                }
            }

            // Workaround for buggy wallpaper service dump
            int idx = buff.indexOf(SECTION_DIVIDER);
            if (idx > 0) {
                if (curSection != null) {
                    curSection.addLine(buff.substring(0, idx));
                }
                buff = buff.substring(idx);
            }

            if (buff.startsWith(SECTION_DIVIDER)) {
                // Another kind of marker
                // Need to read the next line
                String sectionName = br.readLine();
                if (sectionName != null) {
                    if ("DUMP OF SERVICE activity:".equals(sectionName)) {
                        // skip over this name, and use the next line as title, the provider thingy
                        sectionName = br.readLine();
                    }
                }
                if (sectionName != null) {
                    Section section = new Section(this, sectionName);
                    addSection(section);
                    curSection = section;
                }
                continue;
            }

            // Add the current line to the current section
            if (curSection == null && partial) {
                // We better not spam the header section, so let's create a fake section
                curSection = new Section(this, secName);
                addSection(curSection);
            }
            if (curSection != null) {
                curSection.addLine(buff);
            } else {
                addHeaderLine(buff);
            }
        }

        br.close();

        if (!formatOk) {
            throw new IOException("Does not look like a bugreport file!");
        }
    }

    /**
     * Load a partial bugreport, for example the output of dumpsys
     * @param fileName The file name of the partial bugreport
     * @param sectionName The name of the section where the header will be collected
     */
    public boolean loadPartial(String fileName, String sectionName) {
        try {
            FileInputStream fis = new FileInputStream(fileName);
            load(fis, true, sectionName);
            fis.close();
            addHeaderLine("Partial bugreport: " + fileName);
            return true;
        } catch (IOException e) {
            System.err.println("Error reading file '" + fileName + "' (it will be ignored): " + e);
            return false;
        }
    }

    private void loadFromDopBox(LineReader br, String buff) {
        printOut(2, "Detect dropbox file...");

        int state = 0; // header
        Section secLog = new Section(this, Section.SYSTEM_LOG);
        Section secStack = new Section(this, Section.VM_TRACES_AT_LAST_ANR);
        do {
            switch (state) {
                case 0: /* header state */
                    if (buff.length() == 0) {
                        state = 1; // log
                    } else {
                        addHeaderLine(buff);
                    }
                    break;
                case 1: /* log state */
                    if (buff.length() == 0) {
                        state = 2; // stack trace
                    } else {
                        secLog.addLine(buff);
                    }
                    break;
                case 2: /* stack trace */
                    secStack.addLine(buff);
                    break;
            }
        } while (null != (buff = br.readLine()));

        addSection(secLog);
        addSection(secStack);

        br.close();
    }

    @Override
    public void generate() throws IOException {
        // This will create the empty index.html and open the file
        super.generate();

        // This will do build some extra chapters and save some non-html files
        collectData();

        if (useFrames()) {
            // In the still opened index html we just create the frameset
            printOut(1, "Writing frameset...");
            writeHeaderLite();
            writeFrames();
            writeFooterLite();
            closeFile();

            // Write the table of contents
            printOut(1, "Writing TOC...");
            openFile(getOutDir() + FN_TOC_HTML);
            writeHeader();
            writeTOC();
            writeFooter();
            closeFile();

            // Write all the chapters
            printOut(1, "Writing Chapters...");
            writeChapters();
        } else {
            // In the still opened index html we save everything
            writeHeader();

            // Write the table of contents
            printOut(1, "Writing TOC...");
            writeTOC();

            // Write all the chapters
            printOut(1, "Writing Chapters...");
            writeChapters();

            // Close the file
            writeFooter();
            closeFile();

        }

        printOut(1, "DONE!");
    }

    private void collectData() throws IOException {
        // Save each section as raw file
        printOut(1, "Saving raw sections");
        saveSections();

        // Collect the process names from the PS output
        analyzePS();

        // Run all the plugins
        runPlugins();

        // Collect detected bugs
        printOut(1, "Collecting errors...");
        collectBugs();

        // Collect process records
        printOut(1, "Collecting process records...");
        collectProcessRecords();

        // Create the header chapter
        printOut(1, "Writing header...");
        writeHeaderChapter();

        // Copy over some builtin resources
        printOut(1, "Copying extra resources...");
        copyRes(Util.COMMON_RES);
    }

    /**
     * Return the gathered information related to a process
     * @param pid The pid of the process
     * @param createIfNeeded if true then the process record will be created if it does not exists yet
     * @param export marks the process record to be exported or not. If at least one call sets this to true
     *   for a given process, then the process will be exported.
     *   This is used so application can create process records early on, but mark them as not important,
     *   so if no other important info is added, the process record won't be saved.
     * @return The process record or null if not found (and not created)
     */
    public ProcessRecord getProcessRecord(int pid, boolean createIfNeeded, boolean export) {
        ProcessRecord ret = mProcessRecordMap.get(pid);
        if (ret == null && createIfNeeded) {
            ret = new ProcessRecord(this, "", pid);
            mProcessRecordMap.put(pid, ret);
            mProcessRecords.add(ret);
        }
        if (ret != null && export) {
            ret.setExport();
        }
        return ret;
    }

    public String createLinkToProcessRecord(int pid) {
        String anchor = Util.getProcessRecordAnchor(pid);
        String link = createLinkTo(mChProcesses, anchor);
        return link;
    }

    protected void collectProcessRecords() {
        // Sort
        Collections.sort(mProcessRecords, new Comparator<ProcessRecord>(){
            @Override
            public int compare(ProcessRecord o1, ProcessRecord o2) {
                return o1.getPid() - o2.getPid();
            }
        });

        // Create chapter
        for (ProcessRecord pr : mProcessRecords) {
            if (pr.shouldExport()) {
                mChProcesses.addChapter(pr);
            }
        }
        addChapter(mChProcesses);

        // Now sort by name
        Collections.sort(mProcessRecords, new Comparator<ProcessRecord>(){
            @Override
            public int compare(ProcessRecord o1, ProcessRecord o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        // And create the alphabetical list
        mChProcesses.addLine("<ul>");
        for (ProcessRecord pr : mProcessRecords) {
            if (pr.shouldExport()) {
                PSRecord ps = getPSRecord(pr.getPid());
                boolean strike = (ps == null && mPSRecords.size() > 0);
                StringBuffer line = new StringBuffer();
                line.append("<li><a href=\"#");
                line.append(Util.getProcessRecordAnchor(pr.getPid()));
                line.append("\">");
                if (strike) {
                    line.append("<strike>");
                }
                line.append(pr.getName());
                if (strike) {
                    line.append("</strike>");
                }
                line.append("</a></li>");
                mChProcesses.addLine(line.toString());
            }
        }
        mChProcesses.addLine("</ul>");
    }

    private void writeFrames() {
        String first = getChapters().getChild(0).getAnchor();
        writeLine("<frameset cols=\"25%,75%\">");
        writeLine("  <frame name=\"toc\" src=\"" + FN_TOC_HTML + "\"/>");
        writeLine("  <frame name=\"content\" src=\"data/" + first + ".html\"/>");
        writeLine("</frameset>");
    }

    private void analyzePS() {
        // Locate the PS section
        Section ps = findSection(Section.PROCESSES_AND_THREADS);
        if (ps == null) {
            printErr(3, "Cannot find section: " + Section.PROCESSES_AND_THREADS + " (ignoring it)");
        }
        if (ps == null) {
            ps = findSection(Section.PROCESSES);
            if (ps == null) {
                printErr(3, "Cannot find section: " + Section.PROCESSES + " (ignoring it)");
            }
        }
        if (ps != null) {
            readPS(ps);
        }
    }

    private void readPS(Section ps) {
        // Process the PS section
        // Read the header and use it to locate the PID, PPID and NAME fields
        int tries = 10;
        String buff = null;
        int idxPid = -1, idxPPid = -1, idxName = -1, idxNice = -1, idxPCY = -1, lineIdx = 0;
        while (tries > 0) {
            buff = ps.getLine(lineIdx++);
            idxPid = Util.indexOf(buff, " PID ", 2);
            idxPPid = Util.indexOf(buff, " PPID ", 2);
            idxNice = Util.indexOf(buff, " NICE ", 2);
            idxPCY = Util.indexOf(buff, " PCY ", 1);
            idxName = Util.indexOf(buff, " NAME", 0);
            if (idxPid > 0 && idxName > 0) break;
            tries--;
        }
        if (idxPid < 0 || idxName < 0) return;

        // Now read and process every line
        int pidZygote = -1;
        int cnt = ps.getLineCount();
        for (int i = lineIdx; i < cnt; i++) {
            buff = ps.getLine(i);
            if (buff.startsWith("[")) break;

            // Extract pid
            int pid = -1;
            if (idxPid >= 0) {
                String sPid = buff.substring(idxPid);
                try {
                    pid = parseInt(sPid);
                } catch (NumberFormatException nfe) {
                    printErr(4, "Error parsing pid from: " + sPid);
                    break;
                }
            }

            // Extract ppid
            int ppid = -1;
            if (idxPPid >= 0) {
                String sPid = buff.substring(idxPPid);
                try {
                    ppid = parseInt(sPid);
                } catch (NumberFormatException nfe) {
                    printErr(4, "Error parsing ppid from: " + sPid);
                    break;
                }
            }

            // Extract nice
            int nice = PSRecord.NICE_UNKNOWN;
            if (idxNice >= 0) {
                String sNice = buff.substring(idxNice);
                try {
                    nice = parseInt(sNice);
                } catch (NumberFormatException nfe) {
                    printErr(4, "Error parsing nice from: " + sNice);
                    break;
                }
            }

            // Extract scheduler policy
            int pcy = PSRecord.PCY_UNKNOWN;
            if (idxPCY >= 0) {
                String sPcy = buff.substring(idxPCY, idxPCY + 2);
                if ("fg".equals(sPcy)) {
                    pcy = PSRecord.PCY_NORMAL;
                } else if ("bg".equals(sPcy)) {
                    pcy = PSRecord.PCY_BATCH;
                } else if ("un".equals(sPcy)) {
                    pcy = PSRecord.PCY_FIFO;
                } else {
                    pcy = PSRecord.PCY_OTHER;
                }
            }

            // Exctract name
            String name = "";
            if (idxName >= 0) {
                name = buff.substring(idxName);
            }

            // Fix the name
            mPSRecords.put(pid, new PSRecord(pid, ppid, nice, pcy, name));

            // Check if we should create a ProcessRecord for this
            if (pidZygote == -1 && name.equals("zygote")) {
                pidZygote = pid;
            }
            ProcessRecord pr = getProcessRecord(pid, true, false);
            pr.suggestName(name, 10);
        }
    }

    public PSRecord getPSRecord(int pid) {
        return mPSRecords.get(pid);
    }

    public Vector<PSRecord> findChildPSRecords(int pid) {
        Vector<PSRecord> ret = new Vector<PSRecord>();
        for (PSRecord psr : mPSRecords.values()) {
            if (psr.getPid() == pid || psr.getParentPid() == pid) {
                ret.add(psr);
            }
        }
        return ret;
    }

    private int parseInt(String sPid) {
        int e = 0, l = sPid.length();
        while (e < l) {
            char c = sPid.charAt(e);
            if ((c < '0' || c > '9') && c != '-') break;
            e++;
        }
        sPid = sPid.substring(0, e);
        return Integer.parseInt(sPid);
    }

    public void setAndroidVersion(String string) {
        String f[] = string.split(".");
        if (f.length >= 1) {
            mVerMaj = Integer.parseInt(f[0]);
            mVer = mVerMaj;
        }
        if (f.length >= 2) {
            mVerMin = Integer.parseInt(f[1]);
            mVer = Float.parseFloat(f[0] + "." + f[1]);
        }
        if (f.length >= 3) {
            mVerRel = Integer.parseInt(f[2]);
        }
    }

    public float getAndroidVersion() {
        return mVer;
    }

    public int getAndroidVersionMaj() {
        return mVerMaj;
    }

    public int getAndroidVersionMin() {
        return mVerMin;
    }

    public int getAndroidVersionRel() {
        return mVerRel;
    }

    public void setAndroidSdkVersion(String string) {
        mVerSdk = Integer.parseInt(string);
    }

    public int getAndroidVersionSdk() {
        return mVerSdk;
    }

}
