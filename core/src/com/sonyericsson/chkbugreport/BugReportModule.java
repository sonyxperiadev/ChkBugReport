/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
 * Copyright (C) 2016 Tuenti Technologies
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

import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.PreText;
import com.sonyericsson.chkbugreport.doc.SimpleText;
import com.sonyericsson.chkbugreport.plugins.AlarmManagerPlugin;
import com.sonyericsson.chkbugreport.plugins.CpuFreqPlugin;
import com.sonyericsson.chkbugreport.plugins.MemPlugin;
import com.sonyericsson.chkbugreport.plugins.MiscPlugin;
import com.sonyericsson.chkbugreport.plugins.PSTreePlugin;
import com.sonyericsson.chkbugreport.plugins.PackageInfoPlugin;
import com.sonyericsson.chkbugreport.plugins.ScreenShotPlugin;
import com.sonyericsson.chkbugreport.plugins.SummaryPlugin;
import com.sonyericsson.chkbugreport.plugins.SurfaceFlingerPlugin;
import com.sonyericsson.chkbugreport.plugins.SysPropsPlugin;
import com.sonyericsson.chkbugreport.plugins.UsageHistoryPlugin;
import com.sonyericsson.chkbugreport.plugins.WindowManagerPlugin;
import com.sonyericsson.chkbugreport.plugins.apps.AppActivitiesPlugin;
import com.sonyericsson.chkbugreport.plugins.battery.BatteryInfoPlugin;
import com.sonyericsson.chkbugreport.plugins.battery.KernelWakeSourcesPlugin;
import com.sonyericsson.chkbugreport.plugins.battery.WakelocksFromLogPlugin;
import com.sonyericsson.chkbugreport.plugins.battery.WakelocksPlugin;
import com.sonyericsson.chkbugreport.plugins.charteditor.ChartEditorPlugin;
import com.sonyericsson.chkbugreport.plugins.ftrace.FTracePlugin;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.SystemLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.kernel.KernelLogPlugin;
import com.sonyericsson.chkbugreport.plugins.stacktrace.StackTracePlugin;
import com.sonyericsson.chkbugreport.ps.PSRecord;
import com.sonyericsson.chkbugreport.ps.PSRecords;
import com.sonyericsson.chkbugreport.ps.PSScanner;
import com.sonyericsson.chkbugreport.util.LineReader;
import com.sonyericsson.chkbugreport.util.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This module process bugreport files or log files.
 * In order to process log files, a dummy bugreport is created.
 */
public class BugReportModule extends Module {

    private static final String SECTION_DIVIDER = "-------------------------------------------------------------------------------";

    private static final String TYPE_BUGREPORT = "!BUGREPORT";

    private Vector<ProcessRecord> mProcessRecords = new Vector<ProcessRecord>();
    private HashMap<Integer, ProcessRecord> mProcessRecordMap = new HashMap<Integer, ProcessRecord>();
    private Chapter mChProcesses;
    private PSRecords mPSRecords;

    private int mVerMaj;
    private int mVerMin;
    private int mVerRel;
    private float mVer;
    private int mVerSdk;

    private Calendar mTimestamp;

    private GuessedValue<Long> mUpTime = new GuessedValue<Long>(0L);

    private Vector<String> mBugReportHeader = new Vector<String>();

    private ThreadsDependencyGraph threadsDependencyGraph;

    /**
     * Create an instance in order to process a bugreport.
     * @param context Contains various configs
     */
    public BugReportModule(Context context) {
        super(context);

        String chapterName = "Processes";
        mChProcesses = new Chapter(getContext(), chapterName);
    }

