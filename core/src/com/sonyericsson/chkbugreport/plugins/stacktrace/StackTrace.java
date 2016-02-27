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

import com.sonyericsson.chkbugreport.doc.Anchor;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Vector;

/* package */ final class StackTrace implements Iterable<StackTraceItem> {

    private String mName;
    private Vector<StackTraceItem> mStack = new Vector<StackTraceItem>();
    private int mTid;
    private int mPrio;
    private String mState;
    private WaitInfo waitInfo;
    private WeakReference<Process> mProc;
    private Vector<String> mProps = new Vector<String>();
    private int mPid;
    private StackTrace mAidlDep;
    private Anchor mAnchor;

    static class WaitInfo {
        private final int threadId;
        private final String lockId;
        private final String lockType;

        public WaitInfo(int threadId, String lockId, String lockType) {
            this.threadId = threadId;
            this.lockId = lockId;
            this.lockType = lockType;
        }

        public int getThreadId() {
            return threadId;
        }

        public String getLockId() {
            return lockId;
        }

        public String getLockType() {
            return lockType;
        }
    }

    public StackTrace(Process process, String name, int tid, int prio, String threadState) {
        mProc = new WeakReference<Process>(process);
        mName = name;
        mTid = tid;
        mPrio = prio;
        mState = threadState;
    }

    public void parseProperties(String s) {
        String kvs[] = s.split(" ");
        for (String kv : kvs) {
            if (kv.length() == 0) continue;
            String pair[] = kv.split("=");
            if (pair.length != 2) continue;
            setProperty(pair[0], pair[1]);

            // Handle some properties specially
            if (pair[0].equals("sysTid")) {
                try {
                    mPid = Integer.parseInt(pair[1]);
                    if (mTid < 0) {
                        mTid = mPid; // Use pid as fallback, since we use mTid as unique id
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setProperty(String key, String value) {
        mProps.add(key.intern());
        mProps.add(value.intern());
    }

    public String getProperty(String key) {
        int cnt = mProps.size();
        for (int i = 0; i < cnt; i += 2) {
            if (key.equals(mProps.get(i))) {
                return mProps.get(i + 1);
            }
        }
        return null;
    }

    public void setStyle(int from, int to, String style) {
        from = Math.max(0, from);
        to = Math.min(getCount(), to);
        for (int i = from; i < to; i++) {
            get(i).setStyle(style);
        }
    }

    public int findMethod(String methodName) {
        int cnt = getCount();
        for (int i = 0; i < cnt; i++) {
            if (methodName.equals(get(i).getMethod())) {
                return i;
            }
        }
        return -1;
    }

    public boolean isFirstJavaItem(int idx) {
        int cnt = Math.min(getCount(), idx);
        for (int i = 0; i < cnt; i++) {
            if (get(i).getType() == StackTraceItem.TYPE_JAVA) {
                return false;
            }
        }
        return true;
    }

    public Process getProcess() {
        return mProc.get();
    }

    public String getName() {
        return mName;
    }

    public int getTid() {
        return mTid;
    }

    public int getPid() {
        return mPid;
    }

    public int getPrio() {
        return mPrio;
    }

    public WaitInfo getWaitOn() {
        return waitInfo;
    }

    public void setWaitOn(WaitInfo waitInfo) {
        this.waitInfo = waitInfo;
    }

    public String getState() {
        return mState;
    }

    public void addStackTraceItem(StackTraceItem item) {
        mStack.add(item);
    }

    public int getCount() {
        return mStack.size();
    }

    public StackTraceItem get(int idx) {
        return mStack.get(idx);
    }

    public void setAidlDependency(StackTrace dstThread) {
        mAidlDep = dstThread;
    }

    public StackTrace getAidlDependency() {
        return mAidlDep;
    }

    public StackTrace getDependency() {
        if (waitInfo != null) {
            return getProcess().findTid(waitInfo.getThreadId());
        }
        return mAidlDep;
    }

    @Override
    public Iterator<StackTraceItem> iterator() {
        return mStack.iterator();
    }

    public Anchor getAnchor() {
        if (mAnchor == null) {
            mAnchor = new Anchor("tid_" + mTid);
        }
        return mAnchor;
    }

}

