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

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.ps.PSRecord;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Vector;

/* package */ final class Process implements Iterable<StackTrace> {

    private int mPid;
    private String mName;
    private Vector<StackTrace> mStacks = new Vector<StackTrace>();
    private Vector<PSRecord> mUnknownThreads= new Vector<PSRecord>();
    private WeakReference<Processes> mGroup;
    private String mDate;
    private String mTime;
    private Chapter mChapter;

    public Process(BugReportModule br, Processes processes, int pid, String date, String time) {
        mGroup = new WeakReference<Processes>(processes);
        mPid = pid;
        mDate = date;
        mTime = time;
        mChapter = new Chapter(br.getContext(), "");
    }

    public Processes getGroup() {
        return mGroup.get();
    }

    public String getDate() {
        return mDate;
    }

    public String getTime() {
        return mTime;
    }

    public void addBusyThreadStack(StackTrace stack) {
        mGroup.get().addBusyThreadStack(stack);
    }

    public StackTrace findTid(int tid) {
        for (StackTrace stack : mStacks) {
            if (stack.getTid() == tid) {
                return stack;
            }
        }
        return null;
    }

    public StackTrace findPid(int pid) {
        for (StackTrace stack : mStacks) {
            if (stack.getPid() == pid) {
                return stack;
            }
        }
        return null;
    }

    public int indexOf(int tid) {
        for (int i = 0; i < mStacks.size(); i++) {
            if (mStacks.get(i).getTid() == tid) {
                return i;
            }
        }
        return -1;
    }

    public int getPid() {
        return mPid;
    }

    public void setName(String name) {
        mName = name;
        mChapter.setName(name + " (" + mPid + ")");
    }

    public String getName() {
        return mName;
    }

    public void addStackTrace(StackTrace stackTrace) {
        mStacks.add(stackTrace);
    }

    public int getCount() {
        return mStacks.size();
    }

    public StackTrace get(int idx) {
        return mStacks.get(idx);
    }

    public void addUnknownThread(PSRecord psr) {
        mUnknownThreads.add(psr);
    }

    public int getUnknownThreadCount() {
        return mUnknownThreads.size();
    }

    public PSRecord getUnknownThread(int idx) {
        return mUnknownThreads.get(idx);
    }

    @Override
    public Iterator<StackTrace> iterator() {
        return mStacks.iterator();
    }

    public Chapter getChapter() {
        return mChapter;
    }

}