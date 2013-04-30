package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.doc.Renderer;

public class PidDecorator extends Decorator {

    private ProcessRecord mPr;

    public PidDecorator(int start, int end, BugReportModule br, int pid) {
        super(start, end);
        mPr = br.getProcessRecord(pid, true, true);
    }

    @Override
    public void render(Renderer r, boolean start) {
        if (start) {
            r.print("<a href=\"" + mPr.getAnchor().getHRef() + "\">");
        } else {
            r.print("</a>");
        }

    }

}
