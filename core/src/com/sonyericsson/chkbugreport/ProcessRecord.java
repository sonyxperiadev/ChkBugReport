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
package com.sonyericsson.chkbugreport;

import com.sonyericsson.chkbugreport.doc.Chapter;

public class ProcessRecord extends Chapter {

    private int mPid;
    private int mNamePrio;
    private String mProcName;
    private boolean mExport = false;

    public ProcessRecord(Context ctx, String name, int pid) {
        super(ctx, name);
        mPid = pid;
        setProcName(name);
    }

    private void setProcName(String name) {
        mProcName = name;
        setName(mProcName + " (" + mPid + ")");
    }

    public int getPid() {
        return mPid;
    }

    public boolean isExported() {
        return mExport;
    }

    public void suggestName(String name, int prio) {
        if (prio > mNamePrio) {
            setProcName(name);
            mNamePrio = prio;
        }
    }

    public String getProcName() {
        return mProcName;
    }

    public void setExport() {
        mExport = true;
    }

    public boolean shouldExport() {
        return mExport;
    }

}
