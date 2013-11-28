/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.settings.BoolSetting;
import com.sonyericsson.chkbugreport.settings.Settings;
import com.sonyericsson.chkbugreport.traceview.TraceModule;
import com.sonyericsson.chkbugreport.util.Util;
import com.sonyericsson.chkbugreport.webserver.ChkBugReportWebServer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

/**
 * The main entry point of the application.
 * This class is started when the application is started. It will process the arguments,
 * create a Context, create an appropriate subclass of Module and uses it to process the input
 * data and create the report.
 */
public class Main implements OutputListener {

    private static final int DEFAULT_LIMIT = 1 * Util.MB;

    private Module mMod;
    private Settings mSettings = new Settings();
    private BoolSetting mShowGui = new BoolSetting(false, mSettings, "showGui", "Launch the GUI automatically when no file name was specified.");
    private BoolSetting mOpenBrowser = new BoolSetting(false, mSettings, "openBrowser", "Launch the browser when output is generated.");
    private boolean mUseServer = false;
    private int mServerPort = 0;
    private Context mContext = new Context();
    private Gui mGui;

    // Profiling
    private boolean mProfile = false;

    private long mStartTime;

    private long mStartMem;

    private long mEndTime;

    private long mEndMem;;

    public Main() {
        // Catch output
        mContext.setOutputListener(this);
        // Change the doc icon on mac
        changeDocIcon();
    }

    /**
     * Returns the Context
     * @return the Context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Returns the Module being processed by this instance
     * @return the Module being processed by this instance
     */
    public Module getModule() {
        return mMod;
    }

    private void changeDocIcon() {
        // I know, this is ugly, but I wanted to do it with minimum impact
        try {
            Class<?> clsApp = Class.forName("com.apple.eawt.Application");
            Method metGetApp = clsApp.getMethod("getApplication");
            Method metSetIcon = clsApp.getMethod("setDockIconImage", Image.class);
            Object app = metGetApp.invoke(null);
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/app_icon.png"));
            metSetIcon.invoke(app, img);
        } catch (Exception e) {
            // This is just some extra decoration, so no problems if it fails
            // Don't even report the stacktrace
        }

    }

    /* package */ Settings getSettings() {
        return mSettings;
    }

