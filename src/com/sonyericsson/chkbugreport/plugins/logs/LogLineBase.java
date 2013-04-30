/*
 * Copyright (C) 2011 Sony Ericsson Mobile Communications AB
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
package com.sonyericsson.chkbugreport.plugins.logs;

import com.sonyericsson.chkbugreport.doc.Anchor;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.doc.Renderer;

import java.io.IOException;

public abstract class LogLineBase extends DocNode {

    public String line;
    public String css;
    public long ts;
    public boolean ok = false;

    private Anchor mAnchor;

    public LogLineBase(String line) {
        this.line = line;
        css = "log-debug";
    }

    public LogLineBase(LogLineBase orig) {
        line = orig.line;
        css = orig.css;
        ts = orig.ts;
        ok = orig.ok;
    }

    public void addStyle(String style) {
        css += " " + style;
    }

    @Override
    public final void render(Renderer r) throws IOException {
        renderChildren(r);
        renderThis(r);
    }

    protected void renderThis(Renderer r) throws IOException {
        // NOP
    }

    public Anchor getAnchor() {
        if (mAnchor == null) {
            add(mAnchor = new Anchor("l" + ts));
        }
        return mAnchor;
    }

    public LogLineProxy symlink() {
        return new LogLineProxy(this);
    }

    @Override
    public abstract LogLineBase copy();

    /* package */ static class LogLineProxy extends DocNode {

        private LogLineBase mLogLine;

        public LogLineProxy(LogLineBase logLine) {
            mLogLine = logLine;
        }

        @Override
        public void render(Renderer r) throws IOException {
            mLogLine.renderThis(r);
        }

    }

}

