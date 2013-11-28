/*
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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.util.HtmlUtil;

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
        if (mLevel <= SPLIT_LEVELS || ch.shouldBeStandalone()) {
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
            HtmlUtil.writeHTMLHeader(mOut, mFileName, "");
        }
    }

    @Override
    public void end() {
        if (mFileName != null) {
            HtmlUtil.writeHTMLFooter(mOut);
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
    public Chapter getChapter() {
        return mChapter;
    }

}