    /**
     * Execute the application
     * @param args Command line arguments
     */
    public void run(final String[] args) {
        System.out.println("ChkBugReport " + Module.VERSION + " (rev " + Module.VERSION_CODE + ") (C) 2012 Sony Ericsson Mobile Communications AB");

        mSettings.load();

        int first = 0;
        if (args.length == 0) {
            // No arguments
            mMod = new BugReportModule(mContext);
            handleNoArgs();
            return;
        }

        // Peek tha first argument, and select the module
        if (args[0].equals("-t")) {
            first = 1;
            mMod = new TraceModule(mContext);
        } else if (args[0].equals("-b")) {
            first = 1;
            mMod = new BugReportModule(mContext);
        } else {
            mMod = new BugReportModule(mContext);
        }

        try {
            for (int argIdx = first; argIdx < args.length; argIdx++) {
                String arg = args[argIdx];
                if (arg.startsWith("-")) {
                    // option
                    String key = arg.substring(1);
                    String param = null;
                    int idx = key.indexOf(':');
                    if (idx > 0) {
                        param = key.substring(idx + 1);
                        key = key.substring(0, idx);
                    }
                    if ("sl".equals(key)) {
                        mMod.addFile(param, Section.SYSTEM_LOG, true);
                    } else if ("ml".equals(key)) {
                        mMod.addFile(param, Section.MAIN_LOG, true);
                    } else if ("el".equals(key)) {
                        mMod.addFile(param, Section.EVENT_LOG, true);
                    } else if ("ft".equals(key)) {
                        mMod.addFile(param, Section.FTRACE, true);
                    } else if ("pk".equals(key)) {
                        mMod.addFile(param, Section.PACKAGE_SETTINGS, false);
                    } else if ("ps".equals(key)) {
                        mMod.addFile(param, Section.PROCESSES, false);
                    } else if ("pt".equals(key)) {
                        mMod.addFile(param, Section.PROCESSES_AND_THREADS, false);
                    } else if ("sa".equals(key)) {
                        mMod.addFile(param, Section.VM_TRACES_AT_LAST_ANR, false);
                    } else if ("sn".equals(key)) {
                        mMod.addFile(param, Section.VM_TRACES_JUST_NOW, false);
                    } else if ("uh".equals(key)) {
                        mMod.addFile(param, Section.USAGE_HISTORY, false);
                    } else if ("pb".equals(key)) {
                        mMod.addFile(param, Section.PARTIAL_FILE_HEADER, false);
                    } else if ("sd".equals(key)) {
                        mMod.addFile(param, Section.META_SCAN_DIR, false);
                    } else if ("ds".equals(key)) {
                        mMod.addFile(param, Section.DUMPSYS, false);
                    } else if ("mo".equals(key)) {
                        mMod.addFile(param, Section.META_PARSE_MONKEY, false);
                    } else if ("o".equals(key)) {
                        mMod.setFileName(param);
                    } else if ("-silent".equals(key)) {
                        mContext.setSilent(true);
                    } else if ("-no-limit".equals(key)) {
                        mContext.setLimit(Integer.MAX_VALUE);
                    } else if ("-limit".equals(key)) {
                        int limit = DEFAULT_LIMIT;
                        if (param != null) {
                            limit = Integer.parseInt(param) * Util.MB;
                        }
                        mContext.setLimit(limit);
                    } else if ("-time-window".equals(key)) {
                        mContext.parseTimeWindow(param);
                    } else if ("-gmt".equals(key)) {
                        mContext.parseGmtOffset(param);
                    } else if ("-browser".equals(key)) {
                        mOpenBrowser.set(true);
                    } else if ("-server".equals(key)) {
                        mUseServer = true;
                    } else if ("-port".equals(key)) {
                        mServerPort = Integer.parseInt(param);
                    } else if ("-gui".equals(key)) {
                        mShowGui.set(true);
                    } else if ("-profile".equals(key)) {
                        mProfile = true;
                    } else {
                        onPrint(1, TYPE_ERR, "Unknown option '" + key + "'!");
                        usage();
                        System.exit(1);
                    }
                } else {
                    mMod.addFile(arg, null, false);
                }
            }
        } catch (IllegalParameterException e) {
            onPrint(1, TYPE_ERR, e.getMessage());
        }

        if (mMod.isEmpty()) {
            handleNoArgs();
            return;
        }

        if (mProfile) {
            System.gc();
            System.gc();
            mStartTime = System.currentTimeMillis();
            Runtime rt = Runtime.getRuntime();
            mStartMem = rt.totalMemory() - rt.freeMemory();
        }

        // Do the actual processing
        try {
            mMod.generate();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (mProfile) {
            mEndTime = System.currentTimeMillis();
            System.gc();
            System.gc();
            Runtime rt = Runtime.getRuntime();
            mEndMem = rt.totalMemory() - rt.freeMemory();
            onPrint(1, TYPE_OUT, String.format("Runtime: %.2fsec", (mEndTime - mStartTime) / 1000.0f));
            onPrint(1, TYPE_OUT, String.format("Used memory: %.3fMB (delta: %.3fMB)", mEndMem / 1024.0f / 1024.0f, (mEndMem - mStartMem) / 1024.0f / 1024.0f));
        }

        if (mUseServer) {
            ChkBugReportWebServer server = new ChkBugReportWebServer(mMod);
            server.setPort(mServerPort);
            server.start(mOpenBrowser.get());
        } else {
            // Launch browser if needed
            openBrowserIfNeeded();
        }
    }

    /* package */ void openBrowserIfNeeded() {
        String indexFile = mMod.getIndexHtmlFileName();
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

    private void handleNoArgs() {
        if (mShowGui.get()) {
            showGui();
            return;
        }
        usage();
        System.exit(1);
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

    /**
     * Called when the Module is selected and instantiated.
     * Subclass can fine tune the Module here (for example add extra plugins)
     * @param mod The module instance
     */
    protected void onModuleCreated(Module mod) {
        // NOP
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
        System.err.println("  -ml:file    - Use file as main log");
        System.err.println("  -mo:file    - Parse monkey output and extract stacktraces from it");
        System.err.println("  -pb:file    - Load partial bugreport (eg. output of dumpsys)");
        System.err.println("  -pk:file    - Load packages.xml file");
        System.err.println("  -ps:file    - Use file as \"processes\" section");
        System.err.println("  -pt:file    - Use file as \"processes and threads\" section");
        System.err.println("  -sa:file    - Use file as \"vm traces at last anr\" section");
        System.err.println("  -sl:file    - Use file as system log");
        System.err.println("  -sn:file    - Use file as \"vm traces just now\" section");
        System.err.println("  -sd:dir     - Load files from directory as partial bugreports");
        System.err.println("  -uh:file    - Load usage-history.xml file");
        System.err.println("Extra options:");
        System.err.println("  --browser   - Launch the browser when done");
        System.err.println("  --gmt:offs  - Set the GMT offset (needed to map UTC times to log times)");
        System.err.println("  --gui       - Launch the Graphical User Interface if no file name is provided");
        System.err.println("  --silent    - Supress all output except fatal errors");
        System.err.println("  --limit     - Limit the input file size");
        System.err.println("                If using the -sl option for example, the log file will");
        System.err.println("                be truncated if it's too long (since the generated html");
        System.err.println("                would be even bigger). This option (and --no-limit as well)");
        System.err.println("                must precede the other options in order to have effect.");
        System.err.println("  --no-limit  - Don't limit the input file size (default)");
        System.err.println("  -o:file     - Specify name to be used as output directory");
        System.err.println("  --server    - Starts the internal web server to serve the files");
        System.err.println("  --port:port - Specifies which port the internal web server should listen on");
        System.err.println("  --profile   - Measure the time and memory used");
    }

    @Override
    public void onPrint(int level, int type, String msg) {
        if (mGui != null) {
            mGui.onPrint(level, type, msg);
        }
        if (!mContext.isSilent()) {
            if (type == OutputListener.TYPE_OUT) {
                System.out.println(msg);
            } else {
                System.err.println(msg);
            }
        }
    }

    /**
     * Main entry point of the application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        new Main().run(args);
    }

}
