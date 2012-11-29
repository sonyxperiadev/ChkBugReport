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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

/**
 * A named collection of text lines.
 */
public class Lines {

    private String mName;

    private Vector<String> mLines = new Vector<String>();

    public Lines(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public void clear() {
        mLines.clear();
    }

    public void addLine(String line) {
        mLines.add(line);
    }

    public void addLine(String line, int idx) {
        mLines.add(idx, line);
    }

    public void removeLine(int idx) {
        mLines.remove(idx);
    }

    public int getLineCount() {
        return mLines.size();
    }

    public String getLine(int idx) {
        return mLines.get(idx);
    }

    public void addLines(Lines lines) {
        int cnt = lines.getLineCount();
        for (int i = 0; i < cnt; i++) {
            addLine(lines.getLine(i));
        }
    }

    public boolean writeTo(String fn) {
        try {
            FileOutputStream fos = new FileOutputStream(fn);
            PrintStream ps = new PrintStream(fos);
            writeTo(ps);
            ps.close();
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void writeTo(PrintStream ps) {
        for (String line : mLines) {
            ps.println(line);
        }
    }

}
