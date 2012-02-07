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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.UIManager;

import com.sonyericsson.chkbugreport.Report.OutputListener;
import com.sonyericsson.chkbugreport.settings.BoolSetting;
import com.sonyericsson.chkbugreport.settings.Settings;
import com.sonyericsson.chkbugreport.traceview.TraceReport;

public class Main implements OutputListener {

    public static final int MODE_BUGREPORT = 0;
    public static final int MODE_TRACEVIEW = 1;
    public static final int MODE_MANUAL = 2;

    /** It's not know yet if the method succeeded or not. */
    public static final int RET_WAIT    = -2;
    /** The method didn't do anything. (i.e. ignore this method call) */
    public static final int RET_NOP     = -1;
    /** The method failed. */
    public static final int RET_FALSE   =  0;
    /** The method succeeded. */
    public static final int RET_TRUE    = +1;

    private static final int B = 1;
    private static final int KB = 1024*B;
    private static final int MB = 1024*KB;

    public static final int NO_LIMIT = Integer.MAX_VALUE;
    public static final int MAX_FTRACE_SIZE = 5*MB;
    public static final int MAX_LOG_SIZE = 1*MB;

    private BugReport mDummy;
    private int mMode = MODE_BUGREPORT;
    private boolean mSilent = false;
    private boolean mLimit = true;
    private Settings mSettings = new Settings();
    private BoolSetting mShowGui = new BoolSetting(false, mSettings, "showGui", "Launch the GUI automatically when no file name was specified.");
    private BoolSetting mUseFrames = new BoolSetting(true, mSettings, "useFrames", "Use HTML frames where possible.");
    private BoolSetting mOpenBrowser = new BoolSetting(false, mSettings, "openBrowser", "Launch the browser when output is generated.");
    private Vector<Extension> mExtensions = new Vector<Extension>();

    private Gui mGui;

    public Main() {
        // Register extensions
        addExtension("AdbExtension");
    }

    private void addExtension(String name) {
        try {
            Class<?> cls = Class.forName("com.sonyericsson.chkbugreport.extensions." + name);
            Extension ext = (Extension) cls.newInstance();
            mExtensions.add(ext);
        } catch (Throwable e) {
            onPrint(1, TYPE_ERR, "Failed to register extension '" + name + "' due to: " + e);
        }
    }

    public Extension findExtension(String name) {
        for (Extension ext : mExtensions) {
            if (ext.getClass().getSimpleName().equals(name)) {
                return ext;
            }
        }
        return null;
    }

    public int getMode() {
        return mMode;
    }

    public Settings getSettings() {
        return mSettings;
    }

    public static void main(String[] args) {
        new Main().run(args);
    }

    public void run(String[] args) {
        String fileName = null;

        mSettings.load();

        for (String arg : args) {
            if (arg.startsWith("-")) {
                // option
                String key = arg.substring(1);
                String param = null;
                int idx = key.indexOf(':');
                if (idx > 0) {
                    param = key.substring(idx + 1);
                    key = key.substring(0, idx);
                }
                if ("t".equals(key)) {
                    mMode = MODE_TRACEVIEW;
                } else if ("sl".equals(key)) {
                    addSection(Section.SYSTEM_LOG, param, MAX_LOG_SIZE);
                } else if ("ml".equals(key)) {
                    addSection(Section.MAIN_LOG, param, MAX_LOG_SIZE);
                } else if ("el".equals(key)) {
                    addSection(Section.EVENT_LOG, param, MAX_LOG_SIZE);
                } else if ("ft".equals(key)) {
                    addSection(Section.FTRACE, param, MAX_FTRACE_SIZE);
                } else if ("ps".equals(key)) {
                    addSection(Section.PROCESSES, param, NO_LIMIT);
                } else if ("pt".equals(key)) {
                    addSection(Section.PROCESSES_AND_THREADS, param, NO_LIMIT);
                } else if ("sa".equals(key)) {
                    addSection(Section.VM_TRACES_AT_LAST_ANR, param, NO_LIMIT);
                } else if ("sn".equals(key)) {
                    addSection(Section.VM_TRACES_JUST_NOW, param, NO_LIMIT);
                } else if ("pb".equals(key)) {
                    mMode = MODE_MANUAL;
                    BugReport br = getDummyBugReport();
                    br.loadPartial(param, Section.PARTIAL_FILE_HEADER);
                } else if ("sd".equals(key)) {
                    mMode = MODE_MANUAL;
                    BugReport br = getDummyBugReport();
                    scanDirForPartials(br, param);
                } else if ("ds".equals(key)) {
                    mMode = MODE_MANUAL;
                    BugReport br = getDummyBugReport();
                    br.loadPartial(param, Section.DUMPSYS);
                } else if ("mo".equals(key)) {
                    parseMonkey(param);
                } else if ("-no-frames".equals(key)) {
                    mUseFrames.set(false);
                } else if ("-frames".equals(key)) {
                    mUseFrames.set(true);
                } else if ("-silent".equals(key)) {
                    mSilent = true;
                } else if ("-no-limit".equals(key)) {
                    mLimit = false;
                } else if ("-limit".equals(key)) {
                    mLimit = true;
                } else if ("-browser".equals(key)) {
                    mOpenBrowser.set(true);
                } else if ("-gui".equals(key)) {
                    mShowGui.set(true);
                } else {
                    onPrint(1, TYPE_ERR, "Unknown option '" + key + "'!");
                    usage();
                    System.exit(1);
                }
            } else {
                if (fileName != null) {
                    onPrint(1, TYPE_ERR, "Multiple files not supported (yet) !");
                    usage();
                    System.exit(1);
                }
                fileName = arg;
            }
        }

        if (fileName == null) {
            if (mShowGui.get()) {
                showGui();
                return;
            }
            usage();
            System.exit(1);
        }

        if (!loadFile(fileName)) {
            System.exit(1);
        }
    }

