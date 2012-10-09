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

import com.sonyericsson.chkbugreport.doc.Bug;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Doc;
import com.sonyericsson.chkbugreport.doc.Link;
import com.sonyericsson.chkbugreport.doc.List;
import com.sonyericsson.chkbugreport.doc.ReportHeader;
import com.sonyericsson.chkbugreport.doc.SimpleText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public abstract class Module {

    public static final String VERSION = "0.4";
    public static final String VERSION_CODE = "168";

    /** Contains some global configuration which could affect the module/plugins behavior */
    private Context mContext;

    /** The list of installed plugins */
    private Vector<Plugin> mPlugins = new Vector<Plugin>();

    /** The resulting document */
    private Doc mDoc;
    /** The header chapter in the document */
    private ReportHeader mHeader;

    private Vector<Bug> mBugs = new Vector<Bug>();
    private HashMap<String, Section> mSectionMap = new HashMap<String, Section>();
    private Vector<Section> mSections = new Vector<Section>();
    private HashMap<String, Object> mMetaInfos = new HashMap<String, Object>();
    private boolean mSQLFailed = false;
    private Connection mSQLConnection;
    private int mNextChapterId = 1;
    private int mNextSectionId = 1;
    private OutputListener mOutListener;
    private HashSet<Plugin> mCrashedPlugins;
    private HashMap<String, Object> mInfos = new HashMap<String, Object>();

    public interface OutputListener {
        /** Constant used for log messages targeted to the standard output */
        public static final int TYPE_OUT = 0;
        /** Constant used for log messages targeted to the error output */
        public static final int TYPE_ERR = 1;

        /**
         * Called when a new log message should be printed.
         *
         * <p>The level should specify the detail level of this message, and not the priority.
         * Explanation:</p>
         *
         * <ul>
         * <li>level 1 means that this message can be shown only once per runtime.
         * In other words this shows which main step of the processing is being executed.</li>
         * <li>level 2 can be shown once per plugin and there can be only one of these
         * when executing a certain step of the plugin. In other words these are intended
         * to show which plugin is executing right now.</li>
         * <li>level 3 can be shown once per plugin run. These are intended to show which step
         * of the plugin is being executed.</li>
         * <li>level 4 can be shown several times per plugin run. For example if the plugin
         * processes log files, these can be parsing errors, since several lines in the log
         * can have wrong format.</li>
         * <li>level 5 is used as a generic low-prio message</li>
         *
         * @param level The detail level of the message
         * @param type The output stream of the message
         * @param msg The message body
         */
        public void onPrint(int level, int type, String msg);
    }

    public Module(Context context, String fileName) {
        mContext = context;
        mDoc = new Doc(this);
        mDoc.setFileName(fileName);
        mDoc.addChapter(mHeader = createHeader());

        // Load internal plugins
        loadPlugins();

        // Load external plugins
        try {
            File homeDir = new File(System.getProperty("user.home"));
            File pluginDir = new File(homeDir, ".chkbugreport-plugins");
            if (pluginDir.exists() && pluginDir.isDirectory()) {
                String files[] = pluginDir.list();
                for (String fn : files) {
                    if (fn.startsWith(".") || !fn.endsWith(".jar")) {
                        continue; // skip
                    }
                    File jar = new File(pluginDir, fn);
                    JarInputStream jis = new JarInputStream(new FileInputStream(jar));
                    Manifest mf = jis.getManifest();
                    if (mf != null) {
                        String pluginClassName = mf.getMainAttributes().getValue("ChkBugReport-Plugin");
                        URL urls[] = { jar.toURI().toURL() };
                        URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader());
                        Class<?> extClass = Class.forName(pluginClassName, true, cl);
                        ExternalPlugin ext = (ExternalPlugin) extClass.newInstance();

                        // Note: printOut will not work here, since a listener is not set yet
                        System.out.println("Loading plugins from: " + jar.getAbsolutePath());
                        ext.initExternalPlugin(this);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading external plugins");
            e.printStackTrace();
        }
    }

    protected abstract void loadPlugins();

    public Context getContext() {
        return mContext;
    }

    protected ReportHeader createHeader() {
        return new ReportHeader(this);
    }

    public void setFileName(String fileName) {
        mDoc.setFileName(fileName);
    }

    public final String getBaseDir() {
        return mDoc.getBaseDir();
    }

    public String getRelRawDir() {
        return mDoc.getRelRawDir();
    }

    public String getIndexHtmlFileName() {
        return mDoc.getIndexHtmlFileName();
    }

    public int allocChapterId() {
        return mNextChapterId++;
    }

    public int allocSectionId() {
        return mNextSectionId++;
    }

    /**
     * Prints a message on the standard output
     * @param level The detail level of the message
     * @param s The message body
     * @see OutputListener#onPrint(int, int, String)
     */
    public void printOut(int level, String s) {
        if (mOutListener != null) {
            mOutListener.onPrint(level, OutputListener.TYPE_OUT, s);
        }
    }

    /**
     * Prints a message on the error output
     * @param level The detail level of the message
     * @param s The message body
     * @see OutputListener#onPrint(int, int, String)
     */
    public void printErr(int level, String s) {
        if (mOutListener != null) {
            mOutListener.onPrint(level, OutputListener.TYPE_ERR, s);
        }
    }

    abstract protected void load(InputStream is) throws IOException;

    public void addPlugin(Plugin plugin) {
        mPlugins.add(plugin);
    }

    public Plugin getPlugin(String pluginName) {
        for (Plugin plugin : mPlugins) {
            String name = plugin.getClass().getSimpleName();
            if (name.equals(pluginName)) {
                return plugin;
            }
        }
        return null;
    }

    public void addHeaderLine(String line) {
        mHeader.addLine(line);
    }

    public String getHeaderLine(int i) {
        return mHeader.getLine(i);
    }

    public void addChapter(Chapter ch) {
        mDoc.addChapter(ch);
    }

    public void addExtraFile(Chapter extFile) {
        mDoc.addExtraFile(extFile);
    }

    protected Doc getDocument() {
        return mDoc;
    }

    public void addSection(Section section) {
        mSections.add(section);
        mSectionMap.put(section.getShortName(), section);
    }

    public Section findSection(String name) {
        return mSectionMap.get(name);
    }

    public void addMetaInfo(String name, Object obj) {
        mMetaInfos.put(name, obj);
    }

    public Object getMetaInfo(String name) {
        return mMetaInfos.get(name);
    }

    public final void generate() throws IOException {
        mDoc.begin();

        // This will do build some extra chapters and save some non-html files
        collectData();

        // Save the generated report
        mDoc.end();

        finish();

        printOut(1, "DONE!");
    }

    protected void collectData() throws IOException {
        // NOP
    }

    protected void finish() {
        // Call finish on each plugin
        // Let's not log this, since this is not used often
        for (Plugin p : mPlugins) {
            if (!mCrashedPlugins.contains(p)) {
                try {
                    p.finish(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    addHeaderLine("Plugin crashed while finishing data: " + p.getClass().getName());
                }
            }
        }
    }

    protected void runPlugins() {
        mCrashedPlugins = new HashSet<Plugin>();

        // First, sort the plugins based on prio
        Collections.sort(mPlugins, new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                return o1.getPrio() - o2.getPrio();
            }
        });
        // Then plugin should process the input data first
        printOut(1, "Plugins are loading data...");
        for (Plugin p : mPlugins) {
            printOut(2, "Running (load) plugin: " + p.getClass().getName() + "...");
            try {
                p.reset();
                p.load(this);
            } catch (Exception e) {
                e.printStackTrace();
                addHeaderLine("Plugin crashed while loading data: " + p.getClass().getName());
                mCrashedPlugins.add(p);
            }
        }
        // Finally, each plugin should save the generated data
        printOut(1, "Plugins are generating output...");
        for (Plugin p : mPlugins) {
            if (!mCrashedPlugins.contains(p)) {
                printOut(2, "Running (generate) plugin: " + p.getClass().getName() + "...");
                try {
                    p.generate(this);
                } catch (Exception e) {
                    e.printStackTrace();
                    addHeaderLine("Plugin crashed while generating data: " + p.getClass().getName());
                }
            }
        }
    }

    protected void copyRes(String resources[]) throws IOException {
        for (String res : resources) {
            copyRes(res, "data" + res);
        }
    }

    protected void copyRes(String fni, String fno) throws IOException {
        InputStream is = getClass().getResourceAsStream(fni);
        if (is == null) {
            printErr(2, "Cannot find resource: " + fni);
            return;
        }

        File f = new File(mDoc.getOutDir() + fno);
        f.getParentFile().mkdirs();
        FileOutputStream fo = new FileOutputStream(f);
        byte buff[] = new byte[1024];
        while (true) {
            int read = is.read(buff);
            if (read <= 0) break;
            fo.write(buff, 0, read);
        }
        fo.close();
        is.close();
    }

    protected void saveSections() throws IOException {
        Chapter ch = new Chapter(this, "Raw data");
        List list = new List();
        ch.add(list);

        for (Section s : mSections) {
            String fn = mDoc.getRelRawDir() + s.getFileName();
            list.add(new Link(mDoc.getRelRawDir() + s.getFileName(), s.getName()));
            FileOutputStream fos = new FileOutputStream(getBaseDir() + fn);
            PrintStream ps = new PrintStream(fos);
            int cnt = s.getLineCount();
            for (int i = 0; i < cnt; i++) {
                ps.println(s.getLine(i));
            }
            ps.close();
            fos.close();
        }

        addChapter(ch);
    }

    public void addBug(Bug bug) {
        mBugs.add(bug);
    }

    protected void collectBugs() {
        // Sort bugs by priority
        Collections.sort(mBugs, Bug.getComparator());

        // Create error report
        String chapterName = "Errors";
        int count = mBugs.size();
        if (count > 0) {
            chapterName += " (" + count + ")";
        }
        Chapter bugs = new Chapter(this, chapterName);
        if (count == 0) {
            bugs.add(new SimpleText("No errors were detected by ChkBugReport :-("));
        } else {
            for (Bug bug : mBugs) {
                Chapter ch = new Chapter(this, bug.getName());
                ch.add(bug);
                bugs.addChapter(ch);
            }
        }

        // Insert error report as first chapter
        mDoc.insertChapter(1, bugs); // pos#0 = Header
    }

    /**
     * Return a new connection to the SQL database.
     * This will return always the same instance, so if you close it,
     * you're doomed. If connection failed, null will be returned
     * (which can happen if the jdbc libraries are not found)
     * @return A connection to the database or null.
     */
    public Connection getSQLConnection() {
        if (mSQLConnection != null) return mSQLConnection;
        // Don't try again
        if (mSQLFailed) return null;
        try {
            Class.forName("org.sqlite.JDBC");
            String fnBase = "raw/report.db";
            String fn = mDoc.getOutDir() + fnBase;
            File f = new File(fn);
            f.delete(); // We must create a new database every time
            mSQLConnection = DriverManager.getConnection("jdbc:sqlite:" + fn);
            if (mSQLConnection != null) {
                mSQLConnection.setAutoCommit(false);
                addHeaderLine("Note: SQLite report database created as " + fnBase);
            }
        } catch (Throwable t) {
            printErr(2, "Cannot make DB connection: " + t);
            mSQLFailed = true;
        }
        return mSQLConnection;
    }

    public int getBugCount() {
        return mBugs.size();
    }

    public Bug getBug(int idx) {
        return mBugs.get(idx);
    }

    public void setOutputListener(OutputListener listener) {
        mOutListener = listener;
    }

    public Iterable<Section> getSections() {
        return mSections;
    }

    public void addInfo(String infoId, Object obj) {
        mInfos.put(infoId, obj);
    }

    public Object getInfo(String infoId) {
        return mInfos.get(infoId);
    }

}
