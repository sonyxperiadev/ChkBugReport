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
package com.sonyericsson.chkbugreport.doc;

import com.sonyericsson.chkbugreport.Module;

import java.util.Vector;

public class ReportHeader extends Chapter {

    private PreText mPre = new PreText();
    private Vector<String> mLines = new Vector<String>();

    public ReportHeader(Module mod) {
        super(mod.getContext(), "Header");
        add(mPre);
        add(new Block());
        add(buildCreatedWith());
        add(buildContacts());
    }

    protected DocNode buildCreatedWith() {
        return new Block()
            .add("Created with ")
            .add(new Link(getContext().getHomePageUrl(), "ChkBugReport").setTarget("_blank"))
            .add(" v")
            .add(new Span().setId("chkbugreport-ver").add(new SimpleText(Module.VERSION)))
            .add(" (rel ")
            .add(new Span().setId("chkbugreport-rel").add(new SimpleText(Module.VERSION_CODE)))
            .add(")");
    }

    protected DocNode buildContacts() {
        return new Block()
            .add("For questions and suggestions feel free to contact me: ")
            .add(new Link("mailto:pal.szasz@sonymobile.com", "Pal Szasz (pal.szasz@sonymobile.com)"))
            .add(new Block().setId("new-version"));
    }

    public void addLine(String line) {
        mPre.add(new SimpleText(line + '\n'));
        mLines.add(line);
    }

    public String getLine(int idx) {
        return mLines.get(idx);
    }

}
