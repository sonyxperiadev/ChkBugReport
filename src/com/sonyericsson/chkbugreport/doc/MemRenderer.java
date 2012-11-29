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

import com.sonyericsson.chkbugreport.Module;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;


public class MemRenderer implements Renderer {

    private PrintStream mOut = null;
    private ByteArrayOutputStream mData = new ByteArrayOutputStream();
    private Module mMod;

    public MemRenderer(Module mod) {
        mMod = mod;
    }

    @Override
    public MemRenderer addLevel(Chapter ch) {
        return null; // Not supported
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public void begin() throws FileNotFoundException {
        mOut = new PrintStream(mData);
    }

    @Override
    public void end() {
        mOut.close();
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
        return null; // Not supported
    }

    @Override
    public MemRenderer getParent() {
        return null; // Not supported
    }

    @Override
    public boolean isStandalone() {
        return true; // Not supported
    }

    @Override
    public Module getModule() {
        return mMod;
    }

    @Override
    public Chapter getChapter() {
        return null; // Not supported
    }

    public byte[] getData() {
        return mData.toByteArray();
    }

}
