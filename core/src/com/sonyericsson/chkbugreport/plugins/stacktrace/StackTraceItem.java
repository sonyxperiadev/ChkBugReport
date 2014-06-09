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

/* package */ final class StackTraceItem {

    public static final int TYPE_JAVA = 0;
    public static final int TYPE_NATIVE = 1;

    public static final String STYLE_ERR = "stacktrace-err";
    public static final String STYLE_BUSY = "stacktrace-busy";

    /** The type of the stack traces: java or native */
    private int mType;
    /** Method/function name, if known (for native stack it might be unknown) */
    private String mMethod;
    /** Address offset from beginning of method, for native stack traces */
    private int mMethodOffset;
    /** The name of the file, if known */
    private String mFileName;
    /** The line number inside the file, if known */
    private int mLine;
    /** For native stack traces, the pc address */
    private long mPC; // long, because soon we could have 64bit addresses
    /** The css style to use for the item */
    private String mStyle = "";

    /**
     * Create a java stack trace item
     * @param method The method name
     * @param fileName The file name
     * @param line The line number
     */
    public StackTraceItem(String method, String fileName, int line) {
        mType = TYPE_JAVA;
        mMethod = (method == null) ? null : method.intern();
        mMethodOffset = -1; // unknown
        mFileName = (fileName == null) ? null : fileName.intern();
        mLine = line;
        mPC = -1; // unknown;
    }

    /**
     * Create a native stack trace item
     * @param method The method name
     * @param fileName The file name
     * @param line The line number
     */
    public StackTraceItem(long pc, String fileName, String method, int methodOffset) {
        mType = TYPE_NATIVE;
        mMethod = (method == null) ? null : method.intern();
        mMethodOffset = methodOffset;
        mFileName = (fileName == null) ? null : fileName.intern();
        mLine = -1; // unknown
        mPC = pc;
    }

    public int getType() {
        return mType;
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

    public int getMethodOffset() {
        return mMethodOffset;
    }

    public long getPC() {
        return mPC;
    }

    public String getFileName() {
        return mFileName;
    }

    public int getLine() {
        return mLine;
    }

}
