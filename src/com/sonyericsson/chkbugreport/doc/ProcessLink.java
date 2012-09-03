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

    public ProcessLink(BugReportModule mod, int pid) {
        this(mod, pid, SHOW_ALL);
    }

    public ProcessLink(BugReportModule mod, int pid, int flags) {
        mMod = mod;
        mPid = pid;
        mFlags = flags;
    }

    @Override
    public void render(Renderer r) throws IOException {
        ProcessRecord pr = mMod.getProcessRecord(mPid, false, false);
        if (pr == null) {
            r.println(Integer.toString(mPid));
        } else if (!pr.isExported()) {
            if (mFlags == SHOW_PID) {
                r.println(Integer.toString(mPid));
            } else {
                r.print(pr.getProcName());
            }
        } else {
            Anchor a = pr.getAnchor();
            r.print("<a href=\"");
            r.print(a.getFileName());
            r.print("#");
            r.print(a.getName());
            r.print("\">");
            if (mFlags == SHOW_PID) {
                r.println(Integer.toString(mPid));
            } else {
                r.print(pr.getProcName());
            }
            r.print("</a>");
        }
    }

}