    @Override
    protected void loadPlugins() {
        addPlugin(new MemPlugin());
        addPlugin(new StackTracePlugin());
        addPlugin(new SystemLogPlugin());
        addPlugin(new MainLogPlugin());
        addPlugin(new EventLogPlugin());
        addPlugin(new KernelLogPlugin());
        addPlugin(new FTracePlugin());
        addPlugin(new AlarmManagerPlugin());
        addPlugin(new BatteryInfoPlugin());
        addPlugin(new CpuFreqPlugin());
        addPlugin(new SurfaceFlingerPlugin());
        addPlugin(new WindowManagerPlugin());
        addPlugin(new SysPropsPlugin());
        addPlugin(new PackageInfoPlugin());
        addPlugin(new SummaryPlugin());
        addPlugin(new PSTreePlugin());
        addPlugin(new ScreenShotPlugin());
        addPlugin(new MiscPlugin());
        addPlugin(new WakelocksPlugin());
        addPlugin(new KernelWakeSourcesPlugin());
        addPlugin(new WakelocksFromLogPlugin());
        addPlugin(new UsageHistoryPlugin());
        addPlugin(new ChartEditorPlugin());
        addPlugin(new AppActivitiesPlugin());

        // The ADB plugin needs special care
        Plugin adbExt = loadPlugin("com.sonyericsson.chkbugreport.AdbExtension");
        if (adbExt != null) {
            addPlugin(adbExt);
        }
    }

    private Plugin loadPlugin(String className) {
        try {
            Class<?> cls = Class.forName(className);
            return (Plugin)cls.newInstance();
        } catch (Throwable e) {
            printErr(1, "Failed to load plugin: " + className);
            return null;
        }
    }

    public Calendar getTimestamp() {
        return mTimestamp;
    }

    private boolean load(InputStream is) throws IOException {
        return load(is, false, null);
    }

    private boolean load(InputStream is, boolean partial, String secName) throws IOException {
        long t0 = System.currentTimeMillis();
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
                    return loadFromDopBox(br, buff);
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

            if (buff.equals(SECTION_DIVIDER)) {
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
                mBugReportHeader.add(buff);
            }
        }

        br.close();

