/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
 * Copyright (C) 2012-2013 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.Chapter;

import java.util.Vector;

/* package */ final class Processes extends Vector<Process> {

    private int mId;
    private String mName;
    private String mSectionName;
    private Vector<StackTrace> mBusy = new Vector<StackTrace>();
    private Chapter mCh;

    public Processes(Module report, int id, String name, String sectionName) {
        mId = id;
        mName = name;
        mSectionName = sectionName;
        mCh = new Chapter(report.getContext(), name);
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getSectionName() {
        return mSectionName;
    }

    public Chapter getChapter() {
        return mCh;
    }

    private static final long serialVersionUID = 1L;

    public void addBusyThreadStack(StackTrace stack) {
        if (!mBusy.contains(stack)) {
            mBusy.add(stack);
        }
    }

    public Vector<StackTrace> getBusyStackTraces() {
        return mBusy;
    }

    public Process findPid(int pid) {
        for (Process p : this) {
            if (p.getPid() == pid) {
                return p;
            }
        }
        return null;
    }

    public Vector<StackTrace> getAIDLCalls() {
        Vector<StackTrace> ret = new Vector<StackTrace>();
        for (Process proc : this) {
            for (StackTrace thread : proc) {
                if (thread.getAidlDependency() != null) {
                    ret.add(thread);
                }
            }
        }
        return ret;
    }

}