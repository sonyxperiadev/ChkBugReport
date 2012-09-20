package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.Util;

import java.io.FileNotFoundException;
import java.io.PrintStream;


public class FileRenderer implements Renderer {

    private static final int SPLIT_LEVELS = 2;

    private Doc mDoc;
    private int mLevel = -1;
    private String mFileName = null;
    private PrintStream mOut = null;
    private FileRenderer mParent;
    private GlobalState mState;
    private Chapter mChapter;

    class GlobalState {
        private int mNextFile = 1;
    }

    public FileRenderer(Doc doc) {
        mDoc = doc;
        mState = new GlobalState();
    }

    private FileRenderer(FileRenderer r, Chapter ch) {
        mDoc = r.mDoc;
        mParent = r;
        mLevel = r.mLevel + 1;
        mState = r.mState;
        mChapter = ch;
        if (mLevel <= SPLIT_LEVELS) {
            mFileName = String.format("f%05d.html", mState.mNextFile++);
        }
    }

    @Override
    public FileRenderer addLevel(Chapter ch) {
        return new FileRenderer(this, ch);
    }

    @Override
    public int getLevel() {
        return mLevel;
    }

    @Override
    public void begin() throws FileNotFoundException {
        if (mFileName == null) {
            mOut = mParent.mOut;
        } else {
            mOut = new PrintStream(mDoc.getBaseDir() + mFileName);
            Util.writeHTMLHeader(mOut, mFileName, "");
        }
    }

    @Override
    public void end() {
        if (mFileName != null) {
            Util.writeHTMLFooter(mOut);
            mOut.close();
        }
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
    public void print(long v) {
        mOut.print(v);
    }

    @Override
    public void print(char c) {
        mOut.print(c);
    }

    @Override
    public String getFileName() {
        return mFileName;
    }

    @Override
    public FileRenderer getParent() {
        return mParent;
    }

    @Override
    public boolean isStandalone() {
        return mFileName != null;
    }

    @Override
    public Module getModule() {
        return mDoc.getModule();
    }

    @Override
    public Chapter getChapter() {
        return mChapter;
    }

}
