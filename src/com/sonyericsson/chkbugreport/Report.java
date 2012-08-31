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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public abstract class Report {

    public static final String VERSION = "0.3";
    public static final String VERSION_CODE = "144";

    private String mFileName;
    private String mOutDir;
    private String mIndexHtml;
    private String mRawDir;
    private String mDataDir;

    private Lines mHeader = new Lines("Header");
    private Vector<Plugin> mPlugins = new Vector<Plugin>();
    private File mFo;
    private FileOutputStream mFos;
    private PrintStream mOut;
    private Chapter mChapters;
    private Vector<Bug> mBugs = new Vector<Bug>();
    private HashMap<String, Section> mSectionMap = new HashMap<String, Section>();
    private Vector<Section> mSections = new Vector<Section>();
    private HashMap<String, Object> mMetaInfos = new HashMap<String, Object>();
    private boolean mSQLFailed = false;
    private Connection mSQLConnection;
    private boolean mUseFrames = false;
    private int mNextChapterId = 1;
    private int mNextSectionId = 1;
    private OutputListener mOutListener;

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

    public Report(String fileName) {
        setFileName(fileName);

        mChapters = new Chapter(this, null);
    }

    protected int allocChapterId() {
        return mNextChapterId++;
    }

    protected int allocSectionId() {
        return mNextSectionId++;
    }

    public void setUseFrames(boolean value) {
        mUseFrames = value;
    }

    public boolean useFrames() {
        return mUseFrames;
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

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
        if (fileName.endsWith(".gz")) {
            fileName = fileName.substring(0, fileName.length() - 3);
        } else if (fileName.endsWith(".zip")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        if (fileName.endsWith(".txt")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        mOutDir = fileName + "_out/";
        mIndexHtml = mOutDir + "index.html";
        mRawDir = mOutDir + "raw/";
        mDataDir = mOutDir + "data/";
    }

    public String getIndexHtmlFileName() {
        return mIndexHtml;
    }

    public String getOutDir() {
        return mOutDir;
    }

    public String getDataDir() {
        return mDataDir;
    }

    /**
     * Return the directory where the html files will be saved.
     * If frames are used, all chapters are saved in the "data" folder.
     * If frames are not used, one huge index.html file is saved in the out folder
     * @return the directory where the html files will be saved.
     */
    public String getBaseDir() {
        return mUseFrames ? mDataDir : mOutDir;
    }

    /**
     * Return the relative path to the data directory.
     * If frames are used, this will be an empty string, since chapters are
     * also saved in the data dir.
     * @return the relative path to the data directory.
     */
    public String getRelDataDir() {
        return mUseFrames ? "" : "data/";
    }

    /**
     * Return the relative path to the raw directory.
     * @return the relative path to the raw directory.
     */
    public String getRelRawDir() {
        return mUseFrames ? "../raw/" : "raw/";
    }

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

    public String getHeaderLine(int idx) {
        return mHeader.getLine(idx);
    }

    public void addChapter(Chapter ch) {
        mChapters.addChapter(ch);
    }

    public void removeChapter(Chapter ch) {
        mChapters.removeChapter(ch);
    }

    protected Chapter getChapters() {
        return mChapters;
    }

    public void addSection(Section section) {
        mSections.add(section);
        mSectionMap.put(section.getShortName(), section);
    }

    public Section findSection(String name) {
        return mSectionMap.get(name);
    }

    public String createLinkTo(Chapter topChapter, String anchor) {
        if (mUseFrames) {
            Chapter top = topChapter.getTopLevelChapter();
            if (top != null) {
                topChapter = top;
            }
            return String.format("ch001_%03d.html#%s", topChapter.getId() , anchor);
        } else {
            return "#" + anchor;
        }
    }

    public void addMetaInfo(String name, Object obj) {
        mMetaInfos.put(name, obj);
    }

    public Object getMetaInfo(String name) {
        return mMetaInfos.get(name);
    }

    public void generate() throws IOException {
        // Create the destination file and file structure
        new File(mOutDir).mkdirs();
        new File(mRawDir).mkdirs();
        new File(mDataDir).mkdirs();
        openFile(mIndexHtml);
    }

    protected void openFile(String fn) throws IOException {
        mFo = new File(fn);
        mFos = new FileOutputStream(mFo);
        mOut = new PrintStream(mFos);
    }

    protected void closeFile() throws IOException {
        mOut.close();
        mFos.close();
    }

    protected void runPlugins() {
        HashSet<Plugin> crashed = new HashSet<Plugin>();

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
                p.load(this);
            } catch (Exception e) {
                e.printStackTrace();
                addHeaderLine("Plugin crashed while loading data: " + p.getClass().getName());
                crashed.add(p);
            }
        }
        // Finally, each plugin should save the generated data
        printOut(1, "Plugins are generating output...");
        for (Plugin p : mPlugins) {
            if (!crashed.contains(p)) {
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

    protected void writeChapters() throws IOException {
        writeChapter(mChapters, null);
    }

    private void writeChapter(Chapter ch, Chapter parent) throws IOException {
        String name = ch.getName();
        int level = ch.getLevel();
        if (level == 1 && mUseFrames) {
            openFile(mDataDir + ch.getAnchor() + ".html");
            writeHeader();
        }
        if (name != null) {
            printOut(2, "Writing chapter: " + ch.getFullName() + "...");
            mOut.println("<a name=\"" + ch.getAnchor() + "\"></a>");
            mOut.println("<h" + level + ">");
            mOut.println(ch.getName());
            if (level > 1 && parent != null) {
                mOut.println("<a class=\"ch-up\" title=\"" + parent.getFullName() + "\" href=\"#" + parent.getAnchor() + "\">(up)</a>");
            }
            mOut.println("</h" + level + ">");
            int cnt = ch.getLineCount();
            for (int i = 0; i < cnt; i++) {
                mOut.println(ch.getLine(i));
            }
        }
        int children = ch.getChildCount();
        for (int i = 0; i < children; i++) {
            writeChapter(ch.getChild(i), ch);
        }
        if (level == 1 && mUseFrames) {
            writeFooter();
            closeFile();
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

        File f = new File(mOutDir + fno);
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

    protected void writeTOC() {
        String css = mUseFrames ? "toc-frames" : "toc";
        mOut.println("<div class=\"" + css + "\">");
        if (mUseFrames) {
            mOut.println("<div><a href=\"../index.html\" target=\"_blank\">[New window]</a></div>");
        }
        mOut.println("<h1>Table of contents:</h1>");
        mOut.println("<div class=\"toc-tree\">");
        writeChapterInTOC(mChapters);
        mOut.println("</div>");
        mOut.println("</div>");
    }

    private void writeChapterInTOC(Chapter ch) {
        String name = ch.getName();
        if (name != null) {
            String link = "#" + ch.getAnchor();
            if (mUseFrames) {
                Chapter top = ch.getTopLevelChapter();
                if (top != null) {
                    link = top.getAnchor() + ".html" + link;
                }
            }
            mOut.println(getLinkToChapter(ch) + ch.getName() + "</a>");
        }
        int cnt = ch.getChildCount();
        if (cnt > 0) {
            mOut.println("<ul>");
            for (int i = 0; i < cnt; i++) {
                Chapter child = ch.getChild(i);
                mOut.println("<li>");
                writeChapterInTOC(child);
                mOut.println("</li>");
            }
            mOut.println("</ul>");
        }
    }

    public String getRefToChapter(Chapter ch) {
        String link = "#" + ch.getAnchor();
        if (mUseFrames) {
            Chapter top = ch.getTopLevelChapter();
            if (top != null) {
                link = top.getAnchor() + ".html" + link;
            }
        }
        return link;
    }

    public String getLinkToChapter(Chapter ch) {
        String link = getRefToChapter(ch);
        String target = mUseFrames ? " target=\"content\"" : "";
        return "<a href=\"" + link + "\"" + target + ">";
    }

    protected void saveSections() throws IOException {
        Chapter ch = new Chapter(this, "Raw data");
        ch.addLine("<ul>");

        for (Section s : mSections) {
            String fn = mRawDir + s.getFileName();
            ch.addLine("<li><a href=\"" + getRelRawDir() + s.getFileName() + "\">" + s.getName() + "</a></li>");
            FileOutputStream fos = new FileOutputStream(fn);
            PrintStream ps = new PrintStream(fos);
            int cnt = s.getLineCount();
            for (int i = 0; i < cnt; i++) {
                ps.println(s.getLine(i));
            }
            ps.close();
            fos.close();
        }

        ch.addLine("</ul>");
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
            bugs.addLine("<p>No errors were detected by ChkBugReport :-(</p>");
        } else {
            for (Bug bug : mBugs) {
                Chapter ch = new Chapter(this, bug.getName());
                ch.addLines(bug);
                bugs.addChapter(ch);
            }
        }

        // Insert error report as first chapter
        mChapters.insertChapter(0, bugs);
    }

    protected void writeHeader() {
        String relData = "";
        if (!mUseFrames) {
            relData = "data/";
        }
        Util.writeHTMLHeader(mOut, mFileName, relData);
        mOut.println("<div class=\"" + (mUseFrames ? "frames" : "noframes") + "\">");
    }

    protected void writeHeaderLite() {
        Util.writeHTMLHeaderLite(mOut, mFileName);
    }

    protected void writeFooterLite() {
        Util.writeHTMLFooterLite(mOut);
    }

    protected void writeLine(String s) {
        mOut.println(s);
    }

    protected void writeHeaderChapter() {
        // Also generate the "Header" chapter
        Chapter ch = new Chapter(this, "Header");
        ch.addLine("<pre>");
        ch.addLines(mHeader);
        ch.addLine("</pre>");
        writeCreatedByHeader(ch);
        mChapters.insertChapter(0, ch);
    }

    protected void writeCreatedByHeader(Chapter ch) {
        ch.addLine("<div>");
        ch.addLine(
                "<div>Created with ChkBugReport "+
                "v<span id=\"chkbugreport-ver\">" + VERSION + "</span> " +
                "(rel <span id=\"chkbugreport-rel\">" + VERSION_CODE + "</span>)</div>");
        ch.addLine("<div>For questions and suggestions feel free to contact me: <a href=\"mailto:pal.szasz@sonyericsson.com\">Pal Szasz (pal.szasz@sonyericsson.com)</a></div>");
    }

    protected void writeFooter() throws IOException {
        mOut.println("</div>");
        Util.writeHTMLFooter(mOut);
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
            String fn = mOutDir + fnBase;
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

}