    public boolean loadFile(String fileName) {
        try {
            if (mMode == MODE_MANUAL) {
                BugReport br = getDummyBugReport();
                br.setUseFrames(mUseFrames.get());
                br.setFileName(fileName);
                processFile(br);
            } else {
                Report br = createReportInstance(fileName, mMode);
                if (mMode != MODE_TRACEVIEW) {
                    // Traceview mode doesn't support frames yet
                    br.setUseFrames(mUseFrames.get());
                }
                int ret = loadReportFrom(br, fileName, mMode);
                if (ret != RET_TRUE) {
                    return false;
                }
                processFile(br);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void processFile(Report br) throws IOException {
        br.generate();
        String indexFile = br.getIndexHtmlFileName();
        if (mOpenBrowser.get() && indexFile != null) {
            try {
                File f = new File(indexFile);
                System.out.println("Launching browser with URI: " + f.toURI());
                java.awt.Desktop.getDesktop().browse(f.toURI());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void showGui() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mGui = new Gui(this);
        mGui.setVisible(true);
    }

    private void scanDirForPartials(BugReport br, String param) {
        File dir = new File(param);
        File files[] = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                br.loadPartial(f.getAbsolutePath(), Section.PARTIAL_FILE_HEADER);
            }
        }
    }

    protected int loadReportFrom(Report report, String fileName, int mode) throws IOException {
        // First try loaded extensions
        for (Extension ext : mExtensions) {
            int ret = ext.loadReportFrom(report, fileName, mode);
            if (ret != RET_NOP) {
                return ret;
            }
        }

        File f = new File(fileName);
        InputStream is = null;
        if (!f.exists()) {
            onPrint(1, TYPE_ERR, "File " + fileName + " does not exists!");
            return RET_FALSE;
        }

        // Try to open it as zip
        try {
            ZipFile zip = new ZipFile(fileName);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    if (!mSilent) System.out.println("Trying to parse zip entry: " + entry.getName() + " ...");
                    if (loadFrom(report, fileName, zip.getInputStream(entry))) {
                        return RET_TRUE;
                    }
                }
            }
        } catch (IOException e) {
            // Failed, so let's just work with the raw file
        }

        // Open file
        try {
            is = new FileInputStream(f);
        } catch (IOException e) {
            onPrint(1, TYPE_ERR, "Error opening file " + fileName + "!");
            return RET_FALSE;
        }

        if (!loadFrom(report, fileName, is)) {
            return RET_FALSE;
        }

        return RET_TRUE;
    }

    private boolean loadFrom(Report report, String fileName, InputStream is) {
        // Try to open it as gzip
        try {
            is = new GZIPInputStream(is);
        } catch (IOException e) {
            // Failed, so let's just work with the raw file
        }

        // Load the file and generate the report
        try {
            report.load(is);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addSection(String name, String fileName, int limit) {
        if (!mLimit) {
            limit = Integer.MAX_VALUE;
        }
        mMode = MODE_MANUAL;
        BugReport br = getDummyBugReport();
        br.addHeaderLine(name + ": " + fileName);
        Section sl = new Section(br, name);
        if (readFile(sl, fileName, limit)) {
            br.addSection(sl);
        }
    }

    private void parseMonkey(String fileName) {
        mMode = MODE_MANUAL;
        BugReport br = getDummyBugReport();
        char state = 'm';
        try {
            FileInputStream fis = new FileInputStream(fileName);
            LineReader lr = new LineReader(fis);

            String line = null;
            Bug bug = null;
            Section sec = null;
            String secStop = null;
            while (null != (line = lr.readLine())) {
                if (state == 'm') {
                    // idle/monkey mode: searching for something useful
                    if (line.startsWith("// NOT RESPONDING")) {
                        // Congratulation... you found an ANR ;-)
                        bug = new Bug(Bug.PRIO_ANR_MONKEY, 0, line);
                        bug.addLine("<pre>");
                        bug.addLine(line);
                        br.addBug(bug);
                        state = 'a';
                        continue;
                    }
                } else if (state == 'a') {
                    // Collect ANR summary
                    if (line.length() == 0) {
                        bug.addLine("</pre>");
                        bug = null;
                        state = 's';
                    } else {
                        bug.addLine(line);
                    }
                } else if (state == 's') {
                    // Section search mode
                    if (line.length() == 0) {
                        continue;
                    } else if (line.startsWith("//") || line.startsWith("    //") || line.startsWith(":")) {
                        state = 'm';
                    } else if (line.startsWith("procrank:")) {
                        sec = new Section(br, Section.PROCRANK);
                        secStop = "// procrank status was";
                    } else if (line.startsWith("anr traces:")) {
                        sec = new Section(br, Section.VM_TRACES_AT_LAST_ANR);
                        secStop = "// anr traces status was";
                    } else if (line.startsWith("meminfo:")) {
                        sec = new Section(br, Section.DUMP_OF_SERVICE_MEMINFO);
                        secStop = "// meminfo status was";
                    } else {
                        // NOP ?
                    }
                    if (sec != null) {
                        br.printOut(2, "[MonkeyLog] Found section: " + sec.getName());
                        br.addSection(sec);
                        br.addHeaderLine(sec.getName() + ": (extracted from) " + fileName);
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
            onPrint(1, TYPE_ERR, "Error reading file '" + fileName + "': " + e);
        }
    }

    private boolean readFile(Section sl, String fileName, int limit) {
        try {
            // Check file size
            File f = new File(fileName);
            long size = f.length();
            FileInputStream fis = new FileInputStream(f);
            if (size > limit) {
                // Need to seek to "end - limit"
                Util.skip(fis, size - limit);
                Util.skipToEol(fis);
                onPrint(1, TYPE_ERR, "File '" + fileName + "' is too long, loading only last " + (limit / MB) + " megabyte(s)...");
            }
            LineReader br = new LineReader(fis);

            String line = null;
            while (null != (line = br.readLine())) {
                sl.addLine(line);
            }
            br.close();
            fis.close();
            return true;
        } catch (IOException e) {
            onPrint(1, TYPE_ERR, "Error reading file '" + fileName + "' (it will be ignored): " + e);
            return false;
        }
    }

    private BugReport getDummyBugReport() {
        if (mDummy == null) {
            mDummy = (BugReport)createReportInstance("", MODE_MANUAL);
            mDummy.addHeaderLine("This was not generated from a full bugreport, but from individual files:");
        }
        return mDummy;
    }

    protected Report createReportInstance(String fileName, int mode) {
        Report ret = null;
        if (mode == MODE_TRACEVIEW) {
            ret = new TraceReport(fileName);
        } else {
            ret = new BugReport(fileName);
        }
        ret.setOutputListener(this);
        return ret;
    }

    private void usage() {
        System.err.println("Usage: chkbugreport bugreportfile");
        System.err.println("  or");
        System.err.println("Usage: chkbugreport -t traceviewfile");
        System.err.println("  or");
        System.err.println("Usage: chkbugreport [sections] dummybugreportfile");
        System.err.println("Where dummybugreportfile does not exists, but will be used to generate");
        System.err.println("a folder name and sections must contain at least one of the following:");
        System.err.println("  -ds:file    - Use file as dumsys output (almost same as -pb)");
        System.err.println("  -el:file    - Use file as event log");
        System.err.println("  -ft:file    - Use file as ftrace dump");
        System.err.println("  -ps:file    - Use file as \"processes\" section");
        System.err.println("  -pt:file    - Use file as \"processes and threads\" section");
        System.err.println("  -ml:file    - Use file as main log");
        System.err.println("  -mo:file    - Parse monkey output and extract stacktraces from it");
        System.err.println("  -pb:file    - Load partial bugreport (eg. output of dumpsys)");
        System.err.println("  -sl:file    - Use file as system log");
        System.err.println("  -sa:file    - Use file as \"vm traces at last anr\" section");
        System.err.println("  -sn:file    - Use file as \"vm traces just now\" section");
        System.err.println("Extra options:");
        System.err.println("  --browser   - Launch the browser when done");
        System.err.println("  --gui       - Launch the Graphical User Interface if no file name is provided");
        System.err.println("  --frames    - Use HTML frames when processing bugreport (default)");
        System.err.println("  --no-frames - Don't use HTML frames when processing bugreport");
        System.err.println("  --silent    - Supress all output except fatal errors");
        System.err.println("  --limit     - Limit the input file size (default)");
        System.err.println("                If using the -sl option for example, the log file will");
        System.err.println("                be truncated if it's too long (since the generated html");
        System.err.println("                would be even bigger). This option (and --no-limit as well)");
        System.err.println("                must precede the other options in order to have effect.");
        System.err.println("  --no-limit  - Don't limit the input file size");
    }

    @Override
    public void onPrint(int level, int type, String msg) {
        if (mGui != null) {
            mGui.onPrint(level, type, msg);
        }
        if (!mSilent) {
            if (type == Report.OutputListener.TYPE_OUT) {
                System.out.println(msg);
            } else {
                System.err.println(msg);
            }
        }
    }

}
