package com.sonyericsson.chkbugreport.webserver.engine;

import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.Renderer;
import com.sonyericsson.chkbugreport.util.HtmlUtil;

import java.io.FileNotFoundException;

public class HTTPRenderer implements Renderer {

    private HTTPResponse mOut;
    private HTTPRenderer mParent;
    private int mLevel;
    private Chapter mCh;
    private String mFileName;

    public HTTPRenderer(HTTPResponse resp, HTTPRenderer parent, Chapter ch) {
        mCh = ch;
        mOut = resp;
        mLevel = (parent != null) ? parent.mLevel + 1 : 1;
        mParent = parent;
    }

    public HTTPRenderer(HTTPResponse resp, String fileName, Chapter ch) {
        mCh = ch;
        mOut = resp;
        mLevel = 1;
        mParent = null;
        mFileName = fileName;
    }

    @Override
    public Renderer addLevel(Chapter ch) {
        return new HTTPRenderer(mOut, this, ch);
    }

    @Override
    public int getLevel() {
        return mLevel;
    }

    @Override
    public void begin() throws FileNotFoundException {
        HtmlUtil.writeHTMLHeader(mOut.getPrintStream(), "", "/data/");
    }

    @Override
    public void end() {
        HtmlUtil.writeHTMLFooter(mOut.getPrintStream());
    }

    @Override
    public void print(String string) {
        mOut.print(string);
    }

    @Override
    public void println(String string) {
        mOut.println(string);
    }

    @Override
    public void print(char c) {
        mOut.print(c);
    }

    @Override
    public void print(long v) {
        mOut.print(v);
    }

    @Override
    public String getFileName() {
        return mFileName;
    }

    @Override
    public Renderer getParent() {
        return mParent;
    }

    @Override
    public boolean isStandalone() {
        return mFileName != null;
    }

    @Override
    public Chapter getChapter() {
        return mCh;
    }

}
