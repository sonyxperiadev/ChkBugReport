package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;

import java.util.Vector;

public class ReportHeader extends Chapter {

    private PreText mPre = new PreText();
    private Vector<String> mLines = new Vector<String>();

    public ReportHeader(Module mod) {
        super(mod, "Header");
        add(mPre);
        add(new Block());
        add(buildCreatedWith());
        add(buildContacts());
    }

    protected DocNode buildCreatedWith() {
        return new Block()
            .add("Created with ChkBugReport v")
            .add(new Span().setId("chkbugreport-ver").add(new SimpleText(Module.VERSION)))
            .add(" (rel ")
            .add(new Span().setId("chkbugreport-rel").add(new SimpleText(Module.VERSION_CODE)))
            .add(")");
    }

    protected DocNode buildContacts() {
        return new Block()
            .add("For questions and suggestions feel free to contact me: ")
            .add(new Link("mailto:pal.szasz@sonymobile.com", "Pal Szasz (pal.szasz@sonymobile.com)"));
    }

    public void addLine(String line) {
        mPre.add(new SimpleText(line + '\n'));
        mLines.add(line);
    }

    public String getLine(int idx) {
        return mLines.get(idx);
    }

}
