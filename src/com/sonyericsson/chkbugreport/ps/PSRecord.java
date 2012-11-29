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
package com.sonyericsson.chkbugreport.ps;

import java.util.Iterator;
import java.util.Vector;

public class PSRecord implements Iterable<PSRecord> {

    /** The policy cannot be recognized (parsing failed) */
    public static final int PCY_OTHER = -2;
    /** The policy is unknown (no data) */
    public static final int PCY_UNKNOWN = -1;
    /** Normal/foreground process */
    public static final int PCY_NORMAL = 0;
    /** FIFO/important process */
    public static final int PCY_FIFO = 1;
    /** Background process */
    public static final int PCY_BATCH = 3;

    /** Special value when the real value is unknown */
    public static final int NICE_UNKNOWN = -100;

    int mPid;
    int mPPid;
    int mPcy;
    int mNice;
    String mName;
    PSRecord mParent;
    Vector<PSRecord> mChildren = new Vector<PSRecord>();

    public PSRecord(int pid, int ppid, int nice, int pcy, String name) {
        mPid = pid;
        mPPid = ppid;
        mNice = nice;
        mPcy = pcy;
        mName = name;
    }

    public int getPid() {
        return mPid;
    }

    public int getParentPid() {
        return mPPid;
    }

    public int getNice() {
        return mNice;
    }

    public int getPolicy() {
        return mPcy;
    }

    public String getPolicyStr() {
        switch (mPcy) {
            case PCY_NORMAL:
                return "fg";
            case PCY_BATCH:
                return "bg";
            case PCY_FIFO:
                return "un";
            default:
                return "??";
        }
    }
    public String getName() {
        return mName;
    }

    @Override
    public Iterator<PSRecord> iterator() {
        return mChildren.iterator();
    }

    public void getChildren(Vector<PSRecord> ret) {
        for (PSRecord child : mChildren) {
            ret.add(child);
        }
    }

}
