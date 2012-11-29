package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.util.Util;

/* package */ class ProcessLog extends Chapter {

    private int mPid;
    private int mLines;
    private DocNode mDiv;

    public ProcessLog(LogPlugin owner, Module mod, int pid) {
        super(mod, String.format(owner.getId() + "log_%05d.html", pid));
        new LogToolbar(this);
        mDiv = new Block(this).addStyle("log");
        mPid = pid;
    }

    public int getPid() {
        return mPid;
    }

    public void add(LogLineBase ll) {
        // LogLines should never be added directly here
        // or else the anchors will be mixed up!
        Util.assertTrue(false);
    }

    public void add(LogLineBase.LogLineProxy ll) {
        mDiv.add(ll);
        mLines++;
    }

    public int getLineCount() {
        return mLines;
    }

}