        long t1 = System.currentTimeMillis();
        printOut(1, String.format("Loaded in %.2f seconds.", (t1 - t0) / 1000.0f));
        if (!formatOk) {
            throw new IOException("Does not look like a bugreport file!");
        }
        return true;
    }

    /**
     * Load a partial bugreport, for example the output of dumpsys
     * @param fileName The file name of the partial bugreport
     * @param sectionName The name of the section where the header will be collected
     * @param ignoreErr When set to true, don't throw exception on error, just ignore it
     */
    private boolean loadPartial(String fileName, String sectionName, boolean ignoreErr) {
        try {
            FileInputStream fis = new FileInputStream(fileName);
            load(fis, true, sectionName);
            fis.close();
            addHeaderLine("Partial bugreport: " + fileName);
            return true;
        } catch (IOException e) {
            if (!ignoreErr) {
                throw new IllegalParameterException("Error reading partial bugreport: " + fileName);
            }
            System.err.println("Error reading file '" + fileName + "' (it will be ignored): " + e);
            return false;
        }
    }

    private boolean loadFromDopBox(LineReader br, String buff) {
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
        return true;
    }

    @Override
    protected void preProcess() throws IOException {
        // Collect the process names from the PS output
        mPSRecords = new PSScanner(this).run();
    }

    @Override
    protected void postProcess() throws IOException {
        // Collect process records
        printOut(1, "Collecting process records...");
        collectProcessRecords();
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
        if (pid <= 0) {
            return null;
        }
        ProcessRecord ret = mProcessRecordMap.get(pid);
        if (ret == null && createIfNeeded) {
            ret = new ProcessRecord(getContext(), "", pid);
            mProcessRecordMap.put(pid, ret);
            mProcessRecords.add(ret);
        }
        if (ret != null && export) {
            ret.setExport();
        }
        return ret;
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
        mChProcesses.sort();

        // And create the alphabetical list
        for (ProcessRecord pr : mProcessRecords) {
            if (pr.shouldExport()) {
                PSRecord ps = getPSRecord(pr.getPid());
                boolean strike = (ps == null && mPSRecords != null && !mPSRecords.isEmpty());
                if (strike) {
                    pr.setNameFlags(SimpleText.FLAG_STRIKE, true);
                }
            }
        }
    }

    public PSRecord getPSRecord(int pid) {
        return mPSRecords == null? null : mPSRecords.getPSRecord(pid);
    }

    public PSRecord getPSTree() {
        return mPSRecords == null? null : mPSRecords.getPSTree();
    }

    public void setUptime(long uptime, int certainty) {
        mUpTime.set(uptime, certainty);
    }

    public long getUptime() {
        return mUpTime.get();
    }

    public void setAndroidVersion(String string) {
        String f[] = string.split("\\.");
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

    public Vector<String> getBugReportHeader() {
        return mBugReportHeader;
    }

    private void scanDirForPartials(String dirName) {
        File dir = new File(dirName);
        File files[] = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                loadPartial(f.getAbsolutePath(), Section.PARTIAL_FILE_HEADER, true);
            }
        }
    }

    private void parseMonkey(String fileName) {
        char state = 'm';
        try {
            FileInputStream fis = new FileInputStream(fileName);
            LineReader lr = new LineReader(fis);

            String line = null;
            Bug bug = null;
            PreText anrLog = null;
            Section sec = null;
            String secStop = null;
            while (null != (line = lr.readLine())) {
                if (state == 'm') {
                    // idle/monkey mode: searching for something useful
                    if (line.startsWith("// NOT RESPONDING")) {
                        // Congratulation... you found an ANR ;-)
                        bug = new Bug(Bug.Type.PHONE_ERR, Bug.PRIO_ANR_MONKEY, 0, line);
                        bug.add(anrLog = new PreText());
                        anrLog.addln(line);
                        addBug(bug);
                        state = 'a';
                        continue;
                    }
                } else if (state == 'a') {
                    // Collect ANR summary
                    if (line.length() == 0) {
                        bug = null;
                        state = 's';
                    } else {
                        anrLog.addln(line);
                    }
                } else if (state == 's') {
                    // Section search mode
                    if (line.length() == 0) {
                        continue;
                    } else if (line.startsWith("//") || line.startsWith("    //") || line.startsWith(":")) {
                        state = 'm';
                    } else if (line.startsWith("procrank:")) {
                        sec = new Section(this, Section.PROCRANK);
                        secStop = "// procrank status was";
                    } else if (line.startsWith("anr traces:")) {
                        sec = new Section(this, Section.VM_TRACES_AT_LAST_ANR);
                        secStop = "// anr traces status was";
                    } else if (line.startsWith("meminfo:")) {
                        sec = new Section(this, Section.DUMP_OF_SERVICE_MEMINFO);
                        secStop = "// meminfo status was";
                    } else {
                        // NOP ?
                    }
                    if (sec != null) {
                        printOut(2, "[MonkeyLog] Found section: " + sec.getName());
                        addSection(sec);
                        addHeaderLine(sec.getName() + ": (extracted from) " + fileName);
                        state = 'c';
                    }
                } else if (state == 'c') {
                    // Section copy mode
                    if (line.startsWith(secStop)) {
                        sec = null;
                        secStop = null;
                        state = 's';
                    } else {
                        sec.addLine(line);
                    }
                }
            }
            lr.close();
            fis.close();
        } catch (IOException e) {
            printErr(1, "Error reading file '" + fileName + "': " + e);
        }
    }

    @Override
    public boolean addFile(String fileName, String type, boolean limitSize) {
        if (super.addFile(fileName, type, limitSize)) {
            return true;
        } else {
            if (type == null) {
                return autodetectFile(fileName, limitSize);
            } else {
                return addFileImpl(fileName, type, limitSize);
            }
        }
    }

    private boolean addFileImpl(String fileName, String type, boolean limitSize) {
        if (type.equals(Section.META_PARSE_MONKEY)) {
            parseMonkey(fileName);
        } else if (type.equals(Section.META_SCAN_DIR)) {
            scanDirForPartials(fileName);
        } else if (type.equals(Section.DUMPSYS)) {
            loadPartial(fileName, type, false);
        } else if (type.equals(Section.PARTIAL_FILE_HEADER)) {
            loadPartial(fileName, type, false);
        } else {
            return false;
        }
        return true;
    }

    private boolean autodetectFile(String fileName, boolean limitSize) {
        File f = new File(fileName);
        if (!f.exists()) {
            printErr(1, "File " + fileName + " does not exists!");
        }

        // Try to open it as zip
        try {
            ZipFile zip = new ZipFile(fileName);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    if (!getContext().isSilent()) {
                        System.out.println("Trying to parse zip entry: " + entry.getName() + " ...");
                    }

                    autodetectFile(fileName + ":" + entry.getName(), zip.getInputStream(entry));
                }
            }
            // We managed to process as zip file, so do not handle as non-zip file
            return true;
        } catch (IOException e) {
            // Failed, so let's just work with the raw file
        }

        // Failed to process as zip file, so try processing as normal file
        try {
            FileInputStream is = new FileInputStream(f);
            autodetectFile(fileName, is);
            return true;
        } catch (FileNotFoundException e) {
            throw new IllegalParameterException("Cannot open file: " + fileName);
        }
    }

    private void autodetectFile(String fileName, InputStream origIs) {
        final int buffSize = 0x1000;
        InputStream is = new BufferedInputStream(origIs, buffSize);

        // Try to open it as gzip
        try {
            is.mark(buffSize);
            is = new BufferedInputStream(new GZIPInputStream(is), buffSize);
        } catch (IOException e) {
            // Failed, so let's just work with the raw file
            try {
                is.reset();
            } catch (IOException e1) {
                e1.printStackTrace(); // FIXME: this is a bit ugly
            }
        }

        // Read the beginning of the file in order to detect it
        byte buff[] = new byte[buffSize];
        int buffLen = 0;
        try {
            is.mark(buffSize);
            while (buffLen < buffSize) {
                int read = is.read(buff, buffLen, buffSize - buffLen);
                if (read <= 0) {
                    break;
                }
                buffLen += read;
            }
            is.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        GuessedValue<String> type = new GuessedValue<String>(null);
        autodetect(buff, 0, buffLen, type);
        if (type.get() == null) {
            throw new IllegalParameterException("Cannot detect the type of file: " + fileName);
        }

        // Load the file and generate the report
        if (type.get().equals(TYPE_BUGREPORT)) {
            try {
                load(is);
                setSource(new SourceFile(fileName, TYPE_BUGREPORT));
                setFileName(fileName, 100);
            } catch (IOException e) {
                throw new IllegalParameterException("Not a bugreport file");
            }
        } else {
            addSection(type.get(), fileName, is, false);
        }
    }

    @Override
    protected void autodetect(byte[] buff, int offs, int len, GuessedValue<String> type) {
        super.autodetect(buff, offs, len, type);
        // TODO: fix this
        // Let's do an ugly solution: if no plugin recognized it, then assume it's a bugreport
        // then when loading the data it will simply fail to load
        type.set(TYPE_BUGREPORT, 1); // low probability, so this will be used only as a fallback
    }

    public void initThreadsDependencyGraph(int v) {
       this.threadsDependencyGraph = new ThreadsDependencyGraph(v);
    }

    public void addNodeToThreadsDependencyGraph(String name) {
        this.threadsDependencyGraph.addThread(name);
    }

    public void addEdgeToThreadsDependencyGraph(String nameV, String nameW, String lockType) {
        this.threadsDependencyGraph.addThreadDependency(nameV, nameW, lockType);
    }

    public ThreadsDependencyGraph getThreadsDependencyGraph() {
        return threadsDependencyGraph;
    }

    /* package */ static class SourceFile {
        String mName;
        String mType;

        public SourceFile(String name, String type) {
            mName = name;
            mType = type;
        }
    }

}


