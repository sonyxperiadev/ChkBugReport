package com.sonyericsson.chkbugreport.plugins.stacktrace;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.ps.PSRecord;

import java.util.Iterator;
import java.util.Vector;

public class Process implements Iterable<StackTrace> {

    private int mPid;
    private String mName;
    private Vector<StackTrace> mStacks = new Vector<StackTrace>();
    private Vector<PSRecord> mUnknownThreads= new Vector<PSRecord>();
    private Processes mGroup;
    private String mDate;
    private String mTime;
    private Chapter mChapter;

    public Process(BugReportModule br, Processes processes, int pid, String date, String time) {
        mGroup = processes;
        mPid = pid;
        mDate = date;
        mTime = time;
        mChapter = new Chapter(br, "");
    }

    public Processes getGroup() {
        return mGroup;
    }

    public String getDate() {
        return mDate;
    }

    public String getTime() {
        return mTime;
    }

    public void addBusyThreadStack(StackTrace stack) {
        mGroup.addBusyThreadStack(stack);
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