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
    private String mFallback;

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
        mFallback = name;
    }

    @Override
    public void render(Renderer r) throws IOException {
        ProcessRecord pr = mMod.getProcessRecord(mPid, false, false);
        int flags = mFlags;
        String name = mFallback;
        Anchor a = null;
        if (pr != null) {
            name = pr.getProcName();
            if (pr.isExported()) {
                a = pr.getAnchor();
            }
        }
        if (name == null) {
            flags = SHOW_PID;
        }
        if (a != null) {
            r.print("<a href=\"");
            r.print(a.getFileName());
            r.print("#");
            r.print(a.getName());
            r.print("\">");
        }

        if (flags == SHOW_PID) {
            r.print(Integer.toString(mPid));
        } else if (flags == SHOW_NAME) {
            r.print(name);
        } else {
            r.print(name);
            r.print("(");
            r.print(Integer.toString(mPid));
            r.print(")");
        }
        if (a != null) {
            r.print("</a>");
        }
    }

}
