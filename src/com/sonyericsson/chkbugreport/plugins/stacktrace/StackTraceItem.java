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
package com.sonyericsson.chkbugreport.plugins.stacktrace;

/* package */ class StackTraceItem {

    public static final String STYLE_ERR = "stacktrace-err";
    public static final String STYLE_BUSY = "stacktrace-busy";

    private String mMethod;
    private String mFileName;
    private int mLine;
    private String mStyle = "";

    public StackTraceItem(String method, String fileName, int line) {
        mMethod = method;
        mFileName = fileName;
        mLine = line;
    }

    public String getStyle() {
        return mStyle;
    }

    public void setStyle(String style) {
        mStyle = style;
    }

    public String getMethod() {
        return mMethod;
    }

    public String getFileName() {
        return mFileName;
    }

    public int getLine() {
        return mLine;
    }

}
