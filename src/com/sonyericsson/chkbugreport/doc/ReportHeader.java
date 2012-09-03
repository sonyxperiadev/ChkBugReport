package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;

public class ReportHeader extends Chapter {

    PreText mPre = new PreText();

    public ReportHeader(Module mod) {
        super(mod, "Header");
        add(mPre);
        add(new Block());
        add(new Block()
                .add("Created with ChkBugReport v")
                .add(new Span().setId("chkbugreport-ver").add(new SimpleText(Module.VERSION)))
                .add(" (rel ")
                .add(new Span().setId("chkbugreport-rel").add(new SimpleText(Module.VERSION_CODE)))
                .add(")")
                );
        add(new Block()
                .add("For questions and suggestions feel free to contact me: ")
                .add(new Link("mailto:pal.szasz@sonymobile.com", "Pal Szasz (pal.szasz@sonymobile.com)")));
    }

    public void addLine(String line) {
        mPre.add(new SimpleText(line));
    }

}
