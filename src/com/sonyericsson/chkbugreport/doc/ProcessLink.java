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

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;

import java.io.IOException;

public class ProcessLink extends DocNode {

    public static final int SHOW_ALL = 0;
    public static final int SHOW_PID = 1;
    public static final int SHOW_NAME = 2;

    private BugReportModule mMod;
    private int mPid;
    private int mFlags = SHOW_ALL;
    private String mName;

    public ProcessLink(BugReportModule mod, int pid) {
        this(mod, pid, SHOW_ALL);
    }

    public ProcessLink(BugReportModule mod, int pid, int flags) {
        this(mod, pid, flags, null);
    }

    public ProcessLink(BugReportModule mod, int pid, String name) {
        this(mod, pid, SHOW_ALL, name);
    }

    public ProcessLink(BugReportModule mod, int pid, int flags, String name) {
        mMod = mod;
        mPid = pid;
        mFlags = flags;
        mName = name;
    }

    @Override
    public String getText() {
        // FIXME: this needs to be solved in different way
        return mName == null ? "" : mName;
    }

    @Override
    public void render(Renderer r) throws IOException {
        ProcessRecord pr = mMod.getProcessRecord(mPid, false, false);

        String name = mName;
        if (name == null) {
            switch (mFlags) {
                case SHOW_PID:
                    name = Integer.toString(mPid);
                    break;
                case SHOW_NAME:
                    name = (pr == null) ? ("(" + Integer.toString(mPid) + ")") : pr.getProcName();
                    break;
                case SHOW_ALL:
                    name = (pr == null) ? "" : pr.getProcName();
                    name += "(" + Integer.toString(mPid) + ")";
                    break;
            }
        }

        Anchor a = null;
        if (pr != null) {
            if (pr.isExported()) {
                a = pr.getAnchor();
            }
        }

        if (a != null) {
            r.print("<a href=\"");
            r.print(a.getHRef());
            r.print("\">");
        }
        r.print(name);
        if (a != null) {
            r.print("</a>");
        }
    }

}
