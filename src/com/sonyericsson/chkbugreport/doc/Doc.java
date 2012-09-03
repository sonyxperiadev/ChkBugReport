package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;

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

    public Doc(Module mod) {
        super(mod, null);
    }

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
        getModule().printOut(1, "Generating TOC ...");
        DocNode root = new Block().addStyle("toc-frames");
        root.add(new Block().add(new Link("../index.html", "[New window]").setTarget("_blank")));
        root.add(new HtmlNode("h1").add("Table of contents:"));
        DocNode toc = new Block().addStyle("toc-tree");
        root.add(toc);
        generateChapterInTOC(this, toc);
        Chapter ret = new Chapter(getModule(), "TOC");
        ret.add(root);
        return ret;
    }

    private void generateChapterInTOC(Chapter ch, DocNode out) {
        DocNode item = new DocNode();
        out.add(item);
        item.add(new Link(ch.getAnchor(), ch.getName()).setTarget("content"));
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
        Renderer r = new Renderer(this);
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
        getModule().printOut(1, "Writing frameset...");
        writeFrames(toc);
    }

    private void writeFrames(Chapter toc) throws FileNotFoundException {
        PrintStream ps = new PrintStream(mIndexHtml);
        Util.writeHTMLHeaderLite(ps, getFileName());
        String tocFn = toc.getAnchor().getFileName();
        String first = getChapter(0).getAnchor().getFileName();
        ps.println("<frameset cols=\"25%,75%\">");
        ps.println("  <frame name=\"toc\" src=\"data/" + tocFn + "\"/>");
        ps.println("  <frame name=\"content\" src=\"data/" + first + "\"/>");
        ps.println("</frameset>");
        Util.writeHTMLFooterLite(ps);
        ps.close();
    }

    public void addExtraFile(Chapter extFile) {
        mExtraFiles.add(extFile);
    }

}
