package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;

import java.io.IOException;


public class ProcessLink extends DocNode {

    private BugReportModule mMod;
    private int mPid;

    public ProcessLink(BugReportModule mod, int pid) {
        mMod = mod;
        mPid = pid;
    }

    @Override
    public void render(Renderer r) throws IOException {
        ProcessRecord pr = mMod.getProcessRecord(mPid, false, false);
        if (pr == null) {
            r.println(Integer.toString(mPid));
        } else {
            Anchor a = pr.getAnchor();
            r.print("<a href=\"");
            r.print(a.getFileName());
            r.print("#");
            r.print(a.getName());
            r.print("\">");
            r.print(pr.getProcName());
            r.print("</a>");
        }
    }

}
