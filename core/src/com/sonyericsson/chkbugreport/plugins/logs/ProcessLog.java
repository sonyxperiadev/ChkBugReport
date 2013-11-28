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
        super(mod.getContext(), String.format(owner.getId() + "log_%05d.html", pid));
        new LogToolbar(this);
        mDiv = new Block(this).addStyle("log");
        mPid = pid;
    }

    public int getPid() {
        return mPid;
    }

    public void add(LogLine ll) {
        // LogLines should never be added directly here
        // or else the anchors will be mixed up!
        Util.assertTrue(false);
    }

    public void add(LogLine.LogLineProxy ll) {
        mDiv.add(ll);
        mLines++;
    }

    public int getLineCount() {
        return mLines;
    }

}
