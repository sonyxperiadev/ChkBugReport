package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Util;

import java.io.FileNotFoundException;
import java.io.PrintStream;


public class Renderer {

    private static final int SPLIT_LEVELS = 2;

    private Doc mDoc;
    private int mLevel = 0;
    private String mFileName = null;
    private PrintStream mOut = null;
    private Renderer mParent;
    private GlobalState mState;

    class GlobalState {
        private int mNextFile = 1;
    }

    public Renderer(Doc doc) {
        mDoc = doc;
        mState = new GlobalState();
    }

    private Renderer(Renderer r) {
        mDoc = r.mDoc;
        mParent = r;
        mLevel = r.mLevel;
        mState = r.mState;
        if (mLevel <= SPLIT_LEVELS) {
            mFileName = String.format("f%05d.html", mState.mNextFile++);
        }
    }

    public Renderer addLevel() {
        return new Renderer(this);
    }

    public int getLevel() {
        return mLevel;
    }

    public void begin() throws FileNotFoundException {
        if (mFileName == null) {
            mOut = mParent.mOut;
        } else {
            mOut = new PrintStream(mDoc.getBaseDir() + mFileName);
            Util.writeHTMLHeader(mOut, mFileName, "");
            mOut.println("<div class=\"frames\">");
        }
    }

    public void end() {
        if (mFileName != null) {
            mOut.println("</div>");
            Util.writeHTMLFooter(mOut);
            mOut.close();
        }
    }

    public void print(String string) {
        mOut.print(string);
    }

    public void println(String string) {
        mOut.println(string);
    }

    public String getFileName() {
        return mFileName;
    }

}
