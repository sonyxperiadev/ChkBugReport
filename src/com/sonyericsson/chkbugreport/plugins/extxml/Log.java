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
package com.sonyericsson.chkbugreport.plugins.extxml;

import java.util.regex.Pattern;

import com.sonyericsson.chkbugreport.Module;
import com.sonyericsson.chkbugreport.doc.Block;
import com.sonyericsson.chkbugreport.doc.Chapter;
import com.sonyericsson.chkbugreport.doc.DocNode;
import com.sonyericsson.chkbugreport.plugins.logs.LogLine;
import com.sonyericsson.chkbugreport.plugins.logs.LogLines;
import com.sonyericsson.chkbugreport.plugins.logs.LogToolbar;
import com.sonyericsson.chkbugreport.plugins.logs.MainLogPlugin;
import com.sonyericsson.chkbugreport.plugins.logs.event.EventLogPlugin;
import com.sonyericsson.chkbugreport.util.XMLNode;

public class Log {

    private Module mMod;
    private Chapter mCh;
    private XMLNode mCode;

    public Log(Module mod, Chapter ch, XMLNode code) {
        mMod = mod;
        mCh = ch;
        mCode = code;
    }

    public void exec() {
        // Collect the data
        LogLines result = new LogLines();
        for (XMLNode node : mCode) {
            String tag = node.getName();
            if (tag == null) {
                // NOP
            } else if ("filter".equals(tag)) {
                filterLog(node, result);
            } else {
                mMod.printErr(4, "Unknown tag in log: " + tag);
            }
        }

        // Sort the log lines
        result.sort();

        // Create the result
        new LogToolbar(mCh);
        DocNode logBlock = new Block(mCh).addStyle("log");
        for (LogLine ll : result) {
            logBlock.add(ll.copy());
        }
    }

    private void filterLog(XMLNode node, LogLines result) {
        Pattern pLine = null, pTag = null, pMsg = null;
        String log = node.getAttr("log");
        if (log == null) throw new RuntimeException("filter needs log attribute");
        String attr = node.getAttr("matchLine");
        if (attr != null) {
            pLine = Pattern.compile(attr);
        }
        attr = node.getAttr("matchTag");
        if (attr != null) {
            pTag = Pattern.compile(attr);
        }
        attr = node.getAttr("matchMsg");
        if (attr != null) {
            pMsg = Pattern.compile(attr);
        }
        if (pLine == null && pTag == null && pMsg == null) {
            throw new RuntimeException("You need to specify at least one of matchLine, matchTag or matchMsg!");
        }

        // Find the log
        LogLines logs = null;
        if (log.equals("event")) {
            logs = (LogLines) mMod.getInfo(EventLogPlugin.INFO_ID_LOG);
        } else if (log.equals("system")) {
            logs = (LogLines) mMod.getInfo(MainLogPlugin.INFO_ID_SYSTEMLOG);
        } else if (log.equals("main")) {
            logs = (LogLines) mMod.getInfo(MainLogPlugin.INFO_ID_MAINLOG);
        }
        if (logs == null || logs.size() == 0) {
            mMod.printErr(4, "Log '" + log + "' not found or empty!");
        }

        // Now try match each line
        for (LogLine ll : logs) {
            // First do the matching, and only after that do the extraction
            if (pLine != null) {
                if (!pLine.matcher(ll.line).find()) {
                    continue;
                }
            }
            if (pTag != null) {
                if (!pTag.matcher(ll.tag).find()) {
                    continue;
                }
            }
            if (pMsg != null) {
                if (!pMsg.matcher(ll.msg).find()) {
                    continue;
                }
            }
            result.add(ll);
        }
    }

}
