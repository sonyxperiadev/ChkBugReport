/*
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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Context;
import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

public class Doc extends Chapter {

    private String mFileName;
    private String mOutDir;
    private String mIndexHtml;
    private String mRawDir;
    private String mDataDir;

    private Vector<Chapter> mExtraFiles = new Vector<Chapter>();

    public Doc(Context context) {
        super(context);
    }

    @Override
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
     * @return the directory where the html files will be saved.
     */
    public String getBaseDir() {
        return mDataDir;
    }

    /**
     * Return the relative path to the raw directory.
     * @return the relative path to the raw directory.
     */
    public String getRelRawDir() {
        return "../raw/";
    }

    protected Chapter generateTOC() {
        getContext().printOut(1, "Generating TOC ...");
        DocNode root = new Block().addStyle("toc");
        Link newWindow = new Link("../index.html", null);
        newWindow.add(new Img("ic_new_window.png"));
        newWindow.setTarget("_blank");
        newWindow.setTitle("[New Window]");
        root.add(new Block().addStyle("btn-new-window").add(newWindow));
        DocNode toc = new Block().addStyle("toc-tree");
        root.add(toc);
        generateChapterInTOC(this, toc);
        Chapter ret = new Chapter(getContext(), "Table of contents");
        ret.removePopout();
        ret.add(root);
        return ret;
    }

    private void generateChapterInTOC(Chapter ch, DocNode out) {
        DocNode item = new DocNode();
        out.add(item);
        Link link = new Link(ch.getAnchor(), null);
        if (ch.getIcon() != null) {
            link.add(ch.getIcon());
        }
        link.add(ch.getName());
        link.setTarget("content");
        if (ch instanceof WebOnlyChapter) {
            new Span(item).addStyle("ws").add(link);
        } else {
            item.add(link);
        }
        int cnt = ch.getChapterCount();
        if (cnt > 0) {
            List list = new List();
            item.add(list);
            for (int i = 0; i < cnt; i++) {
                generateChapterInTOC(ch.getChapter(i), list);
            }
        }
    }

    public void begin() throws IOException {
        // Create the destination file and file structure
        new File(mOutDir).mkdirs();
        new File(mRawDir).mkdirs();
        new File(mDataDir).mkdirs();
    }

    public void end() throws IOException {
        // Cleanup: remove empty chapters
        cleanup();

        Renderer r = new FileRenderer(this);
        Chapter toc = generateTOC();

        toc.prepare(r);
        prepare(r);
        for (Chapter ext : mExtraFiles) {
            ext.prepare(r);
        }

        toc.render(r);
        render(r);
        for (Chapter ext : mExtraFiles) {
            ext.render(r);
        }

        // In the still opened index html we just create the frameset
        getContext().printOut(1, "Writing frameset...");
        writeFrames(toc);
    }

    private void writeFrames(Chapter toc) throws FileNotFoundException {
        PrintStream ps = new PrintStream(mIndexHtml);
        HtmlUtil.writeHTMLHeaderLite(ps, getFileName());
        String tocFn = toc.getFileName();
        String first = getChapter(0).getFileName();
        ps.println("<frameset cols=\"25%,75%\">");
        ps.println("  <frame name=\"toc\" src=\"data/" + tocFn + "\"/>");
        ps.println("  <frame name=\"content\" src=\"data/" + first + "\"/>");
        ps.println("</frameset>");
        HtmlUtil.writeHTMLFooterLite(ps);
        ps.close();
    }

    public void addExtraFile(Chapter extFile) {
        mExtraFiles.add(extFile);
    }

}
