/*
 * Copyright (C) 2013 Sony Mobile Communications AB
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

import com.sonyericsson.chkbugreport.BugReportModule;
import com.sonyericsson.chkbugreport.ProcessRecord;
import com.sonyericsson.chkbugreport.util.XMLNode;

import java.util.regex.Pattern;

public class LogMatcher {

    private BugReportModule mMod;
    private Pattern mPLine = null;
    private Pattern mPTag = null;
    private Pattern mPMsg = null;
    private Pattern mPProc = null;
    private XMLNode mXml;

    public LogMatcher(BugReportModule mod, XMLNode node) {
        mMod = mod;
        mXml = node;
        String attr = node.getAttr("matchLine");
        if (attr != null) {
            mPLine = Pattern.compile(attr);
        }
        attr = node.getAttr("matchTag");
        if (attr != null) {
            mPTag = Pattern.compile(attr);
        }
        attr = node.getAttr("matchMsg");
        if (attr != null) {
            mPMsg = Pattern.compile(attr);
        }
        attr = node.getAttr("matchProc");
        if (attr != null) {
            mPProc = Pattern.compile(attr);
        }
        if (mPLine == null && mPTag == null && mPMsg == null && mPProc == null) {
            throw new RuntimeException("You need to specify at least one of matchLine, matchTag, matchMsg or matchProc!");
        }
    }

    public XMLNode getXML() {
        return mXml;
    }

    public boolean matches(LogLine ll) {
        // First do the matching, and only after that do the extraction
        if (mPLine != null) {
            if (!mPLine.matcher(ll.line).find()) {
                return false;
            }
        }
        if (mPTag != null) {
            if (!mPTag.matcher(ll.tag).find()) {
                return false;
            }
        }
        if (mPMsg != null) {
            if (!mPMsg.matcher(ll.msg).find()) {
                return false;
            }
        }
        if (mPProc != null) {
            // Note: the getPSRecord might be more precise, when present, since the process
            // record name (used below) is guessed (and sometimes incorrectly).
            // however the PS records are present only when processing a full bugreport
            ProcessRecord ps = mMod.getProcessRecord(ll.pid, false, false);
            if (ps == null || !mPProc.matcher(ps.getProcName()).find()) {
                return false;
            }
        }
        return true;
    }

}
